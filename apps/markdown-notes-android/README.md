# Markdown (Android)

A simple Markdown **viewer**. Open a `.md` file, pinch to zoom, and jump headings. Tables, pictures, and Mermaid diagrams render in place. There is no editor.

Lives in the [cookbook](https://github.com/living-relation/cookbook) monorepo under `apps/markdown-notes-android/`.

## Modules

| Module | Role |
| --- | --- |
| `:app` | Shell, theme, Open / Recents |
| `:core-markdown` | Heading outline for the contents list |
| `:core-files` | SAF open and recents |
| `:feature-reader` | Zoomable reader (WebView + Mermaid) |

## Build

```bash
cd apps/markdown-notes-android
cp local.properties.example local.properties
# set sdk.dir
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Tests

```bash
./gradlew :app:testDebugUnitTest \
  :core-markdown:testDebugUnitTest \
  :core-files:testDebugUnitTest \
  :feature-reader:testDebugUnitTest
```

## Sample notes

| File | Purpose |
| --- | --- |
| `fixtures/notes/welcome.md` | Headings, mermaid, table, picture |
| `fixtures/notes/sample-diagrams.md` | Flow + chart fences |
| `fixtures/notes/sample-table.md` | GFM table |
| `fixtures/notes/sample-headings.md` | Heading levels |
| `fixtures/notes/sample-code-fence.md` | Fenced code |

## Using the app

- **Open** searches this phone for a markdown file.
- **Recents** reopens files you already picked.
- **Contents** jumps to a heading.
- Pinch to zoom; double-tap resets.

Remote pictures need a network connection. Relative pictures work when the file is opened from a normal file path, or when they ship next to the bundled welcome note.
