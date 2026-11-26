# Section 13, Chap142 : MySQL dans Docker avec `@DynamicPropertySource`, ce que nous avons amélioré

**Branche :** `78.3-using-dynamic-properties-sb411` · **Section / Chapitre :** Sec13_Chap142 · **Commit(s) :**
[`d0987762`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/d0987762),
[`68850b73`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/68850b73)

**Contexte :** jusqu'ici, les tests tournaient sur H2, une base en mémoire. Cette leçon ajoute un premier test contre
une **vraie base MySQL**, lancée dans un *container* Docker par **Testcontainers** (une bibliothèque qui démarre des
containers pour les tests). Le container change d'URL, d'utilisateur et de mot de passe à chaque démarrage : il faut
donc les donner à Spring. Ce document dit ce que nous avons fait **mieux que JT**, puis ce que nous avons
**ajouté**. C'est une photo de la fin de la leçon (18-09-2026) : il n'est jamais mis à jour.

Réécrit le 26-11-2026 avec les mêmes faits ; l'ancienne version, en anglais, reste sur la branche
`109-lock-demo-optimistic-sb411` ([`69f0a32b`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec13-chap142-dynamic-properties-improvements.md)).

## La leçon de JT

Branche de JT : `78.3-using-dynamic-properties`. JT ajoute le test
`src/test/java/guru/springframework/spring7restmvc/repositories/MySqlTest.java` : un `@SpringBootTest` qui démarre
un container MySQL (`@Container`), puis copie à la main, avec une méthode `@DynamicPropertySource`, l'URL,
l'utilisateur et le mot de passe du container dans les propriétés `spring.datasource.*` :

```java
@DynamicPropertySource
static void mySqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.username", mySQLContainer::getUsername);
    registry.add("spring.datasource.password", mySQLContainer::getPassword);
    registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
}
```

## Mieux que JT

### 1. `MySQLContainer` de Testcontainers 2.x, et un test plus net

**Le problème :** dans `MySqlTest`, JT utilise `org.testcontainers.containers.MySQLContainer<?>`, l'ancienne classe de
Testcontainers 1.x, **dépréciée** dans la version 2.0.5 du projet. Le test garde aussi un champ `DataSource` jamais
utilisé.

**Ce qui a changé** ([`d0987762`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/d0987762), Sec13_Chap142) :

<table>
<tr><th>Avant (JT)</th><th>Après</th></tr>
<tr>
<td>

```diff
-import org.testcontainers.containers.MySQLContainer;
 // ...
-public class MySqlTest {
     @Container
-    static MySQLContainer<?> mySQLContainer =
-            new MySQLContainer<>("mysql:9.2");
 // ...
-    @Autowired
-    DataSource dataSource;
 // ...
-    assertThat(beers.size()).isGreaterThan(0);
```

</td>
<td>

```diff
+import org.testcontainers.mysql.MySQLContainer;
 // ...
+class MySqlTest {
     @Container
+    static MySQLContainer mySQLContainer =
+            new MySQLContainer("mysql:9.2");
 // ...
 // ...
 // ...
+    assertThat(beers).hasSizeGreaterThan(0);
```

</td>
</tr>
</table>

**Pourquoi :**

- **a.** la classe de Testcontainers 2.x vit dans `org.testcontainers.mysql` et n'est plus générique (plus de `<?>`) :
  l'avertissement de dépréciation disparaît ;
- **b.** la classe n'a plus besoin d'être `public` : JUnit 5 n'en a pas besoin ;
- **c.** moins de bruit : plus de champ inutile, et une assertion qui se lit directement.

### 2. Un seul container partagé par toute la suite de tests

**Le problème :** JT déclare le container **dans** `MySqlTest`, avec `@Container`. Chaque classe de test qui voudra
MySQL devra déclarer le sien, et JUnit en démarrera un nouveau pour chaque classe : lent, dès qu'il y aura plusieurs
classes de ce genre.

**Ce qui a changé** ([`68850b73`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/68850b73), Sec13_Chap142) :
une nouvelle classe mère, `src/test/java/guru/springframework/spring7restmvc/MySqlContainerBase.java`, qui porte le
container et la méthode `@DynamicPropertySource`. `MySqlTest` en hérite :

<table>
<tr><th>Avant (JT) : <code>MySqlTest</code></th><th>Après : <code>MySqlContainerBase</code></th></tr>
<tr>
<td>

```diff
 @Testcontainers
 @SpringBootTest
 @ActiveProfiles("localmysql")
 public class MySqlTest {

-    @Container
-    static MySQLContainer<?> mySQLContainer =
-            new MySQLContainer<>("mysql:9.2");

     @DynamicPropertySource
     // ...
```

</td>
<td>

```diff
 @Testcontainers(disabledWithoutDocker = true)
 @ActiveProfiles("testcontainers")
 public abstract class MySqlContainerBase {

+    protected static final MySQLContainer MY_SQL_CONTAINER =
+            new MySQLContainer("mysql:8.4");
+
+    static {
+        MY_SQL_CONTAINER.start();
+    }

     @DynamicPropertySource
     // ...
```

</td>
</tr>
</table>

**Pourquoi :** le container est un *singleton* : un seul container, démarré une fois (dans le bloc `static`), partagé
par toutes les classes qui héritent de `MySqlContainerBase`. Il n'a **pas** `@Container` : sinon JUnit l'arrêterait
après chaque classe. C'est Ryuk, un petit container de Testcontainers, qui le supprime à la fin de la JVM.

### 3. MySQL 8.4 au lieu de 9.2

**Le problème :** JT démarre `mysql:9.2`. La ligne 9.x de MySQL est faite de versions *innovation*, qui vivent peu de
temps ; le serveur MySQL local tournait alors en 8.0.27. Le test ne tournait donc pas sur une version proche de
celle réellement utilisée.

**Ce qui a changé** ([`68850b73`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/68850b73), Sec13_Chap142),
dans `MySqlContainerBase` :

```diff
-new MySQLContainer<>("mysql:9.2");
+// 8.4 is MySQL's long-term-support line; 9.x releases are short-lived "innovation" releases
+new MySQLContainer("mysql:8.4");
```

**Pourquoi :** 8.4 est la ligne **LTS** (*long-term support*) de MySQL : une version stable et maintenue longtemps.

### 4. Un profil `testcontainers` à la place de `localmysql`

**Le problème :** un *profil* Spring est un paquet de réglages qu'on active d'un mot. JT active `localmysql`, le
profil prévu pour lancer l'application Spring Boot sur ce PC contre le MySQL local. Le test n'a besoin que de
« lancer Flyway et vérifier les tables », mais il reçoit tout le reste :

- **a.** une URL, un utilisateur et un mot de passe **jetés aussitôt** par `@DynamicPropertySource` : un lecteur peut
  croire que le test parle à la base locale ;
- **b.** du **bruit** : `localmysql` affiche chaque valeur envoyée à la base. Sur une exécution, 45 lignes du log sur
  227 n'étaient que ça (`binding parameter (1:VARCHAR) <- ...`) ;
- **c.** un **lien caché** : modifier `localmysql` pour le travail local (couper Flyway, changer le port) change le test
  sans qu'on s'en rende compte.

Une analogie : on a besoin d'une agrafeuse, et on emprunte tout le bureau du collègue.

**Ce qui a changé** ([`68850b73`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/68850b73), Sec13_Chap142) :
un profil réservé aux tests, `src/test/resources/application-testcontainers.properties`, activé par
`@ActiveProfiles("testcontainers")` sur `MySqlContainerBase` :

```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

**Pourquoi :** le test ne reçoit que ce dont il a besoin ; le container fournit lui-même la connexion. Les 45 lignes de
valeurs du log sont tombées à 0.

### 5. `@DataJpaTest` au lieu de `@SpringBootTest`

**Le problème :** `@SpringBootTest` démarre **toute** l'application Spring Boot (controllers, services, sécurité…),
alors que `MySqlTest` ne teste qu'un repository.

**Ce qui a changé** ([`68850b73`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/68850b73), Sec13_Chap142) :

<table>
<tr><th>Avant (JT)</th><th>Après</th></tr>
<tr>
<td>

```diff
-@Testcontainers
-@SpringBootTest
-@ActiveProfiles("localmysql")
-public class MySqlTest {
 // ...
```

</td>
<td>

```diff
+@DataJpaTest
+@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
+@Import(BootstrapData.class)
+class MySqlTest extends MySqlContainerBase {
+
+    @Autowired
+    BootstrapData bootstrapData;
+
+    @BeforeEach
+    void setUp() {
+        bootstrapData.run();
+    }
 // ...
```

</td>
</tr>
</table>

**Pourquoi :** `@DataJpaTest` ne charge que la partie JPA (entités, repositories, Flyway) : le test démarre plus vite.
Mais `BootstrapData`, qui insère les données de départ, n'en fait pas partie : il faut l'importer (`@Import`).
`@AutoConfigureTestDatabase(replace = NONE)` devait empêcher `@DataJpaTest` de remplacer MySQL par H2.

**Voir Chap143 :** deux de ces lignes se sont révélées inutiles à la leçon suivante.
`@AutoConfigureTestDatabase` ne servait à rien, et `setUp()` chargeait les données une deuxième fois
(`docs/sec13-chap143-service-connection-improvements.md`, blocs 3 et 4).

## Ajouts

### 6. Le test est sauté au lieu d'échouer quand Docker est arrêté

**Le problème :** sans Docker, `MySqlTest` échoue, et tout le build échoue avec lui, même pour un changement qui ne
touche pas MySQL.

**Ce qui a changé** ([`68850b73`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/68850b73), Sec13_Chap142),
sur `MySqlContainerBase` :

```diff
+@Testcontainers(disabledWithoutDocker = true)
```

**Pourquoi :** sans Docker, le test est signalé comme **sauté** (`Skipped: 1`), et le build reste vert.

**Un piège :** un test sauté ressemble à un test réussi.

| Situation | Le test échoue | Le test est sauté |
|---|---|---|
| Sur un PC sans Docker, en modifiant un controller | ❌ bloqué par un test sans rapport | ✅ on continue |
| **Le serveur de CI a perdu Docker** | ✅ ça se voit tout de suite | ❌ **build vert, MySQL jamais testé** |

La vraie réponse arrive à la leçon suivante de JT (Chap144) : séparer les tests, pour que `mvn test` n'ait pas besoin
de Docker et que `mvn verify` échoue bruyamment sans lui.

**Un piège :** le saut n'a pas pu être vérifié pendant la leçon. Diriger `DOCKER_HOST` vers un port fermé ne simule
pas l'absence de Docker : Testcontainers se rabat sur le *named pipe* de Docker Desktop (le canal de communication de
Docker sous Windows), et le test tourne quand même. Pour vérifier, il faut vraiment arrêter Docker Desktop ;
`mvnw test -Dtest=MySqlTest` doit alors afficher `Skipped: 1`.
