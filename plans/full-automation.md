# Fully automated build (no manual steps for you)

## What runs by itself

1. Push to branch `Living-relation/markdown-android-full-auto-ee3c` (or manual “Run workflow” on GitHub).
2. **Orchestrate** job runs every bot task in order against this repo (cloud agents commit to the same branch).
3. **Android CI** job builds a debug APK and uploads it as a workflow artifact.

## Secrets (two places, same key value)

| Where | Why |
| --- | --- |
| Cloud Agent environment | Bots run from an agent VM (orchestrator on the branch). |
| GitHub Actions repo secret `CURSOR_API_KEY` | Same key, so the **orchestrate** job on GitHub can run without a VM. |

After both are set, automation owns scaffold → brain → reader → zoom → editor → tables → polish.

## Where things live

| Piece | Path |
| --- | --- |
| App | `apps/markdown-notes-android/` |
| Bot task list | `plans/dag-markdown-android.json` |
| Runner | `scripts/markdown-android-full-auto/orchestrate.mts` |
| Workflow | `.github/workflows/markdown-android-full-auto.yml` |

## How you get the app

Download **markdown-notes-debug-apk** from the latest green workflow run (Actions tab).
