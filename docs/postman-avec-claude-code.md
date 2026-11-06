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

**Où le CLI range la clé :** vérifié après le login, voir la section 3 (`C:\Users\messina\.postman\postmanrc`).

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

---

## 4. Trouver et récupérer la collection (05-11-2026)

**Le but :** trouver l'id de mon workspace, puis celui de ma collection, puis récupérer la collection entière pour l'analyser.

**Les commandes**, dans l'ordre (chacune utilise l'id trouvé par la précédente) :

| # | Qui | Commande Postman CLI | Ce qu'elle a affiché |
|---|---|---|---|
| 1 | moi | `postman workspace list --filter "My Workspace"` | une ligne : `<workspaceId>`, type `personal`, nom `My Workspace` |
| 2 | Claude | `postman collection list --workspace <workspaceId> --filter "Spring Framework 7"` | une ligne : `<collectionId>`, type `HTTP`, nom `UDEMY-Spring Framework 7-JT` |
| 3 | Claude | `postman collection get <collectionId> --json > <scratchpad>/collection-jt.json` | rien à l'écran : la sortie va dans un fichier (121 Ko de JSON) |

- Un **id** n'est pas un secret : il dit *où* se trouve un élément, pas *qui a le droit* d'y entrer (il faut ma clé pour ça).
  Les ids réels ne sont pas recopiés ici.
- L'id de la collection commence par le numéro de mon compte Postman (`<n°>-<id de la collection>`).
- Le fichier va dans le **scratchpad** de la session Claude Code (un dossier temporaire de mon profil,
  `C:\Users\messina\AppData\Local\Temp\claude\...\scratchpad`), **hors du dépôt Git** : il contient les secrets en clair.
- Pourquoi un fichier plutôt que l'écran : la collection fait des centaines de lignes ; l'afficher coûterait cher en tokens.
  Claude n'en lit que les parties utiles, avec `jq`.
- Le CLI propose une mise à jour (1.86.0) : pas pendant l'exploration, le document est écrit avec la 1.75.0.

### Ce que contient la collection

22 dossiers (`Sec6` à `Sec56`), 65 requêtes, qui appellent **plusieurs applications du cours** :

| Adresse appelée | Application | Dans ce dépôt ? |
|---|---|---|
| `/api/v1/beer`, `/api/v1/customer` (ports 8080, 8081) | **rest-mvc** (ce projet) | oui |
| `/api/v2/beer` | reactive (WebFlux) | non, autre dépôt |
| `/api/v3/beer` | reactive-mongo, gateway | non, autre dépôt |
| `/actuator/...`, `localhost:30080` | Actuator, Kubernetes (Sec43, Sec56) | à voir requête par requête |

### Où sont les secrets (valeurs non affichées)

| Endroit | Combien | Quoi |
|---|---|---|
| auth **OAuth 2.0** des requêtes | 37 requêtes | `clientId` (2 valeurs : `oidc-client` et `messaging-client`), `clientSecret` (**1 seule valeur**, la même partout, voir section 5), en clair |
| auth **Basic** | 5 requêtes (Sec19, Sec21, Sec22) | `username` (2 valeurs), `password` (3 valeurs, dont une identique à un `username`), en clair |
| **scripts** *beforeRequest* | 2 requêtes (Sec40, `Chap374-Patch-...`) | `const CLIENT_ID = '...'`, `const CLIENT_SECRET = '...'` écrits **dans le code** |

**Le piège :** un secret écrit dans un **script** ne déclenche pas le triangle jaune de Postman. Il n'a été trouvé
qu'en cherchant les mots `secret`, `password`, `client_id`... dans le code des scripts.

### La décision : seulement `/api/v1` (option A)

Ne ramener dans le dépôt **que les requêtes vers cette API** (`/api/v1/...`), triées **requête par requête** :
certains dossiers (Sec44, Sec46, Sec50) mélangent `/api/v1` et `/api/v2` ou `/api/v3`.
Une requête vers `/api/v2` ou `/api/v3` ne peut pas être testée avec le code de ce dépôt.

Restructurer et améliorer la collection (j'ai organisé mes requêtes comme j'ai pu) : un ticket à part, après #39.

---

## 5. Ranger la collection dans le dépôt, sans secrets (05-11-2026)

**Le but :** transformer le gros fichier JSON récupéré en fichiers lisibles dans Git, sans aucun secret.

**Le format dans le dépôt** (format « v3 » de Postman) : un dossier par collection, **un petit fichier YAML par requête**.
Une modification d'une requête ne touche qu'un fichier, comme une classe Java.

```
postman/
├── collections/UDEMY-Spring Framework 7-JT/
│   ├── .resources/definition.yaml                  <- réglages communs (variables de la collection)
│   ├── Sec38/.resources/definition.yaml            <- un par dossier
│   ├── Sec38/Chap357-Get Beers-JWT-CC.request.yaml <- une requête
│   └── ...
└── environments/
    ├── local-example.environment.yaml              <- modèle, valeurs vides : dans Git
    └── local.environment.yaml                      <- mes vraies valeurs : IGNORÉ par Git
```

**Les décisions :**

| Question | Décision |
|---|---|
| copie fidèle ou restructurée ? | **fidèle** : mes dossiers et mes noms tels quels. Restructurer = un ticket à part, pour voir chaque amélioration séparément |
| quelles requêtes ? | **50 sur 65** : celles de cette API (`/api/v1`), plus les 2 requêtes OAuth2 de Sec23 (`/userinfo`, `Get-Token`). Pas Actuator (absent du `pom.xml`), pas `/api/v2`, `/api/v3`, Kubernetes |
| où mettre les vraies valeurs ? | **un fichier local ignoré par Git** (option B), comme `.env` / `.env.example` pour Docker. Écartés : le coffre-fort partagé de Postman (lié à une *team*, mon essai Enterprise se termine), les valeurs en paramètre de commande (restent dans l'historique du shell) |

**Comment** (fait par Claude) : un script Python dans le scratchpad lit le JSON et écrit les fichiers YAML ;
pas de `postman workspace pull`, qui ramènerait **toutes** mes collections.

**Les secrets deviennent des variables :**

| Variable | Remplace | Valeur à mettre dans `local.environment.yaml` |
|---|---|---|
| `{{oidcClientId}}` | le client ID des requêtes « oidc-client » | `SecurityConfig.java` de `spring-7-auth-server`, `.clientId(...)` |
| `{{clientSecret}}` | l'unique client secret (37 requêtes + 2 scripts de Sec40) | même fichier, `.clientSecret(...)`, **sans** le préfixe `{noop}` |
| `{{messagingClientId}}` | le client des requêtes « messaging-client » (Sec22, Sec23, Sec36, Sec43-Sec50) | `messaging-client` (`SecurityConfig.java:99` de l'**ancien** serveur `spring-6-auth-server-jt`), ou vide : ces requêtes ne marchent pas avec `spring-7-auth-server`, qui ne connaît que `oidc-client` |
| `{{basicUsername1/2}}`, `{{basicPassword1/2}}` | les logins HTTP Basic (Sec19-22) | **vides** : l'API n'accepte plus HTTP Basic (`SpringSecConfig.java` : seulement `oauth2ResourceServer`) |

**Pourquoi un seul `{{clientSecret}}` pour deux clients :** en passant à `spring-7-auth-server` (Spring Boot 4), j'ai gardé
le secret de JT pour ne pas avoir à changer mes requêtes. Il est identique dans `spring-6-auth-server-jt`
(`messaging-client`) et dans `spring-7-auth-server` (`oidc-client`) : vérifié le 05-11-2026 sur les 25 branches de
`spring-6-auth-server-jt` qui ont ce fichier (valeurs comparées, jamais affichées). Le séparer en deux variables : une option du ticket de restructuration.

**Pourquoi 3 mots de passe et seulement `{{basicPassword1/2}}` :** dans deux requêtes de Sec22 (`Chap227-...HttpBasic`),
le mot de passe était identique au nom d'utilisateur ; le script de conversion l'a remplacé par `{{basicUsername2}}`
(vérifié le 06-11-2026 dans le JSON d'origine, valeurs non affichées).

Dans les scripts de Sec40 : `const CLIENT_SECRET = '...'` est devenu `const CLIENT_SECRET = pm.variables.get('clientSecret')`.

**Les deux seules entorses à la copie fidèle :** dans les descriptions de `Sec23/Chap245_Get-Token_ClientCredentials`
et `Sec23/Chap245_Get-UserInfo_AuthCodePKCE`, j'avais écrit le secret et le login en clair : remplacés par `{{clientSecret}}`
et `{{basicUsername1}}`. Les client IDs restent dans les noms de requêtes : un client ID est un **identifiant**, pas un secret.

**Les vérifications avant le commit :**

| Vérification | Commande | Résultat |
|---|---|---|
| format de la collection | `postman collection lint "postman/collections/UDEMY-Spring Framework 7-JT"` | 50 requêtes, 0 erreur |
| format des environnements | `postman environment lint postman/environments --fail-severity warning` | 0 erreur (le nom doit finir par `.environment.yaml`) |
| le fichier des valeurs est ignoré | `git check-ignore -v postman/environments/local.environment.yaml` | ignoré par la ligne 39 du `.gitignore` |
| aucune valeur secrète dans les fichiers commités | script de recherche (valeurs jamais affichées) | 0 |

**Les pièges rencontrés :**
- mes valeurs secrètes sont des **mots courants** : la recherche a sonné 16 fois, dont 15 fausses alertes
  (le mot dans `type: 'secret'`, dans le nom du champ `password`, dans une phrase) et **1 vraie fuite** (le secret dans une description) ;
- un secret dans un **script** ou une **description** ne déclenche pas le triangle jaune de Postman ;
- les deux noms de fichiers d'environnement se ressemblent : j'ai d'abord rempli **le modèle** au lieu du fichier local.
  Le contrôle « remplie / vide » de chaque fichier l'a montré avant tout commit.

**Le commit :** `fdd183b3` sur la branche `127-springdoc-security-cfg-jt-sb412-postman` (72 fichiers : 70 de la collection,
le modèle d'environnement, `.gitignore`).

**La CI et le dossier `postman/` :** le commit `fdd183b3` a lancé la CI pour rien (elle construit le code Java, que les
fichiers Postman ne changent pas). Corrigé dans `c2c7e487` : `postman/**` ajouté aux `paths-ignore` de
`.github/workflows/build.yml` (et `CLAUDE.md`, `docs/procedures-tickets-et-ci.md` mis à jour dans le même commit).
Un push qui change **aussi** du Java lance toujours la CI. Si un jour la CI doit **lancer** les requêtes Postman, revoir cette ligne.

---

## 6. Lancer une requête depuis le Postman CLI (05-11-2026)

**Le but :** prouver que la collection du dépôt, sans secrets, marche contre ma vraie API et mon vrai serveur d'autorisation.

**Avant :** démarrer le serveur d'autorisation (`spring-7-auth-server`, port 9000), Docker Desktop, puis l'API
(`./mvnw spring-boot:run`, profil `localmysql`, port 8081, MySQL sur 3308 démarré par Compose).
Contrôle rapide : l'API répond `401` **sans** jeton (la sécurité marche).

**La commande Postman CLI :**

```
postman collection run "postman/collections/UDEMY-Spring Framework 7-JT" \
  -e postman/environments/local.environment.yaml \
  -i "Sec38/Chap357-Get Beers-JWT-CC"
```

**Attention :** cette commande lit les fichiers du dépôt Git, dans le dossier
`postman/collections/UDEMY-Spring Framework 7-JT/`.
Elle ne lit aucune collection de mon compte Postman.

| Paramètre | Rôle |
|---|---|
| le chemin | la collection **locale** (celle du dépôt), pas celle de mon compte |
| `-e` | le fichier où le CLI lit `{{oidcClientId}}` et `{{clientSecret}}` |
| `-i "dossier/requête"` | ne lancer qu'**une** requête |

**1er essai : `401 Unauthorized`.** Mon propre test l'a expliqué dès le n°1 : *Authorization header must be present*.
**Le Postman CLI ne va pas chercher tout seul un jeton OAuth 2.0** : dans l'application, c'est le bouton
*Get New Access Token* qui le fait ; le CLI ne l'a pas.

**La correction (Chap357 seulement, 3e entorse à la copie fidèle, commit `e6346099`) :** l'auth de la requête passe de
`oauth2` à `bearer {{cc_access_token}}`, et un script `beforeRequest` demande le jeton au serveur d'autorisation
(*client credentials*), le garde dans `{{cc_access_token}}` jusqu'à son expiration. Code repris de mes scripts de Sec40.
Faire la même chose **une fois pour toute la collection** (script dans `definition.yaml`, auth héritée par les 34 requêtes
OAuth2) : dans le ticket de restructuration.

**2e essai : `200 OK`, 6 tests sur 6 passent** (jeton présent et bien formé, statut 200, schéma des bières, quantités et prix).

**Ce que ça prouve :** la collection du dépôt, sans aucun secret, marche contre ma vraie API et mon vrai serveur
d'autorisation, depuis la ligne de commande. **Ce que ça ne prouve pas :** les 49 autres requêtes (non lancées).

**Le `[errored]` rouge n'est pas une erreur de l'auth server :** le CLI affiche `POST http://localhost:9000/oauth2/token [errored] Could not read response data`,
mais le log de `spring-7-auth-server` montre `Generated access token` pour cette requête (vérifié le 06-11-2026 à 10:05:04.598).
C'est un défaut d'affichage du Postman CLI.

**Commits sur la branche `-postman` :** `fdd183b3` (la collection), `c2c7e487` (la CI ignore `postman/**`), `e6346099` (Chap357).

---

## 7. Voir la collection du dépôt Git dans l'application Postman (05/06-11-2026, #40)

**Le but :** ouvrir dans l'application Postman la collection du dépôt Git (dossier `postman/collections/UDEMY-Spring Framework 7-JT/`),
sans toucher à ma collection `UDEMY-Spring Framework 7-JT` de My Workspace.

### Les étapes

1. **Claude** copie le dossier `postman/collections/UDEMY-Spring Framework 7-JT/` du dépôt Git dans un dossier temporaire ;
   dans cette copie, il renomme la collection en `UDEMY-Spring Framework 7-JT-Claude` (fichier `.resources/definition.yaml`, ligne 2).
2. **Claude**, dans ce dossier temporaire, crée un workspace séparé et y envoie la copie :
   ```
   postman workspace create --visibility personal --name "Spring 7 REST MVC - Claude" --path .
   postman workspace diff --push-strategy default        -> 1 to change, 0 to delete
   postman workspace push --push-strategy default --yes  -> Created: 1
   ```
3. **Moi**, dans l'application Postman, workspace `Spring 7 REST MVC - Claude` : « … » sur `UDEMY-Spring Framework 7-JT-Claude`
   -> **Create a fork** -> Fork label `Claude fork`, Workspace **My Workspace**, Auto-pull **décoché** -> **Fork Collection**.
4. **Moi**, dans l'application Postman : menu **No Environment** (en haut à droite) -> **Create new environment** ->
   nom `Spring7 Rest MVC`, deux variables :
   - `oidcClientId` = `oidc-client` ;
   - `clientSecret` = le secret (ligne 103 de `SecurityConfig.java` dans `spring-7-auth-server`). **Ne pas cliquer sur le cadenas « Secure ».**

   L'enregistrement est automatique.
5. **Moi** : démarrer `spring-7-auth-server` (port 9000), puis `spring-7-rest-mvc` avec le profil `localmysql` (port 8081).
   Dans My Workspace : `UDEMY-Spring Framework 7-JT-Claude` > Sec38 > Chap357-Get Beers-JWT-CC, environnement actif
   `Spring7 Rest MVC`, **Send**.

**Résultat :** `200 OK`, onglet **Test Results** : **6/6 PASSED**.

### Pour comprendre

- **Pourquoi un workspace séparé, et pas directement My Workspace :** le Postman CLI n'envoie une collection que dans un
  workspace relié à un dossier, et `postman workspace diff` montre par défaut le mode `force-sync`, qui afficherait toutes mes
  autres collections de My Workspace comme « à supprimer ». Un workspace séparé laisse My Workspace intact.
- **Pourquoi Fork, et pas Move :** Move aurait vidé le workspace `Spring 7 REST MVC - Claude` ; Fork copie. Auto-pull décoché :
  la copie ne change jamais toute seule.
- **Le cadenas « Secure »** range la valeur dans le Postman Vault. Pas utilisé pour l'instant : non vérifié avec les scripts et
  avec le plan gratuit. Idée pour plus tard.
- **Deux environnements séparés :** `Spring7 Rest MVC` vit dans mon compte Postman (utilisé par l'application Postman) ;
  `postman/environments/local.environment.yaml` est un fichier du dépôt Git (lu par le Postman CLI avec `-e`).
  Mêmes noms de variables, deux copies séparées.
- **La collection existe à 3 endroits :** voir le tableau du commentaire de clôture de #39.
- **Ce que montrent les logs** de `spring-7-auth-server` et de `spring-7-rest-mvc` :
  1. le script pre-request envoie `POST /oauth2/token` -> `Generated access token` (`Principal=oidc-client`) ;
  2. l'application Spring Boot `spring-7-rest-mvc` va chercher `/.well-known/openid-configuration` et `/oauth2/jwks` au premier jeton ;
  3. `Authenticated token`, puis `200`.
- **Pièges de ma version de Postman :** pas de « + » dans la section ENVIRONMENTS, pas de bouton Save (enregistrement automatique) :
  un environnement se crée depuis le menu **No Environment**.
