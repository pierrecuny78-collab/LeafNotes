# LeafGraph

LeafGraph est une vue locale et hors ligne du réseau de connaissances LeafNotes.

## Fonctionnalités intégrées
- Une carte de nœuds générée depuis les notes existantes.
- Regroupement spatial par catégorie.
- Liens explicites détectés dans le corps des notes avec la syntaxe `[[Titre exact de la note]]`.
- Zoom par pincement et boutons + / −.
- Déplacement de la carte au doigt.
- Sélection d'un nœud puis ouverture de la note.
- Les favoris sont différenciés visuellement.
- Aucun service distant ni compte n'est nécessaire.

## Créer des liens
Dans le texte d'une note, ajoute par exemple :

```text
Cette notion dépend de [[Introduction à la biologie]] et complète [[Cycle cellulaire]].
```

Les titres entre doubles crochets sont rapprochés des titres des notes existantes, sans distinction entre majuscules et minuscules.

## Limites actuelles
- Les liens sont détectés à partir du titre exact ; l'édition graphique des liens n'est pas encore disponible.
- Les liens de même catégorie servent de repères visuels de regroupement, ils ne représentent pas une relation sémantique déclarée.
- La disposition est calculée localement à l'ouverture ; la sauvegarde d'une position personnalisée des nœuds viendra dans une évolution ultérieure.
- Les schémas libres (formes, flèches, annotations) et l'import Mermaid restent à développer.

## Principes
Les notes et catégories existantes ne sont jamais supprimées par LeafGraph. La vue est une représentation de lecture ; elle ne modifie pas les notes tant que l'utilisateur n'ouvre pas explicitement un nœud.
