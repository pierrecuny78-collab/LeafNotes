# LeafNotes

**LeafNotes** est une application Android native de l'écosystème LeafVibe, conçue comme un espace personnel de notes, de connaissances et de création visuelle.

> Projet en développement. La conception cible est local-first/offline-first, avec IA hybride facultative et synchronisation automatique entre appareils autorisés. Ces capacités sont planifiées et ne doivent pas être considérées comme déjà disponibles dans l'APK.

## Vision produit

- Bibliothèque de notes avec catégories, sous-dossiers imbriqués, tags et recherche globale.
- Modules Leaf : LeafNotes, LeafGraph, LeafFlow, LeafSwipe, LeafLearning, LeafQuiz, LeafBackup, LeafAI et LeafSync.
- LeafGraph : graphe de connaissances, cartes mentales et tableaux blancs.
- LeafFlow : workflows visuels séparés du graphe de connaissances, avec simulation et exécution locale contrôlée.
- LeafAI : fonctions locales et fournisseurs IA distants facultatifs, avec consentement explicite avant transfert de contenu.
- LeafSync : synchronisation automatique des appareils appairés, file d'attente hors ligne et résolution non destructive des conflits.
- LeafBackup : archives indépendantes, sauvegardes complètes ou sélectives et restauration contrôlée.

## Principes

- Les notes restent consultables et modifiables hors ligne.
- La synchronisation n'est pas une sauvegarde : LeafBackup reste indépendant.
- Aucune transmission distante de notes privées sans activation/consentement explicite.
- Aucune suppression irréversible ou migration destructive silencieuse.
- Les fonctionnalités planifiées ne sont annoncées comme disponibles qu'après implémentation et tests.

## État du socle historique

Le socle initial comprend notes texte, catégories simples, recherche titre/contenu, favoris, thème clair/sombre et persistance locale. Des modules et prototypes supplémentaires existent sur des branches de développement ; leur présence dans le code ne garantit pas leur validation fonctionnelle.

## Documentation

- [Plan de consolidation V0.2](docs/V0.2_CONSOLIDATION.md)
- [Feuille de route](docs/ROADMAP.md)

## Construire l'APK

Ouvrir le projet dans Android Studio ou utiliser le workflow Android APK dans GitHub Actions. L'APK ne doit être distribuée comme vérifiée qu'après réussite du workflow, présence de l'artefact et contrôle du fichier.

## Compatibilité

- Android API 26 et supérieure (configuration historique à revalider lors des évolutions).
- Appareil de validation cible : Samsung Galaxy A15 5G.
- La compatibilité Galaxy Fit3 reste un chantier expérimental distinct.
