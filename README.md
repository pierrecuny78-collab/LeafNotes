# LeafNotes

**LeafNotes** est une application Android native de prise de notes et de bibliothèque personnelle de l'écosystème LeafVibe.

> Projet en développement — la première version vise une base Android installable, locale et hors ligne.

## Objectifs
- Fonctionnement hors ligne, sans compte ni abonnement.
- Bibliothèque organisée par catégories avec navigation horizontale.
- Catégories protégées et personnalisables (nom, emoji, couleur), avec sous-dossiers imbriqués.
- Aucune catégorie n'est supprimée par l'application.
- Notes présentées par groupes de cinq éléments avec navigation.
- Création, modification, favoris et mise à la corbeille des notes.
- Étiquettes personnalisées et recherche dans les étiquettes.
- Listes à cocher interactives dans l'éditeur.
- Questions/réponses liées aux notes et révisions espacées avec LeafQuiz.
- Couleurs de catégories visibles dans la bibliothèque et LeafGraph.
- Modification et suppression des catégories avec conservation des notes.
- Corbeille persistante avec restauration et suppression définitive.
- Export/import de sauvegardes `.leafnotes` via le sélecteur de fichiers Android.
- Fusion d’import sans écraser les notes locales existantes.
- Recherche dans les titres, contenus, tags et listes à cocher.
- Icônes/emoji personnalisés par note.
- Listes à cocher et cartes question/réponse LeafQuiz.
- LeafGraph interactif : filtres, fonds Crème/Nuit/Sauge/Papier, formes Rond/Carré/Losange, zoom, déplacement et liens [[Titre]] avec flèches.
- LeafSwipe et LeafLearning pour réviser et reformuler.
- Rappels locaux Android avec notifications et annulation.
- Export texte TXT en complément des archives LeafNotes.
- Thème clair/sombre et sauvegarde locale automatique.
- Architecture prévue pour PDF/TXT/LEAFNOTES, pièces jointes, dessin, audio, rappels et sauvegardes externes.

## État de la version 0.3.0 en test
Le socle comprend bibliothèque, catégories hiérarchiques modifiables et colorées, notes, recherche (titre, contenu, étiquettes et checklist), favoris, pagination de cinq notes, thème, persistance locale, corbeille avec restauration, import/export, checklists et LeafQuiz hors ligne avec répétition espacée.
Les tests JVM et instrumentés couvrent le format d'archive, la fusion sans écrasement, la corbeille, l'organisation, la persistance des étiquettes/checklists/cartes de quiz, la répétition espacée et les principaux écrans.
Les pièces jointes/images intégrées, dessin, audio, rappels personnalisés date/heure, historique de versions, modèles, synchronisation automatique et IA hybride ne sont pas encore complets et restent à intégrer/tester.

## Construire l'APK
Ouvrir dans Android Studio ou lancer le workflow **Android APK** dans l'onglet Actions. Le workflow compile l'APK debug et le publie comme artefact téléchargeable en cas de succès.

## Compatibilité
- Android API 26 et supérieure.
- Cible principale de validation : Samsung Galaxy A15 5G.
- La compatibilité Galaxy Fit3 reste un chantier expérimental distinct.

## Confidentialité
Les notes sont stockées localement. Aucun compte LeafNotes ni service distant n'est requis.
