# JT's branches in Section 22 (Spring Security) - what to watch out for

Checked on 16-10-2026 against JT's upstream branches (`JT-Origin/...`).

## Which branch belongs to which lesson

| Lesson | JT's branch | Our branch |
|---|---|---|
| Chapter 225 | `110-spring-sec-maven-deps` | `110-spring-sec-maven-deps-jt-sb411` |
| Chapters 227 + 228 | `111-security-set-user-pwd`, `112-security-test-auth` (same code) | `112-security-test-auth-jt-sb411` |
| Chapter 229 | `113-security-config` | `113-security-config-jt-sb411` |
| Assignment 24 | `114-security-complete-mvctests` | local only, renamed |
| Chapter 230 | `115-security-add-context-wac` | local only, renamed |
| Assignment 25 | `116-security-fix-remaining-tests` | `116-security-fix-remaining-tests-jt-sb411` |

## Confusion 1: two branches numbered 115

- `115-security-add-context-wac` is the right one (Chapter 230).
- `115-security-fix-remaining-tests` belongs to JT's **old Spring 6 project**
  (`spring6restmvc` packages, different `pom.xml`, `mvnw`, `compose.yaml`...).
  It is a leftover - **ignore it**.

## Confusion 2: `114` and `115` contain exactly the same code

`114-security-complete-mvctests` and `115-security-add-context-wac` have the same Git tree.
From `115` to `116` only `BeerControllerIT` changes.

## Confusion 3: the video of Chapter 230 and JT's repository disagree

In the video of Chapter 230, JT notices that his `SecurityFilterChain` from Chapter 229
replaced Spring Boot's default chain without `authenticated()` and `httpBasic()`, so the
tests passed even without credentials. He fixes it on screen and adds
`apply(springSecurity())` to `BeerControllerIT`. **None of this was committed** to his
branches:

| JT's branch | `authenticated()` / `httpBasic()` in `SpringSecConfig`? | `apply(springSecurity())` in `BeerControllerIT`? |
|---|---|---|
| `115-security-add-context-wac` (Chapter 230) | no | no |
| `116-security-fix-remaining-tests` (Assignment 25) | no | no |
| `117-resource-server-maven-dep` | no | no |
| `118-resource-server-sec-config` | `authenticated()` only, with OAuth2 instead of HTTP Basic | no |

Our code follows the video, and was ahead of it: `authenticated()` and `httpBasic()` were
already in our `SpringSecConfig` in Chapter 229.

## Why we skip `apply(springSecurity())`

JT builds `MockMvc` **by hand** in `BeerControllerIT`
(`MockMvcBuilders.webAppContextSetup(wac).build()`). A hand-built `MockMvc` does not
include the Spring Security filters, so he has to add them with `apply(springSecurity())`.

Our `BeerControllerIT` uses `@AutoConfigureMockMvc` (since commit `877c0e64`), which
already adds the security filters - that is why our IT tests got 401 instead of passing.
So Chapter 230 brings nothing new for us; we go straight to Assignment 25.
