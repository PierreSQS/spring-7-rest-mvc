# Section 13, Chap143 : `@ServiceConnection`, ce que nous avons amélioré

**Branche :** `78.4-Using-Service-Connection-sb411` · **Section / Chapitre :** Sec13_Chap143 · **Commit(s) :**
[`663d6b81`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/663d6b81),
[`3a9ce02e`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/3a9ce02e),
[`899af040`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/899af040),
[`9ed60d52`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/9ed60d52)

**Contexte :** le test `MySqlTest` tourne contre une vraie base MySQL, lancée dans un *container* Docker par
**Testcontainers** (une bibliothèque qui démarre des containers pour les tests). À la leçon précédente (Chap142),
il fallait dire à Spring, à la main, où trouver cette base : son URL, son utilisateur et son mot de passe changent à
chaque démarrage. Dans cette leçon, `@ServiceConnection` fait ce travail tout seul. Ce document dit ce que nous avons
fait **mieux que JT**, puis ce que nous avons **ajouté**. C'est une photo de la fin de la leçon (18-09-2026) : il
n'est jamais mis à jour.

Réécrit le 26-11-2026 avec les mêmes faits ; l'ancienne version, en anglais, reste sur la branche
`109-lock-demo-optimistic-sb411` ([`69f0a32b`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec13-chap143-service-connection-improvements.md)).

## La leçon de JT

Branche de JT : `78.4-Using-Service-Connection`. JT remplace la méthode `@DynamicPropertySource`, qui copiait à la
main l'URL, l'utilisateur et le mot de passe du container dans les propriétés `spring.datasource.*`, par une seule
annotation, `@ServiceConnection`, posée sur le container. Spring Boot reconnaît un container MySQL et en déduit
lui-même la connexion.

## Mieux que JT

### 1. `@ServiceConnection` sur le container partagé de `MySqlContainerBase`

**Le problème :** JT déclare le container **dans la classe de test** `MySqlTest`, avec `@Container`. Chaque classe
de test qui veut MySQL doit déclarer son propre container, et chacune le redémarre. Chez nous, depuis Chap142, le
container vit déjà dans une classe mère partagée,
`src/test/java/guru/springframework/spring7restmvc/MySqlContainerBase.java`.

**Ce qui a changé** ([`663d6b81`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/663d6b81), Sec13_Chap143) :
`@ServiceConnection` va sur le container de cette classe mère, et la méthode `@DynamicPropertySource` disparaît.

<table>
<tr><th>JT : <code>MySqlTest</code></th><th>Nous : <code>MySqlContainerBase</code></th></tr>
<tr>
<td>

```diff
 @Testcontainers
 @SpringBootTest
 @ActiveProfiles("localmysql")
 public class MySqlTest {

-    @Container
     @ServiceConnection
-    static MySQLContainer<?> mySQLContainer =
-            new MySQLContainer<>("mysql:9.2");
     // ...
```

</td>
<td>

```diff
 @ActiveProfiles("testcontainers")
 public abstract class MySqlContainerBase {

     @ServiceConnection
+    protected static final MySQLContainer MY_SQL_CONTAINER =
+            new MySQLContainer("mysql:8.4");
+
+    static {
+        MY_SQL_CONTAINER.start();
+    }
 }
```

</td>
</tr>
</table>

**Pourquoi :** le container est un *singleton* : un seul container, démarré une fois (dans le bloc `static`), sert à
toute la suite de tests. Une classe de test qui veut MySQL n'a qu'à hériter de `MySqlContainerBase`
(`class MySqlTest extends MySqlContainerBase`). Nous avons vérifié que Spring Boot trouve bien `@ServiceConnection`
sur ce champ hérité, qui n'est pas un champ `@Container` : `MySqlTest` se connecte au MySQL 8.4 du container, et les
41 tests passent.

## Ajouts

### 2. `BootstrapData` dit dans le log s'il a chargé les données

**Le problème :** `BootstrapData`
(`src/main/java/guru/springframework/spring7restmvc/bootstrap/BootstrapData.java`) insère les données de départ
(3 bières, 3 customers) seulement quand les tables sont vides. Rien dans le log ne disait s'il l'avait fait ou non.

**Ce qui a changé** ([`899af040`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/899af040), Sec13_Chap143) :

```diff
     beerRepository.save(beer3);
+
+    log.info("### {} beers loaded into the DB", beerRepository.count());
+} else {
+    log.info("### Beers are present in the DB. Bootstrap skipped");
 }
 // ... de même pour les customers
```

**Pourquoi :** le log du serveur montre tout de suite ce qui s'est passé. Contre le MySQL local, déjà rempli :
deux fois `Bootstrap skipped`. Dans les tests, sur une base neuve : `3 ... loaded`. C'est ce log qui a révélé le
problème du bloc 3.

### 3. Les données ne sont plus chargées deux fois dans `MySqlTest`

**Le problème :** dans `src/test/java/guru/springframework/spring7restmvc/repositories/MySqlTest.java`, les données
étaient chargées **deux fois** :

- **a.** `@Import(BootstrapData.class)` ajoute `BootstrapData` au test. Comme c'est un `CommandLineRunner`
  (une classe que Spring Boot exécute automatiquement au démarrage), elle charge les données **dès le démarrage du test** ;
- **b.** puis la méthode `setUp()`, marquée `@BeforeEach`, appelait **encore** `bootstrapData.run()` avant chaque test.

Le log du bloc 2 le montrait : d'abord `### 3 beers loaded into the DB`, puis
`### Beers are present in the DB. Bootstrap skipped`.

**Ce qui a changé** ([`9ed60d52`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/9ed60d52), Sec13_Chap143) :

<table>
<tr><th>Avant</th><th>Après</th></tr>
<tr>
<td>

```diff
 @DataJpaTest
 @Import(BootstrapData.class)
 class MySqlTest extends MySqlContainerBase {

     @Autowired
     BeerRepository beerRepository;

-    @Autowired
-    BootstrapData bootstrapData;
-
-    @BeforeEach
-    void setUp() {
-        bootstrapData.run();
-    }

     @Test
     void testListBeers() {
         // ...
```

</td>
<td>

```diff
 @DataJpaTest
+// once imported, it runs as a CommandLineRunner
+// when the test context starts and seeds the data
 @Import(BootstrapData.class)
 class MySqlTest extends MySqlContainerBase {

     @Autowired
     BeerRepository beerRepository;

     @Test
     void testListBeers() {
         // ...
```

</td>
</tr>
</table>

**Pourquoi :** un seul chargement, au démarrage, fait par Spring lui-même. Le test est plus court, et il ne laisse
plus croire que les données dépendent de `setUp()`. Après le changement, le log n'affiche plus que
`### 3 beers loaded into the DB`.

### 4. `@AutoConfigureTestDatabase` supprimé : il ne servait à rien

**Le problème :** `@DataJpaTest` remplace normalement la base par H2, une base en mémoire. Pour garder MySQL, notre
`MySqlTest` de Chap142 ajoutait donc
`@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`, « ne remplace rien ».

**Ce qui a changé** ([`3a9ce02e`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/3a9ce02e), Sec13_Chap143) :

```diff
-// keep the container's datasource instead of letting @DataJpaTest swap in an embedded H2
-@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
```

**Pourquoi :** dans Spring Boot 4.1.1, la valeur par défaut est déjà `replace = NON_TEST` : H2 ne remplace que les
bases que le **test** n'a pas fournies lui-même. Une base fournie par `@ServiceConnection` (ou par
`@DynamicPropertySource`) est gardée. Un essai sans l'annotation l'a confirmé : `MySqlTest` reste sur le MySQL du
container. Elle n'avait donc jamais servi, même en Chap142.
