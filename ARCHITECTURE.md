# IngressosNaMao – Architecture

Kotlin · Jetpack Compose · MVVM · Clean Architecture · Koin · Coroutines · Retrofit · Room.
Base package: `com.innaval.ingresosnamao`.

## Module graph (arrows = "may depend on")

```
                       :app  (composition root: DI, navigation, config)
          ┌──────┬───────┼────────┬─────────┐
          ▼      ▼       ▼        ▼         ▼
     :feature: :feature: :feature: :feature: :feature:
       auth     events    cart    payment    ticket
        │ │      │ │       │ │      │ │       │ │
        │ └──────┼─┼───────┼─┼──────┼─┼───────┘ │   (every feature → :core:ui, :core:common)
        ▼        ▼ ▼       ▼ ▼      ▼ ▼         ▼
   :core:network (payment)                   :core:database (cart, ticket)
        │                                         │
        └───────────────► :core:common ◄──────────┘
                              ▲
                          :core:ui  (no deps on other core modules)
```

| Module | May depend on | Must NOT depend on |
|---|---|---|
| `:core:common` | – | everything else |
| `:core:ui` | – (Compose, Material3, lifecycle only) | `:core:*`, features |
| `:core:network` | `:core:common` | `:core:database`, `:core:ui`, features |
| `:core:database` | `:core:common` | `:core:network`, `:core:ui`, features |
| `:feature:auth` | `:core:common`, `:core:ui`, Firebase Authentication and Firestore | other features, `:core:database` |
| `:feature:events` | `:core:common`, `:core:ui`, Firebase Remote Config | other features, `:core:database` |
| `:feature:payment` | `:core:common`, `:core:ui`, `:core:network` | other features, `:core:database` |
| `:feature:cart` | `:core:common`, `:core:ui`, `:core:database` | other features |
| `:feature:ticket` | `:core:common`, `:core:ui`, `:core:database` | other features |
| `:app` | everything | – |

Rules: features never depend on features; dependencies point downward only; the graph is a DAG by construction.
Cross-feature flows (events → cart → payment → tickets) are wired by `:app/navigation/AppNavHost` through primitives
passed as route callbacks/args (e.g. `totalCents`). If features ever need to share a type, put a small contract in
`:core:common`, never a feature→feature dependency.

## Gradle dependencies per module

Versions live in `gradle/libs.versions.toml` (single source of truth).

| Module | Plugins | Libraries |
|---|---|---|
| `:core:common` | library | coroutines-core (api), koin-core, junit, coroutines-test |
| `:core:ui` | library, compose | Compose BOM, ui, graphics, material3, tooling-preview, lifecycle-runtime-compose, lifecycle-viewmodel-compose (all `api`) |
| `:core:network` | library, serialization | retrofit (api), serialization-json (api), retrofit kotlinx converter, okhttp (BOM) + logging, koin-core |
| `:core:database` | library, ksp | room-runtime/ktx (api), room-compiler (ksp), koin-core/android |
| `:feature:*` | library, compose (+serialization where Retrofit DTOs) | koin-core/android/compose, junit, coroutines-test + modules from the table above |
| `:app` | application, compose | all modules, koin, navigation-compose, activity-compose |

Notes: AGP 9 has built-in Kotlin, so no `kotlin.android` plugin; KSP must be ≥ 2.3 for that reason.
Room schemas export to `core/database/schemas` (commit them).

## Package layout & naming

```
core/<name>/src/main/java/com/innaval/ingresosnamao/core/<name>/...
feature/<name>/src/main/java/com/innaval/ingresosnamao/feature/<name>/
 ├─ domain/        pure Kotlin: models, Repository interfaces, UseCases   (no Android, Retrofit, Room)
 ├─ data/          RepositoryImpl, Api (Retrofit), Dto, mappers; `internal`
 ├─ presentation/  XxxViewModel, XxxUiState, XxxRoute (stateful), XxxScreen (stateless)
 └─ di/            `xxxModule` (Koin)
```

- Domain: `Event`, `EventRepository`, `GetEventsUseCase` (verb + noun + `UseCase`, single `operator fun invoke`).
- Data: `EventDto`, `EventsApi`, `EventRepositoryImpl`, `EventEntity` (in `:core:database`), mapper `toDomain()`.
- Presentation: `EventsViewModel`, `EventsUiState` (sealed/data class), `EventsRoute`/`EventsScreen`.
- Only `di/` module val and `presentation/*Route` are public; everything in `data/` is `internal`.
- Money in cents (`Long`); shared infrastructure uses `AppResult`/`AppError` and injected dispatchers.
  Auth, events and cart use their own typed results and errors.

## Architecture decisions

1. **Feature modules + layers as packages.** Features are isolated (parallel builds, ownership, no cross-feature
   coupling). Splitting each into domain/data/presentation Gradle modules would triple module count for little gain
   now; `internal` visibility and package rules enforce layering. Extract later if needed.
2. **Unidirectional data flow MVVM.** ViewModel exposes one `StateFlow<UiState>`; UI sends events via functions.
   `Route` (koinViewModel + collectAsStateWithLifecycle) vs `Screen` (stateless) keeps previews/tests trivial.
3. **Domain independent of frameworks.** Repository interfaces in domain, implementations in data → Dependency Inversion;
   UseCases hold business rules and are unit-testable with fakes.
4. **Koin, one module per Gradle module.** Each exposes `xxxModule`; `:app` aggregates them (`AppModules.kt`) and supplies
   environment config (`NetworkConfig` from `BuildConfig`), so libraries stay environment-agnostic.
5. **Navigation lives in `:app`.** Features expose routes with callbacks; this is what keeps features decoupled.
6. **Network/DB as infrastructure.** `:core:network` provides Json/OkHttp/Retrofit; features declare their own `Api`
   interfaces. `:core:database` owns `AppDatabase` (Room requires one place to list entities) and DAOs; features map
   entities to domain models and never leak them to UI.
7. **Coroutines/Flow.** Suspend for one-shot calls, `Flow` for observable data, main-safe repositories
   (`withContext(dispatchers.io)`), `CancellationException` rethrown in `runCatchingApp`.
8. **Single version catalog** and a convention-plugin migration path (`build-logic/`) once per-module build files repeat
   too much.

## Design system (`:core:ui`)

- `theme/Color.kt`: private palette → Material3 `ColorScheme` (light/dark). Features never reference raw colors.
- `theme/Tokens.kt`: `Spacing`, `ExtraColors` (tokens Material lacks) via `CompositionLocal`s.
- `theme/Theme.kt`: `IngressosTheme { }` wraps `MaterialTheme` and provides tokens; access with
  `MaterialTheme.colorScheme/typography/shapes` and `IngressosTheme.spacing/extras`.
- `components/`: opinionated, stateless building blocks (`IngressosButton`, `LoadingState`, `ErrorState`) built only
  from tokens. Rule: if two features need a composable, it moves here; feature-specific UI stays in the feature.
- `format/PriceFormatter.kt`: formats integer cents as BRL without floating-point arithmetic.
- `:core:ui` exposes Compose/Material3 as `api`, so features get one consistent UI dependency.
- Add next: typography/shapes definitions, icon set, string resources, previews, screenshot tests.

## Next steps

Replace placeholder endpoints; add auth token interceptor in `:core:network`; add unit tests per
UseCase/ViewModel; introduce Detekt/ktlint and a module-graph check (e.g. Gradle `dependencyGuard` or custom task).

Authentication flow, Firebase setup, Firestore rules and test strategy are documented in
[`feature/auth/AUTHENTICATION.md`](feature/auth/AUTHENTICATION.md).
The events Remote Config contract, cache behavior and test strategy are documented in
[`feature/events/EVENTS.md`](feature/events/EVENTS.md).
The cart's persistence, account isolation and checkout behavior are documented in
[`feature/cart/CART.md`](feature/cart/CART.md).
