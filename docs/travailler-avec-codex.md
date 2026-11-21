# Travailler avec Codex

Cette page rassemble les manipulations utiles de Codex dans ce projet.
Les premières notes concernent Codex CLI, lancé avec `codex` dans le terminal
Git Bash d’IntelliJ IDEA sous Windows.

## 1. Effacer une ligne avant d’envoyer le message

**Le but :** remplacer le texte que je suis en train de saisir dans Codex CLI.

**Le problème :** le curseur peut être au milieu de la ligne. `Ctrl+U` efface
la partie située à sa gauche, jusqu’au début de cette ligne.

**Ce qu’il faut :**

1. Cliquer dans le terminal où Codex CLI attend le message.
2. Appuyer sur **Fin** (`End`) pour aller à la fin de la ligne.
3. Appuyer sur **Ctrl+U** pour effacer la ligne.

**Résultat attendu :** la ligne est vide et je peux écrire un nouveau texte.
Les messages déjà envoyés restent dans la conversation.

Exemple, avec le curseur à la fin :

```text
Avant Ctrl+U    Explique-moi Optional.map|
Après Ctrl+U   |
```

### Corriger seulement une partie du texte

| Ce que je veux effacer | Touche ou raccourci |
|---|---|
| Le caractère à gauche du curseur | Retour arrière (`Backspace`) |
| Le caractère à droite du curseur | Suppr (`Delete`) |
| Le mot à gauche du curseur | `Ctrl+W` |
| Du curseur au début de la ligne | `Ctrl+U` |
| Du curseur à la fin de la ligne | `Ctrl+K` |

Ces raccourcis décrivent la saisie standard de Codex CLI. Pour un brouillon
de plusieurs lignes, `Fin` puis `Ctrl+U` vise la ligne où se trouve le curseur.
Si le terminal intercepte une touche ou si les raccourcis ont été personnalisés,
il faut vérifier le réglage concerné.

**Référence :** les raccourcis et l’édition du texte sont définis dans le
[code officiel de Codex](https://github.com/openai/codex/blob/main/codex-rs/tui/src/keymap.rs)
et son [éditeur de saisie](https://github.com/openai/codex/blob/main/codex-rs/tui/src/bottom_pane/textarea.rs).
Comportement relu le 22 novembre 2026. L’essai interactif dans le terminal
d’IntelliJ reste à valider avec Pierrot.
