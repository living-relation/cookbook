# Markdown Notes (Android)

Multi-module Kotlin / Jetpack Compose app for reading and editing Markdown notes with GFM tables. This repository scaffold is extended by the automated pipeline on branch `Living-relation/markdown-android-full-auto-ee3c`.

## Modules

| Module | Role |
| --- | --- |
| `:app` | Application shell, navigation, theme |
| `:core-markdown` | Parse / emit block AST (CommonMark + GFM tables) |
| `:core-files` | SAF open/save, recents, autosave (placeholder) |
| `:feature-reader` | Read-only Compose UI |
| `:feature-editor` | Block editor with toolbar, read/preview toggle, dirty → autosave |
| `:feature-table` | Table grid editor (GFM `Block.Table`) |

## Prerequisites

- JDK 17 or newer
- Android SDK with **API 35** and **Build-Tools 35.0.0** (CI also installs Build-Tools 34 as needed)

Set `sdk.dir` in `local.properties` (see `local.properties.example`) or export `ANDROID_HOME` and point Gradle at it:

```bash
echo "sdk.dir=${ANDROID_HOME}" >> local.properties
```

## Build

From this directory:

```bash
chmod +x gradlew
./gradlew :app:assembleDebug
./gradlew :core-markdown:test
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions (workflow **Markdown Android — full auto build**) runs the same assemble step and uploads **markdown-notes-debug-apk**.

## Sample notes

Markdown fixtures for tests and manual QA live under `fixtures/notes/`:

- `welcome.md` — headings, paragraph, table (matches the in-app demo)
- `sample-table.md` — table-focused note
- `sample-headings.md` — heading levels
- `sample-code-fence.md` — fenced code block

## Manual smoke test

1. Install the debug APK on a device or emulator (API 26+).
2. Launch **Markdown Notes** — the **Read** tab shows parsed demo content.
3. Switch to **Edit** and **Table** tabs; edit blocks and table cells (changes mark the note dirty and autosave when backgrounded).
