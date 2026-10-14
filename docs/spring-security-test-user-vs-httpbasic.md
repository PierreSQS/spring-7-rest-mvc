# `user(...)` vs `httpBasic(...)` in a MockMvc test

Section 22, Chapter 228. Both come from `SecurityMockMvcRequestPostProcessors`
(library `spring-security-test`) and are added to a request with `.with(...)`.

## `user(...)` - a mock user, nothing is checked

```java
mockMvc.perform(get(BeerController.BEER_PATH)
        .with(user("user").password("password").roles("USER")))
```

- It **skips the login**: an already-authenticated user is put straight into
  Spring Security for this one request.
- **Nothing is checked**: the user does not have to exist, the password is ignored.
- Same as the annotation `@WithMockUser`, but per request.

## `httpBasic(...)` - real credentials, real login

```java
mockMvc.perform(get(BeerController.BEER_PATH)
        .with(httpBasic("user", password)))
```

- It sends an `Authorization` header, like `curl -u user:password`.
- Spring Security's **real login** runs and checks the credentials against the
  configured user (`spring.security.user.*`, password from `.env`).
- A wrong password gives **401**, as in real life.

## Small example

| | `user("toto").password("wrong")` | `httpBasic("toto", "wrong")` |
|---|---|---|
| Result | 200, passes | **401**, refused |

## When to use which

- **`user(...)`**: to test what happens *after* the login - controller logic, roles
  ("an admin may delete, a normal user may not").
- **`httpBasic(...)`**: to also test the login itself with the real configuration.

This lesson uses `httpBasic(...)`, with the password read from the property
`spring.security.user.password` instead of being hard-coded:

```java
@Value("${spring.security.user.password}")
String password;
```
