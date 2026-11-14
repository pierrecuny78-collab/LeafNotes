# Feuille de route LeafNotes

> Feuille de route cible. Une case n'est cochée qu'après implémentation et vérification. Les fonctionnalités planifiées ne sont pas nécessairement présentes dans l'APK actuelle.

## V0.1 — Socle de bibliothèque
- [x] Application Android native Kotlin + Jetpack Compose
- [x] Création, modification, favoris et recherche titre/contenu
- [x] Catégories de premier niveau
- [x] Persistance locale et thème clair/sombre
- [x] Navigation horizontale initiale

## V0.2 — Organisation et navigation
- [ ] Modèle de dossiers à identifiants stables et profondeur imbriquée
- [ ] Création, renommage, déplacement et tri de sous-dossiers
- [ ] Fil d'Ariane et navigation dans l'arborescence
- [ ] Barre latérale rétractable + swipe horizontal complémentaire
- [ ] Recherche globale notes, dossiers, catégories et tags
- [ ] Épingles, archives, corbeille et restauration
- [ ] Suppression de dossier avec aperçu des éléments affectés
- [ ] Couleurs, icônes et ordre personnalisables
- [ ] Migration non destructive des anciennes catégories et notes

## V0.3 — LeafGraph : espace visuel complet
- [ ] Graphe de connaissances : liens éditables, backlinks, notes orphelines
- [ ] Vues globale et locale, filtres par dossier/tag
- [ ] Carte mentale avec branches, nœuds, couleurs et annotations
- [ ] Tableau blanc avec formes, textes, flèches et éléments libres
- [ ] Zoom, déplacement, sélection et disposition persistante
- [ ] Relations croisées vers notes, dossiers et projets
- [ ] Export/import séparé de chaque type de graphe

## V0.4 — LeafFlow : workflows visuels
- [ ] Éditeur de nœuds et connexions inspiré des workflows n8n
- [ ] Nœuds départ, action, condition, délai et sortie
- [ ] Validation des connexions et détection des cycles non autorisés
- [ ] Simulation avant exécution
- [ ] Exécution locale contrôlée, permissions par action
- [ ] Historique, journal d'exécution et reprise sur erreur
- [ ] Import/export JSON versionné
- [ ] Aucun code arbitraire importé exécuté automatiquement

## V0.5 — Éditeur enrichi et pièces jointes
- [ ] Titres, Markdown, checklists, tableaux et blocs de code
- [ ] Images, PDF, audio, liens et pièces jointes
- [ ] Dessin et annotations manuscrites
- [ ] Modèles de notes (cours, projet, réunion, idée)
- [ ] Liens internes, tags et références entre modules

## V0.6 — LeafBackup et protection
- [ ] Format d'archive .leafnotes versionné avec manifeste
- [ ] Export complet ou sélectif par notes, dossiers, catégories et modules
- [ ] Export JSON, Markdown/TXT et PDF
- [ ] Export des graphes, workflows et pièces jointes
- [ ] Import avec aperçu, détection des doublons et stratégie de fusion
- [ ] Vérification d'intégrité par empreintes
- [ ] Sauvegardes locales automatiques avec rétention configurable
- [ ] Historique, restauration et récupération après erreur

## V0.7 — LeafTasks, LeafCalendar et LeafTimeline
- [ ] Tâches liées aux notes et dossiers
- [ ] Priorités, échéances, états et progression
- [ ] Calendrier et rappels
- [ ] Chronologie des événements et modifications
- [ ] Vues quotidiennes, hebdomadaires et par projet

## V0.8 — IA hybride LeafAI
- [ ] Interface AIProvider indépendante des fournisseurs
- [ ] Fonctions locales disponibles sans Internet ni compte externe
- [ ] Adaptateur de modèle local lorsque compatible avec l'appareil
- [ ] Fournisseurs distants facultatifs, configurables et désactivables
- [ ] Résumés, explications, suggestions de tags et de classement
- [ ] Suggestions de liens entre notes et aide à la création de quiz
- [ ] Aperçu et validation avant toute modification de données
- [ ] Consentement explicite avant tout envoi de contenu à un service distant
- [ ] Indication claire du moteur actif et des données transmises
- [ ] Gestion des erreurs et fonctionnement normal sans IA

## V0.9 — LeafSync : synchronisation automatique
- [ ] Couche SyncProvider et choix/configuration d'une destination compatible
- [ ] Appairage explicite et révocation d'appareils
- [ ] File locale durable pour les changements hors ligne
- [ ] Synchronisation automatique à la reconnexion et en arrière-plan
- [ ] Synchronisation différentielle et reprise après interruption
- [ ] Chiffrement en transit et conception du chiffrement de bout en bout
- [ ] Gestion non destructive des conflits et conservation des versions
- [ ] Statut de synchronisation visible et diagnostic des erreurs
- [ ] Préférences Wi-Fi/données mobiles et contrôle utilisateur
- [ ] Tests multi-appareils, réseau instable, doublons et révocation
- [ ] Distinguer clairement synchronisation et sauvegarde LeafBackup

## V1.0 — Écosystème LeafNotes
- [ ] Recherche transversale dans notes, dossiers, tâches, graphes et pièces jointes
- [ ] Relations cohérentes entre les modules Leaf
- [ ] Tableau de bord et navigation personnalisables
- [ ] Thèmes clair, sombre, système et palette LeafVibe indépendante d'Android
- [ ] Accessibilité, performances et compatibilité téléphone/tablette
- [ ] Tests de sécurité, confidentialité, migration et récupération
- [ ] APK Debug/Release compilée, artefact vérifié et rapport de tests

## Principes non négociables
- Local-first et offline-first : les notes restent accessibles et modifiables sans réseau.
- IA hybride : aucune transmission distante silencieuse de contenu privé.
- Synchronisation automatique : uniquement entre appareils/destinations autorisés, avec gestion sûre des conflits.
- Sauvegarde distincte de la synchronisation.
- Aucune suppression irréversible sans action explicite.
- Toute migration doit être testée sur une copie et préserver les données existantes.
- Ne pas déclarer une fonctionnalité terminée avant tests reproductibles.
- Aucune fusion dans main sans autorisation explicite du propriétaire.

## État
Les éléments cochés dans V0.1 décrivent le socle historique. Les étapes V0.2 à V1.0 sont des objectifs de développement et doivent être validées par code, tests et APK.
