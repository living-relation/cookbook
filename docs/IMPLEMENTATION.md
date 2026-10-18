# Implementation plan — Markdown Notes (Android)

Single build guide for **Markdown Notes**, merging upstream research into one place: module layout, public APIs, navigation, and test strategy anchored on `apps/markdown-notes-android/fixtures/notes/`.

**Upstream sources (detail lives there; this doc is the merged contract):**

| Document | Scope |
| --- | --- |
| [research-markdown-model.md](./research-markdown-model.md) | Block AST, parse/emit, GFM tables, round-trip rules |
| [research-android-files.md](./research-android-files.md) | SAF open/save, recents, autosave, `:core-files` boundaries |
| [apps/markdown-notes-android/README.md](../apps/markdown-notes-android/README.md) | Build commands, module list, smoke test |
| [plans/dag-markdown-android.json](../plans/dag-markdown-android.json) | Pipeline task order and dependencies |

**Repository path:** `apps/markdown-notes-android/`  
**Branch:** `Living-relation/markdown-android-full-auto-ee3c`

---

## Product scope (v1)

Notes-first Android app: open user `.md` files via SAF, read with pinch zoom, edit blocks (paragraphs, headings, code fences, GFM tables), autosave. Not a raw Markdown source editor; not full CommonMark (lists, blockquotes, inline AST are out of scope until later phases).

---

## Module graph

Gradle project `MarkdownNotes` (`settings.gradle.kts`):

```
                    ┌─────────────┐
                    │    :app     │
                    │ shell, nav  │
                    └──────┬──────┘
           ┌───────────────┼───────────────┬──────────────┐
           ▼               ▼               ▼              ▼
   ┌──────────────┐ ┌──────────────┐ ┌─────────────┐ ┌─────────────┐
   │:feature-reader│ │:feature-editor│ │:feature-table│ │ :core-files │
   └──────┬───────┘ └──────┬───────┘ └──────┬──────┘ └──────┬──────┘
          │                │                │               │
          └────────────────┴────────────────┘               │
                           │                                │
                           ▼                                │
                    ┌──────────────┐                        │
                    │:core-markdown│◄───────────────────────┘
                    │ parse / emit │   (autosave calls emit)
                    └──────────────┘
```

| Module | Package root | Depends on | Responsibility |
| --- | --- | --- | --- |
| `:app` | `com.livingrelation.markdownnotes` | all feature + core modules | `MainActivity`, Compose theme, Navigation Compose, SAF Activity Results, lifecycle → `AutosaveController.flush()` |
| `:core-markdown` | `…core.markdown` | commonmark-java + GFM tables | `Block` ADT, `MarkdownEngine` parse/emit/roundTrip — **no Android UI** |
| `:core-files` | `…core.files` | AndroidX core | SAF intents, read/write UTF-8, recents DataStore, debounced autosave — **no Compose** |
| `:feature-reader` | `…feature.reader` | `:core-markdown` | `ReadScreen(document)`, zoom/pan, later TOC drawer |
| `:feature-editor` | `…feature.editor` | `:core-markdown` | Block editing, read/edit toggle, dirty flag → autosave |
| `:feature-table` | `…feature.table` | `:core-markdown` | Grid editor for `Block.Table`, wired from editor |

**Dependency rule:** Features never depend on each other laterally; `:app` wires them and holds shared session state (current `Document`, `uri`, dirty).

**Platform:** `minSdk` 26, `targetSdk` / `compileSdk` 35, JDK 17, Compose BOM 2024.10.01.

---

## `:core-markdown` API

### Types (`Blocks.kt`)

```kotlin
sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Table(val headerRow: Boolean, val rows: List<List<String>>) : Block
    data object HorizontalRule : Block
    data class CodeFence(val language: String?, val code: String) : Block
}

data class Document(val blocks: List<Block>)
```

- **Inline policy:** No inline AST in v1. Parser flattens commonmark inline nodes to plain `String` (lossy for `**bold**`, links, etc.). See [research-markdown-model.md](./research-markdown-model.md).
- **Unsupported root blocks** (lists, blockquotes, HTML, indented code) are **not** mapped; content may be dropped on parse until `Block.Unknown` or extended mapping exists.

### `MarkdownEngine`

| Method | Contract |
| --- | --- |
| `parse(markdown: String): Document` | Trim outer whitespace; walk document-root children only; map Heading, Paragraph, FencedCodeBlock, ThematicBreak, GFM `TableBlock`. |
| `emit(document: Document): String` | Canonical Markdown: one blank line between blocks; single trailing `\n`; HR as `---`; GFM pipe tables with `| --- |` separator row. |
| `roundTrip(markdown: String): String` | `emit(parse(markdown))` — used in fixture tests. |

### Implementation decisions (locked for v1)

These resolve open items from the research docs:

| Topic | Decision |
| --- | --- |
| Heading emit | **Clamp levels 4–6 to 3** on emit (`coerceIn(1, 3)`). Parse still stores true level; reader styles 1–3 only. |
| Code fence `language` | Empty info string → store as empty/`null`; emit omits language tag when blank. |
| Table `headerRow` | Always `true` for parsed GFM tables; row 0 is header; delimiter row not stored. |
| Table alignment (`:---:`) | Not stored; emit uses plain `---` columns. |
| Ragged rows | Pad to `max(columnCount)` with `""` on emit. |
| Empty table | Skip block on emit (`rows.isEmpty()`). |
| Cell whitespace | **Store parsed text as-is** from inline walker; UI may trim for display only. |
| Pipe in cell text | **Emit must escape** `\|` inside cells so column count is stable (add tests in `core-markdown` task). |
| Document parse | Input trimmed with `markdown.trim()` at boundary (leading/trailing blank lines removed). |

Parser stack: `Parser` + `TablesExtension` (commonmark-java 0.24.0). Emit is hand-rolled (not `MarkdownRenderer` on the AST) so editor-shaped documents stay canonical.

---

## `:core-files` API (to implement)

Scaffold today is `FilesPlaceholder`; behavior is specified in [research-android-files.md](./research-android-files.md). Planned surface:

### SAF intents

```kotlin
object SafIntents {
    fun openDocumentIntent(): Intent
    fun createDocumentIntent(suggestedTitle: String): Intent
}
```

- **Open:** `ACTION_OPEN_DOCUMENT`, `text/*` + `EXTRA_MIME_TYPES` for markdown/plain, `CATEGORY_OPENABLE`, flags: read + write + persistable URI grant.
- **Save As:** `ACTION_CREATE_DOCUMENT`, type `text/markdown` (fallback `text/plain`), suggested `*.md` title, write + persistable flags.

### I/O

```kotlin
object NoteIo {
    suspend fun readText(context: Context, uri: Uri): Result<String>
    suspend fun writeText(context: Context, uri: Uri, text: String): Result<Unit>
    fun persistPermissions(context: Context, uri: Uri)
}
```

- UTF-8, no BOM; write via `openOutputStream(uri, "wt")`.
- **Mutex** on all writes to the same `uri` (manual Save + autosave).

### Recents

```kotlin
data class RecentNote(
    val uriString: String,
    val displayName: String,
    val lastOpenedAtEpochMs: Long,
)

interface RecentsRepository {
    fun observeRecents(): Flow<List<RecentNote>>
    suspend fun touch(uri: Uri, displayName: String)
    suspend fun remove(uri: Uri)
    suspend fun clear()
}
```

- Cap **N = 20**; DataStore-backed; touch on successful open / Save As (not every autosave).
- Validate cold start via `persistedUriPermissions` or probe read; drop stale entries on `SecurityException`.

### Autosave

```kotlin
class AutosaveController(
    // debounce 800 ms, max coalesce 5 s, flush() on lifecycle stop
) {
    fun schedule()
    suspend fun flush()
}
```

| Session | Autosave target |
| --- | --- |
| Writable `uri`, dirty | `NoteIo.writeText` after `MarkdownEngine.emit(document)` |
| No `uri`, dirty | App-private draft file / DataStore key |
| Clean | No-op |

### Errors

Sealed `FileError`: `AccessDenied`, `NotFound`, `ProviderError`, `EncodingError`; `Cancelled` handled in `:app` when picker dismissed.

### Session aggregate (optional in `:app` or `:core-files`)

`NoteSession`: holds `uri`, `displayName`, `isDirty`, `lastSavedAt`, coordinates IO + recents + autosave.

---

## Feature modules

### `:feature-reader`

- **`ReadScreen(document: Document)`** — Renders block list (headings, paragraphs, code, HR, table rows as text). Pinch zoom 0.8×–3×, pan when zoomed (reset offset when scale ≤ 1).
- **Later (`reader-zoom-pan`):** Double-tap and fit-width reset; polish gestures.
- **Later (`reader-toc`):** Drawer from `Block.Heading` list, scroll-to-section.

### `:feature-editor`

- Block-level editing (no symbol soup); toolbar for block types supported in AST.
- Read/edit toggle; sets **dirty** on change; calls `AutosaveController.schedule()`.
- Depends on emitted markdown for save path only (not on raw source).

### `:feature-table`

- Spreadsheet-style grid bound to `Block.Table` (rectangular rows, row 0 = header).
- Insert/delete rows/columns; cell editing updates AST; must respect pipe-escape rules on save.

---

## Navigation and app shell

### Current scaffold (`MainActivity`)

Bottom **NavigationBar** + **Navigation Compose** `NavHost`:

| Route | Label | Screen |
| --- | --- | --- |
| `read` | Read | `ReadScreen(document)` |
| `edit` | Edit | `EditorPlaceholder()` |
| `table` | Table | `TableEditorPlaceholder()` |

- Single in-memory `Document` today (hard-coded sample matching `fixtures/notes/welcome.md`).
- Nav options: `launchSingleTop`, `restoreState`, `saveState` on bottom bar switches.

### Target wiring (after `core-files` + features)

```
┌──────────────────────────────────────────────────────────────┐
│ App bar: title = displayName, actions = Open, Save, Save As  │
├──────────────────────────────────────────────────────────────┤
│ Optional: Recents sheet / first-run → pick file              │
├──────────────────────────────────────────────────────────────┤
│ NavHost: Read | Edit | Table  (same routes)                  │
│   shared ViewModel / NoteSession: Document + uri + dirty     │
└──────────────────────────────────────────────────────────────┘
         │ OPEN_DOCUMENT / CREATE_DOCUMENT (Activity Result)
         ▼
    parse(markdown) → Document → features
         ▲
    emit(document) ← autosave / Save
```

**Flow:**

1. User opens file → `:app` receives `Uri` → `NoteIo.readText` → `MarkdownEngine.parse` → update session + `RecentsRepository.touch`.
2. User edits in Edit/Table → mutate `Document` → dirty → debounced autosave → `emit` → `NoteIo.writeText`.
3. Save As → `CREATE_DOCUMENT` → new uri → write → replace session uri → recents.
4. `ON_STOP` → `autosave.flush()`.

No nested navigation graphs required for v1; TOC can be a modal drawer on Read route only.

---

## End-to-end data flow

```
  SAF Uri
     │ readText (UTF-8)
     ▼
  Markdown string ──parse──► Document ──► ReadScreen / Editor / Table UI
     ▲                            │
     │                            │ user edits
     │                            ▼
     └── emit ◄── Document (dirty)
            │
            writeText (mutex)
            ▼
          SAF Uri
```

---

## Test strategy (fixtures/notes)

Fixtures directory (repo-relative):

`apps/markdown-notes-android/fixtures/notes/`

| File | Exercises |
| --- | --- |
| `welcome.md` | H1, paragraph, 2×2 table (+ header/delimiter) — aligns with in-app demo |
| `sample-table.md` | H1, 2×3 table |
| `sample-headings.md` | H1–H3, paragraph, horizontal rule |
| `sample-code-fence.md` | H1, Kotlin fence, trailing paragraph |

### `:core-markdown` (JVM — primary gate)

Run: `./gradlew :core-markdown:test` from `apps/markdown-notes-android/`.

| Test category | Assertion |
| --- | --- |
| **Fixture load** | For each `fixtures/notes/*.md`, load from filesystem (test helper resolves path relative to project root or copies into `src/test/resources`). |
| **Structural parse** | Count blocks by type; heading levels; table row/column dimensions; code language + body; HR present where expected. |
| **Round-trip (golden)** | `roundTrip(source)` preserves structure per [research-markdown-model.md](./research-markdown-model.md) invariants (acceptable string normalization documented there). |
| **Semantic idempotence** | `parse(emit(parse(s)))` equals `parse(s)` for fixture set (data class equality). |
| **Editor-origin** | Hand-built `Document` → `parse(emit(d)) == d`. |
| **Table edge cases** | Dedicated tests: ragged rows, header-only, empty cells, `\|` in cell (once emit escapes). |

**Loading fixtures in tests:** Prefer `src/test/resources/notes/` synced from `fixtures/notes/` **or** a small test utility:

```kotlin
fun fixturePath(name: String): File =
    File(System.getProperty("markdown.fixtures.root")
        ?: "../../fixtures/notes", name)
```

Set `markdown.fixtures.root` in CI to the repo fixtures path so one source of truth remains `fixtures/notes/`.

### `:core-files`

| Layer | Tests |
| --- | --- |
| Unit / Robolectric | Recents ordering, cap at 20, remove on stale uri |
| androidTest (optional) | `MockContentProvider` read/write |
| Manual | Open each fixture from device storage (adb push to Downloads), edit, kill process, reopen from recents |

Regression scenarios (manual + later automated):

1. Open → edit → debounce save → force-stop → recent → content matches.
2. Save As new name → recents shows new title; original file unchanged.
3. Revoke URI permission in Settings → graceful error, entry removed.

### Feature / app

- **Compose UI tests (optional v1):** Smoke navigate Read/Edit/Table tabs.
- **Manual QA:** Install debug APK; follow [README](../apps/markdown-notes-android/README.md) smoke steps; extend with file open and table edit once implemented.

### CI

Workflow **Markdown Android — full auto build**: `./gradlew :app:assembleDebug` and `:core-markdown:test`; artifact `app-debug.apk`.

---

## Pipeline task map

Order from `plans/dag-markdown-android.json` (dependencies must stay green before downstream work):

| Task id | Delivers |
| --- | --- |
| `scaffold-project` | Modules, fixtures, assembleDebug |
| `research-markdown-model` | `docs/research-markdown-model.md` |
| `research-android-files` | `docs/research-android-files.md` |
| **`implementation-plan`** | **`docs/IMPLEMENTATION.md` (this file)** |
| `core-markdown` | Full parse/emit + fixture JVM tests |
| `core-files` | SAF, recents, autosave |
| `feature-reader` | Read screen + file load + nav |
| `reader-zoom-pan` | Reader gesture polish |
| `feature-editor` | Block editor + dirty/autosave |
| `feature-table` | Table grid + AST wiring |
| `reader-toc` | Heading jump list |
| `readme` | Human setup + QA checklist |

---

## Manual QA checklist (consolidated)

1. `./gradlew :app:assembleDebug` and `:core-markdown:test` succeed.
2. Install APK on API 26+ device/emulator.
3. Push `fixtures/notes/*.md` to Downloads; open each via SAF.
4. Read tab: content matches file; pinch zoom and pan when zoomed.
5. Edit tab: change paragraph/heading; confirm dirty + autosave after backgrounding.
6. Table tab: add row/column, fill cells; Save; reopen file externally — table intact.
7. Save As copy; recents shows new entry.
8. Force-stop app; reopen from recents — last saved content present.

---

## Out of scope (v1)

Inline AST, lists/blockquotes as blocks, folder tree (`OPEN_DOCUMENT_TREE`), sync, conflict UI, share target, multi-document tabs. Listed as future work in the research docs.

---

## Quick commands

From `apps/markdown-notes-android/`:

```bash
./gradlew :app:assembleDebug
./gradlew :core-markdown:test
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
