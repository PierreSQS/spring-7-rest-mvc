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
