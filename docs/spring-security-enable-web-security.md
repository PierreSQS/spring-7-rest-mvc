# `@EnableWebSecurity` - what it does, when you need it, and `debug = true`

Section 22, Assignment 24. Checked against `spring-boot-security-4.0.6`,
`spring-security-config-7.0.5` and `spring-security-web-7.0.5`.

## What it does

`@EnableWebSecurity` switches Spring Security on in a web application. It sets up:

- the **filter chain** (`springSecurityFilterChain`) every HTTP request goes through
  before it reaches a controller;
- the **`HttpSecurity`** object Spring passes to your
  `securityFilterChain(HttpSecurity httpSecurity)` method.

## Do you need it?

| Project | `@EnableWebSecurity` needed? |
|---|---|
| Plain Spring (no Boot) | **yes**, otherwise Spring Security does not start |
| Spring Boot (this project) | **no**, Boot adds it for you |

Boot does it in `ServletWebSecurityAutoConfiguration.EnableWebSecurityConfiguration`,
which turns `@EnableWebSecurity` on **only if**:

- Spring Security is on the classpath, and
- there is no `springSecurityFilterChain` bean yet (nobody switched it on already).

So with or without the annotation, the result is the same. JT adds it in his config
(branch `114-security-complete-mvctests`); in our `SpringSecConfig` it is not needed,
and the config already worked without it in Chapter 229.

## The really useful variant: `debug = true`

```java
@EnableWebSecurity(debug = true)   // debugging only - remove afterwards
@Configuration
public class SpringSecConfig { ... }
```

It adds Spring Security's `DebugFilter` in front of the chain. For **each request** the
log then shows:

- `Request received for ...`: method, URL, servlet path and the request headers
  (including `Authorization` - so it can print credentials);
- `Security filter chain: [...]`: the list of security filters the request goes through.

This helps to understand a **401** (who are you? - no or wrong credentials) or a
**403** (refused - e.g. CSRF) without stepping through the code.

| | `@EnableWebSecurity(debug = true)` | `logging.level.org.springframework.security=trace` (JT) |
|---|---|---|
| Shows | the request and the list of filters | what each filter does and decides, step by step |
| Amount of output | short, one block per request | very long |
| Where to switch it on | on the configuration class (code) | in `application.properties` |

**Debugging only:** Spring Security itself logs this warning at startup when debug is on,
because the output may contain sensitive data (headers, credentials, session ids):

```
********************************************************************
**********        Security debugging is enabled.       *************
**********    This may include sensitive information.  *************
**********      Do not use in a production system!     *************
********************************************************************
```

Never leave it on in a real application.
