# Section 16 : pagination et tri de la liste des bières, ce que nous avons amélioré

**Branches :** `94-page-add-parameters-sb411` (Chap166), puis `97-paging-add-sort-sb411` (Chap167 à 169) · **Section / Chapitre :** Sec16_Chap166 à Sec16_Chap169 · **Commit(s) :**
[`b79d8409`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/b79d8409),
[`47727ac5`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/47727ac5),
[`59e259da`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/59e259da),
[`2ce2a98f`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/2ce2a98f),
[`b7e134fc`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/b7e134fc),
[`2adc6143`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/2adc6143)

**Contexte :** depuis la Section 14, la base contient **2413 bières**. `GET /api/v1/beer` les renvoyait toutes d'un
coup. Cette section découpe la réponse en *pages* (des morceaux de taille fixe, par exemple 25 bières), que le
client demande une par une avec `pageNumber` et `pageSize`. Ce document dit ce que nous avons fait **mieux que
JT**, puis ce que nous avons **ajouté**. C'est une photo de la fin de la section (24-09-2026) : il n'est jamais mis
à jour.

Réécrit le 26-11-2026 avec les mêmes faits ; l'ancienne version, en anglais, reste sur la branche
`109-lock-demo-optimistic-sb411` ([`69f0a32b`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec16-paging-improvements.md)).

Les chiffres de ce document viennent des données de départ : parmi les 2413 bières, **336** ont `IPA` dans leur
nom, **572** sont du style `IPA`, et **324** sont les deux à la fois.

## La leçon de JT

Branches de JT : `94-page-add-parameters` à `97-paging-add-sort`. JT avance dans l'ordre du TDD (*test-driven
development* : d'abord un test qui échoue, puis le code qui le fait passer) :

- **Chap166 :** `BeerController` et `BeerService` reçoivent deux paramètres de plus, `pageNumber` et `pageSize` ;
- **Chap167 :** `BeerServiceJPA.buildPageRequest` traduit ces paramètres en `PageRequest` de Spring Data. L'API
  compte les pages à partir de **1**, Spring Data à partir de **0** : la page 1 de l'API est la page 0 de Spring
  Data. Sans paramètre : page 1, 25 bières. `pageSize` est plafonné à 1000 ;
- **Chap168 :** les méthodes de recherche du repository prennent un `Pageable` et rendent une `Page<Beer>` au lieu
  d'une `List<Beer>`. Le service et le contrôleur rendent une `Page<BeerDTO>`. Une `Page` porte les bières de la
  page **et** les totaux : 336 bières « IPA » au total, réparties sur 14 pages ;
- **Chap169 :** chaque page est triée par nom de bière (`Sort.by(Sort.Order.asc("beerName"))`). Sans tri, « les 25
  premières lignes » dépendent de la base de données : une bière pourrait apparaître sur deux pages.

Les chapitres 167 et 168 ont laissé la suite de tests rouge un moment, exprès : le test TDD venait d'abord, et les
commits qui ne pouvaient pas encore le faire passer portent `-NOK`.

## Mieux que JT

### 1. `buildPageRequest` plus simple, et `pageSize=0` ne répond plus 500

**Le problème :** dans `src/main/java/guru/springframework/spring7restmvc/services/BeerServiceJPA.java`, la méthode
`buildPageRequest` de JT fait 22 lignes de `if/else` imbriqués. Surtout, elle ne vérifie que `pageSize == null`.
Avec `pageSize=0` ou `pageSize=-5`, la valeur arrive telle quelle dans `PageRequest.of`, qui lance une exception
(`Page size must not be less than one`) : le client reçoit une erreur **500** au lieu d'une page. Le nombre `1000`
est aussi écrit deux fois en dur, sans dire ce qu'il représente.

**Ce qui a changé** ([`b79d8409`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/b79d8409), Sec16_Chap167) :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
-public PageRequest buildPageRequest(
-        Integer pageNumber, Integer pageSize) {
-    int queryPageNumber;
-    int queryPageSize;
-
-    if (pageNumber != null && pageNumber > 0) {
-        queryPageNumber = pageNumber - 1;
-    } else {
-        queryPageNumber = DEFAULT_PAGE;
-    }
-
-    if (pageSize == null) {
-        queryPageSize = DEFAULT_PAGE_SIZE;
-    } else {
-        if (pageSize > 1000) {
-            queryPageSize = 1000;
-        } else {
-            queryPageSize = pageSize;
-        }
-    }
     // ...
 }
```

</td>
<td>

```diff
+PageRequest buildPageRequest(
+        Integer pageNumber, Integer pageSize) {
+    int queryPageNumber =
+            pageNumber != null && pageNumber > 0
+            ? pageNumber - 1
+            : DEFAULT_PAGE;
+
+    int queryPageSize =
+            pageSize != null && pageSize > 0
+            ? Math.min(pageSize, MAX_PAGE_SIZE)
+            : DEFAULT_PAGE_SIZE;
     // ...
 }
```

</td>
</tr>
</table>

**Pourquoi :**

- **a.** `pageSize > 0` : une taille de 0 ou moins prend la valeur par défaut (25), comme une taille absente.
  `PageRequest.of` ne peut plus lancer d'exception, et le client reçoit toujours une page ;
- **b.** `MAX_PAGE_SIZE` dit ce que représente `1000` : la limite qui empêche un client de vider toute la table en
  une seule requête ;
- **c.** 8 lignes au lieu de 22, pour le même comportement. La méthode n'est plus `public` : seuls la classe et
  son test l'utilisent.

Le test `BeerServiceJPATest` (JUnit seul, sans Spring) fige 11 cas, par exemple : la page 1 devient la page 0, une
taille de 5000 est ramenée à 1000, une taille de 0 donne 25.

### 2. `showInventory=false` vide les DTO, plus les entités

**Le problème :** avec `showInventory=false`, le client ne doit pas voir le stock (`quantityOnHand`). JT l'efface
**sur les entités** `Beer`, avant de les transformer en DTO. Or une entité chargée par Hibernate est *suivie* : dans
une transaction, Hibernate prend ce champ vidé pour une vraie modification et l'écrit dans la base au moment du
*flush* (l'envoi des changements à la base). Le stock de ces bières serait effacé pour de bon.

**Ce qui a changé** ([`b7e134fc`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/b7e134fc), Sec16_Chap169) :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
-if (showInventory != null && !showInventory) {
-    beerPage.forEach(beer ->
-            beer.setQuantityOnHand(null));
-}
-
-return beerPage.map(beerMapper::beerToBeerDto);
```

</td>
<td>

```diff
+Page<BeerDTO> beerDtoPage =
+        beerPage.map(beerMapper::beerToBeerDto);
+
+// hide the stock on the copies the client gets,
+// never on the entities
+if (showInventory != null && !showInventory) {
+    beerDtoPage.forEach(beerDto ->
+            beerDto.setQuantityOnHand(null));
+}
+
+return beerDtoPage;
```

</td>
</tr>
</table>

**Pourquoi :** les DTO sont des copies faites pour le client ; Hibernate ne les suit pas. On peut donc les modifier
sans risque pour la base.

*Analogie :* pour cacher un prix sur un devis, on noircit la photocopie, pas l'original rangé dans le classeur.

### 3. La réponse JSON en `PagedModel`

**Le problème :** JT envoie la `Page` telle quelle en JSON. Elle contient une douzaine de champs, dont beaucoup sont
la cuisine interne de Spring (`pageable`, `sort`, `empty`, `numberOfElements`...). Spring Data affiche d'ailleurs un
avertissement à chaque démarrage : il ne promet rien sur la forme de ce JSON, qui peut changer d'une version à
l'autre.

**Ce qui a changé** ([`47727ac5`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/47727ac5), Sec16_Chap168),
dans `src/main/java/guru/springframework/spring7restmvc/Spring7RestMvcApplication.java` :

```diff
 @SpringBootApplication
+@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
 public class Spring7RestMvcApplication {
```

La réponse devient un `PagedModel`, un format documenté par Spring Data. Exemple pris du projet, pour
`GET /api/v1/beer?beerName=IPA&beerStyle=IPA&pageNumber=2&pageSize=50` :

```json
{
  "content": [ "... 50 bières, triées par nom ..." ],
  "page": { "size": 50, "number": 1, "totalElements": 324, "totalPages": 7 }
}
```

**Pourquoi :** le client reçoit les bières (`content`) et quatre valeurs documentées sous `page`, et l'avertissement
disparaît. Les six tests qui vérifient un total lisent maintenant `$.page.totalElements` au lieu de
`$.totalElements`.

**Un piège :** cette annotation remplace la configuration web de Spring Data que Spring Boot ferait tout seul. Les
propriétés `spring.data.web.pageable.*` ne s'appliquent donc plus. Ce projet ne les utilise pas : ses valeurs par
défaut sont dans `BeerServiceJPA`.

## Ajouts

### 4. Les tests vérifient le contenu des pages, pas seulement leur taille

**Le problème :** après le passage aux pages, les tests de liste de
`src/test/java/guru/springframework/spring7restmvc/controller/BeerControllerIT.java` ne vérifiaient plus que la
**taille** de la page. Or cette taille est simplement la taille par défaut. Les cinq tests auraient passé avec une
recherche complètement cassée, par exemple une recherche « IPA » qui renverrait n'importe quelles 25 bières.

**Ce qui a changé** ([`59e259da`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/59e259da), Sec16_Chap168) :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
 .queryParam("beerName", "IPA")
 .queryParam("beerStyle", BeerStyle.IPA.name())
 .queryParam("showInventory", "false")
-.queryParam("pageSize", "800"))
 .andExpect(status().isOk())
-.andExpect(jsonPath("$.content.size()",
-        is(310)))
-.andExpect(jsonPath(
-        "$.content[0].quantityOnHand")
-        .value(IsNull.nullValue()));
```

</td>
<td>

```diff
 .queryParam("beerName", "IPA")
 .queryParam("beerStyle", BeerStyle.IPA.name())
 .queryParam("showInventory", "false"))
 .andExpect(status().isOk())
+.andExpect(jsonPath("$.content.size()")
+        .value(DEFAULT_PAGE_SIZE))
+.andExpect(jsonPath("$.page.totalElements")
+        .value(BEERS_NAMED_IPA_OF_STYLE_IPA))
+.andExpect(jsonPath("$.content..beerName",
+        everyItem(containsStringIgnoringCase("IPA"))))
+.andExpect(jsonPath("$.content..beerStyle",
+        everyItem(is(BeerStyle.IPA.name()))))
+// showInventory=false keeps the field but empties it
+.andExpect(jsonPath("$.content..quantityOnHand",
+        everyItem(nullValue())));
```

</td>
</tr>
</table>

**Pourquoi :** `everyItem(...)` vérifie **chaque** bière de la page : son nom contient « IPA », son style est `IPA`,
son stock est vide. Le total (`$.page.totalElements`, 324) prouve en plus que la recherche a trouvé toutes les
bières, pas seulement une page.

**Un piège :** `showInventory=false` ne **retire** pas `quantityOnHand` du JSON : il le met à `null`. L'assertion
`jsonPath("$.content..quantityOnHand").doesNotExist()` échoue donc, avec `found: [null, null, ...]`. La bonne
assertion est `everyItem(nullValue())`. Trouvé en lançant les tests, pas en lisant le code.

### 5. La page 2 contient d'autres bières que la page 1

**Le problème :** compter les bières d'une page ne prouve pas que la pagination marche. Une implémentation qui
ignorerait `pageNumber` et renverrait toujours les 50 premières bières passerait ce test.

**Ce qui a changé** ([`2adc6143`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/2adc6143), Sec16_Chap169),
un nouveau test dans `BeerControllerIT` :

```java
@Test
void testSecondPageHoldsOtherBeersThanTheFirst() {
    Page<BeerDTO> firstPage = beerController.listBeers("IPA", BeerStyle.IPA, true, 1, PAGE_SIZE);
    Page<BeerDTO> secondPage = beerController.listBeers("IPA", BeerStyle.IPA, true, 2, PAGE_SIZE);

    assertThat(firstPage.getContent()).hasSize(PAGE_SIZE);
    assertThat(secondPage.getContent()).hasSize(PAGE_SIZE);

    List<UUID> beersOfTheFirstPage = firstPage.getContent().stream().map(BeerDTO::getId).toList();

    assertThat(secondPage.getContent())
            .extracting(BeerDTO::getId)
            .doesNotContainAnyElementsOf(beersOfTheFirstPage);
}
```

**Pourquoi :** le test compare les identifiants des deux pages : aucune bière de la page 2 ne doit être sur la
page 1. Il prouve à la fois que `pageNumber` est pris en compte et que le tri empêche les doublons d'une page à
l'autre. Dans le même commit, `BeerServiceJPATest` vérifie que le tri porte sur `beerName`, et que ce nom est
vraiment un champ de `Beer`.

### 6. Des tests plus clairs : `DEFAULT_PAGE_SIZE` et `BeerRepositoryTest`

**Le problème :**

- **a.** dans `BeerControllerIT`, le nombre `25` apparaissait 7 fois, sans dire d'où il venait ;
- **b.** dans `src/test/java/guru/springframework/spring7restmvc/repositories/BeerRepositoryTest.java`, JT passe
  `null` comme `Pageable` : la requête n'est donc pas paginée, et le test ne prouve rien sur les pages.

**Ce qui a changé** ([`59e259da`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/59e259da), Sec16_Chap168,
et [`2ce2a98f`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/2ce2a98f), Sec16_Chap168) :

- **a.** une constante `private static final int DEFAULT_PAGE_SIZE = 25;` dans `BeerControllerIT`, à la valeur de
  `BeerServiceJPA.DEFAULT_PAGE_SIZE`. La constante `PAGE_SIZE = 50` reste pour le test qui choisit sa taille exprès ;
- **b.** `BeerRepositoryTest` :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
 @Test
 void testGetBeerListByName() {
-    Page<Beer> list = beerRepository
-        .findAllByBeerNameIsLikeIgnoreCase(
-            "%IPA%", null);
-
-    assertThat(list.getContent().size())
-        .isEqualTo(336);
 }
```

</td>
<td>

```diff
 @Test
 void testGetBeerListByName() {
+    PageRequest firstPage = PageRequest.of(0, 25);
+
+    Page<Beer> page = beerRepository
+        .findAllByBeerNameIsLikeIgnoreCase(
+            "%IPA%", firstPage);
+
+    // the page comes back full, and it also
+    // reports how many beers match in total
+    assertThat(page.getContent())
+        .hasSize(firstPage.getPageSize())
+        .allSatisfy(beer -> assertThat(
+            beer.getBeerName())
+                .containsIgnoringCase("IPA"));
+    assertThat(page.getTotalElements())
+        .isEqualTo(BEERS_NAMED_IPA);
 }
```

</td>
</tr>
</table>

**Pourquoi :** **a.** `DEFAULT_PAGE_SIZE` dit d'où vient le nombre. **b.** Le test prouve les deux faces d'une
page : son contenu (25 bières, toutes « IPA ») et le total de la recherche (336).

## Où en est l'API à la fin de la Section 16

| Comportement | Règle |
|---|---|
| Numéros de page | l'API compte à partir de 1, Spring Data à partir de 0 ; `buildPageRequest` fait la traduction |
| Aucun paramètre | page 1 avec 25 bières (`DEFAULT_PAGE_SIZE`) |
| `pageSize` au-dessus de 1000 | ramené à `MAX_PAGE_SIZE` |
| `pageNumber` ou `pageSize` en dessous de 1 | traité comme absent : la valeur par défaut s'applique (bloc 1) |
| Ordre | toujours par `beerName` croissant |
| `showInventory=false` | `quantityOnHand` vidé **sur les DTO**, jamais sur les entités (bloc 2) |
| Forme de la réponse | `PagedModel` : `content`, plus quatre valeurs sous `page` (bloc 3) |
