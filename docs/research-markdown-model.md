# Research: Markdown block model (GFM)

Design reference for `:core-markdown` in **Markdown Notes (Android)**. This document defines the **block-level AST**, **parse/emit round-trip rules**, and **GFM table edge cases**. It does not describe UI.

**Consumers:** `docs/IMPLEMENTATION.md` (merged plan), `MarkdownEngine`, fixture tests under `apps/markdown-notes-android/fixtures/notes/`.

---

## Goals

1. **Notes-first editing** — Users think in blocks (title, paragraph, table, code snippet), not raw delimiter syntax.
2. **Predictable persistence** — Saving a note and reopening it (here or in another Markdown viewer) should not surprise users.
3. **Bounded scope** — Support the block types that appear in sample notes and typical personal notes, not full CommonMark + all GFM extensions.

Non-goals for v1: rich inline editing (bold/links as AST nodes), list/blockquote block types, HTML blocks, task lists, strikethrough, autolinks as structured data.

---

## Standards and libraries

| Layer | Choice |
| --- | --- |
| Block structure | CommonMark block grammar |
| Tables | [GFM tables](https://github.github.com/gfm/#tables-extension-) via `TablesExtension` |
| Parser | [commonmark-java](https://github.com/commonmark/commonmark-java) (`Parser` + GFM tables) |

The app AST is **not** a copy of commonmark-java’s `Node` tree. We **project** parser output into a small Kotlin `Block` ADT, then **serialize** that ADT back to Markdown text.

---

## Block AST

Root type: `Document(blocks: List<Block>)`.

`Block` is a sealed hierarchy — one variant per **top-level** block the editor can select, edit, or reorder.

### Variants (v1)

| Variant | Fields | Meaning |
| --- | --- | --- |
| `Heading` | `level: Int`, `text: String` | ATX heading (`#` … `######`). `text` is plain text (no inline AST). |
| `Paragraph` | `text: String` | Ordinary paragraph; plain text. |
| `Table` | `headerRow: Boolean`, `rows: List<List<String>>` | GFM pipe table. Row 0 is the header row when `headerRow == true`. Cell values are plain text. |
| `CodeFence` | `language: String?`, `code: String` | Fenced code block (info string → `language`). |
| `HorizontalRule` | (singleton) | Thematic break (`---`, `***`, `___` in source). |

Reference shape (as in scaffold):

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

### Inline content policy

There is **no inline AST** in v1. Parser walks commonmark **inline** nodes under each block and concatenates `Text` literals into a single `String`.

**Consequences:**

- `**bold**`, `*italic*`, `` `code` ``, `[label](url)` parse correctly in commonmark-java but **collapse to** `bold`, `italic`, `code`, `label` in the AST.
- Hard line breaks inside a paragraph (`  \n` or `<br>`) become newline characters inside `text` if the walker preserves them; emit writes `text` as one line unless it already contains `\n`.
- Images and raw HTML are not first-class; alt text or inner text may appear as plain characters depending on node types the walker visits.

Document this as **intentionally lossy inlines** until a later phase adds `Inline` nodes or a source-preserving mode.

### Block types we drop on parse

commonmark-java may produce block nodes that are **not** mapped to `Block`. Today’s engine ignores anything that is not `Heading`, `Paragraph`, `FencedCodeBlock`, `ThematicBreak`, or GFM `TableBlock`.

Examples that disappear from the AST (unless we extend mapping later):

- Bullet / ordered lists → list items become unrepresented (often absorbed oddly or omitted).
- Block quotes, HTML blocks, indented code blocks.
- Setext headings (underlined titles) — parser may still emit `Heading` nodes; treat like ATX once mapped.

**Editor rule:** Notes authored only with supported blocks (fixtures) round-trip cleanly. Notes with unsupported blocks may **lose content** on load; future work can add `Block.Unknown(raw: String)` or “passthrough spans.”

---

## Parse pipeline

```
Markdown string
    → trim outer whitespace (document boundary)
    → Parser (TablesExtension)
    → walk direct children of Document root
    → List<Block>
```

### Per-block mapping

| commonmark-java node | `Block` |
| --- | --- |
| `Heading` | `Heading(level, inlineText(node))` |
| `Paragraph` | `Paragraph(inlineText(node))` |
| `FencedCodeBlock` | `CodeFence(info, literal)` — `info` may be empty → `null` or `""`; pick one convention in implementation and stick to it |
| `ThematicBreak` | `HorizontalRule` |
| `TableBlock` | `Table(headerRow = true, rows = …)` — see tables section |

Only **sibling** blocks under the document root are collected; nested structures inside lists/quotes are out of scope.

### Heading levels

CommonMark allows levels 1–6. The **reader UI** may style only 1–3; the AST should still store the parsed `level`. **Emit** may clamp display-oriented output (see round-trip).

---

## Emit pipeline (canonical Markdown)

```
Document
    → StringBuilder, one block at a time
    → trimEnd document, append single trailing `\n`
```

### Canonical block formatting

| Block | Emitted form |
| --- | --- |
| `Heading` | `#`.repeat(level) + space + text + blank line |
| `Paragraph` | text + blank line |
| `CodeFence` | ` ``` ` + optional language + newline + code (trimEnd per line block) + newline + ` ``` ` + blank line |
| `HorizontalRule` | `---` + blank line |
| `Table` | GFM pipe table; see tables section |

**Inter-block spacing:** One blank line between blocks (paragraph’s trailing blank line is the separator).

**Document boundary:** No leading blank lines; exactly one trailing newline after trim.

---

## Round-trip rules

Define:

- `parse(s)` — string → `Document`
- `emit(d)` — `Document` → string
- `roundTrip(s) = emit(parse(s))`

### Invariants (must hold in tests)

1. **Structural stability for fixtures** — For every file in `fixtures/notes/`, `roundTrip` preserves:
   - heading count and levels (after any documented clamping),
   - paragraph text (modulo inline loss),
   - table dimensions and cell text (modulo padding),
   - code fence body and language,
   - presence of horizontal rule.

2. **Parse ∘ emit ∘ parse (semantic)** — For documents produced by `emit` from any AST built in the editor, a second parse yields an **equal** AST (Kotlin data class equality), not necessarily byte-identical Markdown.

3. **Idempotent emit** — `emit(parse(emit(d))) == emit(d)` for editor-origin documents.

### Normalization (expected semantic drift)

These are **acceptable** when comparing `s` vs `roundTrip(s)` as raw strings:

| Source quirk | After round-trip |
| --- | --- |
| Trailing spaces on lines | Trimmed at document parse (`trim()`) |
| `***` or `___` thematic break | `---` |
| Heading levels 4–6 | May emit as level 3 if emit clamps to 1–3 (document in IMPLEMENTATION if we keep clamp) |
| Table column align `:---:` in separator | Separator normalized to `---` columns (alignment not stored in AST) |
| Variable pipe spacing `\|a\|b\|` vs `\| a \| b \|` | Spaces around pipes normalized |
| Ragged row width | Short rows padded with empty cells |
| Multiple blank lines between blocks | Collapsed to single blank line |
| Setext vs ATX heading | If parsed as `Heading`, emit uses ATX `#` form |
| Inline markup in paragraphs/cells | Plain text only in AST; emit has no `**` |
| Fenced code trailing newline on file | Fence body uses `trimEnd()` on literal; final newline before closing fence normalized |

### Unacceptable (bugs)

- Table row/column count changes without user edit.
- Cell text reordered or merged.
- Code fence content altered (other than trailing newline normalization).
- Silent drop of a supported block type that appeared in input (for fixture subset).

### Round-trip test matrix

| Input source | Test type |
| --- | --- |
| `fixtures/notes/*.md` | Golden `roundTrip` + structural assert on `parse` |
| Hand-built `Document` in tests | `parse(emit(d)) == d` |
| Table edge cases (below) | Dedicated JVM tests in `:core-markdown` |

---

## GFM tables

### Recognition (parser)

A table block requires:

1. A **header row** — one line of pipe cells.
2. A **delimiter row** — pipes and dashes only (optional colons for alignment).
3. Zero or more **body rows**.

If the delimiter row is missing, commonmark-java treats lines as **paragraphs**, not a table. The editor cannot infer a table without that row.

### AST layout

- `rows[0]` — header cells (always when `headerRow == true`).
- `rows[1..]` — body rows only; the delimiter line is **not** stored (it is implied on emit).

Parser today sets `headerRow = true` whenever a `TableBlock` is seen.

### Emit algorithm (canonical)

1. If `rows.isEmpty()`, emit **nothing** (skip block).
2. `columnCount = max(row.size)` over all rows.
3. Pad each row with `""` to `columnCount`.
4. Emit header line: `| c1 | c2 | … |`.
5. Emit delimiter: `| --- | --- | … |` (no alignment colons in v1).
6. Emit each body row similarly.
7. Trailing blank line after table.

This matches fixtures such as:

```markdown
| Day | Focus |
| --- | --- |
| Mon | Reading |
```

### Table edge cases

| Case | Parse behavior | Emit / editor behavior |
| --- | --- | --- |
| **Missing delimiter row** | Not a table; lines stay paragraphs | User must add `\|---\|` row (or use table UI that inserts it) |
| **Ragged rows** (fewer cells in one row) | Parser fills missing cells per GFM rules | Pad with empty strings; rectangular grid in UI |
| **Extra cells in one row** | Extra cells appear in that row’s list | Emit uses max column count; all rows padded |
| **Empty table** (`rows.isEmpty()`) | Unusual in parsed output | Skip on emit; avoid creating in editor |
| **Header-only** (header + delimiter, no body) | Valid table | Emit header + delimiter only |
| **Empty header cell** | `\| \| foo \|` | Preserve empty string in cell list |
| **Empty body cell** | `\| a \| \|` | Preserve `""` in row |
| **Single column** | Valid | `\| h \|\n\| --- \|\n\| v \|` |
| **Pipe character in cell text** | Must be escaped as `\|` in source for GFM | If user types `\|` in UI, emit must escape pipes in cell strings |
| **Cell with only spaces** | Trimming policy: define once — recommend **trim cell for display**, store raw or trimmed consistently | Document choice in IMPLEMENTATION |
| **Alignment** `:---`, `---:`, `:---:` | Parser accepts; AST does not store alignment | Emit loses alignment (all `---`) |
| **Inline in cells** (`**x**`, links) | Becomes plain text in AST | Re-emitted without markup |
| **Table immediately after heading** (no blank line) | CommonMark often still parses table as sibling block | Emit always inserts blank line between blocks |
| **Table inside list or quote** | Not in v1 block model | Unsupported |
| **Very wide tables** | No limit in model | UI may horizontal-scroll; AST is full grid |

### Pipe escaping on emit (required for correctness)

When serializing cell text `t`:

- Replace `\|` sequences appropriately: cell interior `|` must become `\|` in the output line so the row still parses as the same number of columns.
- Backslash itself: follow commonmark-java renderer behavior in tests; if we hand-roll emit, add tests for `\|` and `\\`.

### `headerRow = false`

GFM always has a header row in practice. If the flag exists for future “headerless” grids, emit would need a rule (e.g. treat row 0 as data and synthesize dummy header). **v1:** always `true` for parsed tables; table editor always shows row 0 as header.

---

## Mapping to sample fixtures

| File | Blocks expected |
| --- | --- |
| `welcome.md` | H1, Paragraph, Table 2×2 (+ header), … |
| `sample-table.md` | H1, Table 2×3 |
| `sample-headings.md` | H1, H2, H3, Paragraph, HR, Paragraph |
| `sample-code-fence.md` | H1, CodeFence(`kotlin`, …), Paragraph |

Automated tests should load these paths from the repo (resources or filesystem) and assert `parse` structure + `roundTrip` constraints from this doc.

---

## Future extensions (out of v1)

Ordered by likely user value:

1. **Inline AST** — bold, italic, code, link for WYSIWYG-style spans within paragraphs and cells.
2. **List blocks** — bullet/ordered as `Block.ListItem` groups.
3. **Table metadata** — column alignment, optional caption.
4. **Unknown / raw blocks** — preserve unsupported Markdown verbatim for forward compatibility.
5. **CommonMark renderer round-trip** — optional second path using `MarkdownRenderer` on commonmark tree for diff/debug (not the editor’s source of truth if inlines are edited structurally).

---

## Summary

- **AST:** Small block union + plain `String` cell/paragraph text; GFM tables as rectangular `rows`.
- **Parse:** commonmark-java + tables; map only supported root blocks; inlines flattened (lossy).
- **Emit:** One canonical Markdown formatting; normalize HR, spacing, table delimiters, and ragged rows.
- **Round-trip:** Semantic equality for editor documents; string equality not required; fixtures define regression baseline.
- **Tables:** Delimiter row required for recognition; pad ragged rows; escape pipes on emit; alignment not preserved in v1.
