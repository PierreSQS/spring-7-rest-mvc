---
name: project-documentation
description: Rules for writing or changing a documentation Markdown file of the spring-7-rest-mvc project - Pierrot's notes on any topic (Spring, Postman, Codex, ...), on any branch - docs/*.md, branch notes (README.md index, regles-a-retenir.md, lesson reviews docs/secNN-*-improvements.md). Use before creating, extending or rewriting such a file. Not for chat answers, GitHub issues, commit messages or comments, Postman collection descriptions, CLAUDE.md, AGENTS.md or .claude/rules/*.md.
---

# Project documentation

Pierrot reads these notes months later to revise. He must understand **what changed and why**
without the conversation, without opening the code, and without being overwhelmed.

## 1. Language

- Write the document in **French**.
- Keep code names exactly as in the code: classes, methods, annotations, properties, branches, commit prefixes.

## 2. Before writing: collect the facts

Never invent a branch, chapter, commit, line of code or behavior. Check each one:

1. **Branch:** only a branch that is pushed (`git branch -r`), with its full name. Never a local or provisional name.
2. **Commit:** the SHA of the commit that made the change (`git log`, `git log -S'<text>'`). Section and chapter come from its prefix (`Sec16_Chap166-X: ...`).
3. **Code:** the real code before and after, from Git (`git show <sha>`, `git show <sha>^:<path>`). Never rebuild a "before" from memory.

If a fact cannot be found, write it plainly: `Commit : introuvable`.

## 3. Structure of the document

### 3.1 Header (always)

```markdown
# <Sujet clair>

**Branche :** `<full pushed branch name>` · **Section / Chapitre :** SecNN_ChapNNN · **Commit(s) :** [`<sha>`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/<sha>)

**Contexte :** <2-4 sentences: what the lesson or the topic is about, and why this document exists.>
```

### 3.2 One block per change (always, in this order)

Number the changes 1, 2, 3... in the order of **this** document. Never reuse a number from another
document (e.g. the row number of an older table): a lone "6." confuses the reader.
Sub-points inside a change are lettered **a, b, c...** (`- **a.** ...`, `- **b.** ...`), never a new 1, 2, 3
that could be mistaken for the changes themselves.

Every change that was **made** gets exactly these three parts:

1. **Le problème** - what was wrong or missing, in 1-3 sentences. Name the file and the class first
   (`src/test/java/.../MySqlIT.java`, class `MySqlIT`), then the problem.
2. **Ce qui a changé** - the commit link, then the before/after (section 4).
3. **Pourquoi** - why this solution, what it brings, in 1-3 sentences.

Template:

```markdown
### <N>. <Titre court du changement>

**Le problème :** ...

**Ce qui a changé** ([`<sha>`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/<sha>), SecNN_ChapNNN) :

<before/after, section 4>

**Pourquoi :** ...
```

### 3.3 Only when it really helps

Add these only when they make the point easier to understand, never to fill the page:

- **A diagram** (Mermaid): for a flow between several components (e.g. a CORS preflight), not for an annotation change.
  Use the real class and method names.
- **An analogy:** one or two sentences from everyday life, technically correct.
- **Un piège:** a verified trap: what happens, why, how to avoid it. Never a speculative one.

### 3.4 A "how to" document (steps to redo alone)

When the document explains how to do something (install, configure, use a tool), not a code change:

1. first **the steps**, short and numbered: what to do, then the expected result;
2. then, separate and below, a part **`## Pour comprendre`** with the details (failed tries, internals, commits),
   read only if needed.

### 3.5 Ideas not done

Do **not** list ideas that were not implemented: they live as GitHub issues
(`PierreSQS/spring-7-rest-mvc`), not in the notes. A document describes only what was done.

## 4. Before / after

**Default: side by side** with an HTML table, one ```` ```diff ```` block per column, so the differences are in colour:

- column **Avant**: removed lines start with `-` (red);
- column **Après**: added lines start with `+` (green);
- unchanged lines start with a space.

Each code block is surrounded by blank lines, otherwise it is not rendered:

````markdown
<table>
<tr><th>Avant</th><th>Après</th></tr>
<tr>
<td>

```diff
 unchanged line
-removed line
```

</td>
<td>

```diff
 unchanged line
+added line
```

</td>
</tr>
</table>
````

- **Keep only the essential lines.** Mark each cut with `// ...`. The full diff is one click away through the commit link.
- **Long code** (does not fit in half the width): two blocks one under the other, `**Avant**` then `**Après**`.
- **A change of one or two lines:** a ```` ```diff ```` block is enough.

## 5. Writing style

- **Main path first.** Side facts only if they help to understand.
- **Short.** One idea per paragraph. If a section grows, cut, do not add.
- **Explain every technical term** the first time it appears, in a few words (e.g. *singleton*: one single container for the whole test run).
- **Name every thing in full:** the file path, "la branche `notes`", "le dépôt Git", the exact collection name.
  Never "le dossier", "l'original", "le fichier ci-dessus". Never a bare "l'application": write
  "l'application Postman" or "l'application Spring Boot spring-7-rest-mvc".
- **Define every variable or letter** you introduce (`x`, `f`, ...): say what it stands for, on which example.
- **Every abstract sentence gets a concrete example right after it.** Show how two parts relate instead of placing them side by side.
- **Say whether an example is invented or taken from the project.**
- **Label honestly:** `Exemple dans le projet : <full path>` for an illustration; `Preuve :` only for a test or a commit
  that proves the rule.
- **No reference to a conversation** ("comme dit plus haut", "comme vu ensemble"): the document must stand alone.

## 6. Where documents go

- **Does not depend on the code of one branch** -> branch `notes`, folder `docs/`.
- **Describes the code of one branch** -> that branch.
- **Every new document on `notes` gets its line in the root `README.md` of `notes`, in the same commit.**
- **Lesson reviews** (`docs/secNN[-chapNNN]-<topic>-improvements.md`, on `notes`) are snapshots: they name the
  branch(es) and commit(s) they describe and are **never updated** afterwards.
- Links to code on lesson branches use a commit SHA, not a branch name, so they keep pointing to the same code.

## 7. Before handing over

Check:

- [ ] every change has its three parts (problème, ce qui a changé, pourquoi) and its commit link;
- [ ] every code block comes from Git, cuts are marked `// ...`;
- [ ] every branch cited is pushed and written in full;
- [ ] no unexplained technical term or variable, no "le dossier", every abstract sentence has its example;
- [ ] no list of ideas not done (they are GitHub issues);
- [ ] new document on `notes` -> line in `README.md`.

Show the document to Pierrot before any commit.
