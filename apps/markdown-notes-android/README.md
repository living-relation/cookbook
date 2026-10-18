# Markdown Notes (Android)

Multi-module Kotlin / Jetpack Compose app for reading and editing Markdown notes with GFM tables. Lives in the [cookbook](https://github.com/living-relation/cookbook) monorepo under `apps/markdown-notes-android/`. The automated build pipeline targets branch `Living-relation/markdown-android-full-auto-ee3c`.

## Modules

| Module | Role |
| --- | --- |
| `:app` | Application shell, navigation, theme |
| `:core-markdown` | Parse / emit block AST (CommonMark + GFM tables) |
| `:core-files` | SAF open/save, recents, autosave, internal draft |
| `:feature-reader` | Read-only Compose UI, pinch zoom, TOC drawer |
| `:feature-editor` | Block editor with toolbar, edit/preview toggle, dirty → autosave |
| `:feature-table` | Table grid editor (GFM `Block.Table`) |

## Prerequisites

| Requirement | Notes |
| --- | --- |
| **JDK 17+** | Matches Gradle / AGP; CI uses Temurin 17 |
| **Android SDK** | **API 35** platform and **Build-Tools 35.0.0** |
| **Android Studio** (recommended) | Ladybug or newer; open this folder as the Gradle root |

Optional for command-line installs: [Android command-line tools](https://developer.android.com/studio#command-line-tools-only) and `sdkmanager` to install the same packages as CI.

### Configure the SDK path

Copy the example file and set your SDK location (or append `sdk.dir` after export):

```bash
cd apps/markdown-notes-android
cp local.properties.example local.properties
# Edit local.properties, or:
echo "sdk.dir=${ANDROID_HOME}" >> local.properties
```

`local.properties` is gitignored and must exist before Gradle sync.

### Android Studio setup

1. **File → Open** → select `apps/markdown-notes-android` (not the monorepo root).
2. Let Gradle sync; install any missing SDK components Studio prompts for (API 35, Build-Tools 35).
3. Create or select a device:
   - **Emulator:** Device Manager → create a virtual device (API **26+**, e.g. API 34).
   - **Physical device:** enable USB debugging; accept the debug prompt on first connect.

## Build

From `apps/markdown-notes-android`:

```bash
chmod +x gradlew
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Install on a connected device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or run from Android Studio: choose the **app** configuration → **Run**.

## Automated tests

Run the same checks as CI locally:

```bash
# Unit tests (JVM / Robolectric where configured)
./gradlew :app:testDebugUnitTest \
  :core-markdown:testDebugUnitTest \
  :core-files:testDebugUnitTest \
  :feature-editor:testDebugUnitTest \
  :feature-reader:testDebugUnitTest \
  :feature-table:testDebugUnitTest

# Markdown engine + fixtures only
./gradlew :core-markdown:test
```

Instrumented UI smoke test (requires a running emulator or device):

```bash
./gradlew :app:connectedDebugAndroidTest
```

HTML reports: `**/build/reports/tests/**`.

## CI

GitHub Actions workflow **Markdown Android — full auto build** (`.github/workflows/markdown-android-full-auto.yml`):

- On push to `Living-relation/markdown-android-full-auto-ee3c` when Android app paths change.
- **android-ci:** assemble debug APK + unit tests; artifact **markdown-notes-debug-apk**.
- **ui-emulator:** `connectedDebugAndroidTest` on macOS with an API 29 emulator.

Download the APK from the workflow run’s Artifacts tab if you are not building locally.

## Sample notes

Markdown fixtures for tests and manual QA:

| File | Purpose |
| --- | --- |
| `fixtures/notes/welcome.md` | Headings, paragraph, GFM table (matches bundled demo) |
| `fixtures/notes/sample-table.md` | Table-focused note |
| `fixtures/notes/sample-headings.md` | Heading levels for TOC |
| `fixtures/notes/sample-code-fence.md` | Fenced code block |

Copy any fixture to the device (Downloads, Drive, etc.) before using **Open** in the app. Bundled demo content ships in `app/src/main/assets/notes/welcome.md`.

## App overview

- **Bottom navigation:** **Read**, **Edit**, **Table** — one shared document in memory.
- **Top bar:** title shows file name; `*` when dirty. **Recents** and **Open** (Storage Access Framework).
- **Autosave:** edits mark the note dirty; saving runs after a short debounce and when the app goes to background (`ON_STOP`). Opened files write back to their URI; the bundled welcome note saves to an internal draft until you **Open** a file on disk.

---

## Manual QA checklist

Use a **debug build** on API **26+** (phone or emulator). Check off each item; note device/OS and build (`versionName` in `app/build.gradle.kts`) on failures.

### A. Install and first launch

- [ ] App installs without errors (`adb install -r` or Studio Run).
- [ ] Cold start shows **Read** tab with heading **Welcome** and a feature/status table.
- [ ] Top bar title is **Markdown Notes** (no `*` until you edit).
- [ ] Bottom bar switches **Read** / **Edit** / **Table** without crash.

### B. Read tab — rendering

- [ ] Headings, body text, and table rows render (table uses ` · ` between cells in read view).
- [ ] Open `fixtures/notes/sample-headings.md` via **Open** — all heading levels appear.
- [ ] Open `fixtures/notes/sample-code-fence.md` — monospace code block on shaded background.
- [ ] Open `fixtures/notes/sample-table.md` — table content matches the file.

### C. Read tab — zoom and scroll

- [ ] Pinch zoom in/out (roughly 0.8×–3×); content scales smoothly.
- [ ] While zoomed in, pan with one finger; vertical list scroll is disabled until zoom returns near fit-width.
- [ ] Double-tap resets zoom to fit width and clears pan offset.
- [ ] At fit width, vertical scroll reaches top and bottom of the note.

### D. Read tab — table of contents

- [ ] On a note with headings (welcome or `sample-headings.md`), FAB (list icon) appears bottom-right.
- [ ] Tap FAB → **Contents** drawer opens; scrim tap or choosing an entry closes it.
- [ ] Tap a TOC entry → drawer closes and view scrolls to that heading.

### E. Open, recents, and errors

- [ ] **Open** → pick a `.md` file → content replaces demo; top bar shows file name.
- [ ] **Recents** lists the opened file; tap reopens the same content.
- [ ] **Recents** empty state explains using **Open** when nothing was opened yet.
- [ ] Invalid/unreadable file shows a snackbar (permission, not found, or encoding) without crashing.

### F. Edit tab — blocks and preview

- [ ] **Edit** tab shows block cards; tap **Select** on a block highlights it.
- [ ] Change paragraph/heading text → switch to **Read** → change is visible.
- [ ] Toolbar inserts **Paragraph**, **Heading**, **Code**, **Rule** after the selected block (or at start if none selected).
- [ ] **Remove** / **Up** / **Down** on a block reorder or delete as expected.
- [ ] **Preview** chip shows read-style layout; **Edit** chip returns to block editor.

### G. Dirty state and autosave

- [ ] After any edit, top bar title gains `*`.
- [ ] Background the app (Home) or switch away → return; for an **opened file**, changes persist when reopening the same file in an external viewer or after force-stop and **Recents**.
- [ ] For **bundled welcome only** (never opened a file), edits survive process death via internal draft (relaunch app — content should match last edit).

### H. Table tab

- [ ] **Table** tab shows grid for the first GFM table in the document.
- [ ] Edit cell text → **Read** tab reflects updated table.
- [ ] **Add row** / **Delete row** / **Add column** / **Delete column** update the grid; save path same as other edits (dirty + autosave).
- [ ] Document with no table shows empty state with control to insert a table; new table appears in **Read** and **Edit**.

### I. Cross-tab consistency

- [ ] Edit a table cell in **Table**, add a heading in **Edit**, read in **Read** — single consistent document.
- [ ] Open a different file while dirty — confirm behavior (snackbar or replace); no silent data loss without noticing `*`.

### J. Regression smoke (optional, ~2 min)

- [ ] Matches instrumented test: **Welcome** visible → **Edit** tab (`editor-screen`) → back to **Read** → **Welcome** still visible.
- [ ] `./gradlew :app:assembleDebug :core-markdown:test` passes on your machine before signing off a release candidate.

---

## Troubleshooting

| Symptom | Things to try |
| --- | --- |
| Gradle “SDK location not found” | Create `local.properties` with `sdk.dir=...` |
| Missing platform 35 | `sdkmanager "platforms;android-35" "build-tools;35.0.0"` |
| `connectedDebugAndroidTest` finds no device | `adb devices`; start emulator or plug in phone |
| Open fails after moving file | Re-pick via **Open**; stale URIs drop from **Recents** after validation |

## License

Part of the living-relation cookbook repository; follow the root repo license and contribution guidelines.
