# Travailler avec Claude Code

## 1. Pourquoi un message coûte plus cher dans une longue conversation

**Contexte :** le 30-10-2026, à la fin de la conversation `sec40-openapi-with-sb-6`,
chaque message, même un simple « ok », consommait beaucoup de la limite d'utilisation.
`/context` affichait environ 330 000 tokens.

**Les mots :**
- *token* : un petit morceau de texte (quelques caractères). Le coût et les limites
  se comptent en tokens.
- *Claude* (le modèle) : l'IA qui lit du texte et répond. Il n'a **aucune mémoire**
  entre deux messages.
- *Claude Code* (le programme autour du modèle) : il tourne dans le terminal.
  Il envoie la conversation au modèle et exécute les outils (lire un fichier,
  lancer git, ...).
- *cache* : une copie de ce qui a déjà été envoyé, que le modèle relit moins cher.

**Le mécanisme :** à chaque message, Claude Code renvoie au modèle **toute** la conversation :
1. les instructions système, les outils et les notes mémoire,
2. tous mes messages et toutes les réponses de Claude,
3. tous les résultats d'outils : fichiers lus, sorties de tests, sorties de `/context`,
   captures d'écran.

Une conversation longue est donc relue en entier **à chaque message**.

**Exemple vécu :**
- fin de la -6 : on a seulement parlé des coûts, et la conversation est passée
  de 294k à 331k tokens. Les sorties de `/context` y sont entrées aussi.
- début de la -7 : 31,6k tokens, soit environ 10 fois moins à relire par message.

**Le cache :** `/usage` montrait 96 % des tokens lus depuis le cache. C'est moins cher
et plus rapide, mais **ça compte quand même dans les limites**. Après une longue pause,
le cache a expiré et tout est relu au prix fort. C'est pour ça que Claude Code demande
« Resume this conversation? ». Observé : environ 5 % de la limite de 5 heures part d'un coup.

**À retenir :**
- conversation devenue longue -> en ouvrir une nouvelle :
  `/clear`, puis `/rename sec40-openapi-with-sb-N+1`
- après une longue pause -> nouvelle conversation plutôt que « Resume »
- ne pas coller de sorties lourdes (`/context`, captures d'écran) sans besoin :
  elles sont relues à chaque message suivant.

## 2. « Resume this conversation? » : oui ou non ?

**Contexte :** quand je reviens sur une conversation déjà longue, Claude Code me pose
cette question avant d'envoyer mon message.

**Le problème :** dire oui, c'est renvoyer toute la conversation au modèle (voir section 1).
Si la pause a été longue, le cache a expiré : tout est relu au prix fort.
Observé le 30-10-2026 sur la -6 (~330k tokens) : environ 5 % de la limite de 5 heures
pour un seul message.

**La règle :**
| Situation | Choix |
|---|---|
| pause courte, la conversation n'est pas trop longue | Resume |
| longue pause (par exemple le lendemain) | nouvelle conversation |
| conversation déjà très longue (`/context` montre des centaines de milliers de tokens) | nouvelle conversation, même sans pause |

**Pourquoi on ne perd rien :** ce qui doit survivre d'une conversation à l'autre est
écrit ailleurs : le code et les commits dans Git, les tâches dans les issues GitHub,
et les notes mémoire de Claude (relues au début de chaque conversation).

## 3. `/clear` ou `/exit` + `claude` ?

**Contexte :** les deux donnent une conversation neuve. Quelle différence ?

**Ce que fait chacun** (doc officielle, page « Commands », vérifiée le 30-10-2026) :
- `/clear` : nouvelle conversation, contexte vide, **dans le même programme**.
  L'ancienne conversation n'est pas perdue : on la retrouve avec `/resume`.
  `/clear un-nom` donne un nom à l'ancienne conversation dans la liste de `/resume`.
- `/exit` puis `claude` : on **quitte le programme** et on le relance. Au démarrage,
  il relit tout : réglages, plugins, serveurs MCP, et il prend une mise à jour
  de Claude Code déjà installée.

**Recharger sans redémarrer :**
- `/reload-plugins` : recharge les plugins.
- `/reload-skills` : relit les skills ajoutées ou changées sur le disque.
- `/mcp` : gère les serveurs MCP (`/mcp reconnect all` pour les reconnecter).
- `/restart` existe aussi, mais seulement sur macOS / Linux : pas sur mon Windows.

**La règle :**
- au quotidien, conversation trop longue -> `/clear`
- après un changement de réglage, de plugin, ou une mise à jour -> `/exit` puis `claude`
