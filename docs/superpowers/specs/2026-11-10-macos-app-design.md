# GlobeRater pour macOS — conception

Date : 2026-11-10 · Statut : à valider

## 1. Objectif

Porter GlobeRater sur macOS avec les mêmes fonctions que l'app Android (convertisseur, budget,
horloge et météo de voyage, mêmes widgets), mais avec un style visuel entièrement macOS : pas de
Material, pas de palettes beige.

**Critères de réussite**

- Chaque widget Android a son équivalent sur le bureau macOS, avec le même comportement
  (pavé cliquable, touche « + » du budget, remplissage du budget, deux horloges, météo de deux lieux).
- L'app reprend les écrans Aujourd'hui, Widgets et Réglages, adaptés aux conventions Mac
  (barre latérale, fenêtre Réglages ⌘,).
- Un panneau dans la barre de menus permet de convertir au clavier et d'ajouter au budget.
- 15 langues, mêmes sources de données, aucune collecte de données.
- Un DMG téléchargeable depuis les Releases GitHub.

**Hors périmètre** : Mac App Store, notarisation, synchronisation iCloud avec Android,
widgets sur iPhone/iPad.

## 2. Contraintes

| Sujet | Choix |
|---|---|
| Distribution | DMG sur GitHub Releases, pas d'App Store |
| Signature | Apple ID gratuit (Personal Team), pas de notarisation |
| Système minimum | macOS 14 Sonoma (boutons interactifs dans les widgets) |
| Outils | Xcode 16.4, Swift 6.1, XcodeGen (déjà installé) |
| Dépendances | Aucune bibliothèque externe |
| Bundle ID | `ch.tageoa.globerater` (widgets : `ch.tageoa.globerater.widgets`), version 1.0.0 |

Préalable manuel : ajouter son Apple ID dans Xcode → Réglages → Comptes pour créer le certificat
« Apple Development » de la Personal Team. Aucun certificat n'existe aujourd'hui sur la machine.

## 3. Architecture

Tout le code Mac vit dans `macos/` ; l'app Android n'est pas modifiée.

```
macos/
  project.yml                  XcodeGen → GlobeRater.xcodeproj (ignoré par git)
  GlobeKit/                    package Swift : logique, sans interface, testable avec `swift test`
    Sources/GlobeKit/
    Tests/GlobeKitTests/
  GlobeRater/                  cible app : fenêtre, Réglages, barre de menus
  WidgetUI/                    vues des widgets + AppIntents, compilés dans l'app (galerie) et l'extension
  GlobeRaterWidgets/           cible extension WidgetKit : déclarations des 5 widgets, timelines
  Shared/Localizable.xcstrings 15 langues, généré
  scripts/build-dmg.sh
```

Les deux cibles dépendent de GlobeKit et partagent un App Group `$(TeamID).ch.tageoa.globerater`
(forme préfixée par le Team ID, la seule utilisable sans profil payant sur macOS).

### 3.1 GlobeKit

| Unité | Rôle |
|---|---|
| `Settings` | Devises d'entrée (3) et de sortie (5), clé API, intervalle de mise à jour des taux, budget (activé, montant, devise, période, période affichée, date de départ, heure de remise à zéro), lieux « chez moi » / « ici », unités, couleur d'accent par défaut. Valeurs par défaut identiques à Android (`AppSettings.kt`). |
| `RatesStore` | Taux par rapport à l'USD, date de mise à jour, taux d'exemple quand rien n'a été téléchargé. |
| `Money` | Conversion, décimales par devise (0 pour JPY, KRW…), formatage selon la locale, symbole de devise. |
| `KeypadInput` | Saisie au pavé : `append`, `backspace`, `clear`, 10 chiffres entiers maximum, `000`, séparateur décimal. Port de la logique de `WidgetCommon.kt`. |
| `BudgetPeriod` | Port fidèle de `BudgetRefreshCalculator` : début de la période courante et de la suivante, calendrier réel, pas de dérive. Facteur d'échelle entre période du budget et période affichée (`SharedBudgetManager.getScaledBudgetAmount`). |
| `BudgetLedger` | Dépenses (montant, devise, date). Ajout converti dans la devise du budget. Le total ne compte que les dépenses de la période courante ; les plus anciennes sont purgées. |
| `PadState` | Montant en cours et devise d'entrée sélectionnée, partagés par tous les convertisseurs et la barre de menus. |
| `WeatherStore` / `WeatherCodes` | Lieux résolus (nom, coordonnées, fuseau), météo par lieu (actuelle, min/max du jour, demain, jour/nuit), codes WMO → SF Symbol et libellé. |
| `ExchangeRateClient` | `GET openexchangerates.org/api/latest.json?app_id=…`, async/await, erreurs typées (clé invalide, quota, réseau). |
| `OpenMeteoClient` | Prévisions et recherche de ville (géocodage Open-Meteo). |
| `Refresher` | Décide si taux (intervalle choisi) et météo (3 h) sont périmés, télécharge, enregistre, appelle `WidgetCenter.reloadTimelines`. |
| `SharedStore` | `UserDefaults(suiteName:)` du groupe pour réglages, taux, météo, pavé ; fichier `ledger.json` dans le conteneur du groupe pour les dépenses. Écritures atomiques. |

### 3.2 Circulation des données

- Les widgets lisent le stockage partagé ; le pavé et le budget ne font jamais d'appel réseau.
- Un appui sur une touche de widget exécute une `AppIntent` (dans le processus de l'extension)
  qui modifie `PadState` ou `BudgetLedger`, puis WidgetKit redessine.
- L'app observe les changements (notification Darwin + relecture) pour que fenêtre, panneau et
  widgets restent cohérents.

## 4. App

Style : SwiftUI, matériaux translucides, SF Symbols, couleur d'accent système, police SF avec
chiffres à chasse fixe, clair/sombre automatiques.

### 4.1 Fenêtre principale (`NavigationSplitView`)

Barre latérale : **Aujourd'hui**, **Widgets**.

**Aujourd'hui** — sections groupées :
1. **Montant** : grand champ texte au clavier, menu de devise d'entrée, état des taux
   (« Taux d'exemple · Ajouter des taux réels » ou « Mis à jour il y a 2 h » + bouton ↻).
2. **Converti** : 5 lignes (symbole dans un carré arrondi, code, montant), chiffres animés.
3. **Dépensé · <période>** (si budget activé) : total, `ProgressView`, « X restants » / « X de dépassement »,
   « remise à zéro le … », bouton **+** → feuille « Ajouter une dépense » (montant, devise).
4. **Chez moi et ici** : deux cartes (rôle · ville, heure, icône météo, température) et le décalage
   (« 6 h d'avance », « même heure »). Lien « Choisir le lieu chez moi » si absent.

**Widgets** — galerie des widgets en 3 groupes (Convertisseur, Budget, Voyage), chaque aperçu
rendu par la vraie vue du widget avec des données d'exemple, et un encadré : « Clic droit sur le
bureau → Modifier les widgets → GlobeRater ». (macOS ne permet pas d'épingler un widget par code.)

### 4.2 Réglages (scène `Settings`, ⌘,)

Onglets dans la barre d'outils :
- **Taux** : clé Open Exchange Rates (champ sécurisé, lien vers l'inscription gratuite, état de
  validation), intervalle de mise à jour, devises d'entrée (3) et de sortie (5).
- **Budget** : activé, montant, devise, période, période affichée, date de départ, heure de remise à zéro,
  bouton « Remettre à zéro maintenant ».
- **Voyage** : lieu chez moi (recherche), lieu ici (ville fixe ou position du Mac), °C/°F.
- **Général** : ouvrir à l'ouverture de session (`SMAppService.mainApp`), afficher dans la barre
  de menus, à propos, crédits, confidentialité.

Localisation : `CLLocationManager`, précision réduite, autorisation « lorsque l'app est active »,
lue seulement à l'ouverture de l'app et à l'ouverture du panneau.

### 4.3 Barre de menus (`MenuBarExtra`, style fenêtre)

Icône SF Symbol `globe`. Panneau d'environ 280 pt :
- champ montant avec le focus à l'ouverture ;
- sélecteur segmenté des 3 devises d'entrée ;
- 5 conversions, clic = copier le montant ;
- barre de budget fine et bouton **Ajouter au budget** (⌘↩) ;
- pied : « Ouvrir GlobeRater », « Réglages… », « Quitter ».

Partage `PadState` avec les widgets convertisseurs.

## 5. Widgets

| Widget | Tailles | Équivalent Android |
|---|---|---|
| Convertisseur | Moyen, Grand | Compact, Tall |
| Convertisseur + budget | Moyen, Grand | Compact + budget, Tall + budget |
| Budget | Petit, Moyen | Vertical et Mini, Horizontal |
| Horloge de voyage | Moyen | Clock |
| Météo | Moyen, Grand | Weather |

- **Convertisseur, moyen** : montant + 3 conversions à gauche, pavé compact à droite.
- **Convertisseur, grand** : montant et 3 puces de devise, pavé 0–9 . 000, 5 conversions, C et ⌫.
- **+ budget** : touche **+** (ajoute le montant courant au budget puis vide le pavé) et barre de progression.
- **Budget, petit** : remplissage de bas en haut + bouton **+** (ajoute le dernier montant saisi).
  Option de configuration « Style » : Remplissage / Anneau (l'anneau remplace le Mini).
- **Budget, moyen** : barre horizontale, dépensé, restant, date de remise à zéro.
- **Horloge** : deux tuiles « Chez moi · ville » / « Ici · ville », `Text(.now, style: .time)` avec
  le fuseau de chaque ville (avance seule, sans redessin chaque minute), date, tuile plus sombre
  la nuit.
- **Météo** : une ligne par lieu (ville, rôle · description, icône SF Symbol multicolore,
  température, min/max, demain). Le grand ajoute le détail de demain.

**Style** : fond de widget système (`containerBackground`), compatible avec le mode atténué de
macOS 14 (`widgetRenderingMode`, `widgetAccentable`). Chaque widget a un paramètre
« Couleur d'accent » (Système, Bleu, Violet, Rose, Rouge, Orange, Jaune, Vert, Graphite) via
`AppIntentConfiguration`, modifiable par clic droit → Modifier. Il remplace les 7 palettes Android.
Un clic hors des boutons ouvre l'app à la bonne section (`widgetURL`).

**Rafraîchissement** : les convertisseurs et le budget renvoient une timeline dont la dernière
entrée est la prochaine remise à zéro du budget ; la météo demande un nouvel affichage toutes les 3 h.

## 6. Mises à jour en arrière-plan

Pas de service système. `Refresher` est appelé :
1. au lancement de l'app et périodiquement tant qu'elle tourne (`NSBackgroundActivityScheduler`) ;
2. par les timelines des widgets Météo (météo) et Convertisseur (taux) quand les données sont périmées ;
3. par les boutons ↻.

La remise à zéro du budget ne dépend d'aucune tâche : le total est calculé sur la période courante.

## 7. Erreurs

| Cas | Comportement |
|---|---|
| Clé API invalide ou quota dépassé | Message dans Réglages → Taux ; derniers taux conservés |
| Pas de réseau | Dernières données conservées, « Mis à jour il y a … » en gris, aucune alerte |
| Aucun taux téléchargé | Taux d'exemple signalés comme tels |
| Ville introuvable | Message sous le champ de recherche |
| Localisation refusée | « Ici » passe en ville fixe, lien vers Réglages Système |
| Fichier de dépenses illisible | Renommé en `.corrupt`, nouveau fichier vide, rien ne plante |

## 8. Traductions

`tools/gen_xcstrings.py` génère `macos/Shared/Localizable.xcstrings` depuis `tools/strings_en.py`
et `tools/i18n/*.py`. Les clés communes réutilisent les traductions existantes ; les textes
propres au Mac sont ajoutés dans ces mêmes fichiers Python, dans un dictionnaire séparé
(`EN_MAC` dans `strings_en.py`, `T_MAC` dans chaque `i18n/<langue>.py`), pour les 15 langues.
`tools/gen_strings.py` ne lit que `EN` et `T`, donc les ressources Android ne changent pas.

## 9. Tests

- **GlobeKit** (Swift Testing, `swift test`) :
  - port de `BudgetResetTest.kt` et `WidgetLogicTest.kt` ;
  - `BudgetPeriod` : fin de mois (31 → 30/28/29), années bissextiles, heure de remise, changement d'heure ;
  - `BudgetLedger` : ajout converti, total de la période, purge ;
  - `KeypadInput` : limites, séparateur, `000`, retour arrière ;
  - décodage des deux API depuis des fichiers JSON d'exemple ;
  - `Refresher` : décision « périmé ou non » avec une horloge injectée.
- **Build** : `xcodebuild` sans avertissement pour les deux cibles.
- **Visuel** : `#Preview` pour chaque widget et chaque taille, captures de l'app en clair et en sombre.

## 10. Distribution

`macos/scripts/build-dmg.sh` : `xcodegen generate` → `xcodebuild archive` (Release, signature
automatique Personal Team) → export → `hdiutil create` → `GlobeRater-1.0.0.dmg`.

README : section macOS (configuration requise, installation, ouverture malgré Gatekeeper via
Réglages Système → Confidentialité et sécurité → Ouvrir quand même), lien à côté de l'APK.

**Risque connu** : sans notarisation, l'ouverture sur un autre Mac doit être testée. Si macOS la
bloque malgré « Ouvrir quand même », il faudra un compte Apple Developer payant.

## 11. Ordre de réalisation

1. Squelette `macos/` + `project.yml` + App Group, build vide signé.
2. GlobeKit avec ses tests.
3. Fenêtre principale et Réglages.
4. Barre de menus.
5. Widgets et intents.
6. Traductions.
7. Script DMG et README.
