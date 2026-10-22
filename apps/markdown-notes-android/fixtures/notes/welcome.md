# Welcome

Open a Markdown file to read it. Pinch to zoom. Use **Contents** to jump around.

This viewer renders tables, pictures, and diagrams in the file. It does not edit them.

## Flow

```mermaid
flowchart LR
  A[Open a .md file] --> B[Read]
  B --> C[Tables]
  B --> D[Diagrams]
  B --> E[Pictures]
```

## Mix

```mermaid
pie title How this file is used
  "Reading" : 50
  "Diagrams" : 30
  "Tables" : 20
```

## Conversation

```mermaid
sequenceDiagram
  participant You
  participant Viewer
  You->>Viewer: Open note
  Viewer->>You: Show text, tables, and graphics
```

## Picture

![Sample graphic](sample-graphic.svg)

## Status

| Feature | Status |
| --- | --- |
| View | Ready |
| Zoom | Ready |
| Tables | Ready |
| Diagrams | Ready |
| Pictures | Ready |
