# Postman avec Claude Code

Créé le 05-11-2026 (ticket #39). Ce document suit, étape par étape, l'exploration de Postman avec Claude Code
sur le projet `spring-7-rest-mvc` : ce qui a été fait, avec quelle commande, ce qu'on a vu et ce que ça prouve.

## Le contexte

Ma collection **« UDEMY-Spring Framework 7-JT »** contient mes requêtes du cours de JT, rangées par section
(`Sec6` ... `Sec40`). Elle se trouve **uniquement dans mon compte Postman**, dans le workspace « My Workspace » :
Postman la synchronise sur ses serveurs, rien n'est dans le dépôt Git.

Exemple de requête, `Sec38 > Chap357-Get Beers-JWT-CC` :

| Élément | Valeur |
|---|---|
| Requête | `GET localhost:8081/api/v1/beer` (URL écrite en dur, « No Environment ») |
| Authentification | OAuth 2.0, *Client Credentials*, jeton demandé à `http://localhost:9000/oauth2/token` |
| Client ID / Client Secret | enregistrés en clair dans la requête (Postman affiche un triangle jaune d'avertissement) |
| Résultat | `200 OK`, `totalElements: 2413` |

**Le but :** ramener cette collection dans le dépôt sous forme de fichiers, sans ses secrets (le dépôt est public),
et lancer une requête depuis le Postman CLI.

## Prérequis : le Postman CLI est-il connu de mon shell ?

Le **Postman CLI** (`postman`) est un produit **séparé** de l'application Postman (les fenêtres) : il s'installe à part.

| Vérification | Commande | Résultat chez moi (05-11-2026) |
|---|---|---|
| le shell connaît-il `postman` ? | `postman --version` (dans mon Git Bash) | `1.75.0` |
| où est-il ? | `command -v postman` | `C:\Users\messina\AppData\Local\Microsoft\WindowsApps\postman` (un seul fichier, 193 Mo) |
| pourquoi il est trouvé | - | ce dossier est dans le **PATH de mon utilisateur** Windows : Git Bash, PowerShell et DOS le connaissent tous |
| depuis quand ? | `stat` sur le fichier | arrivé sur mon disque le **18-10-2026** (date de création) ; installation ou mise à jour, le fichier ne le dit pas. Sa date de modification (16-10-2026) est celle de la version fabriquée par Postman |

Si `postman --version` répond `command not found`, le CLI n'est pas installé (ou pas dans le PATH de ce shell).

---

## 1. Comment Claude accède à ma collection

### Les outils en jeu

| Outil | Ce que c'est | Ce qu'il apporte |
|---|---|---|
| **Postman CLI** (`postman`) | le programme en ligne de commande de Postman, installé sur mon PC | il parle aux serveurs de Postman à ma place |
| **Plugin Postman** de Claude Code | des *skills* : des textes d'instructions que Claude lit avant d'agir (`postman:api-engineer`, `postman:bootstrap`, ...) | le mode d'emploi : quelle commande pour quelle situation. **Aucun accès** à mon compte |
| **Ma clé d'accès Postman** | donnée par Postman au CLI au moment de `postman login` | c'est elle, et elle seule, qui ouvre l'accès à mon compte |

**Sans `postman login`, Claude n'a aucun accès à mon compte.** Tout ce qu'il savait de ma collection avant le login
venait de ma copie d'écran.

### Les commandes déjà lancées (05-11-2026, sans connexion, lecture seule)

| Commande (Postman CLI, Git ou shell) | Ce qu'elle fait | Ce qu'on a appris |
|---|---|---|
| `postman --version` | affiche la version du CLI | le CLI est installé : **1.75.0** |
| `ls -a \| grep -iE 'postman'` | cherche un dossier ou un fichier Postman à la racine du dépôt | il n'y en a aucun |
| `git ls-files \| grep -iE 'postman\|collection'` | cherche un fichier Postman suivi par Git | aucun |
| `postman -h` | l'aide générale du CLI | la liste des commandes : `login`, `whoami`, `collection`, `workspace`, ... |
| `postman collection -h` | l'aide de `collection` | il existe `list` (lister les collections) et `get <id>` (récupérer **une** collection) |
| `postman workspace pull -h` | l'aide de `workspace pull` | `pull` ramène **tout** un workspace, donc les collections de tous mes cours : ce n'est pas ce qu'on veut |

### La chaîne pour atteindre ma collection (prévue)

Cinq commandes du Postman CLI, dans cet ordre. Chacune utilise le résultat de la précédente
(syntaxe vérifiée avec `postman <commande> -h`, CLI 1.75.0) :

| # | Qui | Commande Postman CLI | Paramètres | Ce qu'elle renvoie |
|---|---|---|---|---|
| 1 | moi | `postman login` | aucun | ouvre le navigateur ; je me connecte à Postman, le CLI reçoit une **clé d'accès** |
| 2 | Claude | `postman whoami` | aucun | le compte connecté (vérifie que l'étape 1 a marché) |
| 3 | Claude | `postman workspace list --filter "My Workspace"` | `--filter` : ne garder que les workspaces de ce nom | le workspace et son **id** (`<workspaceId>`) |
| 4 | Claude | `postman collection list --workspace <workspaceId> --filter "Spring Framework 7"` | `--workspace` : l'id trouvé en 3 ; `--filter` : le nom de la collection | la collection et son **id** (`<collectionId>`) |
| 5 | Claude | `postman collection get <collectionId> --json` | l'id trouvé en 4 ; `--json` : tout le contenu, pas un résumé | **toute la collection** (dossiers, requêtes, auth), affichée en JSON |

Les étapes 2 à 5 ne font que **lire** : rien ne change dans mon compte.
L'étape 5 *affiche* la collection ; comment la ranger en fichiers dans le dépôt (et sans les secrets) se décide à ce moment-là.

**Ce qui se passe derrière chaque commande :**

```
CLI  --HTTPS + ma clé d'accès-->  serveurs Postman
CLI  <--------- réponse JSON ---  serveurs Postman
CLI  -> affiche le résultat -> Claude le lit
```

Claude ne voit jamais la clé : seul le CLI s'en sert. Il ne voit que ce que les commandes affichent,
et montre chaque commande avant de la lancer.

**Couper l'accès :** `postman logout` supprime la clé gardée par le CLI.

**Encore à vérifier :** dans quel fichier de mon profil le CLI range la clé (à montrer après le login).

---

## 2. Créer la branche (05-11-2026)

**Le but :** une branche à part pour les fichiers Postman, pour ne pas les mêler au travail de la leçon (branch 127).
Le nom suit ma convention : le nom de la branche de départ + le suffixe `-postman`.

**Les commandes** (lancées par moi, dans Git Bash) :

| Commande Git | Ce qu'elle fait |
|---|---|
| `git switch -c 127-springdoc-security-cfg-jt-sb412-postman a79dc240` | crée la branche à partir du commit `a79dc240` (pointe de la branch 127) et s'y place |
| `git push -u origin 127-springdoc-security-cfg-jt-sb412-postman` | envoie la branche sur GitHub ; `-u` relie la branche locale à celle de GitHub (ensuite, `git push` suffit) |

**Le résultat :** la branche existe en local et sur GitHub, au commit `a79dc240`. Le code est identique à celui de la branch 127.

**Ce que ça prouve :** le point de départ est connu et propre. **Aucun run CI** n'a démarré : la branche ne contient
aucun commit nouveau. Explication probable, non vérifiée : le dernier commit, `a79dc240`, ne change que de la documentation, que la CI ignore.

---

## 3. Se connecter : `postman login` (05-11-2026)

**Le but :** connecter le Postman CLI à mon compte Postman, pour que les commandes suivantes puissent lire ma collection.

**Les commandes** (lancées par moi, dans Git Bash) :

| Commande Postman CLI | Ce qu'elle a affiché |
|---|---|
| `postman whoami` (avant) | `No Postman account details are available for this CLI session.` : aucun compte connu |
| `postman login` | `Please complete the authentication in the browser` -> connexion dans le navigateur -> `Welcome <mon nom> <mon email>`, `Logged in successfully.` |
| `postman whoami` (après) | `Logged in to Postman as <mon email>` |

Claude a relancé `postman whoami` dans son propre shell : même résultat. La connexion vaut donc pour **tous** mes shells
(elle est enregistrée dans mon profil Windows, pas dans le terminal).

**Où le CLI range la connexion :** dans `C:\Users\messina\.postman\`, **hors du dépôt Git** :

| Fichier | Rôle (déduit, fichier non ouvert) |
|---|---|
| `postmanrc` | la configuration du CLI, avec la **clé d'accès** : écrit à la minute exacte du login |
| `sessions\<id>.json` | une petite trace par session (100 octets) |
| `logs\postman-cli.log` | le journal du CLI (vide) |

« rc » est une vieille convention Unix (*run commands*) pour un **fichier de réglages** : comme `.bashrc` (Bash), `.npmrc` (npm).
`postmanrc` n'est pas un client : le client, c'est le programme `postman`. Il **lit** sa clé dans `postmanrc` et l'envoie,
à chaque commande, aux serveurs de l'API Postman (comme mon API sur 8081 reçoit le jeton délivré par l'auth server sur 9000).

**Ce que ça prouve :** le CLI peut maintenant interroger mon compte. La clé n'est ni dans le dépôt, ni dans un log,
ni dans un commit. **Couper l'accès :** `postman logout`.
