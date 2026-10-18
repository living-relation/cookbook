# Research: Android file I/O (SAF, recents, autosave)

Design reference for `:core-files` in **Markdown Notes (Android)**. This document defines how the app **opens and saves user `.md` notes** via the Storage Access Framework (SAF), maintains a **recents list**, and **autosaves** edited content. It does not describe Compose UI or Activity wiring beyond the contracts the app layer must honor.

**Consumers:** `docs/IMPLEMENTATION.md` (merged plan), `:core-files` implementation, `:feature-editor` dirty/autosave integration, manual QA with `apps/markdown-notes-android/fixtures/notes/`.

---

## Goals

1. **User-owned storage** — Notes live where the user puts them (Downloads, Drive, SD card providers). The app does not require broad storage permissions.
2. **Resume quickly** — Recents let users reopen the last few notes without navigating the system picker every time.
3. **Safe editing** — Autosave reduces data loss; explicit Save / Save As remain available for user control and new files.
4. **Testable core** — File logic lives in `:core-files` (no Compose) so JVM/Android instrumented tests can cover I/O and persistence rules.

Non-goals for v1: syncing, cloud accounts, folder browsing (`OPEN_DOCUMENT_TREE`), binary attachments, conflict merge UI, encryption at rest beyond what the host provider offers.

---

## Platform constraints

| Setting | Value | Implication |
| --- | --- | --- |
| `minSdk` | 26 | SAF and persistable URI permissions are always available; no legacy `READ/WRITE_EXTERNAL_STORAGE` for user documents. |
| `targetSdk` | 35 | Scoped storage rules apply; user media paths are not the primary API. |
| Content | UTF-8 Markdown | Read/write as `String` via streams; charset **UTF-8**, no BOM on emit. |

The app **does not** request `MANAGE_EXTERNAL_STORAGE` or hold `WRITE_EXTERNAL_STORAGE` for note files. All user document access goes through **content `Uri`s** returned by SAF.

---

## Document identity

A note in memory is tied to optional storage metadata:

| Field | Meaning |
| --- | --- |
| `uri` | Content `Uri` from SAF (`content://…`). `null` = unsaved draft (never written or only in-memory). |
| `displayName` | Human filename (e.g. `welcome.md`) from `OpenableColumns.DISPLAY_NAME` or picker title. |
| `isDirty` | Editor content differs from last successful load or save. |
| `lastSavedAt` | Wall-clock millis after last successful persist to `uri` (or draft checkpoint). |
| `lastLoadedRevision` | Optional: content hash or length after load/save for cheap change detection. |

**Binding rule:** After **Open** or successful **Save As**, the editor holds one `uri` until the user picks another file or Save As replaces it. Autosave writes to the **current** `uri` when present and dirty.

---

## SAF: open existing file

### Intent contract

Use **`Intent.ACTION_OPEN_DOCUMENT`** (not `ACTION_GET_CONTENT` alone — open document is the long-lived document model Google recommends for editors).

| Extra | Value |
| --- | --- |
| `type` | `text/*` (or `"*/*"` with MIME filter below) |
| `Intent.EXTRA_MIME_TYPES` | `arrayOf("text/markdown", "text/plain", "text/x-markdown")` |
| `Intent.CATEGORY_OPENABLE` | Required |
| `Intent.FLAG_GRANT_READ_URI_PERMISSION` | Required on the intent used to start the picker |
| `Intent.FLAG_GRANT_WRITE_URI_PERMISSION` | Set when the app will edit and save in place |
| `Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION` | Required if recents should work after process death |

`:core-files` exposes a factory (no Activity):

```kotlin
fun openDocumentIntent(): Intent
```

The **`:app` module** registers an `ActivityResultContract` and passes the result `Uri?` into `:core-files`.

### Read path

1. Validate `uri != null` and scheme is `content`.
2. Optionally call `ContentResolver.takePersistableUriPermission(uri, read|write flags)` (see Permissions).
3. Open `contentResolver.openInputStream(uri)` (use `use` / try-with-resources).
4. Decode bytes as UTF-8 to `String`.
5. Resolve `displayName` via `ContentResolver.query(uri, DISPLAY_NAME)` or `DocumentFile.fromSingleUri`.

**Errors:** `SecurityException` (revoked access), `FileNotFoundException`, empty provider — surface as sealed `FileError` variants for the UI layer.

### MIME and extensions

Providers vary: many `.md` files are served as `text/plain`. The picker filter should be permissive; the app does not rely on MIME for parsing (extension is hint only).

---

## SAF: save and Save As

Two flows:

### A. Save in place (existing `uri`)

When the user already opened a document with write grant:

1. `contentResolver.openOutputStream(uri, "wt")` — `"wt"` truncates and writes UTF-8 text.
2. On success: clear `isDirty`, update `lastSavedAt`.

Use a **single writer** coroutine / mutex so autosave and manual Save do not interleave.

### B. Save As (new or copy)

Use **`Intent.ACTION_CREATE_DOCUMENT`**:

| Extra | Value |
| --- | --- |
| `type` | `text/markdown` (fallback `text/plain` if provider rejects) |
| `Intent.EXTRA_TITLE` | Suggested filename, e.g. `Untitled.md` or previous `displayName` |
| `FLAG_GRANT_WRITE_URI_PERMISSION` | Required |
| `FLAG_GRANT_PERSISTABLE_URI_PERMISSION` | Required for recents |

```kotlin
fun createDocumentIntent(suggestedTitle: String): Intent
```

After the user confirms location/name:

1. `takePersistableUriPermission` on the new `uri`.
2. Write full current markdown (from `:core-markdown` emit pipeline, or raw string if still source-mode).
3. Replace session `uri` + `displayName`; add/update recents entry.

**Unsaved draft:** Until the first successful Create Document, there is **no** SAF target. Autosave may only persist to an **internal draft** slot (see Autosave); it must not silently write to arbitrary paths.

---

## Permissions and persistence

Temporary grants from the picker expire when the process dies unless persisted.

After a successful open or create:

```kotlin
context.contentResolver.takePersistableUriPermission(
    uri,
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
)
```

Store the **URI string** in recents, not file paths. On cold start, recents entries are validated by:

1. Checking `contentResolver.persistedUriPermissions` for read (and write if we advertise editing), **or**
2. Probing `openInputStream(uri)` (catch `SecurityException` → mark entry stale).

**Revocation:** If the user revokes access in system Settings, remove that entry from recents on next failed open and prompt re-open via picker.

Do **not** call `releasePersistableUriPermission` on ordinary close; keep access for recents until the user removes the entry or revokes globally.

---

## Recents

### Purpose

Show the last **N** notes (recommend **N = 20**) with display name and relative open time. Selecting an entry loads content without SAF UI when permission still holds.

### Stored record

Persist in **DataStore Preferences** (or EncryptedSharedPreferences if we add secrets later — not required for v1):

```kotlin
data class RecentNote(
    val uriString: String,
    val displayName: String,
    val lastOpenedAtEpochMs: Long,
)
```

JSON array or proto-lite in one preference key is fine for 20 items.

### Update rules

| Event | Recents behavior |
| --- | --- |
| Open success | Move entry to top (or insert); bump `lastOpenedAt`. |
| Save As success | Same as open for the new `uri`. |
| Save in place | Optionally bump `lastOpenedAt` only on open, not every autosave (less churn). |
| Failed access | Remove entry or mark disabled with reason. |
| User clears recents | Wipe list; does not revoke URI permissions (optional separate “forget” could call `releasePersistableUriPermission`). |

### Display metadata

If `displayName` is missing at rest, refresh from provider on load. Do not store absolute filesystem paths.

### API sketch (`:core-files`)

```kotlin
interface RecentsRepository {
    fun observeRecents(): Flow<List<RecentNote>>
    suspend fun touch(uri: Uri, displayName: String)
    suspend fun remove(uri: Uri)
    suspend fun clear()
}
```

---

## Autosave

### Requirements (from product pipeline)

- **Debounced** writes while editing (avoid saving on every keystroke).
- **Flush** on lifecycle `ON_STOP` / process backgrounding (app coordinates lifecycle; `:core-files` exposes `flush()`).
- Works with **dirty** state from `:feature-editor`.
- **No Compose** in `:core-files` — controller is coroutine + callback based.

### Debounce policy

| Parameter | Suggested value | Rationale |
| --- | --- | --- |
| Debounce delay | **800 ms** | Balance battery and loss window; tunable constant. |
| Max wait (coalesce) | **5 s** | If user types continuously, still save periodically. |
| Immediate flush | Lifecycle stop, manual Save, before swapping `uri` | Avoid losing last edits. |

Implementation pattern: `snapshotFlow { content }` or explicit `notifyContentChanged()` → `debounce` → `saveIfDirty()`.

### What gets saved

| Session state | Autosave target |
| --- | --- |
| Has writable `uri`, dirty | Emit markdown string → SAF `openOutputStream(uri, "wt")` |
| No `uri`, dirty | Internal **draft** key in app-private storage (DataStore or `filesDir/drafts/<uuid>.md`) |
| Clean | No-op |

After internal draft autosave, recents are unchanged until the user Save As to a SAF location.

### Concurrency and failure

- Hold a **Mutex** around all writes to the same `uri`.
- If autosave fails, keep `isDirty = true`; expose `lastSaveError` for a non-blocking snackbar on retry.
- Do not queue unbounded failed retries; exponential backoff on repeated provider errors (optional v1: single retry).

### Relationship to `:core-markdown`

Editor works on AST or block list; autosave calls **`MarkdownEngine.emit(document)`** (or equivalent) to produce the canonical markdown string before write. Raw-source editing, if added later, would bypass emit for that mode only.

---

## Module boundaries

```
┌─────────────────────────────────────────────────────────┐
│ :app                                                     │
│  Activity Result (OPEN / CREATE), lifecycle, navigation │
└───────────────────────────┬─────────────────────────────┘
                            │ Uri, commands
┌───────────────────────────▼─────────────────────────────┐
│ :core-files                                              │
│  SafIntents, NoteIo, RecentsRepository, AutosaveController │
└───────────────────────────┬─────────────────────────────┘
                            │ String markdown
┌───────────────────────────▼─────────────────────────────┐
│ :core-markdown                                           │
│  parse / emit                                            │
└─────────────────────────────────────────────────────────┘
```

| Component | Responsibility |
| --- | --- |
| `SafIntents` | Build OPEN / CREATE intents with correct flags and MIME filters. |
| `NoteIo` | `readText(context, uri)`, `writeText(context, uri, text)`, `persistPermissions(context, uri)`. |
| `RecentsRepository` | Persist and observe recents list. |
| `AutosaveController` | Debounce, mutexed save, draft fallback, `flush()`. |
| `NoteSession` (optional aggregate) | Holds uri, displayName, dirty, wires IO + recents + autosave. |

`:feature-editor` sets dirty on block edits and calls `autosave.schedule()`; it does not construct SAF intents.

---

## Error model

Sealed hierarchy for callers:

| Variant | Typical cause |
| --- | --- |
| `AccessDenied` | Revoked persistable permission |
| `NotFound` | Uri deleted at provider |
| `ProviderError` | IPC / remote exception from DocumentsProvider |
| `EncodingError` | Invalid UTF-8 ( rare for user notes ) |
| `Cancelled` | User dismissed picker (app layer; not from IO) |

Map to user-visible strings in UI; `:core-files` returns typed errors only.

---

## Testing strategy

| Layer | What to test |
| --- | --- |
| JVM (Robolectric or androidTest) | `RecentsRepository` ordering, cap at N, remove stale |
| androidTest with `ActivityScenario` | Optional: stub provider or use `InstrumentationRegistry` context with `MockContentProvider` |
| Manual QA | Open each file under `fixtures/notes/`, edit, rotate, kill process, reopen from recents |

Fixtures path on device: copy `welcome.md` etc. into Downloads via adb, then open with the app picker.

Regression checks:

1. Open → edit → debounce → kill app → reopen recent → content matches.
2. Save As new name → recent list shows new title; old uri unchanged on disk.
3. Revoke permission in Settings → recent entry fails gracefully.

---

## Security and privacy

- Recents store **URIs and display names** only in app-private DataStore.
- No upload or telemetry of note content from `:core-files`.
- Backup: if `android:allowBackup` is true, exclude DataStore/drafts from backup manifest rules if drafts may contain sensitive unsaved text (implementation detail in `:app` manifest merge).

---

## Future extensions (out of v1)

1. **OPEN_DOCUMENT_TREE** — pick a notebook folder and enumerate `.md` children (heavy provider variance).
2. **Share target** — receive `ACTION_SEND` text and Save As once.
3. **External change detection** — compare provider `LAST_MODIFIED` before overwrite.
4. **Multi-tab** — one `AutosaveController` per open document session.
5. **Import from GET_CONTENT** — one-shot read without persistable write.

---

## Summary

- **Open:** `ACTION_OPEN_DOCUMENT` with read/write/persistable flags; read UTF-8 via `ContentResolver`.
- **Save:** in-place `"wt"` on existing uri; **Save As** via `ACTION_CREATE_DOCUMENT` with suggested `.md` title.
- **Permissions:** `takePersistableUriPermission` after every successful open/create used for editing/recents.
- **Recents:** DataStore-backed list (≈20), URI-based, validated on load; touch on open/Save As.
- **Autosave:** debounced (~800 ms) + lifecycle flush; SAF when uri exists, internal draft when not; mutexed writes; emit markdown via `:core-markdown`.
- **No UI in `:core-files`:** intents and I/O only; `:app` owns Activity Results and lifecycle hooks.
