# 3D fractal viewer 2017 → 2026 : modernisation, nouvelles fractales et médias vitrine

Date : 2026-10-06
Statut : design approuvé (brainstorming), spec en revue

## 1. Objectif

Faire revivre le projet DUT 2017 (JavaFX, MVC) **en gardant Java/JavaFX comme cœur** :
le rendre compilable et lançable sans Eclipse, l'enrichir de fractales plus spectaculaires,
et produire des **médias vitrine** (GIF, MP4, captures) pour le README et le portfolio.

Pas de version web (CheerpJ gère mal JavaFX) : l'app n'a pas besoin d'être utilisable en ligne.

### Critères de succès

1. `mvn javafx:run` lance l'app sur macOS (Java 21), sans Eclipse.
2. 6 fractales fonctionnelles (Mandelbrot, Julia, Burning Ship, Newton, Lévy, Carré),
   les 4 palettes s'appliquent aux fractales concernées.
3. `scripts/make-media.sh` régénère en une commande : 4 MP4, 4 GIF, 6 PNG dans `docs/media/`.
4. Le README affiche ces médias (GIF en tête + grille de captures).
5. `mvn verify` passe (tests JUnit du noyau de calcul).

### Hors périmètre

- Mode « plan » (jamais implémenté en 2017, malgré le README).
- Paramètres différents par fractale (`TODO.md`).
- Fractales 3D (Mandelbulb…) : demanderaient un moteur de raymarching.
- Refonte de `SphereViewer` et passage en anglais du code 2017.
- Intégration dans `~/perso/portfolio` (tâche séparée, autre repo).
- Version web.

## 2. État de départ (constats)

- Sources utiles : ~1 200 lignes sous `PalluelGenty/src/` (`SphereViewer.java` = 592).
  ~70 fichiers suivis par git sont du bruit Eclipse (`.metadata/`, `bin/`, `.class`, PNG exportés).
- Julia est déjà sélectionnable dans l'UI ; la variable `isJulia` de
  `MandelBrotController` porte un nom inversé (vraie quand la fractale **n'est pas** Julia).
- `MainController.getCurrentController()` n'a pas de `case JULIA` (tombe sur `default`).
- Les « threads » de calcul appellent `this.run()` dans leur constructeur :
  calcul séquentiel sur le thread UI, image 2000×1000.
- `view.css` est référencé mais absent (simple warning JavaFX).
- L'export PNG utilise `SwingFXUtils` → module `javafx-swing` requis.
- `refreshSphere()` ajoute un nouveau `Group` au pane à chaque mise à jour.
- Le « zoom » 2017 de Mandelbrot n'est pas un vrai zoom : `ZOOM` multiplie `z` à chaque
  itération (déforme la fractale au lieu de l'agrandir). Pour Julia, il multiplie le point de départ.

## 3. Build et nettoyage du repo

- `pom.xml` à la racine, arborescence Maven standard : `git mv PalluelGenty/src/*` →
  `src/main/java/` (historique conservé). Tests sous `src/test/java/`.
- Java 21, OpenJFX 21 (`javafx-controls`, `javafx-swing`), `javafx-maven-plugin`, JUnit 5.
- Profil Maven `demo` qui passe `--demo` à l'application : `mvn javafx:run -Pdemo`.
- Suppression : `.metadata/`, `bin/`, `classes/`, `.classpath`, `.project`,
  `*.userlibraries`, `RemoteSystemsTempFiles/`, les 2 PNG exportés. `PalluelGenty/` disparaît.
- `.gitignore` : `target/`, fichiers IDE (`.idea/`, `.vscode/`, `.classpath`, `.project`,
  `.settings/`, `.metadata/`).
- Correctifs 2017 limités à ce qui bloque compilation/exécution ou fausse le comportement :
  chemin CSS (retirer la référence), commentaire mal encodé, `case JULIA`, renommage de
  `isJulia`. `java.util.Observable` (déprécié) est conservé. Commentaires français d'origine
  conservés ; le nouveau code est commenté en anglais.

## 4. Noyau de calcul : package `fractal` (sans JavaFX)

Toutes les classes de ce package sont pures (aucune dépendance JavaFX) et produisent un
`int[]` ARGB de taille `width × height`, rangé ligne par ligne.

### 4.1 `Viewport` (record)

`Viewport(double centerRe, double centerIm, double scale, int width, int height)`,
`scale` = unités complexes par pixel. Méthodes `re(col)` et `im(row)`.
Un helper construit le viewport depuis l'état du `Modele` en reproduisant le cadrage 2017
(largeur visible = 4 unités à zoom 1, pixels carrés, centre en 0) :
`scale = 4 × ZOOM / width`. **Changement de comportement assumé** : le zoom devient un vrai
zoom géométrique pour Mandelbrot, Julia et Burning Ship. Les boutons Zoom +/− de l'UI ne sont
pas concernés : ils déplacent la caméra (translation Z de la sphère) ; `Modele.ZOOM` n'est
piloté que par le mode démo.

### 4.2 `EscapeTimeRenderer`

- `enum Formula { MANDELBROT, JULIA, BURNING_SHIP }`.
- `int[] render(Formula f, Viewport v, int maxIter, double juliaRe, double juliaIm, Palette p)`.
- Itération :
  - Mandelbrot : `z₀ = 0`, `c = pixel`, `z ← z² + c`.
  - Julia : `z₀ = pixel`, `c = (Z, Zi)` du `Modele` (champs Alpha/Beta de l'UI).
  - Burning Ship : comme Mandelbrot avec `z ← (|Re z| + i|Im z|)² + c`.
- Coloriage lisse : rayon d'échappement 256 ; `μ = n + 1 − log(log|z|) / log 2` ;
  couleur = `p.colorAt(μ / 64)` (cyclique). Points non échappés à `maxIter` → noir.
- Parallélisme : `IntStream.range(0, height).parallel()`, une ligne par tâche.

### 4.3 `NewtonRenderer`

- `int[] render(Viewport v, int maxIter, Palette p)` pour `f(z) = z³ − 1`.
- `z ← z − (z³ − 1) / (3z²)` jusqu'à `|z − racine| < 1e-6` ou `maxIter`.
- Teinte = `p.colorAt(k / 3)` pour la racine `k ∈ {0,1,2}` ; luminosité décroissante avec
  le nombre d'itérations. Non convergé → noir.
- Même parallélisme par lignes.

### 4.4 `Palette` (enum)

- `CLASSIC` (formule HSB 2017 : `hsb(i/4, 1, i/(i+5))` ramenée sur `t`), `FIRE`, `OCEAN`, `NEON`.
- Dégradés à points d'arrêt, précalculés en table de 1024 couleurs ARGB.
- `int colorAt(double t)` : `t` pris modulo 1.

### 4.5 Branchement sur l'existant

- `Modele.ListControllers` : ajout de `BURNING_SHIP`, `NEWTON`. Ajout d'un champ `palette`
  (défaut `CLASSIC`) avec setter qui notifie les observateurs, et d'un centre de viewport
  (défaut 0,0 ; utilisé par le mode démo).
- `MandelBrotController` : délègue à `EscapeTimeRenderer` (formule déduite de la fractale
  courante) et copie le tableau dans sa `WritableImage` via `PixelWriter.setPixels`.
- Nouveau `NewtonController`, même principe.
- `MainController.getCurrentController()` : `JULIA` et `BURNING_SHIP` → `MandelBrotController`,
  `NEWTON` → `NewtonController`.
- `SphereViewer` : 2 boutons radio (Burning Ship, Newton) + une `ComboBox<Palette>`.
  Rien d'autre ne change dans la vue.

## 5. Mode démo et médias

### 5.1 `DemoDirector` (package `demo`)

- Activé par l'argument `--demo` ; joue tous les clips puis quitte (`Platform.exit()`).
- Déroulé **frame par frame, indépendant du temps réel** : pour la frame `i` d'un clip,
  le directeur règle l'état (rotation, zoom, centre, fractale, palette, constante Julia),
  recalcule la texture si l'état a changé, puis fait `snapshot()` du panneau 3D seul
  (sans menu) et écrit `target/demo-frames/<clip>/frame-0001.png`.
- Enchaînement des frames via `Platform.runLater`, 30 fps logiques.
- Toute exception pendant une frame → message sur stderr et sortie avec code non nul.

### 5.2 Clips (5 à 8 s chacun)

| Clip | Contenu |
|---|---|
| `sphere-spin` | Rotation 360° de la sphère, Mandelbrot, palette `FIRE` |
| `deep-zoom` | Zoom progressif vers −0.745 + 0.11i (vallée des hippocampes), sphère en rotation lente |
| `julia-morph` | Constante `c` parcourant un cercle de rayon 0.7885 ; la Julia se transforme |
| `tour` | Mandelbrot → Julia → Burning Ship → Newton → Lévy → Carré, palette changeante |

Plus 6 captures fixes plein cadre, une par fractale : `mandelbrot.png`, `julia.png`,
`burning-ship.png`, `newton.png`, `levy.png`, `square.png`.

### 5.3 `scripts/make-media.sh`

- `set -euo pipefail` ; vérifie la présence de `mvn` et `ffmpeg` (message clair sinon).
- Lance `mvn -q javafx:run -Pdemo`, puis pour chaque clip :
  - MP4 : h264, `-pix_fmt yuv420p`, `-crf 20`, 30 fps ;
  - GIF : 15 fps, 640 px de large, `palettegen` + `paletteuse`, cible < 5 Mo.
- Sorties dans `docs/media/` (commité) ; les frames brutes restent dans `target/` (ignoré).
- Durée attendue de génération : 1 à 3 min (hypothèse : < 0,5 s par frame 2000×1000
  une fois parallélisé, à mesurer).

## 6. README

- GIF `tour` en tête, grille des 6 captures, liens vers les MP4.
- « Getting started » : prérequis (Java 21, Maven), `mvn javafx:run`, `scripts/make-media.sh`.
- Retrait de la mention du mode plan ; ajout des nouvelles fractales et des palettes.

## 7. Tests et vérification

Tests JUnit 5 (noyau `fractal` uniquement, sans écran) :

- Mandelbrot : `(0, 0)` ne s'échappe pas (noir) ; `(1, 1)` s'échappe en ≤ 6 itérations (rayon 256).
- Burning Ship : `(−1.75, 0)` (antenne, sur l'axe réel) ne s'échappe pas ; `(3, 3)` s'échappe en ≤ 4 itérations.
- Newton : un pixel placé sur la racine `1` converge vers la racine d'indice 0.
- `Palette` : `colorAt(0)` = premier point d'arrêt, `colorAt(1)` = `colorAt(0)` (cyclique).
- Rendu parallèle identique à un rendu séquentiel de référence sur une petite image.

Vérification manuelle avant PR : `mvn verify` ; `mvn javafx:run` et passage sur les
6 fractales × 4 palettes ; `scripts/make-media.sh` produit les 14 fichiers attendus.

## 8. Livraison

Feature branch → PR vers `master` → merge en squash. Aucun push sans accord explicite.

## 9. Amendements pendant l'implémentation (2026-10-06)

- **OpenJFX 26.0.2 au lieu de 21** : sur macOS 26, OpenJFX 21.0.x n'affiche la texture que
  sur une calotte d'environ 45° de la sphère (le reste est noir) ; OpenJFX 26 corrige le rendu.
  Le code reste compilé en Java 21, mais l'exécution demande un **JDK 24 ou plus**.
- **Médias 2D en vedette** (choix de Laurent) : `deep-zoom`, `julia-morph` et les 6 captures
  sont enregistrés **à plat** (la texture 2000×1000 elle-même) ; `sphere-spin` et `tour`
  restent sur la sphère. Le README affiche un GIF 2D en tête et une section « En 3D ».
- **Bug 2017 corrigé** : la courbe de Lévy ne dessinait que 4 caractères de l'axiome (image noire).
- Cadrages ajustés : Mandelbrot centré en −0,5 ; Burning Ship sur le « navire » (−1,755 ; −0,02).
