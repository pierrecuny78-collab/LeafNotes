# Feuille de route LeafNotes — version complète

Cette feuille de route distingue le code présent, les fonctions réellement accessibles dans l'application et les travaux restant à faire. Un modèle de données ou une classe utilitaire ne signifie pas qu'une fonction est déjà disponible dans l'interface.

## V0.1 — Socle de bibliothèque

- [x] Application Android native Kotlin + Jetpack Compose
- [x] Catégories initiales et navigation
- [x] Création et modification de notes texte
- [x] Favoris, suppression simple et recherche titre/contenu
- [x] Pagination par groupes de cinq notes
- [x] Thème clair/sombre
- [x] Persistance locale des notes et catégories
- [x] Première vue LeafGraph

## V0.2 — Organisation de bibliothèque

- [x] Catégories hiérarchiques et sous-catégories (premier niveau de navigation)
- [x] Pagination des catégories par groupes de cinq
- [ ] Grille responsive 5 × 5 sur les grands écrans, adaptation mobile
- [ ] Fil d'Ariane complet avec retour à chaque ancêtre
- [ ] Renommer une catégorie et modifier son emoji/couleur
- [ ] Réordonner et déplacer catégories/sous-catégories
- [ ] Déplacer une note entre catégories depuis la liste et l'éditeur
- [ ] Suppression de catégorie avec choix explicite : déplacer, archiver ou supprimer son contenu
- [ ] Validation des cycles et de l'intégrité des hiérarchies dans chaque action UI
- [ ] Tags multiples, filtres combinés, tri par nom/date/création/modification
- [ ] Épingles, archives et vues intelligentes
- [ ] Modèles personnalisables

Le domaine contient des fonctions de validation des déplacements et de recherche par tags, mais les commandes correspondantes doivent encore être branchées à l'interface.

## V0.3 — Données, sauvegarde et ordinateur

- [x] Définition du format portable versionné .leafnotes
- [x] Encodeur/décodeur JSON et fusion sans écrasement des identifiants existants
- [ ] Écran et actions Android d'export/import via Storage Access Framework
- [ ] Sauvegarde/restauration de toute la bibliothèque, catégories, notes, métadonnées et pièces jointes
- [ ] Export TXT et PDF
- [ ] Export/import JSON documenté
- [ ] Export ZIP des pièces jointes avec manifeste et sommes de contrôle
- [ ] Aperçu avant import, détection doublons et choix de résolution
- [ ] Sauvegarde planifiée locale avec rétention configurable
- [ ] Transfert ordinateur par fichier USB/partage Android; aucune synchronisation cloud obligatoire
- [ ] Vérification d'intégrité et reprise après import interrompu

## V0.4 — Éditeur complet

- [ ] Éditeur enrichi avec mise en forme, titres, listes, citations et liens
- [x] Modèles de domaine pour checklist et pièces jointes
- [ ] Interface checklist interactive et tableaux
- [ ] Ajout d'images, documents et fichiers joints via SAF
- [ ] Dessin manuscrit avec stylet/doigt, gomme, couleurs et export
- [ ] Enregistrement audio, lecture et transcription locale si moteur disponible
- [ ] Prévisualisation des pièces jointes et gestion du stockage
- [ ] Liens entre notes et backlinks
- [ ] Autosauvegarde, indicateur d'état et restauration après fermeture inattendue

## V0.5 — Rappels et productivité

- [x] Modèles de données pour rappels et modèles de notes
- [ ] Interface de création de rappel et notifications Android
- [ ] Rappels ponctuels et récurrents avec fuseau horaire
- [ ] Vue calendrier/journal
- [ ] Notes épinglées, tâches et échéances
- [ ] Historique des révisions, annulation/rétablissement persistant

## V0.6 — Protection, partage et accessibilité

- [ ] Verrouillage biométrique ou code, verrouillage automatique
- [ ] Chiffrement des sauvegardes avec phrase secrète et format documenté
- [ ] Protection des exports et avertissement avant partage de données sensibles
- [ ] Partage Android entrant/sortant et impression
- [ ] Accessibilité TalkBack, tailles de texte, contraste et navigation clavier
- [ ] Localisation française complète et préparation multilingue
- [ ] Statistiques locales optionnelles

## V0.7 — Qualité et compatibilité

- [ ] Tests unitaires des hiérarchies, import/export, doublons et corbeille
- [ ] Tests UI des parcours principaux et des permissions Android
- [ ] Tests de migration de données entre versions
- [ ] Tests sur petit écran, tablette et rotation
- [ ] Tests de performance avec grande bibliothèque et pièces jointes
- [ ] Vérification confidentialité : fonctionnement avion, aucune requête réseau non sollicitée
- [ ] Tests d'installation/mise à jour sur Samsung Galaxy A15 5G
- [ ] Build release signé et APK final vérifié
- [ ] Guide utilisateur, notes de version et procédure de restauration

## Règle de livraison

Une fonction n'est considérée terminée que lorsque son interface est accessible, que les données survivent à un redémarrage, que les cas d'erreur sont traités et qu'un test pertinent passe. La présence d'une classe de domaine seule ne suffit pas.

## État des derniers ajouts

- `LeafNotesTransfer.kt` : format .leafnotes, lecture/écriture et fusion sûre. L'écran d'import/export n'est pas encore raccordé.
- `LeafNotesDomain.kt` : modèles checklist/pièces jointes/rappels/modèles/révisions, fonctions tags/hiérarchie et historique en mémoire. Les écrans et la persistance correspondants restent à réaliser.
