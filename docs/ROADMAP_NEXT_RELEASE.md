# Feuille de route — prochaine version LeafNotes

Cette feuille regroupe les demandes confirmées pour la prochaine version de test. Toutes les données et catégories existantes doivent être préservées. Aucun changement sur `main`, aucune fusion automatique.

## P0 — retours du dernier test
- [ ] LeafGraph : choisir une image PNG/JPG/JPEG comme icône propre à chaque note.
- [ ] Persister les icônes localement (URI/document persistant ou copie interne), les afficher dans LeafGraph, permettre de recadrer/ajuster et de rétablir l'icône par défaut.
- [ ] Gestion des catégories : bouton Supprimer visible sur chaque grande catégorie et sous-catégorie.
- [ ] Confirmation explicite « Êtes-vous sûr de vouloir supprimer ? » avec Annuler / Supprimer.
- [ ] Politique de suppression claire : proposer de déplacer notes et descendants vers un dossier cible ou de supprimer le dossier vide ; ne jamais effacer silencieusement notes/pièces jointes.
- [ ] Corriger les boutons des modules Leaf : un seul clic = une seule action ; vérifier callbacks, recompositions, clés stables et doubles événements.
- [ ] Tests de non-régression pour création/suppression catégories, sélection, pagination et rappels.

## P1 — LeafQuiz
- [ ] Modes : choix multiple, vrai/faux, réponse libre, plusieurs réponses possibles.
- [ ] Création de quiz depuis notes sélectionnées ou une catégorie.
- [ ] Mélange des questions, difficulté, limite de temps optionnelle.
- [ ] Explication/correction après réponse, reprise et historique des résultats.
- [ ] Statistiques par catégorie et liste des erreurs à réviser.
- [ ] Ne pas présenter une question générée automatiquement comme fiable sans validation ; proposer la modification avant enregistrement.

## P2 — modules Leaf et expérience générale
- [ ] LeafFolders : arborescence profonde, déplacement et renommage sans perte.
- [ ] LeafLearning / LeafSwipe : révisions espacées, progression persistante.
- [ ] LeafReminder / LeafCalendar : date/heure, répétition, rappels liés aux notes et annulation/modification.
- [ ] LeafMedia : pièces jointes image, vidéo, audio, PDF et autres fichiers.
- [ ] LeafSearch : recherche globale dans titres, contenus, catégories et noms de pièces jointes.
- [ ] LeafBackup / LeafExport : export complet ou sélectif, formats documentaires et restauration avec fusion/aperçu plutôt que remplacement silencieux.
- [ ] LeafWidget : widgets Android notes favorites, rappels et aperçu LeafGraph, avec actualisation et tailles adaptatives.
- [ ] LeafAI : fonctions hybrides et locales lorsque possible ; expliciter les fonctions nécessitant réseau.
- [ ] Charte LeafColor : Oasis #206B4D, Noyer #6B4935, or naturel et crème foncée ; interface responsive téléphone/tablette/ordinateur.

## Validation de livraison
- [ ] Compiler depuis la branche de travail.
- [ ] Vérifier les logs et les tests automatisés.
- [ ] Vérifier que l'artefact contient réellement un APK téléchargeable.
- [ ] Installer/tester sur Android (Galaxy A15 cible).
- [ ] Fournir le lien de workflow et l'APK seulement après vérification.
- [ ] Documenter les fonctions terminées, partielles et restantes.

## Règles de sécurité
- Préserver toutes les catégories et données existantes.
- Aucun changement sur `main`, aucune fusion de PR sans autorisation explicite.
- Fonctionnement hors ligne prioritaire ; synchronisation automatique seulement avec gestion des conflits.
- Ne jamais déclarer une fonctionnalité ou un test terminé sans preuve.