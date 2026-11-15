# Feuille de route LeafNotes — version complète

Cette feuille de route distingue les fonctions intégrées à l'interface, celles qui disposent d'une persistance et celles encore planifiées. Une classe de domaine seule ne constitue pas une fonction livrée.

## Socle intégré — version 0.3.0 en test

- [x] Application Android Kotlin + Jetpack Compose, locale et offline-first
- [x] Bibliothèque de notes, favoris, recherche et pagination de cinq notes
- [x] Grille de notes responsive selon la largeur disponible
- [x] Catégories hiérarchiques, cinq éléments par page, navigation parent/enfant
- [x] Catégories protégées : aucune suppression de catégorie ; renommage et personnalisation nom/emoji/couleur
- [x] Notes avec icône/emoji, tags, checklist interactive et cartes Q/R
- [x] Recherche dans titre, contenu, tags et tâches
- [x] Corbeille de notes avec restauration et suppression définitive explicite
- [x] LeafBackup : export/import .leafnotes, fusion sans écrasement des notes locales et conservation de la corbeille
- [x] Réparation des catégories manquantes référencées par des notes importées
- [x] Export TXT incluant tags, tâches et cartes Q/R
- [x] LeafGraph interactif : filtre par catégorie, fonds Crème/Nuit/Sauge/Papier, formes Rond/Carré/Losange, zoom, déplacement, liens [[Titre]] avec flèches
- [x] Préférences du fond et de la forme de LeafGraph persistées localement
- [x] LeafSwipe : révision par cartes et geste horizontal
- [x] LeafLearning : reformulation active avant révélation et statistiques de session
- [x] LeafQuiz : cartes question/réponse et répétition espacée avec progression persistante
- [x] Rappels locaux Android via WorkManager, permission de notification et annulation
- [x] Panneau Paramètres/Aide et accès LeafBackup
- [x] Icône Android LeafVibe
- [x] Tests JVM, tests instrumentés et tests UI ajoutés
- [ ] Validation finale CI de tous les tests et compilation de l'APK 0.3.0
- [ ] Installation et validation manuelle sur Samsung Galaxy A15 5G

## Prochaine étape — éditeur et médias

- [ ] Images et pièces jointes intégrées à une note, avec copie locale et sauvegarde portable des fichiers
- [ ] Dessin manuscrit avec stylet/doigt, gomme, couleurs et export
- [ ] Enregistrement audio et lecture
- [ ] Mise en forme enrichie : titres, gras, listes, citations et liens
- [ ] Modèles de notes réutilisables
- [ ] Historique des révisions persistant, annulation et rétablissement
- [ ] Rappels personnalisés date/heure, répétition, modification et annulation depuis l'éditeur

## Organisation avancée

- [ ] Fil d'Ariane complet et navigation vers chaque ancêtre
- [ ] Déplacement de catégories et sous-dossiers avec prévention des cycles
- [ ] Déplacement multiple de notes, tri et filtres combinés
- [ ] Sous-notes et backlinks
- [ ] Vues intelligentes : récemment modifiées, épinglées, archivées, tâches à faire
- [ ] Export sélectif et ZIP avec manifeste, pièces jointes et empreintes SHA-256
- [ ] Export PDF et Markdown

## Modules Leaf

- [ ] LeafGraph : liens graphiques éditables, positions personnalisées des nœuds et vues locales/globales
- [ ] LeafFlow : éditeur de workflows distinct du graphe de connaissances, simulation et exécution locale contrôlée
- [ ] LeafLearning : progression durable par note et statistiques à long terme
- [ ] LeafQuiz : questions à choix multiples et évaluations avancées
- [ ] IA hybride facultative, locale prioritaire, aucun envoi distant sans consentement
- [ ] Synchronisation automatique entre appareils autorisés, file hors ligne et résolution non destructive des conflits

## Validation et publication

- [ ] Tests de migration depuis les anciennes structures
- [ ] Tests d'import/export sur bibliothèque volumineuse
- [ ] Tests permissions, rappels et notifications Android
- [ ] Tests performance, accessibilité, tablette et rotation
- [ ] Installation/mise à jour sur Samsung Galaxy A15 5G
- [ ] Build release signé et APK final vérifié
- [ ] Guide utilisateur et procédure de restauration

## Règle de livraison

Une fonctionnalité n'est considérée terminée que si elle est accessible dans l'interface, que ses données sont persistées, que les erreurs sont traitées et qu'un test pertinent réussit. La validation sur l'appareil réel reste distincte de la compilation en émulateur.
