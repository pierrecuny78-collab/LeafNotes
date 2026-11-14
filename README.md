# LeafNotes

**LeafNotes** est une application Android native de prise de notes et de bibliothèque personnelle de l'écosystème LeafVibe.

> Projet en développement — la première version vise une base Android installable, locale et hors ligne.

## Objectifs
- Fonctionnement hors ligne, sans compte ni abonnement.
- Bibliothèque organisée par catégories avec navigation horizontale.
- Catégories personnalisables (nom et emoji).
- Notes présentées par groupes de cinq éléments avec navigation.
- Création, modification, favoris et mise à la corbeille des notes.
- Corbeille persistante avec restauration et suppression définitive.
- Export/import de sauvegardes `.leafnotes` via le sélecteur de fichiers Android.
- Fusion d’import sans écraser les notes locales existantes.
- Recherche dans les titres et contenus.
- Thème clair/sombre et sauvegarde locale automatique.
- Architecture prévue pour PDF/TXT/LEAFNOTES, pièces jointes, dessin, audio, rappels et sauvegardes externes.

## État de la version 0.2.0 en test
Le socle comprend bibliothèque, catégories hiérarchiques, notes texte, recherche, favoris, pagination de cinq notes, thème, persistance locale, corbeille avec restauration, et import/export de sauvegardes.
Les tests instrumentés couvrent le format d'archive, la fusion sans écrasement, la corbeille, l'organisation et la persistance entre deux instances du stockage.
Les fonctions avancées (éditeur riche, pièces jointes, dessin, audio, rappels, modèles et révisions) restent planifiées tant qu'elles ne sont pas intégrées à l'interface et validées.

## Construire l'APK
Ouvrir dans Android Studio ou lancer le workflow **Android APK** dans l'onglet Actions. Le workflow compile l'APK debug et le publie comme artefact téléchargeable en cas de succès.

## Compatibilité
- Android API 26 et supérieure.
- Cible principale de validation : Samsung Galaxy A15 5G.
- La compatibilité Galaxy Fit3 reste un chantier expérimental distinct.

## Confidentialité
Les notes sont stockées localement. Aucun compte LeafNotes ni service distant n'est requis.
