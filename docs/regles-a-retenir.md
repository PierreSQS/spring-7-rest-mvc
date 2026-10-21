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

### R22 — Une entité lue est surveillée : la modifier, c'est l'écrire en base

Dans une transaction, Hibernate garde une copie de chaque entité qu'il a lue. Au `flush()` (commit
ou appel explicite), il compare l'entité à sa copie et écrit **tout seul** un `UPDATE` pour chaque
différence (« dirty checking »). Pas besoin de `save()`.

```java
Beer beer = beerRepository.findById(id).get();   // stock = 122 : Hibernate garde la copie
beer.setQuantityOnHand(null);                    // l'entité diffère de sa copie
// au flush : UPDATE beer SET quantity_on_hand = NULL   ← le stock est effacé en base
```

**Donc :** pour changer ce qu'on **affiche** (cacher un champ, le formater…), on modifie le **DTO**,
jamais l'entité. Et le DTO modifié doit être **celui qu'on renvoie**.

**Dans un test** `@Transactional`, le commit n'arrive jamais (rollback). Pour voir ce qu'Hibernate
aurait écrit : `flush()`, puis `clear()` (sinon `findById` relit la mémoire, voir R6), puis relire
depuis la base. Voir aussi R5.

**Preuve :** commit `8f187948` (`listBeersWithoutInventoryKeepsTheStockInTheDatabase` : rouge quand
le stock était caché sur l'entité, vert sur le DTO).

### R23 — Une validation refusée au commit arrive emballée : `getRootCause()` retrouve le champ et le message

**Le contexte :** un PATCH n'a pas de `@Validated` dans le contrôleur (un PATCH peut être partiel, R3).
Une valeur invalide passe donc le contrôleur ; elle n'est refusée que par les annotations de l'**entité**
(`@Size`, `@NotBlank`…), au moment où Hibernate écrit en base, c'est-à-dire au commit (R5).
Le client doit quand même savoir **quel champ** est faux et **pourquoi**.

**Exemple :** un PATCH avec un nom de bière de plus de 50 caractères.

```http
PATCH /api/v1/beer/9c0d…
{ "beerName": "New Name 12345678901234567890123456789012345678901234567890…" }
```

Réponse voulue :

```json
400
[ { "beerName": "Beer name must be at most 50 characters" } ]
```

Le nom est refusé par l'entité `Beer` :

```java
@Size(max = 50, message = "Beer name must be at most 50 characters")
private String beerName;
```

**Ce qui se passe au commit :** l'erreur traverse trois couches. Chacune emballe l'exception de la couche
du dessous et la garde comme **cause** (`getCause()`).

| Couche | Lance | Sa cause (`getCause()`) |
|---|---|---|
| 1. Hibernate Validator | `ConstraintViolationException` | aucune : c'est elle qui contient le champ (`beerName`) et le message |
| 2. JPA (Hibernate) | `jakarta.persistence.RollbackException` | la `ConstraintViolationException` |
| 3. Spring (`JpaTransactionManager`) | `TransactionSystemException` | la `RollbackException` |

Ce qui arrive dans le `@ControllerAdvice`, c'est la couche **extérieure** (3). Les informations utiles
sont **tout au fond** (1).

**La règle :** attraper la `TransactionSystemException`, puis descendre à la cause la plus profonde avec
`getRootCause()`, quel que soit le nombre de couches.

```java
@ExceptionHandler
ResponseEntity<List<Map<String, String>>> handleJPAViolations(TransactionSystemException exception) {
    // getRootCause() : la cause la plus profonde ; instanceof vérifie son type ET la met dans violationException
    if (exception.getRootCause() instanceof ConstraintViolationException violationException) {
        List<Map<String, String>> errorList = violationException.getConstraintViolations().stream()
                .map(violation -> Map.of(violation.getPropertyPath().toString(),   // le champ : "beerName"
                                         violation.getMessage()))                  // le message de @Size
                .toList();                                                         // [ {beerName=…} ]
        return ResponseEntity.badRequest().body(errorList);
    }
    return ResponseEntity.badRequest().build();   // autre cause (base injoignable…) : 400 sans corps
}
```

| Appel | Rend |
|---|---|
| `exception` | `TransactionSystemException` (couche 3) |
| `exception.getCause()` | `RollbackException` (couche 2) : pas encore ce qu'on cherche |
| `exception.getRootCause()` | `ConstraintViolationException` (couche 1) |

Pourquoi `getRootCause()` plutôt que `getCause().getCause()` : on n'a pas à compter les couches. Si une
version de Spring ou de Hibernate en ajoute ou en retire une, le code marche toujours.

**Même réponse, quelle que soit la couche qui refuse :**

| Requête | Qui refuse | Exception reçue | Handler | Réponse |
|---|---|---|---|---|
| POST / PUT, nom trop long | `@Validated`, dans le contrôleur | `MethodArgumentNotValidException` | `handleBindErrors` | `400` + `[{beerName: …}]` |
| PATCH, nom trop long | `@Size` sur l'entité, au commit | `TransactionSystemException` → `getRootCause()` | `handleJPAViolations` | `400` + `[{beerName: …}]` |

**Exemple dans le projet :** `src/main/java/guru/springframework/spring7restmvc/controller/CustomErrorController.java:24`

**Preuve :** `BeerControllerIT.testPatchBeerBadName`, qui vérifie `$[0].beerName` (commit `7ea665b3`).
Couche 3 vérifiée dans `spring-orm-7.0.9.jar` : `JpaTransactionManager.doCommit` attrape la
`RollbackException` et lance `new TransactionSystemException("Could not commit JPA transaction", ex)`.

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
- Le resource server (JWT), lui, entre dans `@WebMvcTest` grâce au starter **principal**
  `spring-boot-starter-security-oauth2-resource-server` : son jar s'inscrit lui-même dans la liste du slice.
  Avec `spring-boot-starter-security-test` (qui apporte `jwt()`), le starter `-test` du resource server
  n'apporte donc rien de plus.
- **Un `${...}` introuvable ne fait pas planter `@WebMvcTest`.** Le contrôle strict des propriétés
  (`PropertyPlaceholderAutoConfiguration`) n'est pas dans la liste du slice : `@Value` garde alors le texte brut.

  ```java
  @Value("${spring.security.user.name}")   // la propriété n'existe plus
  String username;
  ```

  | Test | Contrôle strict ? | Résultat |
  |---|---|---|
  | `@SpringBootTest` (toute l'appli) | oui | le contexte ne démarre pas : `Could not resolve placeholder 'spring.security.user.name'` |
  | `@WebMvcTest` (le slice) | non | `username = "${spring.security.user.name}"` (le texte lui-même), le test tourne et échoue plus loin (ici 401/403) |

  Piège : un slice peut donc rouler avec une valeur fausse sans rien signaler.

**Preuve :** commit `d3f1c182` (Chap250) : `BeerControllerIT` ne démarre pas, `BeerControllerTest` envoie
`Authorization: Basic` = `${spring.security.user.name}:${spring.security.user.password}` (en-tête décodé) ;
listes du slice vérifiées dans les jars Spring Boot 4.1.1.

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
| `oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))` (API protégée par JWT) | personne ne lit le jeton → 401 ; et **sans `.jwt(...)`, l'appli ne démarre pas** |

**Preuve :** commentaires des commits `a06a6a49` et `db7d94e6`.

### R13 — 401 ou 403 ?

- **401** : « qui es-tu ? » → pas d'identifiants, ou faux identifiants.
- **403** avec les bons identifiants : « je sais qui tu es, mais je refuse » → souvent la protection CSRF.

### R14 — `user(...)` ou `httpBasic(...)` dans un test MockMvc

`user(...)` crée un utilisateur fictif sans rien vérifier ; `httpBasic(...)` envoie de vrais
identifiants et passe par le vrai login. Détails : `docs/spring-security-test-user-vs-httpbasic.md`.

### R15 — Désactiver CSRF seulement pour l'API, et seulement pour des clients non-navigateurs

CSRF protège les navigateurs qui envoient automatiquement leurs cookies. Pour une API REST utilisée
par des programmes (Postman, autres applications), on peut l'ignorer, mais **seulement** pour `/api/**`.

**Avec un serveur de ressources (JWT), cette ligne devient inutile :** Spring Security ignore
déjà le CSRF pour toute requête qui porte un en-tête `Authorization: Bearer ...`
(`OAuth2ResourceServerConfigurer`, vérifié dans le jar 7.1.1). Preuve : commentaire du commit `db7d94e6`.

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

### R21 — `Optional.map` / `flatMap` : transformer la valeur d'un `Optional`, au lieu de la sortir dans une `AtomicReference` (la « boîte » de R20)

**L'idée :** `map` transforme **toujours** un `Optional<A>` en `Optional<B>`, et on le renvoie.

- `Optional<A>` plein → `map` exécute le lambda sur le contenu et met ce que le lambda **renvoie** dans un `Optional<B>` ;
- `Optional<A>` vide → `map` n'exécute **pas** le lambda et rend un `Optional<B>` vide.

**Dans nos services (PUT d'une bière) :** on part d'un `Optional<Beer>` et on renvoie un `Optional<BeerDTO>`.

```java
return beerRepository.findById(beerId)                                    // Optional<Beer> : la bière, ou rien
        .map(foundBeer -> {                                               // ce qui se passe DANS la transformation :
            beerMapper.updateBeerFromDto(beerDTO, foundBeer);             //   appliquer les changements
            return beerMapper.beerToBeerDto(beerRepository.save(foundBeer)); // enregistrer, puis convertir en DTO
        });                                                               // → Optional<BeerDTO> : le DTO, ou rien
```

| `findById` trouve… | `Optional<Beer>` | `map` | `Optional<BeerDTO>` | Réponse |
|---|---|---|---|---|
| la bière | avec la bière | exécute le lambda | avec le DTO de la bière **modifiée** | 204 |
| rien | vide | n'exécute rien | vide | 404 |

**`map` n'est qu'un raccourci pour ce `if / else` :**

```java
Optional<Beer> optionalBeer = beerRepository.findById(beerId);
if (optionalBeer.isPresent()) {
    Beer foundBeer = optionalBeer.get();
    beerMapper.updateBeerFromDto(beerDTO, foundBeer);
    return Optional.of(beerMapper.beerToBeerDto(beerRepository.save(foundBeer)));
}
return Optional.empty();
```

**Pourquoi c'est mieux que la boîte de JT (R20) :** le lambda **renvoie** son résultat (`return`) au lieu
de le déposer dans une `AtomicReference`. Plus de boîte, plus de règle « effectively final » à contourner.

#### Les classes des exemples ci-dessous

`findNickname`, `findOrder`, `findCustomer`, `findAddress`, `findSupplierByUpc`, `Employee`… sont **inventés
pour l'entraînement** (exercices du 21-10-2026). Seul l'exemple de la fin vient du projet.

#### 1. Reconnaître le motif `map` dans du code existant

**Le contexte :** on a un `Optional` qui contient peut-être une valeur, et on veut transformer cette valeur
seulement si elle existe. Le code « à l'ancienne » le fait en trois temps ; `map` le fait en un.

**Exemple :** une méthode rend le surnom d'un utilisateur, s'il en a un. On veut la longueur de ce surnom :
`"Pierrot"` → `Optional[7]` ; pas de surnom → `Optional.empty`.

```java
Optional<String> findNickname(Long userId)   // le surnom, ou un Optional vide
```

```java
// à l'ancienne : trois temps
String nickname = findNickname(userId).orElse(null);   // 1. sortir : le surnom, ou null s'il n'y en a pas
if (nickname == null) {
    return Optional.empty();                           //    pas de surnom → Optional vide
}
return Optional.of(nickname.length());                 // 2. transformer : la longueur
                                                       // 3. remettre : dans un Optional<Integer>

// avec map : la même chose
return findNickname(userId).map(String::length);
```

**La forme générale**, avec deux noms pour les deux morceaux qui changent d'un cas à l'autre :

- **`x`** = l'`Optional` de départ. Ici : `findNickname(userId)`, un `Optional<String>`.
- **`f`** = la transformation à appliquer à la valeur. Ici : `String::length`, qui prend un `String` et rend sa longueur.

| Code à l'ancienne | Avec `map` |
|---|---|
| `Optional.ofNullable(f(x.orElse(null)))` | `x.map(f)` |

**Attention :** la forme à l'ancienne sans `if` ne marche que si `f` accepte `null` (comme un mapper MapStruct,
qui rend `null` pour `null`). Sinon `f(null)` lance une `NullPointerException` : c'est pourquoi on la trouve
souvent écrite avec un `if (… == null)`, comme dans l'exemple.

#### 2. `map` ou `flatMap` ?

**Le contexte :** parfois, la transformation `f` **cherche** elle-même quelque chose qui peut ne pas exister.
Elle rend donc déjà un `Optional`.

**Exemple :** on veut l'adresse d'un client. Le client peut ne pas exister, et un client peut ne pas avoir d'adresse.

```java
Optional<Customer> findCustomer(Long id)   // le client, ou rien
Optional<Address>  findAddress(Customer c) // l'adresse du client, ou rien
```

```java
findCustomer(id).map(this::findAddress)       // → Optional<Optional<Address>> : un Optional dans un Optional
findCustomer(id).flatMap(this::findAddress)   // → Optional<Address>           : ce qu'on veut
```

`map` met **tel quel** ce que rend `f` dans un nouvel `Optional`. Si `f` rend déjà un `Optional`, on en obtient
deux, l'un dans l'autre. `flatMap` n'en ajoute pas.

**La règle : regarder ce que rend `f`.**

| `f` rend… | On utilise | Exemple |
|---|---|---|
| une valeur simple (`String`, `BigDecimal`, `BeerDTO`…) | `map` | `String::length` rend un `int` |
| déjà un `Optional<…>` | `flatMap` | `findAddress` rend un `Optional<Address>` |

#### 3. Dans du vieux code : `map` ou `flatMap` ?

**Le contexte :** devant un vieux code avec `orElse(null)` et `if (… == null)`, comment savoir s'il faut le
remplacer par `map` ou par `flatMap` ? Regarder **la dernière ligne**, celle qui rend le résultat quand la valeur existe.

| Dernière ligne | Ce qu'elle montre | On écrit |
|---|---|---|
| `return Optional.of(order.getTotal());` | `getTotal()` rend une valeur simple, qu'il faut mettre dans un `Optional` avec `Optional.of` | `findOrder(orderId).map(Order::getTotal)` |
| `return findMainCategory(beer);` | `findMainCategory` rend **déjà** un `Optional<Category>` : pas d'`Optional.of` | `findBeer(beerId).flatMap(this::findMainCategory)` |

En bref : `Optional.of(...)` sur la dernière ligne → `map` ; pas d'`Optional.of` → `flatMap`.

#### 4. Enchaîner plusieurs étapes

**Le contexte :** on veut l'e-mail du fournisseur d'une bière. Il faut trois étapes :
la bière → son code UPC → le fournisseur de ce code → son e-mail.

```java
Optional<Beer>     findBeer(UUID beerId)            // la bière, ou rien
Optional<Supplier> findSupplierByUpc(String upc)    // le fournisseur, ou rien
// Beer a String getUpc() ; Supplier a String getEmail()
```

On décide `map` ou `flatMap` **à chaque étape**, avec la règle du point 2 :

| Étape | `f` | `f` prend… et rend… | On utilise | Résultat |
|---|---|---|---|---|
| départ | | | | `Optional<Beer>` |
| 1 | `Beer::getUpc` | `Beer` → `String` | `map` | `Optional<String>` |
| 2 | `this::findSupplierByUpc` | `String` → `Optional<Supplier>` | `flatMap` | `Optional<Supplier>` |
| 3 | `Supplier::getEmail` | `Supplier` → `String` | `map` | `Optional<String>` |

```java
return findBeer(beerId)
        .map(Beer::getUpc)                   // étape 1
        .flatMap(this::findSupplierByUpc)    // étape 2
        .map(Supplier::getEmail);            // étape 3
```

**Et si une étape ne trouve rien ?** L'`Optional` devient vide, et les étapes suivantes ne font rien :
leur `f` n'est même pas appelée. Le résultat final est un `Optional` vide.

| Cas | Étape 1 | Étape 2 | Étape 3 | Résultat |
|---|---|---|---|---|
| tout existe | rend l'UPC | rend le fournisseur | rend l'e-mail | `Optional["contact@…"]` |
| bière inconnue | vide (`getUpc` pas appelé) | ne fait rien | ne fait rien | `Optional.empty` |
| pas de fournisseur pour cet UPC | rend l'UPC | vide | ne fait rien (`getEmail` pas appelé) | `Optional.empty` |

On n'a donc besoin d'aucun `if (… == null)` entre les étapes.

#### 5. Enchaîner plutôt qu'emboîter

**Le contexte :** il y a deux façons d'écrire plusieurs étapes. **Enchaîner** : les étapes l'une après l'autre.
**Emboîter** : l'étape suivante écrite à l'intérieur du lambda de la précédente.

**Exemple :** trouver le chef du service d'un employé.

```java
// enchaîné : se lit de gauche à droite, une étape à la fois
findEmployee(id).flatMap(emp -> findDepartment(emp)).flatMap(dept -> findManager(dept))

// emboîté : même résultat, mais il faut compter les parenthèses
findEmployee(id).flatMap(emp -> findDepartment(emp).flatMap(dept -> findManager(dept)))
```

Préférer la forme **enchaînée**.

**Le seul cas où il faut emboîter :** une étape a besoin d'une valeur d'une étape **plus ancienne**.
Exemple : afficher `"Pierrot — Marketing"`, qui demande l'employé **et** son service en même temps.

```java
findEmployee(id).flatMap(emp -> findDepartment(emp).map(dept -> emp.getName() + " — " + dept.getName()))
//                       ^^^                                    ^^^ emp est encore utilisable ici
```

En forme enchaînée, l'étape 2 ne reçoit que `dept` : `emp` n'existe plus à cet endroit.

#### 6. Trois façons d'écrire `f`

| Ce que fait `f` | Référence de méthode | Lambda (la même chose) |
|---|---|---|
| appelle une méthode **de l'objet reçu** | `Customer::getEmail` | `customer -> customer.getEmail()` |
| passe l'objet reçu à une méthode **de notre classe** | `this::toDto` | `beer -> toDto(beer)` |
| passe l'objet reçu à une méthode **d'un autre objet** | `beerMapper::beerToBeerDto` | `beer -> beerMapper.beerToBeerDto(beer)` |

En cas de doute, écrire le lambda : il montre clairement qui reçoit quoi.

**Exemple dans le projet :** `src/main/java/guru/springframework/spring7restmvc/services/BeerServiceJPA.java:91`

```java
return beerRepository.findById(id).map(beerMapper::beerToBeerDto);
```

Ici `x` = `beerRepository.findById(id)` (un `Optional<Beer>`) et `f` = `beerMapper::beerToBeerDto`
(`Beer` → `BeerDTO`, 3e ligne du tableau) ; le résultat est un `Optional<BeerDTO>`.
`flatMap` n'est pas encore utilisé dans le projet.
