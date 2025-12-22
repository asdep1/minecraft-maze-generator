# Minecraft Labyrinth Generator

Un outil externe puissant pour générer des labyrinthes complexes et personnalisés pour Minecraft. Ce projet permet de créer des structures prêtes à l'emploi, allant des formats schématiques classiques aux mondes Minecraft complets.

## 🚀 Fonctionnalités

- **Configuration Flexible** : 
  - Taille ajustable (X, Z).
  - Hauteur intérieure personnalisable (Y).
  - Largeur des couloirs et épaisseur des murs modulables.
  - Option pour activer ou désactiver le plafond.
- **Système de Salles** : Ajoutez des salles personnalisées avec un nombre d'entrées spécifique au sein de votre labyrinthe.
- **Érosion Avancée** :
  - Paramètre d'érosion global pour un aspect "ruiné".
  - **Zones d'érosion** : Appliquez des facteurs d'érosion différents à des zones spécifiques (ex: une zone plus dévastée qu'une autre).
  - Protection automatique des murs extérieurs et des salles contre l'érosion.
- **Thèmes et Blocs** :
  - Définition de presets de blocs pour le sol, les murs et le plafond via des fichiers externes dans `versions/{version}/themes/`.
  - **Sélection pondérée** : Gérez la rareté des blocs directement dans le fichier de thème.
- **Support Multi-version** :
  - Système de registre de blocs via fichiers CSV (`versions/`).
  - Compatible 1.12.2 (Legacy) et 1.20.1+ (Moderne).
  - Support des blocs de mods.

## 📂 Formats d'Exportation

Le générateur supporte plusieurs formats pour une intégration facile :
- **Monde Minecraft** : Génère un dossier de monde complet (format Anvil) avec un biome plat et des couches de sol adaptées.
- **.schematic** : Format classique compatible avec WorldEdit et MCEdit (inclut le support des IDs > 255).
- **.png** : Une vue de dessus du labyrinthe pour une visualisation rapide.

## 🛠️ Utilisation

Le point d'entrée principal se trouve dans `fr.asdep.labgen.cli.Main`. Vous pouvez y configurer votre labyrinthe avant de lancer la génération.

Exemple de configuration :
```java
GenerationConfig config = new GenerationConfig();
config.setGameVersion("1.20.1");
config.setWidth(50); // X
config.setDepth(50); // Z
config.setHeight(10); // Y
config.setCorridorWidth(3);
config.setWallWidth(1);
config.setErosion(0.1f);
```

Ajout de salle :
```java
config.addRoom(new Room(5, 5, 20, 20, 2));
```

Ajout d'une zone d'érosion :
```java
config.addErosionZone(new ErosionZone(0, 0, 25, 25, 0.3f));
```

Pallete de blocs par version dans `versions/`.

## 📜 Crédits

Ce projet utilise des algorithmes de génération de labyrinthes (Recursive Backtracker, Kruskal, Prim, etc.) basés sur le travail de **js42721**.
- **Dépôt original** : [js42721/maze](https://github.com/js42721/maze/tree/master)

Utilisation d'IA (Junie par JetBrains) pour la génération du ReadMe.md et système d'export vers un monde Minecraft et vers un schematic.

## 📝 TODO List

- [x] Utilisation pratique avec modification des valeurs.
- [x] Choix des moyens d'exportation.
- [x] Themes personnalisables par version via des fichiers.
- [x] Choix de l'algorythme
- [ ] Optimiser davantage l'exportation pour les labyrinthes de taille gigantesque.
- [ ] Ajouter une interface graphique (GUI) pour une configuration simplifiée.
- [ ] Implémenter plus d'algorithmes de génération (ex: labyrinthes circulaires).
- [ ] Ajouter un système de "biomes" au sein du même labyrinthe (changement de thème dynamique).
