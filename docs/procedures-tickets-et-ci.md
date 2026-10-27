# Procédures : tickets GitHub et pipeline CI

Ce document explique comment le travail est suivi dans ce dépôt depuis le 27-10-2026 :
les **tickets** (issues GitHub) disent *quoi faire*, la **pipeline CI** (GitHub Actions)
prouve que *c'est testé*. Il sert à suivre et à contrôler ce que Claude fait, sans avoir
besoin de se souvenir de la conversation où tout a été mis en place.

Dépôt : https://github.com/PierreSQS/spring-7-rest-mvc (public, fork du dépôt de JT).

## Sommaire

1. [Le cycle d'un ticket, en un coup d'œil](#1-le-cycle-dun-ticket-en-un-coup-dœil)
2. [Les tickets](#2-les-tickets)
3. [La pipeline CI](#3-la-pipeline-ci)
4. [Fermer un ticket : les 3 conditions](#4-fermer-un-ticket--les-3-conditions)
5. [Contrôler soi-même : les commandes](#5-contrôler-soi-même--les-commandes)
6. [Pièges déjà rencontrés](#6-pièges-déjà-rencontrés)

---

## 1. Le cycle d'un ticket, en un coup d'œil

Exemple : le ticket #1 (« les dates des entités ne sont pas toutes gérées par Hibernate »).

```
 1. Claude écrit le texte du ticket  ──►  je le relis  ──►  Claude le crée sur GitHub
 2. je corrige le code sur la branche du ticket (ici 126)
 3. Claude lance les tests concernés en local
 4. je valide  ──►  Claude commite (il demande avant)  ──►  le message cite « #1 »
 5. push  ──►  GitHub lance la pipeline tout seul  ──►  ✅ ou ❌ sur le commit
 6. si ✅ et que les critères « Done when » du ticket sont remplis :
    Claude coche les cases et ferme le ticket avec « Fixed in <sha> » (il demande avant)
```

Si la pipeline est ❌ : on lit l'erreur (section 5), on corrige, on repousse. Le ticket reste ouvert.

---

## 2. Les tickets

### 2.1 Ce que contient un ticket

Chaque ticket a toujours les mêmes parties, en anglais :

| Partie | Rôle | Exemple (ticket #2) |
|---|---|---|
| **Branch** | sur quelle branche corriger (les branches de leçon ne sont jamais fusionnées) | `126-springdoc-maven-jt-sb412` |
| **Problem** | ce qui ne va pas, avec le fichier et la ligne | `BootstrapData.java:49-61`, le `switch` sur les noms exacts |
| **Rules** | la règle de `docs/regles-a-retenir.md` (branche 123) qui s'applique | R19 |
| **Done when** | à quoi on reconnaît que c'est fini | « `beers.csv` has no `â€` sequence left » |

**Ce qu'un ticket `training` ne contient jamais : la solution.** Il dit *quoi* et *où*, pas *comment* :
c'est à moi de trouver. Claude donne au plus un indice, si je le demande.

### 2.2 Les étiquettes (labels)

| Étiquette | Couleur | Signification |
|---|---|---|
| `sec40`, `sec18`, `sec22`… | bleu | la **section** du cours, donc le contexte du ticket |
| `optimization` | vert | amélioration du code de JT |
| `training` | jaune | **c'est moi qui le fais** (exercice) |
| `claude` | violet | **c'est Claude qui le fait** (par exemple Docker Compose, la base). Je peux le reprendre : il suffit de changer l'étiquette |

Sur GitHub on ne peut pas « assigner » un ticket à Claude (il n'a pas de compte) : l'étiquette joue ce rôle.

### 2.3 Un ticket par branche

Quand le même bug existe sur plusieurs branches, il y a **un ticket par branche**, chacun avec
l'étiquette de sa section, et les tickets se renvoient l'un à l'autre :

> Same bug on branch 123: #7

Fermer un ticket ne concerne donc que **sa** branche. On voit d'un coup d'œil où le bug est
déjà corrigé et où il reste à faire. Corriger le même bug plusieurs fois est voulu : c'est un exercice de plus.

### 2.4 Pourquoi `Closes #1` ne ferme pas le ticket ici

Sur GitHub, un message de commit qui contient `Closes #1` ferme le ticket **seulement quand le commit
arrive sur la branche par défaut** (`master`). Mes branches de leçon ne sont jamais fusionnées :
le ticket resterait ouvert. Donc :

- le message de commit cite simplement `#1` : le commit apparaît dans l'historique du ticket ;
- le ticket est fermé **à la main**, par Claude, quand les 3 conditions de la section 4 sont remplies.

---

## 3. La pipeline CI

### 3.1 Ce que c'est

Un fichier, **`.github/workflows/build.yml`** (GitHub ne cherche les workflows qu'à cet endroit).
À chaque push, GitHub prête une machine Linux neuve, y lance toute la suite de tests, puis jette la machine.
Le résultat s'affiche sur le commit : ✅ (tout est vert) ou ❌ (au moins un test ou une étape a échoué).

### 3.2 Ce qu'il fait, étape par étape

| Bloc du fichier | Ce qu'il fait |
|---|---|
| `on: push` / `workflow_dispatch` | démarre à chaque push, ou à la main avec le bouton « Run workflow » de l'onglet *Actions* |
| `paths-ignore` | **sauf** si le push ne change **que** de la documentation (`docs/**`, `**/*.md`) : pas de run. Dès qu'un autre fichier change (code, `pom.xml`, `.properties`, `mvnw`…), le run a lieu |
| `runs-on: ubuntu-latest` | une machine Linux de GitHub, avec Docker (nécessaire pour `MySqlIT` et Testcontainers) |
| `actions/checkout@v7` | télécharge le commit poussé |
| `actions/setup-java@v6` | installe Java 25 (Temurin) et garde les jars Maven en **cache** entre deux runs |
| `./mvnw -B verify` | la même commande qu'en local : tests unitaires `*Test` (plugin surefire), puis tests d'intégration `*IT` (plugin failsafe) |
| `actions/upload-artifact@v7` | **seulement en cas d'échec** : garde les rapports de test (`target/surefire-reports/`, `target/failsafe-reports/`), téléchargeables depuis la page du run |

### 3.3 Ce que la pipeline ne teste pas

- **Le vrai Auth Server** : les tests fonctionnent avec des jetons simulés (`jwt()`), ils n'ont jamais
  besoin du serveur sur le port 9000. Le test avec le vrai Auth Server reste le mien, avec Postman.
- **Les branches qui n'ont pas le fichier** : un workflow ne tourne que sur les branches qui contiennent
  `build.yml`. Pour l'instant : **seulement la 126**. Le jour où on traite un ticket sur une autre branche,
  Claude y ajoute le fichier (un commit `-X`). Les copies peuvent diverger d'une branche à l'autre :
  ce n'est pas grave, chaque branche est une photo de son époque.

---

## 4. Fermer un ticket : les 3 conditions

C'est **Claude** qui décide qu'un ticket est réglé (ma décision du 27-10-2026), et il **me demande avant de fermer**.
Une case est cochée, ou un ticket fermé, seulement si les **trois** conditions sont réunies :

1. **Poussé** : le commit du correctif est sur GitHub (pas seulement en local, pas seulement modifié dans IntelliJ) ;
2. **Testé** : la pipeline est ✅ **sur ce commit-là** ;
3. **Fini** : chaque point du « Done when » du ticket est rempli (Claude le vérifie en lisant le code, sans rien modifier).

Ensuite Claude écrit dans le ticket « Fixed in `<sha>` » avec le lien du run vert, puis le ferme.
Si c'était une erreur, je rouvre le ticket (bouton « Reopen issue »).

**Exemple :** un correctif qui existe dans IntelliJ mais n'est pas commité (`git status` le montre en `M`)
ne compte pas, même si les tests sont verts en local : la condition 1 manque.

---

## 5. Contrôler soi-même : les commandes

À lancer dans Git Bash, depuis le dossier du projet (ou avec `-R PierreSQS/spring-7-rest-mvc` ailleurs).

| Je veux… | Commande |
|---|---|
| voir les tickets ouverts | `gh issue list` |
| voir les tickets d'une section | `gh issue list --label sec40` |
| lire un ticket | `gh issue view 2` |
| voir les derniers runs de ma branche | `gh run list --branch 126-springdoc-maven-jt-sb412 --limit 5` |
| suivre un run en direct | `gh run watch <id>` |
| lire uniquement l'erreur d'un run rouge | `gh run view <id> --log-failed` |
| relancer la pipeline sans nouveau commit | `gh workflow run build.yml --ref 126-springdoc-maven-jt-sb412` (ou le bouton « Run workflow ») |
| ouvrir le dépôt dans le navigateur | `gh browse` |

`<id>` = le numéro du run, affiché par `gh run list` (par exemple `40497620678`).

**Vérifier `build.yml` avant de le pousser : `actionlint`.**
Un outil qui relit les fichiers de `.github/workflows/` : la syntaxe YAML **et** les règles de GitHub Actions
(option mal écrite, action inconnue…). Installé sur la machine (pas dans le projet) avec `winget install rhysd.actionlint`.
Depuis le dossier du projet, lancer simplement `actionlint` : s'il n'affiche rien, tout est bon ; sinon il donne
le fichier, la ligne et l'erreur, par exemple :

```
.github/workflows/build.yml:9:5: unexpected key "path-ignore" for "push" section [syntax-check]
```

---

## 6. Pièges déjà rencontrés

### 6.1 `./mvnw` cassé sur une machine neuve (corrigé dans `448cc5a7`)

**Symptôme** (premier run) :
```
./mvnw: 295: Syntax error: end of file unexpected (expecting "fi")
```
**Cause** : dans `mvnw` (lignes 204-217), un bloc `<<-END ... END` n'accepte que des **tabulations** devant `END`.
Le fichier de JT avait des **espaces**. Le shell ne trouvait jamais la fin du bloc.

**Pourquoi invisible en local** : Maven est déjà téléchargé dans `~/.m2/wrapper`, alors le script lance Maven
à la ligne 154 et s'arrête avant d'arriver au bloc cassé. Sur Windows, c'est de toute façon `mvnw.cmd` qui sert.
Il fallait une machine vierge pour le voir — c'est exactement à ça que sert une CI.

**À savoir** : le même `mvnw` cassé est sur la branche 123 (fichier identique). À corriger le jour où elle reçoit la pipeline.
Détail complet : commentaire sur le commit [448cc5a7](https://github.com/PierreSQS/spring-7-rest-mvc/commit/448cc5a74091bacc098805bd6b0c7f1b2c4bb6ee#commitcomment-206853544).
