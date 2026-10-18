# Bot walkthrough — Markdown Android app

Plain-language guide to running helper agents in order. Technical details live in the DAG and repo, not in chat.

## Before you start

- GitHub repo for the app (can be empty).
- Cursor API key set on the machine that runs the helpers.
- Android Studio on that same machine (helpers write code; you build and tap-test).
- A few sample notes (with a table) in `fixtures/notes/` in the repo.

## Walkthrough phases

### Phase 0 — Empty box

**Bot:** Creates the Android project skeleton (modules, theme, “hello” screen).

**You:** Open in Android Studio; run on phone or emulator once. If it doesn’t install, fix before Phase 1.

---

### Phase 1 — Two planners (parallel)

**Bots:** One writes “how markdown becomes blocks”; one writes “how we open and save files.”

**You:** Skim the two docs only if you care; no action required.

---

### Phase 2 — One master plan

**Bot:** Merges plans into a single build guide for the team.

**You:** Optional read; confirms scope matches what you wanted.

---

### Phase 3 — Brain + tests

**Bot:** Builds the markdown translator and runs automated checks on your sample notes.

**You:** If this fails, stop — everything else depends on it. Re-run or fix this phase only.

---

### Phase 4 — Read + open (parallel)

**Bots:** Reading screen (pretty, no typing) and “pick a file / recents.”

**You:** Open a sample note; you should read it end-to-end.

---

### Phase 5 — Pinch and zoom

**Bot:** Makes the reader behave like zooming a photo.

**You:** Pinch, drag when zoomed, double-tap reset. Note anything sticky or janky.

---

### Phase 6 — Edit blocks

**Bot:** Tap-to-edit paragraphs, titles, lists, etc. — no symbol soup.

**You:** Change text, leave, come back; confirm save warning and autosave feel right.

---

### Phase 7 — Table room

**Bot:** Spreadsheet-style table editor hooked to the brain.

**You:** Add rows/columns, fill cells, save, reopen file elsewhere if you can — table should survive.

---

### Phase 8 — Polish (parallel)

**Bots:** Jump list for headings; README for humans.

**You:** Use the jump list; share README with anyone helping test.

---

### Phase 9 — You sign off

**You only:** Real notes, real thumb, real day — open, zoom, edit, table, kill app, reopen.

---

## Running the chore chart

1. Put Phase 0 done (or add scaffold as first DAG task).
2. Start the DAG runner from your app repo with the JSON in this folder.
3. Open the live canvas to watch boxes turn green.
4. If a box goes red, fix that task; downstream boxes skip until upstream is green.
5. Commit after each green phase if you want easy rollback.

## When to use cloud bots instead

Same phases, one pull request per phase, if your laptop shouldn’t run long jobs. You still build and test locally or in GitHub Actions.

## What “done” means

Sample tests green; you edited a table without thinking about markdown syntax; a friend would call it a notes app, not a developer tool.
