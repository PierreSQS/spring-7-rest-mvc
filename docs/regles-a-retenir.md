# Règles à retenir

Les règles apprises en travaillant sur ce projet (Spring Boot 4, Spring Data JPA, Hibernate 7,
MapStruct, Spring Security 7). Chaque règle : **la règle**, un **exemple**, et **la preuve**
(le test ou le commit qui la montre). Document enrichi au fil des leçons.

Créé le 17-10-2026.

---

## Table des matières

27 règles, numérotées R1 à R27 dans l'ordre où elles ont été apprises - donc pas dans
l'ordre des sections : R19 est rangée avec les fichiers, R20-R21 avec les lambdas, R22-R23 avec Hibernate,
R24 avec PUT et PATCH, R25-R26 avec la base de données, R27 avec Spring Security.


**1. MapStruct et DTO**

- [R1 — Dans le sens DTO → entité, ignorer ce qui appartient à la base](#r1--dans-le-sens-dto--entité-ignorer-ce-qui-appartient-à-la-base)
- [R2 — Dans le sens entité → DTO, tout renvoyer](#r2--dans-le-sens-entité--dto-tout-renvoyer)

**2. PUT et PATCH**

- [R3 — PATCH ne copie que ce qui est envoyé, PUT remplace tout](#r3--patch-ne-copie-que-ce-qui-est-envoyé-put-remplace-tout)
- [R4 — Un texte vide `""` n'est pas `null`](#r4--un-texte-vide--nest-pas-null)
- [R24 — Bean Validation : seules `@NotNull`, `@NotEmpty` et `@NotBlank` refusent `null`](#r24--bean-validation--seules-notnull-notempty-et-notblank-refusent-null)

**3. Hibernate et transactions**

- [R5 — Certaines choses n'arrivent qu'au moment où Hibernate écrit en base](#r5--certaines-choses-narrivent-quau-moment-où-hibernate-écrit-en-base)
- [R6 — Dans une transaction, `findById` lit la mémoire d'Hibernate, pas la base](#r6--dans-une-transaction-findbyid-lit-la-mémoire-dhibernate-pas-la-base)
- [R7 — L'`id` et la `version` appartiennent à Hibernate](#r7--lid-et-la-version-appartiennent-à-hibernate)
- [R8 — `@Column(updatable = false)` protège la date de création](#r8--columnupdatable--false-protège-la-date-de-création)
- [R22 — Une entité lue est surveillée : la modifier, c'est l'écrire en base](#r22--une-entité-lue-est-surveillée--la-modifier-cest-lécrire-en-base)
- [R23 — Une validation refusée au commit arrive emballée : `getRootCause()` retrouve le champ et le message](#r23--une-validation-refusée-au-commit-arrive-emballée--getrootcause-retrouve-le-champ-et-le-message)

**4. Tests**

- [R9 — Un test `@Transactional` n'écrit jamais en base : quand ajouter `flush()`, quand retirer `@Transactional`](#r9--un-test-transactional-nécrit-jamais-en-base--quand-ajouter-flush-quand-retirer-transactional)
- [R10 — Un test doit prouver quelque chose : le faire échouer une fois](#r10--un-test-doit-prouver-quelque-chose--le-faire-échouer-une-fois)
- [R11 — `@WebMvcTest` ne charge que la couche web](#r11--webmvctest-ne-charge-que-la-couche-web)

**5. Spring Security**

- [R12 — Une chaîne de filtres personnalisée remplace celle de Spring Boot](#r12--une-chaîne-de-filtres-personnalisée-remplace-celle-de-spring-boot)
- [R13 — 401 ou 403 ?](#r13--401-ou-403-)
- [R14 — `user(...)` ou `httpBasic(...)` dans un test MockMvc](#r14--user-ou-httpbasic-dans-un-test-mockmvc)
- [R15 — Désactiver CSRF seulement pour l'API, et seulement pour des clients non-navigateurs](#r15--désactiver-csrf-seulement-pour-lapi-et-seulement-pour-des-clients-non-navigateurs)
- [R16 — Ne jamais mettre de mot de passe en clair dans Git](#r16--ne-jamais-mettre-de-mot-de-passe-en-clair-dans-git)
- [R27 — Trouver les adresses d'un serveur d'autorisation : partir de l'`issuer-uri`](#r27--trouver-les-adresses-dun-serveur-dautorisation--partir-de-lissuer-uri)

**6. Entités**

- [R17 — `equals` / `hashCode` : l'identité d'`Object` suffit souvent avec Hibernate](#r17--equals--hashcode--lidentité-dobject-suffit-souvent-avec-hibernate)
- [R18 — Une méthode d'aide bidirectionnelle met à jour les deux côtés](#r18--une-méthode-daide-bidirectionnelle-met-à-jour-les-deux-côtés)

**7. Fichiers et encodage**

- [R19 — Lire un fichier du classpath en flux, et toujours en UTF-8](#r19--lire-un-fichier-du-classpath-en-flux-et-toujours-en-utf-8)

**8. Java : lambdas et threads**

- [R20 — `AtomicReference` sert aux threads ; pour sortir une valeur d'un lambda, ce n'est qu'une boîte](#r20--atomicreference-sert-aux-threads--pour-sortir-une-valeur-dun-lambda-ce-nest-quune-boîte)
- [R21 — `Optional.map` / `flatMap` : transformer la valeur d'un `Optional`, au lieu de la sortir dans une `AtomicReference` (la « boîte » de R20)](#r21--optionalmap--flatmap--transformer-la-valeur-dun-optional-au-lieu-de-la-sortir-dans-une-atomicreference-la--boîte--de-r20) - *et le piège : `map` emballe toujours ce que le lambda renvoie*

**9. Base de données : Flyway et Docker Compose**

- [R25 — Une migration Flyway déjà appliquée ne se modifie plus, même d'une ligne](#r25--une-migration-flyway-déjà-appliquée-ne-se-modifie-plus-même-dune-ligne)
- [R26 — `java -jar` : le dossier de lancement compte, et Docker Compose n'est pas dans le jar](#r26--java--jar--le-dossier-de-lancement-compte-et-docker-compose-nest-pas-dans-le-jar)

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

### R24 — Bean Validation : seules `@NotNull`, `@NotEmpty` et `@NotBlank` refusent `null`

**Contexte :** la suite directe de R4, le 26-10-2026, sur la branche `126-springdoc-maven-jt-sb412` (Section 40).
Le PATCH d'un client effaçait le nom ou l'email envoyé vide (le piège de R4, revenu avec `patchCustomerFromDto`).
Pour l'email, on voulait : **facultatif** (les anciens clients n'en ont pas) **mais jamais vide**.

- **Où :** `src/main/java/guru/springframework/spring7restmvc/entities/Customer.java`, champ `email`.
- **Le bug :** [commit b0ce1d6a](https://github.com/PierreSQS/spring-7-rest-mvc/commit/b0ce1d6a) (`-NOK`).
- **La correction :** [commit af6b987d](https://github.com/PierreSQS/spring-7-rest-mvc/commit/af6b987d).

**La règle :** chaque contrainte a un seul travail. Celles qui ne s'appellent pas « Not… » (`@Size`, `@Pattern`,
`@Email`, `@Min`…) vérifient la **forme** d'une valeur : si la valeur est `null`, elles n'ont rien à vérifier et
disent « OK ». Refuser l'absence de valeur, c'est le travail exclusif des trois « Not ».

| Valeur | `@NotNull` | `@NotEmpty` | `@NotBlank` | `@Size(min = 1)` | `@Pattern(regexp = "\\s*\\S.*")` |
|---|---|---|---|---|---|
| `null` | ❌ | ❌ | ❌ | ✅ rien à vérifier | ✅ rien à vérifier |
| `""` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `"   "` | ✅ | ✅ | ❌ | ✅ | ❌ |
| `"pierre@exemple.com"` | ✅ | ✅ | ✅ | ✅ | ✅ |

**Pourquoi la spécification fait ça :** pour pouvoir **combiner**. `@Pattern(...)` seul = facultatif mais jamais
vide ; `@NotNull @Pattern(...)` = obligatoire et jamais vide. Si `@Pattern` refusait déjà `null`, on ne
pourrait plus exprimer « facultatif ».

**Dans le projet — le code final :**

```java
// email optional but never blank: null is accepted (old customers have none),
// "" and "   " are rejected - @Size and @Pattern let null through, only @NotXxx refuse it
@Column(length = 255)
@Size(max = 255)                                             // pas plus long que la colonne
@Pattern(regexp = "\\s*\\S.*", message = "must not be blank")  // au moins un caractère qui n'est pas un espace
private String email;
```

L'expression `\s*\S.*` se lit : des espaces éventuels (`\s*`), puis **un** caractère qui n'est pas un espace
(`\S`), puis n'importe quoi (`.*`). `""` et `"   "` n'ont aucun `\S`, donc ils échouent. En Java la barre se
double dans une chaîne : `"\\s*\\S.*"`.

**Pourquoi pas `.*\S.*` (la première version) :** elle donne le même résultat, mais SonarQube la signale
(*« Simplify this regular expression… super-linear performance due to backtracking »*). Le premier `.*` peut
avaler n'importe quoi, y compris le caractère que `\S` cherche : le moteur doit essayer toutes les découpes
possibles avant de conclure. Dans `\s*\S.*`, `\s*` (des espaces) et `\S` (pas un espace) ne
peuvent jamais prendre le même caractère : une seule découpe possible, plus d'hésitation.

**Les deux essais qui ont cassé le démarrage :**

| Essai | Message au démarrage | Pourquoi |
|---|---|---|
| `@NotNull` | `darf nicht null sein`, `propertyPath=email` | refuse `null`, et `BootstrapData` crée ses clients sans email ; en plus laisse passer `""` |
| `@NotEmpty` | `darf nicht leer sein`, `propertyPath=email` | refuse aussi `null` (pour lui, `null` compte comme vide) |

**Le piège à retenir :** une contrainte sur l'**entité** est revérifiée à **chaque** enregistrement, sur **tous**
les champs. Si des lignes déjà en base ne la respectent pas (ici : des clients sans email), elles ne peuvent plus
être enregistrées — ni créées par le bootstrap, ni modifiées par un PATCH qui ne touche que le nom.

**Preuve :** un test temporaire a fait valider les 4 valeurs par Hibernate Validator sur `Customer.email`
(26-10-2026) : `null` accepté, `""` refusé, `"   "` refusé, une adresse de 29 caractères acceptée. Tests verts
après la correction : `BootstrapDataTest` 1, `CustomerControllerIT` 9, `CustomerControllerTest` 7.

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

### R9 — Un test `@Transactional` n'écrit jamais en base : quand ajouter `flush()`, quand retirer `@Transactional`

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

#### Ce que montre le log SQL (démonstration du 27-10-2026)

**Contexte.** On a lancé des tests en regardant le log SQL
(`logging.level.org.hibernate.SQL=debug` : chaque requête **réellement envoyée** à la base
apparaît dans le log). Pas de requête dans le log = la base n'a rien reçu ni rien contrôlé.

**Exemple 1 : le test de JT** (projet, branche `126-springdoc-maven-jt-sb412`,
`src/test/java/guru/springframework/spring7restmvc/repositories/CategoryRepositoryTest.java:27-39`) :

```java
@BeforeEach
void setUp() {
    testBeer = beerRepository.findAll().get(0);      // la première bière de la base
}

@Transactional
@Test
void testAddCategory() {
    Category savedCat = categoryRepository.save(Category.builder()
                    .description("Ales")
            .build());

    testBeer.addCategory(savedCat);                  // relie la bière et la catégorie en Java
    Beer saveBeer = beerRepository.save(testBeer);   // le lien doit aller dans la table beer_category

    System.out.println(saveBeer.getBeerName());
}
```

`testBeer.addCategory(...)` touche `Beer.categories`, une collection **lazy** : Hibernate doit
d'abord la charger depuis la base.

**Exemple 2 : un test jetable** (écrit pour la démonstration, puis supprimé). `Customer.name` porte
`@NotBlank`, donc un nom vide doit être refusé :

```java
@Transactional
@Test
void testSaveCustomerBlankName() {
    assertThatThrownBy(() -> customerRepository.save(Customer.builder()   // puis : saveAndFlush
                    .name("")
                    .build()))
            .isInstanceOf(ConstraintViolationException.class);
}
```

**Ce qu'on a observé :**

| Situation dans le test | `INSERT` envoyé à la base ? | Contraintes vérifiées ? | Données après le test |
|---|---|---|---|
| `@Transactional`, sans `flush()` | non | non | rien (rollback) |
| `@Transactional` + `flush()` / `saveAndFlush()` | oui | oui | rien (rollback) |
| sans `@Transactional` | oui, commit à chaque `save()` | oui | **restent en base** |

Ligne par ligne :

1. **`@Transactional`, sans `flush()`.** Exemple 1 : aucun `insert into category`, aucun
   `insert into beer_category` dans le log, et le test est vert (il n'a pas d'assertion).
   Exemple 2 : **rouge**, « Expecting code to raise a throwable » : le nom vide n'a jamais été
   validé. Le test se trompe donc sur le code, alors que l'application refuse bien un nom vide.
2. **`@Transactional` + `flush()`.** Exemple 1 avec `entityManager.flush()` après le second
   `save()` : `insert into category`, `update beer ... version=?` (la collection de la bière a
   changé, donc sa `@Version` augmente) et `insert into beer_category` apparaissent **exactement**
   au moment du `flush()`, pas au moment des `save()`. Après le test : 0 catégorie en base
   (compté dans une méthode `@AfterTransaction`, qui tourne après le rollback).
   Exemple 2 avec `saveAndFlush` (= `save` + `flush()`) : **vert**. Aucun `insert into customer`
   n'apparaît : Hibernate valide l'entité **avant** de construire l'`INSERT`, donc la requête
   n'est jamais envoyée.
3. **Sans `@Transactional`.** Exemple 1 : `insert into category`, puis
   **`LazyInitializationException`** sur `addCategory`. `findAll()` (dans `setUp`) a tourné
   dans **sa propre** transaction : une fois celle-ci terminée, `testBeer` est détachée et sa
   collection lazy ne peut plus être chargée. La catégorie « Ales », déjà commitée, **reste en
   base**.

**À retenir :** dans un test, `@Transactional` fait **deux** choses :

- il annule tout à la fin (rollback), donc la base reste propre ;
- il garde le persistence context ouvert pendant tout le test, donc le lazy loading fonctionne.

Mais il **empêche** l'écriture réelle. Pour qu'elle ait lieu dans le test, il faut un `flush()`.

**`saveAndFlush` ou pas de `@Transactional` ?** Dans un test de **repository**, `saveAndFlush`
suffit : l'exception de validation est levée directement par l'appel. Dans un test **HTTP**
(MockMvc) qui attend un `400`, il faut que le commit du service ait lieu, donc **pas** de
`@Transactional` sur le test (ligne 3 du premier tableau).

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

### R27 — Trouver les adresses d'un serveur d'autorisation : partir de l'`issuer-uri`

**Le contexte :** une API protégée par des jetons JWT (un *resource server*) ne fabrique pas ses
jetons. Un autre programme les délivre : le **serveur d'autorisation** (ici, le nôtre tourne sur le
port 9000). Pour obtenir un jeton à la main (Postman, Swagger UI, curl), il faut l'adresse où le
demander, le *token endpoint*. Elle n'est écrite nulle part dans le projet.

**La règle :** l'API connaît l'adresse de base de son serveur d'autorisation, l'**issuer**
(« l'émetteur » des jetons), par la propriété
`spring.security.oauth2.resourceserver.jwt.issuer-uri`. La norme OpenID Connect fixe une adresse
unique qui décrit tout le serveur :

    <issuer> + /.well-known/openid-configuration

Ce document JSON liste toutes les autres adresses :

| Champ | Ce que c'est |
|---|---|
| `token_endpoint` | l'adresse où demander un jeton |
| `jwks_uri` | les clés publiques qui servent à vérifier la signature des jetons |
| `token_endpoint_auth_methods_supported` | comment envoyer `client_id` et `client_secret` (`client_secret_basic` = en-tête, `client_secret_post` = dans le corps) |

Spring Boot fait la même chose au démarrage : il lit ce document pour trouver `jwks_uri`.

**Exemple dans le projet :** `src/main/resources/application.properties:10`
(`issuer-uri=http://localhost:9000`) → `http://localhost:9000/.well-known/openid-configuration`
→ `token_endpoint` = `http://localhost:9000/oauth2/token`.

**Le piège :** un 401 de l'API renvoie l'en-tête
`WWW-Authenticate: Bearer resource_metadata=".../.well-known/oauth-protected-resource"`
(nouveau avec Spring Security 7). On pourrait croire que cette adresse mène au serveur d'autorisation :
avec notre configuration, elle **ne le nomme pas** (pas de champ `authorization_servers`, vérifié
le 03-11-2026).

**Preuve :** la page `/.well-known/openid-configuration` ouverte dans le navigateur le 03-11-2026,
et le jeton obtenu au port 9000 accepté par Swagger UI (#27, branche `127-springdoc-security-cfg-jt-sb412`).

---

## 6. Entités

### R17 — `equals` / `hashCode` : l'identité d'`Object` suffit souvent avec Hibernate

- `super.equals(o)` dans une classe sans `extends` appelle `Object.equals`.
  `Object.equals` pose la question **« même objet ? »** (`this == o`).
  Toute comparaison après cette ligne ne sert à rien.
- Un `hashCode` basé sur un champ modifiable (comme une description) perd l'objet dans un `HashSet`
  si le champ change après l'ajout.
- Dans une même transaction, Hibernate renvoie le même objet Java pour la même ligne **d'une table**
  (par exemple la ligne de la table `category` dont l'`id` est X) : l'identité suffit (voir R6).

#### Démonstration du 28-10-2026

**Contexte.** Le `Category` de JT avait ces deux méthodes (supprimées depuis, sur les branches 123 et 126) :

```java
public class Category {                                // pas de "extends" : la classe parente est Object

    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                    // (1)
        if (!(o instanceof Category category)) return false;  // (2)
        if (!super.equals(o)) return false;            // (3) super.equals(o) = Object.equals(o)
        return getDescription() != null ?              // (4) compare les descriptions
                getDescription().equals(category.getDescription()) : category.getDescription() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);  // dépend de description
        return result;
    }
}
```

Le code de `Object.equals` dans le JDK :

```java
public boolean equals(Object obj) {
    return (this == obj);                              // « même objet ? »
}
```

**« Même objet » ou « même contenu » :**

```java
Category a = new Category("Ales");     // new -> un 1er objet en mémoire
Category b = new Category("Ales");     // new -> un 2e objet, avec le même contenu
Category c = a;                        // pas de new : c désigne le même objet que a

a == c;    // true  : un seul objet, deux noms
a == b;    // false : deux objets différents, même si le contenu est identique
```

**Partie 1 : le `equals` de JT ne compare jamais les descriptions.** Avec `a.equals(b)` :

- (1) `a == b` ? faux, donc pas de `return`, on continue ;
- (2) `b` est une `Category` ? oui, on continue ;
- (3) `Object.equals(b)`, c'est-à-dire `a == b` ? faux, donc `return false` : **la méthode s'arrête ici** ;
- (4) n'est **jamais** atteinte.

(1) et (3) posent la même question. Ce `equals` revient donc à « même objet », exactement comme s'il
n'y avait pas de méthode `equals` du tout.

**Partie 2 : le `hashCode` de JT perd l'objet dans un `HashSet`.** Un `HashSet` :

- `add(x)` : calcule `x.hashCode()` et range l'objet **à l'endroit** qui correspond à ce nombre ;
- `contains(x)` / `remove(x)` : recalcule `x.hashCode()`, **cherche seulement à cet endroit**,
  puis compare avec `equals`.

Test jetable (écrit pour la démonstration, puis supprimé), avec une classe qui recopie les deux méthodes de JT :

```java
Set<Category> categories = new HashSet<>();
Category ales = new Category("Ales");

categories.add(ales);
ales.setDescription("Stouts");         // le hashCode change

categories.contains(ales);
categories.remove(ales);
categories.size();
```

Sortie :

```
hashCode() with description Ales   : 1990154381
contains(ales) before the change   : true
hashCode() with description Stouts : 179914554
contains(ales) after the change    : false
remove(ales)                       : false
size() after remove(ales)          : 1
```

L'objet est toujours **dans** le `HashSet`, rangé à l'endroit de « Ales ». `contains` et `remove` le
cherchent à l'endroit de « Stouts » et ne le trouvent pas. `remove` échoue **sans erreur**.

**Exemple dans le projet :** `src/main/java/guru/springframework/spring7restmvc/entities/Beer.java:76-79`

```java
public void removeCategory(Category category){
    this.categories.remove(category);                  // avec le hashCode de JT : échoue si la description a changé
    category.getBeers().remove(this);
}
```

Avec le `hashCode` de JT, si la description de la catégorie avait changé entre-temps, le lien
bière ↔ catégorie restait dans le `Set`, donc en base, sans aucune erreur.

**À retenir :**

- le `equals` de JT ne servait à rien : il faisait déjà ce que fait `Object` ;
- son `hashCode` était dangereux : il changeait avec `description` ;
- la correction : **supprimer les deux méthodes**. `Category` utilise alors l'identité d'`Object`,
  qui ne change jamais, et Hibernate garantit un seul objet Java par ligne dans une transaction.

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

#### Le piège de `map` : il emballe TOUJOURS ce que le lambda renvoie

**Contexte :** le PUT d'une bière, le 25-10-2026, première écriture de la version sans `AtomicReference`.

- **Où :** `src/main/java/guru/springframework/spring7restmvc/services/BeerServiceJPA.java`,
  méthode `updateBeerById` (puis `patchBeerById`, et les deux mêmes méthodes de `CustomerServiceJPA`).
- **Branche :** `126-springdoc-maven-jt-sb412` (le code d'origine de JT, Section 40 Chapitre 374).
- **Le diff réel :** [commit 14c8cbba](https://github.com/PierreSQS/spring-7-rest-mvc/commit/14c8cbba).

**La règle :** `map` ne regarde pas ce que le lambda lui renvoie. Quoi que ce soit, il le met dans un
`Optional`. Donc si le lambda renvoie **déjà** un `Optional`, on se retrouve avec deux couches.

**Version écrite d'abord — elle marche, mais fait un détour :**

```java
return beerRepository.findById(beerId).map(foundBeer -> {
    foundBeer.setBeerName(beer.getBeerName());
    // ... les autres setXxx
    return Optional.of(beerMapper.beerToBeerDto(beerRepository.save(foundBeer)));
    //     ^^^^^^^^^^^ le lambda renvoie un Optional<BeerDTO>
}).orElse(Optional.empty());
//  ^^^^^^ obligatoire : enlève la couche fabriquée juste au-dessus
```

| Étape | Expression | Type |
|---|---|---|
| 1 | `beerRepository.findById(beerId)` | `Optional<Beer>` |
| 2 | `.map(... return Optional.of(dto))` | `Optional<Optional<BeerDTO>>` |
| 3 | `.orElse(Optional.empty())` | `Optional<BeerDTO>` |

**Version finale — le lambda renvoie la `BeerDTO` toute nue :**

```java
return beerRepository.findById(beerId).map(foundBeer -> {
    foundBeer.setBeerName(beer.getBeerName());
    // ... les autres setXxx
    return beerMapper.beerToBeerDto(beerRepository.save(foundBeer));
    //     le lambda renvoie une BeerDTO -> map emballe UNE fois -> Optional<BeerDTO>
});
```

Même comportement, mêmes tests verts (`BeerControllerIT` 18 tests, `BeerControllerTest` 9 tests,
le 25-10-2026), mais plus de type `Optional<Optional<BeerDTO>>` au milieu et deux bouts de code en moins.

**Comment repérer le piège :** dès qu'on écrit `.orElse(Optional.empty())` à la fin d'un `map`, c'est
qu'on a emballé deux fois. Le compilateur le dit aussi, en nommant le type `Optional<Optional<BeerDTO>>`
dans son message d'erreur si on se trompe de déclaration.

**Et quand on veut vraiment renvoyer un `Optional` depuis le lambda ?** C'est le rôle de `flatMap` : il
attend un lambda qui rend déjà un `Optional` et n'ajoute **pas** de couche. Cas typique : le lambda
appelle une méthode qui rend elle-même un `Optional` (`findById`, `findByUpc`…).

**Preuve :** [commit `14c8cbba`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/14c8cbba) — les quatre
méthodes des services JPA (`updateBeerById`, `patchBeerById`, `updateCustomerById`, `patchCustomerById`)
passées de la boîte `AtomicReference` à `Optional.map`, tests verts : 18 + 9 (bière), 9 + 7 (client).

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


## 9. Base de données : Flyway et Docker Compose

### R25 — Une migration Flyway déjà appliquée ne se modifie plus, même d'une ligne

**Contexte.** Flyway applique les fichiers `src/main/resources/db/migration/V1__...sql`, `V2__...sql`, ...
dans l'ordre, et note chaque migration appliquée dans la table `flyway_schema_history` de la base.
Pour chaque fichier, il y garde une **somme de contrôle** (*checksum*) : un nombre calculé sur le
**contenu** du fichier, ligne par ligne. À chaque démarrage, il recalcule ce nombre pour chaque fichier
et le compare à celui de la table.

**La règle.**

- Une migration déjà appliquée sur une base ne se modifie plus : ni une ligne en moins, ni une autre
  mise en forme. Sinon Flyway refuse de démarrer : « Migration checksum mismatch for migration version 1 ».
- Pour changer le schéma, on écrit une **nouvelle** migration (V8, V9, ...).
- Deux branches qui partagent une même base Docker doivent avoir **exactement les mêmes fichiers**
  de migration.

**Exemple dans le projet** (ticket #6, branche `126-springdoc-maven-jt-sb412`, 28-10-2026).
La base Docker `sec22-assn25` a été construite avec les migrations des branches de la Section 38 (fichiers V1 à V7
identiques depuis `122-server-port-mapping-jt-sb411`, commit `5a5581d0`). La branche 126
avait les fichiers originaux de JT : même schéma, mais un contenu différent. Par exemple `V3__add-order-tables.sql` :

```diff
-drop table if exists beer_order_line;     // version de JT : ces lignes en plus
-drop table if exists beer_order;
-
 create table beer_order ( ...             // la suite est identique
```

Sur une base neuve, ces `drop` ne suppriment rien : les deux versions créent exactement les mêmes tables.
Mais pour Flyway, le contenu est différent, donc la somme de contrôle aussi :

```
                       V1        V2   V3        V4        V5        V6      V7
fichiers de la 122     OK        OK   OK        OK        OK        OK      OK
fichiers de JT (126)   mismatch  OK   mismatch  mismatch  mismatch  absent  absent
```

Correction : la 126 a repris les fichiers V1 à V7 de la 122, tels quels.

**Comment savoir si Flyway a accepté une base existante.** Au démarrage, deux logs sont possibles :

- **la base existante est utilisée** : Flyway compare les sommes de contrôle, tout correspond, rien à appliquer :

  ```
  Successfully validated 7 migrations
  Schema `jt_spring7_rest_sec22_assn25_db` is up to date. No migration necessary.
  ```

  C'est la preuve que les fichiers correspondent à la base.

- **la base est neuve** (volume vide) : Flyway applique tout depuis zéro :

  ```
  Migrating schema `jt_spring7_rest_sec22_assn25_db` to version "1 - init-mysql-database"
  ...
  Successfully applied 7 migrations
  ```

  L'application démarre aussi, mais ça ne prouve **rien** sur une base existante.

**Piège : `docker compose down -v` cache le problème.** `down -v` supprime le conteneur **et son volume** :
chaque démarrage repart d'une base neuve (2e cas), et un mismatch ne se voit jamais. Avec
`docker compose stop`, le volume survit à l'arrêt de l'application, et les sommes de contrôle sont
vérifiées contre la base existante à chaque démarrage. Réglage dans `application-localmysql.properties` :
`spring.docker.compose.stop.command`.

**Vérifier soi-même** (lecture seule), le conteneur étant démarré :

```bash
docker exec mysql-spring7-rest-sec22-assn25 sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "select version, checksum from $MYSQL_DATABASE.flyway_schema_history order by installed_rank"'
```

`$MYSQL_ROOT_PASSWORD` et `$MYSQL_DATABASE` sont les variables d'environnement **du conteneur**
(venues du fichier `.env`) : le mot de passe ne s'affiche jamais dans le terminal.

**Repartir d'une base propre** (les données changent, pas le schéma) : `docker compose down -v`, puis
démarrer l'application. Docker recrée une base vide avec les mêmes noms, Flyway y applique V1 à V7.
Des **nouveaux noms** (projet, conteneur, volume, base) ne sont nécessaires que quand le **schéma** change
(une nouvelle migration).

### R26 — `java -jar` : le dossier de lancement compte, et Docker Compose n'est pas dans le jar

**Contexte.** Section 38 (Spring Boot Maven Plugin) : JT démarre l'application avec
`java -jar target/spring-7-rest-mvc-0.0.1-SNAPSHOT.jar`, au lieu de `./mvnw spring-boot:run`.
Le jar contient le code, les fichiers `src/main/resources` et les bibliothèques. Deux surprises
(31-10-2026, branche `125-build-info-jt-sb411`, commit `9d444bda`).

**La règle.**

- Spring Boot lit aussi un dossier **`config/` dans le dossier d'où on lance la commande**, et ses
  valeurs remplacent celles du jar. Le même jar se comporte donc différemment selon ce dossier.
- Le module `spring-boot-docker-compose` n'est **pas** mis dans le jar (Spring Boot l'exclut : il ne
  sert qu'au développement). Avec `java -jar`, personne ne démarre le conteneur MySQL ni ne donne
  son adresse : il faut la passer soi-même.

**Exemple 1 : racine du projet ou `target/`.** Le dossier `config/` (ignoré par Git, réglages de debug)
est à la racine du projet :

```
spring-7-rest-mvc/
├── config/application.properties   <- logging.level.org.hibernate.SQL=debug
└── target/spring-7-rest-mvc-0.0.1-SNAPSHOT.jar
```

| Lancé depuis | Commande | `config/` lu ? | Lignes de log `org.hibernate.SQL` |
|---|---|---|---|
| racine du projet | `java -jar target/spring-7-rest-mvc-0.0.1-SNAPSHOT.jar` | oui | 2443 |
| `target/` | `java -jar spring-7-rest-mvc-0.0.1-SNAPSHOT.jar` | non (pas de `target/config/`) | 0 |

**Exemple 2 : `-Dspring.profiles.active=localmysql` plante dans Flyway**, même avec le conteneur
MySQL démarré à la main :

1. Le profil `localmysql` ne contient pas d'URL de base : c'est Docker Compose qui la fournit, et il
   n'est pas dans le jar.
2. Spring Boot trouve H2 dans le jar et crée une base H2 en mémoire :
   `RestDB-Pool - Added connection conn0: url=jdbc:h2:mem:...`
3. Flyway exécute les scripts **MySQL** sur cette base **H2** et échoue au premier :
   `Failed to execute script V1__init-mysql-database.sql` - `Unbekannter Datentyp: "DATETIME"`.

Le message parle de Flyway, mais la vraie cause est la mauvaise base : la ligne `url=jdbc:h2:mem`
le montre.

**La commande qui marche** (Git Bash, depuis la racine du projet, le conteneur
`mysql-spring7-rest-sec22-assn25` déjà démarré) :

```bash
set -a; . ./.env; set +a
java -Dspring.profiles.active=localmysql -jar target/spring-7-rest-mvc-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3308/jt_spring7_rest_sec22_assn25_db \
  --spring.datasource.username="$MYSQL_USER" \
  --spring.datasource.password="$MYSQL_PASSWORD"
```

- La 1re ligne charge `MYSQL_USER` et `MYSQL_PASSWORD` depuis `.env` : le mot de passe ne s'écrit
  pas dans la commande.
- Les trois `--spring.datasource...` remplacent ce que Docker Compose donnait avec `spring-boot:run`.

Preuve (31-10-2026) : `Database: jdbc:mysql://localhost:3308/jt_spring7_rest_sec22_assn25_db`,
`Schema ... is up to date`, `Tomcat started on port 8081`.
