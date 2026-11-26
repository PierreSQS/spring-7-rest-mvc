# Section 13, Chap144 : le plugin Maven Failsafe, ce que nous avons amélioré

**Branche :** `78.5-Using-Maven-Failsafe-sb411` · **Section / Chapitre :** Sec13_Chap144 · **Commit(s) :**
[`25c7ef56`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/25c7ef56),
[`0e77669e`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/0e77669e),
[`73e54fb9`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/73e54fb9),
[`17ad02c1`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/17ad02c1),
[`f8f44020`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/f8f44020)

**Contexte :** depuis Chap142, le test `MySqlTest` tourne contre une vraie base MySQL dans un container Docker.
Comme son nom finit par `Test`, il tournait à chaque `mvn test` : impossible de lancer les tests unitaires sans
Docker. Cette leçon sépare les tests en deux familles, lancées par deux plugins Maven différents. Ce document dit ce
que nous avons fait **mieux que JT**, puis ce que nous avons **ajouté**. C'est une photo de la fin de la leçon
(19-09-2026) : il n'est jamais mis à jour.

Réécrit le 26-11-2026 avec les mêmes faits ; l'ancienne version, en anglais, reste sur la branche
`109-lock-demo-optimistic-sb411` ([`69f0a32b`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec13-chap144-maven-failsafe-improvements.md)).

## La leçon de JT

Branche de JT : `78.5-Using-Maven-Failsafe`. JT ajoute le plugin **Failsafe** au `pom.xml` et renomme `MySqlTest`
en `MySqlIT` (`src/test/java/guru/springframework/spring7restmvc/repositories/MySqlIT.java`). Les deux plugins de
test de Maven se partagent alors le travail :

- **Surefire** lance les classes `*Test` et `*Tests` pendant `mvn test` : les tests unitaires, rapides ;
- **Failsafe** lance les classes `*IT` (*integration test*) pendant `mvn verify`, une phase qui vient après
  `test` : les tests qui ont besoin de plus, ici de Docker.

Ce que chaque commande lance à la fin de la leçon (chiffres pris du projet, branche
`78.5-Using-Maven-Failsafe-sb411`) :

| Commande | Ce qui tourne | Docker nécessaire |
|---|---|---|
| `mvnw test` | les 21 tests unitaires (`*Test`, `*Tests`) | non |
| `mvnw verify` | les tests unitaires, puis 20 tests d'intégration : `BeerControllerIT`, `CustomerControllerIT`, `MySqlIT` | oui, pour `MySqlIT` |
| `mvnw verify "-Dit.test=MySqlIT"` | les tests unitaires, puis seulement `MySqlIT` | oui |
| `mvnw verify -DskipITs` | tout le build, avec seulement les tests unitaires (bloc 4) | non |

Effet de bord : `BeerControllerIT` et `CustomerControllerIT` tournent maintenant tout seuls dans `mvn verify`.
Avant cette leçon, ils ne tournaient que si on les nommait avec `-Dtest=...`.

## Mieux que JT

### 1. Failsafe déclaré sans version : le parent Spring Boot fournit le reste

**Le problème :** dans le `pom.xml`, JT fixe `maven-failsafe-plugin` en version **3.5.2** et écrit lui-même ses
deux *goals* (les tâches du plugin) : `integration-test`, qui lance les tests `*IT`, et `verify`, qui fait échouer
le build si l'un d'eux a échoué. Or le `pom.xml` du projet hérite du *parent* `spring-boot-starter-parent` 4.1.1 (un
`pom.xml` dont il reprend les réglages), et ce parent prépare déjà Failsafe. La version de JT est aussi plus ancienne
que celle du parent (**3.5.6**), et que celle de Surefire (3.5.6 aussi).

**Ce qui a changé** ([`25c7ef56`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/25c7ef56), Sec13_Chap144) :

<table>
<tr><th>JT</th><th>Nous</th></tr>
<tr>
<td>

```diff
 <plugin>
     <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-failsafe-plugin</artifactId>
-    <version>3.5.2</version>
-    <executions>
-        <execution>
-            <goals>
-                <goal>integration-test</goal>
-                <goal>verify</goal>
-            </goals>
-        </execution>
-    </executions>
 </plugin>
```

</td>
<td>

```diff
+<!-- runs the *IT classes during 'mvn verify'; version, goals (integration-test, verify)
+     and classesDirectory come from the Spring Boot parent's pluginManagement -->
 <plugin>
     <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-failsafe-plugin</artifactId>
 </plugin>
```

</td>
</tr>
</table>

**Pourquoi les goals ont disparu :** le parent les écrit déjà, dans sa section `pluginManagement`. Extrait de
`spring-boot-starter-parent-4.1.1.pom` (dans le dépôt Maven local, `~/.m2/repository`) :

```xml
<pluginManagement>
  <plugins>
    <!-- ... -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-failsafe-plugin</artifactId>
      <executions>
        <execution>
          <goals>
            <goal>integration-test</goal>
            <goal>verify</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <classesDirectory>${project.build.outputDirectory}</classesDirectory>
      </configuration>
    </plugin>
```

Ce sont exactement les goals que JT recopie. Mais `pluginManagement` n'est qu'un **catalogue** : un plugin qui y
est décrit ne tourne pas tant que le projet ne le déclare pas dans sa propre section `<plugins>`. C'est le rôle de nos
trois lignes `groupId` / `artifactId` : elles disent « je veux Failsafe », et Maven complète avec tout ce que le
catalogue du parent prévoit (version, goals, `classesDirectory`).

*Analogie :* commander « le menu du jour » au restaurant. Pas besoin de réciter l'entrée, le plat et le dessert :
la carte les donne déjà.

**Pourquoi c'est mieux :** quatre lignes au lieu de treize, et Failsafe suit la version de Spring Boot à chaque
mise à jour du parent, au lieu de rester bloqué en 3.5.2.

## Ajouts

### 2. L'agent Mockito pour Failsafe aussi, écrit une seule fois

**Le problème :** dans le `pom.xml`, Surefire démarre ses tests avec une ligne `argLine` (les options de la JVM de
test) qui charge Mockito comme *agent Java* (un programme chargé au démarrage de la JVM). Sans elle, Mockito
s'attache tout seul pendant les tests, et la JVM affiche des avertissements (« Mockito is currently
self-attaching », « Sharing is only supported... »). Failsafe démarre **sa propre** JVM : il n'hérite pas de la
ligne de Surefire, et les avertissements sont revenus pendant `mvn verify`.

**Ce qui a changé** ([`0e77669e`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/0e77669e) puis
[`17ad02c1`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/17ad02c1), Sec13_Chap144) :

- **a.** Failsafe reçoit la même ligne `argLine` que Surefire (`0e77669e`) ;
- **b.** pour ne pas l'écrire deux fois, elle va dans une propriété Maven, `test.jvm.argLine`, que les deux plugins
  utilisent (`17ad02c1`) :

<table>
<tr><th>Avant</th><th>Après</th></tr>
<tr>
<td>

```diff
 <properties>
     <!-- ... -->
 </properties>
 <!-- ... -->
 <artifactId>maven-surefire-plugin</artifactId>
 <configuration>
-    <argLine>-javaagent:${org.mockito:mockito-core:jar} -Xshare:off</argLine>
 </configuration>
 <!-- ... -->
 <artifactId>maven-failsafe-plugin</artifactId>
```

</td>
<td>

```diff
 <properties>
     <!-- ... -->
+    <test.jvm.argLine>-javaagent:${org.mockito:mockito-core:jar} -Xshare:off</test.jvm.argLine>
 </properties>
 <!-- ... -->
 <artifactId>maven-surefire-plugin</artifactId>
 <configuration>
+    <argLine>${test.jvm.argLine}</argLine>
 </configuration>
 <!-- ... -->
 <artifactId>maven-failsafe-plugin</artifactId>
+<configuration>
+    <argLine>${test.jvm.argLine}</argLine>
+</configuration>
```

</td>
</tr>
</table>

**Pourquoi :** `mvn clean verify` tourne sans avertissement, et la ligne ne se change qu'à un seul endroit.
Il restait un doute : `${org.mockito:mockito-core:jar}` (le chemin du jar de Mockito, rempli par le goal
`dependency:properties`) est-il encore remplacé quand il passe par une propriété ? Oui : `mvnw -X` (le mode détaillé
de Maven) montre que les deux plugins reçoivent le vrai chemin de `mockito-core-5.23.0.jar`.

### 3. Sans Docker, `mvn verify` échoue au lieu de sauter `MySqlIT`

**Le problème :** à Chap142, `MySqlContainerBase`
(`src/test/java/guru/springframework/spring7restmvc/MySqlContainerBase.java`) a reçu
`@Testcontainers(disabledWithoutDocker = true)` : sans Docker, le test MySQL est **sauté** au lieu d'échouer. La
seule raison était que ce test tournait alors à chaque `mvn test`. Avec Failsafe, cette raison disparaît :
`mvn test` ne touche plus jamais au container. Dans `mvn verify`, le saut ferait l'inverse de ce qu'on veut : sans
Docker, `MySqlIT` serait sauté en silence et le build resterait **vert**, alors que MySQL n'a jamais été testé.
C'est exactement le piège décrit à Chap142 (`docs/sec13-chap142-dynamic-properties-improvements.md`, bloc 6).

**Ce qui a changé** ([`73e54fb9`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/73e54fb9), Sec13_Chap144) :

```diff
  /**
   * ...
-  * Without a running Docker daemon the subclasses are skipped instead of failing.
+  * Subclasses are integration tests ({@code *IT}), run by Failsafe in {@code mvn verify}. Without a
+  * running Docker daemon they fail on purpose, so a missing Docker cannot hide behind a green build;
+  * {@code mvn test} does not run them and needs no Docker.
   */
-@Testcontainers(disabledWithoutDocker = true)
 @ActiveProfiles("testcontainers")
 public abstract class MySqlContainerBase {
```

**Pourquoi :** sans Docker, `mvn verify` échoue maintenant **bruyamment**, et `mvn test` marche toujours. C'est la
séparation faite par Failsafe qui protège ; le saut n'est plus utile. L'annotation part entièrement, pas seulement
son attribut : `@Testcontainers` ne gère que les champs `@Container`, et `MySqlContainerBase` n'en a pas (son
container est démarré dans un bloc `static`). Sans `disabledWithoutDocker`, elle ne ferait plus rien.

**Vérifié pendant la leçon :** essai avec Docker Desktop en pause. `mvnw test` passe (21 tests). `mvnw verify` se termine en
**BUILD FAILURE**, car `MySqlIT` échoue avec « Could not find a valid Docker environment » ; les deux tests
d'intégration des contrôleurs passent toujours.

### 4. `-DskipITs` noté dans `CLAUDE.md`

**Le problème :** sans Docker, `mvn verify` échoue maintenant (bloc 3). Pourtant, on veut parfois le build complet
(tests unitaires et jar) même quand Docker est arrêté. La commande existe dans Failsafe, mais `CLAUDE.md` ne la
citait pas.

**Ce qui a changé** ([`f8f44020`](https://github.com/PierreSQS/spring-7-rest-mvc/commit/f8f44020), Sec13_Chap144),
dans la liste des commandes de `CLAUDE.md` :

```diff
 .\mvnw.cmd verify "-Dit.test=MySqlIT"             # single *IT class (unit tests still run first)
+.\mvnw.cmd verify -DskipITs                       # full build with unit tests only, e.g. when Docker is off
```

**Pourquoi :** `-DskipITs` dit à Failsafe de sauter tous les tests `*IT`, **parce qu'on le demande**, et non en
silence comme au bloc 3. Essai : les 21 tests unitaires tournent, Failsafe affiche « Tests are skipped » et aucun
container ne démarre.
