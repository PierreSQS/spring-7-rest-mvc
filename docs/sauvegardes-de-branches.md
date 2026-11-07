# Mes sauvegardes de branches

Créé le 07-11-2026. Quand je supprime une branche du dépôt `spring-7-rest-mvc`, Claude en garde
d'abord une **sauvegarde**. Ce document dit comment les retrouver, les lire, et restaurer une branche.

Au 07-11-2026, il y en a **69**, sur GitHub et dans ma copie locale.

## Les commandes

Dans Git Bash, depuis la racine du dépôt.

**1. Lister mes sauvegardes**

```bash
git ls-remote origin 'refs/backup/*'      # celles de GitHub (la référence)
git show-ref | grep refs/backup/          # celles de ma copie locale
```

Chaque ligne donne le SHA du commit et le nom : `refs/backup/<nom de la branche supprimée>`.
**Cette liste est la liste des branches supprimées.** Elle n'est recopiée nulle part, pour ne jamais être périmée.

**2. Voir ce que contient une sauvegarde**

```bash
git log -1 --date=short --format='%h %ad %s' <le SHA de la ligne>
```

Exemple (07-11-2026) : `refs/backup/125-build-info-sb331-int` →
`289ffe9c 2024-07-03 Sec39_Chap358-XXX: fixed the issue with the csv-file in the jar file`.

**3. Restaurer une branche supprimée**

```bash
git push origin refs/backup/<nom>:refs/heads/<nom>
```

La branche réapparaît sur GitHub, au commit exact où elle avait été supprimée. La sauvegarde, elle, reste en place.

**4. Supprimer une branche (toujours dans cet ordre)**

```bash
git push origin refs/remotes/origin/<nom>:refs/backup/<nom>   # 1. sauvegarder
git push origin --delete <nom>                                # 2. supprimer
git fetch --prune origin                                      # 3. nettoyer ma copie locale
```

---

## Pour comprendre

**Ce qu'est une sauvegarde ici.** Une branche Git n'est qu'un **nom qui pointe sur un commit**. Git appelle
cela une **référence**, et il les range par familles :

| Famille | Contenu |
|---|---|
| `refs/heads/<nom>` | les **branches** normales, celles qu'affiche `git branch` |
| `refs/backup/<nom>` | mes **sauvegardes** |

Une sauvegarde n'est donc pas une copie de fichiers : c'est **le même commit**, gardé sous un autre nom.
Elle n'apparaît pas dans la liste des branches, ni sur GitHub ni en local — c'est voulu, elle ne gêne pas —
mais elle empêche Git d'effacer le commit, et tout le travail qui mène jusqu'à lui reste atteignable.

**Deux endroits.** Une référence peut exister à un endroit et pas à l'autre :

| Endroit | Où | Comment la lire |
|---|---|---|
| ma copie locale | `C:\Pierrot\05_Git_Repositories\Udemy\JohnThomson\spring-7-rest-mvc\.git` | `git show-ref` |
| GitHub (`origin`) | sur les serveurs de GitHub | `git ls-remote origin` |

Le 07-11-2026, j'ai découvert que mes 68 sauvegardes d'alors n'existaient **que** dans ma copie locale :
elles auraient disparu avec le disque dur, ou lors d'un nouveau clone. Elles sont depuis **sur GitHub aussi**.

**Pourquoi la liste n'est pas recopiée dans ce document :** un tableau écrit à la main mentirait dès la
sauvegarde suivante. La vérité est dans Git, et la commande du point 1 la donne en une seconde.

**À savoir :** les sauvegardes des nettoyages de septembre et octobre 2026 portent le nom de la branche
supprimée, sans date. Pour savoir de quand elles datent, utiliser le point 2 : la date du commit le dit.
