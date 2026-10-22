(function () {
  const DIAGRAM_LANGS = { mermaid: true, mmd: true, chart: true };

  function escapeHtml(text) {
    return String(text)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function resolveUrl(href, base) {
    if (!href) return "";
    if (/^(https?:|data:|file:|content:)/i.test(href)) return href;
    if (!base) return href;
    return base.replace(/\/?$/, "/") + href.replace(/^\.\//, "");
  }

  function stripTags(html) {
    const node = document.createElement("div");
    node.innerHTML = html;
    return node.textContent || node.innerText || "";
  }

  function bootMermaid() {
    if (!window.mermaid) return;
    window.mermaid.initialize({
      startOnLoad: false,
      securityLevel: "strict",
      theme: "base",
      fontFamily: 'Georgia, "Times New Roman", serif',
      themeVariables: {
        background: "#f3eee4",
        primaryColor: "#dce8e1",
        primaryTextColor: "#1c1814",
        primaryBorderColor: "#1e5844",
        lineColor: "#6b5344",
        secondaryColor: "#e8dcc8",
        tertiaryColor: "#f3eee4",
        clusterBkg: "#fffdf8",
        clusterBorder: "#d2c6b6",
        titleColor: "#1c1814",
        pie1: "#1e5844",
        pie2: "#6b5344",
        pie3: "#c4a574",
        pie4: "#4a7c6c",
        pieTitleTextSize: "18px",
      },
      flowchart: { htmlLabels: false, curve: "basis" },
    });
  }

  function headingRenderer() {
    return function heading(text, level) {
      const depth = typeof text === "object" ? text.depth : level;
      const html = typeof text === "object" ? text.text : text;
      return "<h" + depth + ">" + html + "</h" + depth + ">";
    };
  }

  function imageRenderer(imageBase) {
    return function image(href, title, text) {
      const token = typeof href === "object" ? href : null;
      const source = resolveUrl(token ? token.href : href, imageBase);
      const alt = token ? token.text : text;
      const caption = (token && token.title) || title || "";
      return (
        '<figure class="graphic"><img src="' +
        escapeHtml(source) +
        '" alt="' +
        escapeHtml(alt || "") +
        '">' +
        (caption ? "<figcaption>" + escapeHtml(caption) + "</figcaption>" : "") +
        "</figure>"
      );
    };
  }

  function codeRenderer() {
    return function code(code, infostring) {
      const token = typeof code === "object" ? code : null;
      const source = token ? token.text : code;
      const info = token ? token.lang : infostring;
      const language = String(info || "")
        .trim()
        .split(/\s+/)[0]
        .toLowerCase();
      if (DIAGRAM_LANGS[language]) {
        return '<div class="diagram-frame"><pre class="mermaid">' + escapeHtml(source) + "</pre></div>";
      }
      return (
        "<pre><code class=\"language-" +
        escapeHtml(language) +
        '">' +
        escapeHtml(source) +
        "</code></pre>"
      );
    };
  }

  function applyHeadingIds(toc) {
    const headings = document.querySelectorAll("#content h1, #content h2, #content h3, #content h4, #content h5, #content h6");
    toc.forEach(function (entry, index) {
      if (headings[index]) headings[index].id = entry.id;
    });
  }

  bootMermaid();

  window.renderMarkdown = async function (markdown, imageBase, toc) {
    const renderer = {
      heading: headingRenderer(),
      image: imageRenderer(imageBase || ""),
      code: codeRenderer(),
    };
    if (window.marked && window.marked.use) {
      window.marked.use({ gfm: true, breaks: true, renderer: renderer });
    }
    const parse = window.marked && (window.marked.parse || window.marked);
    const dirty = typeof parse === "function" ? parse(markdown || "") : escapeHtml(markdown || "");
    const clean = window.DOMPurify
      ? window.DOMPurify.sanitize(dirty, {
          ADD_TAGS: ["figure", "figcaption"],
          ADD_ATTR: ["id", "class"],
        })
      : dirty;
    document.getElementById("content").innerHTML = clean;
    applyHeadingIds(Array.isArray(toc) ? toc : []);
    if (window.mermaid && window.mermaid.run) {
      try {
        await window.mermaid.run({ querySelector: "#content .mermaid" });
      } catch (error) {
        console.error(error);
      }
    }
  };

  window.scrollToHeading = function (id) {
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
  };
})();
