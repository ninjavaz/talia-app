# @talia/ui

Biblioteka komponentów Talii (Angular CLI library, bez Nx). Import w aplikacji: `@talia/ui`, mapowany w `tsconfig.json` bezpośrednio na źródła, więc nie trzeba jej budować osobno.

Podgląd wszystkich komponentów z przykładowymi danymi: **http://localhost:4200/ui** (bez logowania).

| Komponent | Do czego |
|---|---|
| `tl-page-header` | tytuł ekranu (np. data) + okrągłe przyciski akcji |
| `tl-panel` | karta z ciemnym paskiem tytułu (opcjonalnie link ze strzałką) |
| `tl-card` | biała zaokrąglona powierzchnia |
| `tl-section-header` | pogrubiony tytuł sekcji, opcjonalnie link |
| `tl-list` / `tl-list-item` | lista w karcie: ikona w beżowym kółku, tytuł, podtytuł, slot `tlListTrailing` |
| `tl-ring` | pierścień postępu (łuk 270° albo pełne koło), treść wyśrodkowana w środku |
| `tl-progress-bar` | cienki pasek postępu |
| `tl-stat` | duża liczba + podpis + podpowiedź (np. „▼ 1,8 kg”) |
| `tl-segmented` | przełącznik segmentowy w kształcie pigułki, `[(value)]` |
| `button[tlRoundButton]` | okrągły przycisk z ikoną (`chip`/`solid`/`ghost`, `sm`/`md`) |
| `button[tlCheck]` | okrągłe odhaczenie (nawyki) |
| `tl-bottom-nav` | pływająca dolna nawigacja |

Kolory pochodzą z tokenów w `src/styles.css` aplikacji (`bg-panel`, `bg-chip`, `stroke-track`, `bg-progress`, `bg-nav`...), więc jasny i ciemny motyw działają automatycznie.

Prymitywy Spartan (helm) są w `helm/` i importuje się je jako `@ui/<nazwa>`. Nowe dodajesz przez `npx ng g @spartan-ng/cli:ui <nazwa>`.

```bash
npx ng test ui --watch=false   # testy biblioteki
npx ng build ui                # build paczki (ng-packagr) do dist/ui
```
