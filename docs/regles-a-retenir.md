# Règles à retenir

Les règles apprises en travaillant sur ce projet (Spring Boot 4, Spring Data JPA, Hibernate 7,
MapStruct, Spring Security 7). Chaque règle : **la règle**, un **exemple**, et **la preuve**
(le test ou le commit qui la montre). Document enrichi au fil des leçons.

Créé le 17-10-2026.

---

## 1. MapStruct et DTO

### R1 — Dans le sens DTO → entité, ignorer ce qui appartient à la base

`id`, `version`, les dates et les relations sont décidés par Hibernate et la base, jamais par le client.

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "version", ignore = true)
@Mapping(target = "createdDate", ignore = true)
@Mapping(target = "updateDate", ignore = true)
Beer beerDtoToBeer(BeerDTO dto);
```

Sans `ignore` sur `id`, un `POST` avec `{"id": "9c0d…", …}` recopie l'id du DTO client
(`beerDto.id`) dans la nouvelle entité (`newBeer.id`). Hibernate voit une bière avec un id mais
sans version et refuse : **500** au lieu de **201**.

**Preuve :** commit `d82e78d4`.

### R2 — Dans le sens entité → DTO, tout renvoyer

Aucun `ignore` sur la méthode qui fabrique la réponse : le client a besoin de l'`id` (pour
retrouver la ressource) et de la `version` (pour le verrouillage optimiste).

Avec `ignore` sur `id`, un `POST` répondait `Location: /api/v1/customer/null`.

**Preuve :** commentaire du commit `ebab5ee8`.

---

## 2. PUT et PATCH

### R3 — PATCH ne copie que ce qui est envoyé, PUT remplace tout

| | PATCH | PUT |
|---|---|---|
| Signification | « corrige seulement ce que j'envoie » | « voici la fiche complète, remplace l'ancienne » |
| Champ absent (`null`) | ne touche pas la valeur existante | la valeur devient `null` |
| MapStruct | `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` | sans `IGNORE` (comportement par défaut) |

Exemple : un PUT sans `quantityOnHand` doit vider la quantité ; avec la méthode du PATCH,
elle garderait l'ancienne valeur (122).

**Preuves :** `updateExistingBeerWithoutQuantityClearsIt` (commit `108e5932`).

### R4 — Un texte vide `""` n'est pas `null`

`IGNORE` ne saute que `null`. Un nom vide est donc **copié**. Seule une contrainte comme
`@NotBlank` sur l'entité le refuse.

| PATCH `{"name": ""}` | Résultat |
|---|---|
| avec `@NotBlank` | refusé, **400**, le nom est gardé |
| sans `@NotBlank` | **le nom est effacé**, et l'API répond quand même 204 |

**Preuves :** `testPatchBeerBlankName`, `testPatchCustomerBlankNameIsRejected` (commits `0fdf1b0f`, `c6ed24ff`).

---

## 3. Hibernate et transactions

### R5 — Certaines choses n'arrivent qu'au moment où Hibernate écrit en base

L'écriture réelle (le « flush ») a lieu au commit de la transaction, ou quand on appelle `flush()`.
Seulement à ce moment-là :

- `@UpdateTimestamp` remplit la date ;
- la validation des entités (`@NotBlank`, `@Size`, `@NotNull`…) est vérifiée ;
- la base signale ses propres erreurs (clé en double, colonne trop courte…).

### R6 — Dans une transaction, `findById` lit la mémoire d'Hibernate, pas la base

Si l'objet est déjà connu dans la transaction (par exemple, on vient de le créer), `findById`
le renvoie directement, sans requête SQL. On voit alors son état **en mémoire**, pas en base.

### R7 — L'`id` et la `version` appartiennent à Hibernate

Ne jamais les mettre à la main. Exemple : `.version(1)` sur un client neuf fait croire à
Spring Data qu'il existe déjà (`merge` au lieu de `persist`). Ça marchait par chance.

**Preuve :** commentaire du commit `46defee3`.

### R8 — `@Column(updatable = false)` protège la date de création

`@CreationTimestamp` remplit la date une seule fois, mais n'empêche pas un code de la modifier
ensuite. `updatable = false` exclut la colonne de tout `UPDATE` : la base garde la vraie date.

---

## 4. Tests

### R9 — `@Transactional`, `flush()` ou ni l'un ni l'autre ?

Une seule question : **« ce que je vérifie arrive-t-il au moment de l'écriture en base ? »** (voir R5)

| Réponse | À faire | Exemple |
|---|---|---|
| Non (je lis, je compte, j'appelle une méthode) | `@Transactional` normal | `testListAll`, `deleteByIdFound` |
| Oui, et l'écriture doit **réussir** | `@Transactional` **+ `flush()`** avant de vérifier | `saveNewCustomerGetsCreatedAndUpdateDate` |
| Oui, et l'écriture doit **échouer** | **pas** de `@Transactional` : le vrai commit doit tourner pour lever l'erreur | `testPatchCustomerBlankNameIsRejected`, `testPatchBeerBadName` |

Pourquoi : un test `@Transactional` tourne dans **une** transaction qui est **annulée** à la fin,
jamais commitée. Sans `flush()`, l'écriture réelle n'a donc jamais lieu.

Sans `@Transactional`, la base ne reste propre **que si l'écriture échoue**. Un test dont
l'écriture réussit laisserait ses données en base.

**Signe d'alerte :** un test qui attend une exception de validation et ne la reçoit jamais,
ou qui reste vert même quand on retire la contrainte.

### R10 — Un test doit prouver quelque chose : le faire échouer une fois

Un nouveau test doit être **rouge** sur l'ancien code (ou sans la correction) et **vert** avec.
Sinon, il ne prouve rien.

Exemples : `BeerTest.testRemoveCategoryUnlinksBothSides` (rouge sur la ligne de JT),
`convertCSVKeepsAccentedCharacters` (rouge sur le CSV cassé).

### R11 — `@WebMvcTest` ne charge que la couche web

- Une classe `@Configuration` (comme `SpringSecConfig`) doit être importée : `@Import(SpringSecConfig.class)`.
- Avec Spring Boot 4, Spring Security n'entre dans `@WebMvcTest` qu'avec `spring-boot-starter-security-test`
  (`spring-security-test` seul n'apporte que les outils comme `httpBasic(...)`).

---

## 5. Spring Security

### R12 — Une chaîne de filtres personnalisée remplace celle de Spring Boot

Dès qu'on déclare sa propre `SecurityFilterChain`, celle de Boot disparaît, avec ses règles.
Il faut tout redéclarer :

| Élément | Si oublié |
|---|---|
| `@Bean` sur la méthode | la chaîne n'est jamais utilisée |
| `authorizeHttpRequests(... anyRequest().authenticated())` | **toute l'API est ouverte** |
| `httpBasic(Customizer.withDefaults())` | personne ne lit les identifiants → 401 |

**Preuve :** commentaire du commit `a06a6a49`.

### R13 — 401 ou 403 ?

- **401** : « qui es-tu ? » → pas d'identifiants, ou faux identifiants.
- **403** avec les bons identifiants : « je sais qui tu es, mais je refuse » → souvent la protection CSRF.

### R14 — `user(...)` ou `httpBasic(...)` dans un test MockMvc

`user(...)` crée un utilisateur fictif sans rien vérifier ; `httpBasic(...)` envoie de vrais
identifiants et passe par le vrai login. Détails : `docs/spring-security-test-user-vs-httpbasic.md`.

### R15 — Désactiver CSRF seulement pour l'API, et seulement pour des clients non-navigateurs

CSRF protège les navigateurs qui envoient automatiquement leurs cookies. Pour une API REST utilisée
par des programmes (Postman, autres applications), on peut l'ignorer, mais **seulement** pour `/api/**`.

### R16 — Ne jamais mettre de mot de passe en clair dans Git

Le lire depuis `.env` (ignoré par Git) ou une variable d'environnement :
`spring.security.user.password=${SPRING_SECURITY_USER_PASSWORD}`. Une valeur publiée une fois
reste dans l'historique : il faut alors la changer.

---

## 6. Entités

### R17 — `equals` / `hashCode` : l'identité d'`Object` suffit souvent avec Hibernate

- `super.equals(o)` dans une classe sans `extends` appelle `Object.equals` : « même objet ? ».
  Toute comparaison après cette ligne ne sert à rien.
- Un `hashCode` basé sur un champ modifiable (comme une description) perd l'objet dans un `HashSet`
  si le champ change après l'ajout.
- Dans une même transaction, Hibernate renvoie le même objet Java pour la même ligne :
  l'identité suffit.

### R18 — Une méthode d'aide bidirectionnelle met à jour les deux côtés

`removeCategory` doit retirer la catégorie de la bière **et** la bière de la catégorie
(`category.getBeers().remove(this)`, pas `remove(category)`).

**Preuve :** `BeerTest.testRemoveCategoryUnlinksBothSides` (commits `832ab596`, `116b217e`).

---

## 7. Fichiers et encodage

### R19 — Lire un fichier du classpath en flux, et toujours en UTF-8

- `resource.getFile()` échoue dans un JAR ; `resource.getInputStream()` marche partout.
- Sans encodage explicite, Java prend celui du système (cp1252 sous Windows) et casse les accents :
  `new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)`.

**Preuves :** commit `1fd507b1`, test `convertCSVKeepsAccentedCharacters`.

---

## 8. Java : lambdas et threads

### R20 — `AtomicReference` sert aux threads ; pour sortir une valeur d'un lambda, ce n'est qu'une boîte

**La règle des lambdas :** une variable déclarée en dehors d'un lambda et utilisée dedans ne doit plus
jamais changer (« effectively final »). Sinon, le compilateur refuse :

```
error: local variables referenced from a lambda expression must be final or effectively final
```

**L'astuce de la boîte (le code de JT) :** on ne change pas la variable, on change son **contenu**.
La variable désigne toujours la même boîte, donc Java accepte :

```java
AtomicReference<Optional<BeerDTO>> atomicReference = new AtomicReference<>();
beerRepository.findById(beerId).ifPresentOrElse(
        foundBeer -> atomicReference.set(Optional.of(...)),   // on remplit la boîte
        () -> atomicReference.set(Optional.empty()));
return atomicReference.get();                                 // on vide la boîte
```

**L'usage prévu : plusieurs threads sur la même valeur.** `count++` se fait en 3 temps (lire, ajouter,
écrire) ; deux threads peuvent se croiser au milieu et écraser l'addition de l'autre.
`incrementAndGet()` fait les 3 temps d'un seul coup indivisible (« atomique »).

Deux threads, chacun 1 million d'additions sur le même compteur (attendu : 2 000 000) :

| Essai | `int` avec `count++` | `AtomicInteger` avec `incrementAndGet()` |
|---|---|---|
| 1 | 1 299 226 ❌ | 2 000 000 ✅ |
| 2 | 1 304 833 ❌ | 2 000 000 ✅ |
| 3 | 1 377 340 ❌ | 2 000 000 ✅ |

`AtomicReference` fait la même chose pour un objet : `compareAndSet(ancien, nouveau)` ne remplace
que si personne ne l'a modifié entre-temps.

**Dans nos services :** la boîte est créée pour une seule requête et un seul thread s'en sert.
L'atomicité ne sert à rien : c'est un détour, et `Optional` permet de s'en passer.

**Preuve :** petit programme de démonstration lancé le 17-10-2026 (deux threads, résultats ci-dessus).
