# Section 14 : charger les bières depuis un fichier CSV, ce que nous avons amélioré

**Branche :** `85-beer-csv-fix-integration-tests-sb411` · **Section / Chapitre :** Sec14_Chap147 à Sec14_Chap153 · **Commit(s) :**
[`1ceb5b89`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/1ceb5b89),
[`2ee70628`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/2ee70628),
[`5bb74a9c`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/5bb74a9c),
[`5b5d1616`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/5b5d1616),
[`27507afb`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/27507afb),
[`4bf77919`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/4bf77919),
[`89f0f3d8`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/89f0f3d8),
[`0a9c201e`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/0a9c201e),
[`cff589a0`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/cff589a0),
[`955ace7e`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/955ace7e)

**Contexte :** jusqu'ici, `BootstrapData` créait seulement 3 bières à la main. Cette section charge en plus, au
démarrage, toutes les bières d'un fichier CSV (*comma-separated values* : un tableau en texte, une ligne par bière,
les colonnes séparées par des virgules) : `src/main/resources/csvdata/beers.csv`, **2410 bières**, 15 colonnes,
100 noms de style différents. Ce document dit ce que nous avons fait **mieux que JT**, puis ce que nous avons
**ajouté**. C'est une photo de la fin de la section (21-09-2026) : il n'est jamais mis à jour.

Réécrit le 26-11-2026 avec les mêmes faits ; l'ancienne version, en anglais, reste sur la branche
`109-lock-demo-optimistic-sb411` ([`69f0a32b`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec14-csv-improvements.md)).

## La leçon de JT

Branches de JT : `79-beer-csv-data` à `85-beer-csv-fix-integration-tests`. JT :

- ajoute le fichier `beers.csv` et la classe `BeerCSVRecord`, dont chaque champ correspond à une colonne du CSV,
  grâce à la bibliothèque **OpenCSV** (`@CsvBindByName`) ;
- écrit le service `BeerCsvServiceImpl`, qui lit le fichier et rend une liste de `BeerCSVRecord` ;
- dans `BootstrapData`, transforme chaque `BeerCSVRecord` en `Beer` et l'enregistre, quand la table contient moins
  de 10 bières ;
- adapte les tests, qui attendent maintenant 2413 bières (3 écrites à la main + 2410 du CSV).

Le chapitre 152 (branche de JT `84-hibernate-create-update-timestamp`) a été sauté : `@CreationTimestamp` et
`@UpdateTimestamp` étaient déjà sur `Beer` depuis une leçon précédente.

## Mieux que JT

### 1. Lire le CSV comme une `Resource`, en UTF-8

**Le problème :** dans `src/main/java/guru/springframework/spring7restmvc/services/BeerCsvServiceImpl.java`, JT lit
le fichier avec un `File` et un `FileReader`. Deux défauts :

- **a.** `BootstrapData` trouve le fichier avec `ResourceUtils.getFile("classpath:csvdata/beers.csv")`. Cela marche
  dans l'IDE, mais **plus quand l'application tourne depuis le jar** : dans un jar, le CSV n'est plus un fichier du
  disque, et Java lance une `FileNotFoundException` ;
- **b.** `FileReader` lit le texte avec l'encodage par défaut de la machine, ici **cp1252** (Windows) : tous les noms
  accentués sont abîmés.

**Ce qui a changé** ([`5bb74a9c`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/5bb74a9c), Sec14_Chap150, et
[`2ee70628`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/2ee70628), Sec14_Chap149) :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
 @Override
-public List<BeerCSVRecord> convertCSV(File csvFile) {
-    try {
-        List<BeerCSVRecord> beerCSVRecords =
-          new CsvToBeanBuilder<BeerCSVRecord>(
-            new FileReader(csvFile))
-                .withType(BeerCSVRecord.class)
-                .build().parse();
-        return beerCSVRecords;
-    } catch (FileNotFoundException e) {
-        throw new RuntimeException(e);
     }
 }
```

</td>
<td>

```diff
 @Override
+public List<BeerCSVRecord> convertCSV(Resource csvResource) {
+    try (Reader reader = new InputStreamReader(
+            csvResource.getInputStream(), StandardCharsets.UTF_8)) {
+        return new CsvToBeanBuilder<BeerCSVRecord>(reader)
+                .withType(BeerCSVRecord.class)
+                .build()
+                .parse();
+    } catch (IOException e) {
+        throw new UncheckedIOException(
+            "Could not read " + csvResource.getDescription(), e);
     }
 }
```

</td>
</tr>
</table>

Et dans `BootstrapData`, `ResourceUtils.getFile(...)` devient `new ClassPathResource(BEERS_CSV)`.

**Pourquoi :** une `Resource` de Spring se lit comme un flux (*stream*), que le CSV soit un fichier du disque ou une
entrée du jar. `StandardCharsets.UTF_8` lit le texte dans l'encodage réel du fichier, quelle que soit la machine.
Enfin, `UncheckedIOException` garde dans son message le nom du fichier qui manque. Deux tests de
`BeerCsvServiceImplTest` le prouvent : `decodesTheFileAsUtf8` et `failsOnAMissingFile`.

### 2. Les styles de bière classés par mots-clés

**Le problème :** le CSV contient 100 noms de style (« American IPA », « Oatmeal Stout »...), mais l'enum `BeerStyle`
n'a que 10 valeurs. Dans `BootstrapData`, JT les relie avec un `switch` qui ne nomme que 16 des 100 styles ; tous les
autres tombent dans `default -> BeerStyle.PILSNER`. Résultat : **1160 bières sur 2410** devenaient des Pilsner.

**Ce qui a changé** ([`27507afb`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/27507afb) et
[`4bf77919`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/4bf77919), Sec14_Chap151) :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
-BeerStyle beerStyle =
-    switch (beerCSVRecord.getStyle()) {
-  case "American Pale Lager" -> BeerStyle.LAGER;
-  case "American IPA",
-       "American Double / Imperial IPA",
-       "Belgian IPA" -> BeerStyle.IPA;
-  case "Oatmeal Stout",
-       "American Stout" -> BeerStyle.STOUT;
   // ...
-  default -> BeerStyle.PILSNER;
-};
```

</td>
<td>

```diff
+static BeerStyle beerStyleOf(String csvStyle) {
+    String style = csvStyle.toUpperCase();
+
+    if (style.contains("IPA")) return BeerStyle.IPA;
+    if (style.contains("STOUT")) return BeerStyle.STOUT;
+    if (style.contains("PORTER")) return BeerStyle.PORTER;
     // ...
+    return BeerStyle.ALE;
+}
```

</td>
</tr>
</table>

**Pourquoi :** un mot-clé couvre d'un coup tous les styles qui le contiennent. Par exemple, `"IPA"` couvre
« American IPA », « Belgian IPA » et « American Double / Imperial IPA ». Après le changement, il ne reste que
**91 Pilsner**, ceux dont le style contient vraiment « Pilsner » ou « Pils ». L'ordre des tests compte :
« American Double / Imperial IPA » doit être une IPA avant d'être autre chose. Le test `BeerStyleMappingTest` fige ces
règles (`4bf77919`).

### 3. Des tests qui ne dépendent pas de la taille du CSV

**Le problème :** JT écrit le nombre de bières en dur dans deux tests : `assertThat(...).isEqualTo(2413)` dans
`BootstrapDataTest`, et `assertThat(dtos.size()).isEqualTo(2413)` dans `BeerControllerIT`. Chaque fois que le CSV
change, il faut corriger ces nombres à la main.

**Ce qui a changé** ([`89f0f3d8`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/89f0f3d8), Sec14_Chap153),
dans `src/test/java/guru/springframework/spring7restmvc/bootstrap/BootstrapDataTest.java` :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
 @Test
 void Testrun() throws Exception {
     bootstrapData.run(null);

-    assertThat(beerRepository.count())
-            .isEqualTo(2413);
 }
```

</td>
<td>

```diff
 @Test
 void testRun() {
+    int csvRecords = beerCsvService.convertCSV(
+        new ClassPathResource("csvdata/beers.csv")).size();
+
     bootstrapData.run();

+    // every CSV record becomes a beer,
+    // on top of the three written by hand
+    assertThat(beerRepository.count())
+            .isEqualTo(HANDWRITTEN_ROWS + csvRecords);
 }
```

</td>
</tr>
</table>

Dans `BeerControllerIT`, `testListBeers` compare maintenant la liste avec ce que contient la base :
`assertThat(dtos).isNotEmpty().hasSize((int) beerRepository.count())`.

**Pourquoi :** le nombre attendu est calculé, pas recopié. Si le CSV gagne ou perd des lignes, les tests restent
justes sans être modifiés.

### 4. `BeerCSVRecord` plus complet et plus strict

**Le problème :** dans `src/main/java/guru/springframework/spring7restmvc/model/BeerCSVRecord.java`, JT :

- **a.** ne lie pas la colonne `brewery` : le nom de la brasserie est lu puis jeté ;
- **b.** nomme un champ `count_y` : Lombok génère alors `getCount_y()`, contraire aux noms Java habituels ;
- **c.** n'oblige aucune colonne à être remplie : une ligne abîmée donnerait des `null` sans prévenir.

**Ce qui a changé** ([`1ceb5b89`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/1ceb5b89), Sec14_Chap149) :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
-@CsvBindByName
 private Integer row;
 // ...
-@CsvBindByName
 private String beer;
 // ...
 @CsvBindByName(column = "count.y")
-private String count_y;
```

</td>
<td>

```diff
+@CsvBindByName(required = true)
 private Integer row;
 // ...
+@CsvBindByName(required = true)
 private String beer;
 // ...
 @CsvBindByName(column = "count.y")
+private String countY;
+
+@CsvBindByName
+private String brewery;
```

</td>
</tr>
</table>

**Pourquoi :** les 15 colonnes du CSV sont maintenant toutes lues. `required = true` est posé sur les 7 colonnes qui
ne sont jamais vides dans les 2410 lignes (`row`, `count`, `id`, `beer`, `style`, `breweryId`, `ounces`) : si un
jour une ligne abîmée arrive, OpenCSV échoue tout de suite au lieu de laisser passer des `null`. Le test
`BeerCSVRecordTest` (`2ee70628`) vérifie que les 2410 lignes sont lues et que les 15 colonnes de la première ligne
arrivent dans les bons champs.

### 5. Deux petites corrections dans `BootstrapData` et son test

**Le problème :**

- **a.** pour couper les noms trop longs (la colonne `beer_name` fait 50 caractères), JT utilise
  `StringUtils.abbreviate(...)`. `StringUtils` vient de la bibliothèque commons-lang3, que le projet ne déclare pas
  lui-même : il ne l'a que **par transitivité**, parce qu'OpenCSV en dépend. Si OpenCSV changeait de dépendance, le
  code ne compilerait plus ;
- **b.** dans `BootstrapDataTest`, JT crée `BootstrapData` lui-même avec `new BootstrapData(...)`, au lieu de laisser
  Spring le fournir.

**Ce qui a changé** ([`5b5d1616`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/5b5d1616), Sec14_Chap151) :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
-.beerName(StringUtils.abbreviate(
-        beerCSVRecord.getBeer(), 50))
 // ...
-@Import(BeerCsvServiceImpl.class)
 class BootstrapDataTest {
     // ...
-    BootstrapData bootstrapData;
-
-    @BeforeEach
-    void setUp() {
-        bootstrapData = new BootstrapData(
-            beerRepository, customerRepository,
-            csvService);
-    }
```

</td>
<td>

```diff
+.beerName(truncateBeerName(
+        beerCSVRecord.getBeer()))
 // ...
+@Import({BootstrapData.class,
+         BeerCsvServiceImpl.class})
 class BootstrapDataTest {
     // ...
+    @Autowired
+    BootstrapData bootstrapData;
```

</td>
</tr>
</table>

**Pourquoi :** **a.** `truncateBeerName` est une petite méthode du projet, sans dépendance cachée. **b.** C'est la
convention du projet : dans une tranche de test (`@DataJpaTest`, qui ne charge que la partie JPA), on ajoute le
composant avec `@Import` et on le reçoit par `@Autowired`, plutôt que de le construire à la main. Le test vérifie
ainsi `BootstrapData` tel que Spring le crée vraiment.

## Ajouts

### 6. Le double encodage du fichier CSV réparé

**Le problème :** le fichier `beers.csv` contenait du texte UTF-8 qui avait été lu une fois par erreur en cp1252, puis
réenregistré. **115 des 2411 lignes** (en-tête compris) portaient des noms abîmés, que la base recevait tels quels et
que l'API renvoyait :

| Dans le fichier | Correct |
|---|---|
| `KÃ¶lsch` | `Kölsch` |
| `MÃ¤rzen / Oktoberfest` | `Märzen / Oktoberfest` |
| `Garce SelÃ©` | `Garce Selé` |
| `Avery Joeâ€™s Premium American Pilsner` | `Avery Joe's Premium American Pilsner` |

**Ce qui a changé** ([`0a9c201e`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/0a9c201e), Sec14_Chap153) :
le fichier est réparé en faisant le chemin inverse de l'erreur, octet par octet : le texte abîmé est réécrit en
cp1252, puis relu en UTF-8. Le décodage est *strict* : une suite d'octets impossible à réparer arrête l'opération,
au lieu de produire le caractère de remplacement `�` (U+FFFD). Les tests qui figeaient les noms abîmés
(`BeerCsvServiceImplTest`, `BeerStyleMappingTest`) figent maintenant les noms corrects.

**Pourquoi :** les noms sont justes dans la base et dans l'API. Le signe que l'erreur venait de cp1252, et pas de
Latin-1 : `â€™`. En cp1252, l'octet `0x80` devient `€` et `0x99` devient `™`, ce que Latin-1 ne fait pas.

**Un piège :** le mot-clé `RZEN` de `beerStyleOf` (bloc 2) reste en ASCII exprès. Il reconnaît `MÄRZEN` aussi bien
que l'ancienne orthographe abîmée `MÃ¤RZEN`.

### 7. `MySqlIT` vérifie le nombre de bières chargées

**Le problème :** `src/test/java/guru/springframework/spring7restmvc/repositories/MySqlIT.java` charge 2413 bières
dans un vrai MySQL (le container de Testcontainers), mais ne vérifiait que `isNotEmpty()` : « au moins une bière ».
Une bière perdue en route ne se voyait pas.

**Ce qui a changé** ([`cff589a0`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/cff589a0), Sec14_Chap153) :

```diff
+int csvRecords = beerCsvService.convertCSV(new ClassPathResource("csvdata/beers.csv")).size();
+
 List<Beer> beers = beerRepository.findAll();

-assertThat(beers).isNotEmpty();
+// real MySQL, not H2: every CSV record survived the varchar(36) ids, the SMALLINT style
+// and the 50-character name column
+assertThat(beers).hasSize(HANDWRITTEN_BEERS + csvRecords);
```

**Pourquoi :** MySQL a ses propres limites de colonnes : des identifiants en `varchar(36)`, le style en `SMALLINT`,
le nom limité à 50 caractères. Si l'une d'elles faisait perdre une bière, le test échouerait au lieu de passer sans
rien voir.

### 8. Les noms de bière coupés apparaissent dans le log

**Le problème :** 2 des 2410 noms dépassent les 50 caractères de la colonne. `truncateBeerName` (bloc 5) les coupait
**en silence** : personne ne savait quels noms étaient raccourcis.

**Ce qui a changé** ([`955ace7e`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/955ace7e), Sec14_Chap153) :

```diff
 String truncated = beerName.substring(0, MAX_BEER_NAME_LENGTH - 3) + "...";
+log.warn("### Beer name longer than {} characters, stored as '{}': {}",
+        MAX_BEER_NAME_LENGTH, truncated, beerName);
 return truncated;
```

**Pourquoi :** au démarrage, le log montre le nom enregistré **et** le nom complet. Les deux lignes du log
(une ligne par nom, coupées ici en deux pour la largeur) :

```text
### Beer name longer than 50 characters, stored as 'Lee Hill Series Vol. 3 - Barrel Aged Imperial S...':
    Lee Hill Series Vol. 3 - Barrel Aged Imperial Stout
### Beer name longer than 50 characters, stored as 'Lee Hill Series Vol. 5 - Belgian Style Quadrupe...':
    Lee Hill Series Vol. 5 - Belgian Style Quadrupel Ale
```
