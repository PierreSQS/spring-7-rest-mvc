# Working with Pierrot

This is Pierrot's learning laboratory, based on John Thompson's Spring Framework
course: a beer and customer REST API. Claude Code and Codex assist him. Lesson
branches, commits and comments are revision material.

Architecture: controller → service → repository → entity, with DTOs and MapStruct
mappers. Java, Spring Boot, MVC, Security/JWT, JPA/Hibernate, Flyway, H2 and MySQL
are described in detail in the current branch's CLAUDE.md.

## Start each session

- Read CLAUDE.md completely, then Codex's external project-notes index at
  `~/.codex/project-notes/spring-7-rest-mvc/INDEX.md` (`~` = Windows user profile).
  If unavailable, report that and consult the repository references below.
- Check branch, HEAD, status and relevant recent changes, including Claude's work.
- Give a short recap and next-step menu, including relevant work across branches
  and open hard-core topics. Let Pierrot choose.

## Working together

Speak French with Pierrot; code and GitHub content use English unless requested.
Explain context first. Commands need directory, prerequisites and expected result.
Training proceeds one step at a time: Pierrot performs exercises and requests
unless he delegates. "Verify" means read his changes and the diff.
Use Git Bash on Windows; distinguish it from WSL. Explain necessary PowerShell use.
Propose useful improvements and explain their benefit before adoption.

## Read the relevant guidance before acting

| Before | Read |
|---|---|
| Training, explanations or session handover | External `collaboration.md` |
| Creating, updating or closing an issue | External `issues.md` and `docs/procedures-tickets-et-ci.md` |
| Commit, amend, push or GitHub commit comment | External `git-and-ci.md` and the actual branch workflow |
| Writing or changing tests | `.claude/rules/testing.md` |
| Database, profiles, Compose or migrations | `.claude/rules/database.md` |
| Course branches or documentation | External `branches-and-docs.md` |
| A learned technical rule | `git show notes:docs/regles-a-retenir.md` |
| Finding project documentation | `git show notes:README.md` |

External filenames refer to the directory containing the project-notes index.
Keep Codex's personal notes and internal files outside the repository, accessible
for consultation. Claude's external memory is read-only. Shared technical guidance
stays in its existing reference file. Read notes with git show; edit that branch
through a separate worktree. Preserve unrelated changes, local settings and secrets.

## Always apply

- A commit without an issue is normal; propose issues when useful.
- Announce Git operations and show proposed messages and full comments. Approval
  covers exactly the announced operations; Codex performs them after approval.
- Before every push, read that branch's `.github/workflows/build.yml`, compare its
  triggers and filters with the files being pushed, and announce whether CI will run.
  Companion branches may differ: -postman also ignores `postman/**`. If expected,
  monitor the exact commit's run to completion and report its result.
- At session end, recap issues created, advanced and closed, decisions, checks,
  published commits/comments and the next concrete steps.
