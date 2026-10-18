import { readFile } from "node:fs/promises";
import { join } from "node:path";
import process from "node:process";
import { Agent, CursorAgentError } from "@cursor/sdk";

interface DagTask {
  id: string;
  depends_on: string[];
  complexity: string;
  subtask_prompt: string;
}

interface DagFile {
  title: string;
  tasks: DagTask[];
}

function topoSort(tasks: DagTask[]): DagTask[] {
  const byId = new Map(tasks.map((t) => [t.id, t]));
  const indeg = new Map<string, number>();
  const adj = new Map<string, string[]>();
  for (const t of tasks) {
    indeg.set(t.id, t.depends_on.length);
    for (const d of t.depends_on) {
      if (!byId.has(d)) throw new Error(`Unknown dependency ${d}`);
      const list = adj.get(d) ?? [];
      list.push(t.id);
      adj.set(d, list);
    }
  }
  const q = tasks.filter((t) => t.depends_on.length === 0).map((t) => t.id);
  const out: DagTask[] = [];
  while (q.length) {
    const id = q.shift()!;
    out.push(byId.get(id)!);
    for (const n of adj.get(id) ?? []) {
      const v = (indeg.get(n) ?? 0) - 1;
      indeg.set(n, v);
      if (v === 0) q.push(n);
    }
  }
  if (out.length !== tasks.length) throw new Error("DAG has a cycle");
  return out;
}

function complexityModel(c: string): string {
  switch (c) {
    case "HIGH":
      return "composer-2.5";
    case "MED":
      return "composer-2.5";
    case "LOW":
      return "composer-2.5";
    default:
      return "composer-2.5";
  }
}

async function main() {
  const dryRun = process.argv.includes("--dry-run");
  const apiKey = process.env.CURSOR_API_KEY?.trim();
  if (!dryRun && !apiKey) {
    console.error("CURSOR_API_KEY is required");
    process.exit(1);
  }

  const repo =
    process.env.GITHUB_REPOSITORY != null
      ? `https://github.com/${process.env.GITHUB_REPOSITORY}`
      : process.env.REPO_URL?.trim();
  if (!dryRun && !repo) {
    console.error("Set GITHUB_REPOSITORY or REPO_URL");
    process.exit(1);
  }

  const branch =
    process.env.TARGET_BRANCH?.trim() ||
    process.env.GITHUB_REF_NAME?.trim() ||
    "Living-relation/markdown-android-full-auto-ee3c";

  const root = process.env.WORKSPACE_ROOT?.trim() || join(process.cwd(), "../..");
  const dagPath =
    process.env.DAG_PATH?.trim() ||
    join(root, "plans/dag-markdown-android.json");
  const raw = JSON.parse(await readFile(dagPath, "utf8")) as DagFile;
  const ordered = topoSort(raw.tasks);

  console.log(`Orchestrating: ${raw.title}`);
  console.log(`Repo: ${repo ?? "(dry)"} branch: ${branch}`);
  console.log(`Tasks: ${ordered.length}`);

  if (dryRun) {
    for (const t of ordered) console.log(`- [${t.complexity}] ${t.id}`);
    return;
  }

  for (let i = 0; i < ordered.length; i++) {
    const task = ordered[i]!;
    const isLast = i === ordered.length - 1;
    const prompt = [
      `You are building the Markdown Notes Android app in ${repo}.`,
      `Work on branch ${branch}. Commit and push your changes.`,
      `Task id: ${task.id}`,
      "",
      task.subtask_prompt,
    ].join("\n");

    console.log(`\n=== Task ${i + 1}/${ordered.length}: ${task.id} ===`);

    try {
      const result = await Agent.prompt(prompt, {
        apiKey: apiKey!,
        model: { id: complexityModel(task.complexity) },
        cloud: {
          repos: [{ url: repo!, startingRef: branch }],
          autoCreatePR: isLast,
          skipReviewerRequest: true,
        },
      });

      if (result.status === "error") {
        console.error(`Task ${task.id} finished with error`);
        process.exit(2);
      }
      console.log(`Task ${task.id}: ${result.status}`);
    } catch (err) {
      if (err instanceof CursorAgentError) {
        console.error(`Startup failed: ${err.message}`);
        process.exit(1);
      }
      throw err;
    }
  }

  console.log("\nAll tasks dispatched.");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
