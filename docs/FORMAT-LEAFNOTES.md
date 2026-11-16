# Format d'archive .leafnotes — version 1

Une archive LeafNotes est un fichier JSON UTF-8. Elle peut être copiée sur un ordinateur, ouverte dans un éditeur de texte et réimportée dans LeafNotes.

## Structure

- `format`: identifiant obligatoire `leafnotes`.
- `version`: version entière du format (actuellement `1`).
- `exportedAt`: date d'export en millisecondes Unix.
- `categories`: tableau de catégories (`name`, `emoji`, `color`, `parent`).
- `notes`: tableau de notes (`id`, `title`, `body`, `category`, `favorite`, `updatedAt`).

## Règles d'import

1. Les identifiants de notes déjà présents sont ignorés : aucune note locale n'est écrasée.
2. Les catégories portant un nom déjà utilisé reçoivent un suffixe (par exemple `Cours (2)`).
3. Les notes importées sont rattachées au nom de catégorie remappé.
4. Les champs inconnus peuvent être ignorés par les versions plus anciennes.
5. La version de format doit être comprise entre 1 et la version prise en charge par l'application.

## Exemple minimal

```json
{
  "format": "leafnotes",
  "version": 1,
  "exportedAt": 1794700800000,
  "categories": [
    { "name": "Cours", "emoji": "📚", "color": 4283851973, "parent": null }
  ],
  "notes": [
    {
      "id": "note-unique-id",
      "title": "Exemple",
      "body": "Contenu modifiable sur ordinateur.",
      "category": "Cours",
      "favorite": false,
      "updatedAt": 1794700800000
    }
  ]
}
```

Le moteur de transfert est présent dans `LeafNotesTransfer.kt`. L'intégration des actions d'import/export dans les écrans Android et les tests d'aller-retour restent nécessaires avant de déclarer cette fonction terminée.
