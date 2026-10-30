# Opal Test Task

Android test task for Opal: a referral rewards screen. Single `:app` module, Jetpack Compose, MVI.

- Brief: [docs/BRIEF.md](docs/BRIEF.md). Design: [docs/design/referral-screen.png](docs/design/referral-screen.png) — match it closely.
- Time budget ~4h: finish and polish the core flow over adding features.
- Open product decisions go to the user; record the agreed ones with reasoning in README.md.

## Architecture

- Feature-first packages: `feature/<name>/` holds `<Name>Screen.kt`, `<Name>ViewModel.kt`, `<Name>Contract.kt`; shared code in `ui/common/`, `ui/theme/`, `data/`.
- MVI per screen:
  - `Contract` defines an immutable `State` data class, a sealed `Intent` for user actions, and a sealed `Effect` for one-off events (navigation, toasts, haptics).
  - `ViewModel` exposes `state: StateFlow<State>` and `effects: Flow<Effect>` (`Channel`-backed), and takes input only through `onIntent(intent)`.
  - State changes go through a pure `reduce(state, intent)` where possible; side effects run in `viewModelScope` and feed results back as intents.
- Screens are split into a stateful `<Name>Screen(viewModel)` that collects with `collectAsStateWithLifecycle()` and a stateless `<Name>Content(state, onIntent)` that previews use.
- Manual DI: `OpalApp.container: AppContainer` (interface) owns app-scoped dependencies; ViewModels are built in `viewModel { }` from `CreationExtras.container` and receive dependencies through the constructor. No Hilt/Koin.

## Data & errors

- Mock data behind repository interfaces in `data/`; the fake implementation simulates latency and failures so a real backend is a drop-in `AppContainer` change.
- Backend-shaped models (`data/`) map to domain models at the repository boundary; UI sees domain models only.
- Repositories return results with a typed error instead of throwing; rethrow `CancellationException`.
- Every async state covers loading, content, and error with retry; actions like claim show in-progress and failure states.
- Local persistence via DataStore only where it adds value (e.g. caching); the backend is the source of truth for referrals and claims.

## Compose

- Blur with Haze: one `HazeState` per blurred backdrop, `hazeSource` on content, `hazeEffect` on the overlay; keep the source and effect in the same window.
- Organize reusable compose code into a design system with tokens, components and helpers (for ripple/gradients/etc); no hardcoded colors or text styles in feature code.
- Adaptive layouts: screens work in portrait, landscape, tablets, foldables and freeform windows of any size. Pick the layout from the available window width (`currentWindowAdaptiveInfo()` size classes), not from device type or orientation. Wide windows get a two-pane layout or a centered column with a max content width, never stretched full-width rows. Nothing may be clipped in short windows, so content must scroll. Keep state across resizes, and preview at phone, landscape and tablet sizes.
- Every `Content` has `@Preview`s for its key states.
- User-visible text in `strings.xml` (plurals for counts); icons get `contentDescription`, `null` when decorative.

## UI interaction quality

- Hit area ≥ 48dp even when the visual is smaller — expand touch, not visuals.
- Haptics on meaningful boundaries (selection change, threshold cross).
- Prefer spring animations; linear only for cross-fades. Animations must be interruptible.
- Support predictive back.

## Code style

- No comments or docs in code. Exception: critical info with a link to an issue or source.
- No XML layouts, no AppCompat, no Material Components (views).

## Commands

```
./gradlew spotlessApply assembleDebug lintDebug 2>&1 | tee /tmp/gradle-build.log | tail -40
./gradlew installDebug && adb shell am start -n vadiole.opaltest.debug/vadiole.opaltest.MainActivity
```

- After completing a task or fix, build, install, and launch the app; check `adb logcat` for crashes.

## Workflow

Roles (user, main agent, gardener, sub-agents), the delivery process, and coordination rules are in [WORKFLOW.md](WORKFLOW.md). Read it before starting work.

## Git

- `develop` is the integration branch; never commit to it directly. Branch from an up-to-date `develop`.
- Main agent: one feature branch per scope, named simply after the user's request, e.g. `timer-screen`; agree on the name with the user first.
- Gardener and sub-agents: separate branches in sibling worktrees (`../OpalTest-<topic>`), prefixed `gardening/`, e.g. `gardening/blur-perf`.
- Merge `develop` into an active branch at suitable checkpoints, not mid-task.
- Commit finished, verified work, scoped so history reads like a changelog. Messages are one line, lowercase, high-level outcome, e.g. `add the timer screen`, `fix blur flicker on scroll`.
- Stage explicit paths; never `git add .` or `git add -A`; don't commit unrelated changes.
- Merging into `develop` happens only after the user's review. Push or open PRs only on explicit request, or with an explicit ok when it's the only step left.
- PR description: one-sentence outcome, plus risks or critical info.

## Meta rules

- Suggest reasonable updates to AGENTS.md as the project evolves.
