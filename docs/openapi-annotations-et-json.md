# OpenAPI : des annotations Java au JSON de `/v3/api-docs`

Créé le 03-11-2026 (Section 40, Chap375, tickets #27 et #31).
Les exemples renvoient à la branche `127-springdoc-security-cfg-jt-sb412`, commit `40d75bbd` (instantané : le code a pu changer depuis).

## Le contexte

springdoc lit les annotations Java et produit **un document JSON** : `/v3/api-docs`
(`http://localhost:8080` avec le profil par défaut H2, `http://localhost:8081` avec le profil `localmysql`).
**Swagger UI ne voit pas le code Java** : il dessine sa page (titre, endpoints, bouton « Authorize », cadenas)
uniquement à partir de ce JSON. Pour comprendre ce qu'affiche Swagger UI, ou pourquoi il n'affiche pas
quelque chose, il faut donc savoir **quelle annotation produit quelle partie du JSON**.

Exemple dans le projet (`40d75bbd`) : `src/main/java/guru/springframework/spring7restmvc/config/OpenApiConfig.java:19-22`.

## La table de correspondance

| Java (`OpenApiConfig.java`) | JSON (`/v3/api-docs`) | Ce que Swagger UI en fait | Si absent |
|---|---|---|---|
| `@OpenAPIDefinition(info = @Info(title = "Beer and Customer API", version = "v1"))` | `info: { title: "Beer and Customer API", version: "v1" }` | le titre et le badge de version en haut de la page | document **invalide** : `info` est obligatoire (#31) |
| `@SecurityScheme(name = "bearerAuth", type = HTTP, scheme = "bearer", bearerFormat = "JWT")` | `components.securitySchemes.bearerAuth: { type: http, scheme: bearer, bearerFormat: JWT }` | **le schéma** : le bouton « Authorize » et sa fenêtre `bearerAuth (http, Bearer)` | pas de bouton |
| `@OpenAPIDefinition(security = ...)` | `security:` | **la consigne** : un cadenas sur chaque endpoint | - |
| `@SecurityRequirement(name = "bearerAuth")` | `0: { bearerAuth: [] }` | joindre `Authorization: Bearer <token>` à chaque « Try it out » | bouton présent, mais token jamais envoyé : **401** |

Détails :
- `name = "bearerAuth"` n'est qu'une **étiquette** : c'est elle qui relie la consigne (`security`) au schéma (`securitySchemes`). Les deux doivent porter le même nom.
- `type = HTTP` + `scheme = "bearer"` disent **comment** écrire l'en-tête : `Authorization: Bearer ...`. `bearerFormat = "JWT"` n'est qu'une information pour le lecteur.
- `bearerAuth: []` : la liste vide est la liste des *scopes* exigés ; avec un schéma `http`, il n'y en a pas.

## Comment vérifier

1. **À la main :** ouvrir `/v3/api-docs` dans Firefox (onglet JSON) → **« Collapse All »** → lire les clés de premier niveau.
   Attendu : `openapi`, `info`, `servers`, `security`, `paths`, `components`.
2. **Par un test :** `jsonPath` lit le même JSON - `$` = la racine, `.clé` = une clé, `[0]` = le premier élément d'une liste.
   Exemple dans le projet (`40d75bbd`) : `src/test/java/guru/springframework/spring7restmvc/config/SpringSecConfigIT.java:31-33` (`info`)
   et `:41-44` (le schéma : 3 valeurs vérifiées avec `.value(...)`, la consigne : présence seulement, avec `.exists()`).

## Tout ne vient pas des annotations (#28, #42)

Une partie du JSON peut aussi être écrite **par du code**, dans un bean `OpenApiCustomizer` : springdoc lui passe
le document déjà construit, et le bean le modifie. C'est ce qui ajoute la version du build dans
`info.x-build-version` (#28) et dans `info.description`, que Swagger UI affiche sous le titre (#42).
Donc, quand une valeur du JSON ne correspond à aucune annotation, la chercher dans ce bean.
Exemple dans le projet : `src/main/java/guru/springframework/spring7restmvc/config/OpenApiConfig.java`
(branche `127-springdoc-security-cfg-jt-sb412`, commit `8c364575`).

## Le piège (#31)

Dès qu'une classe porte `@OpenAPIDefinition`, springdoc la prend comme description de l'API et **ne remplit plus**
son `info` par défaut (`"OpenAPI definition"` / `"v0"`). Toujours y mettre `info = @Info(title = ..., version = ...)`.
**Leçon :** après un changement d'annotation OpenAPI, lire les clés du JSON (étape 1 ci-dessus), pas seulement la page Swagger UI.
