# Opal Test Task

Android test task for Opal. Single `:app` module, Jetpack Compose, MVI, Haze for blur.

## Architecture

- Feature-first packages: `feature/<name>/` holds `<Name>Screen.kt`, `<Name>ViewModel.kt`, `<Name>Contract.kt`; shared code in `ui/common/`, `ui/theme/`, `data/`.
- MVI per screen:
  - `Contract` defines an immutable `State` data class, a sealed `Intent` for user actions, and a sealed `Effect` for one-off events (navigation, toasts, haptics).
  - `ViewModel` exposes `state: StateFlow<State>` and `effects: Flow<Effect>` (`Channel`-backed), and takes input only through `onIntent(intent)`.
  - State changes go through a pure `reduce(state, intent)` where possible; side effects run in `viewModelScope` and feed results back as intents.
- Screens are split into a stateful `<Name>Screen(viewModel)` that collects with `collectAsStateWithLifecycle()` and a stateless `<Name>Content(state, onIntent)` that previews and tests use.
- Manual DI: app-scoped singletons live in the `Application` companion; ViewModels are created via `viewModel { }` factories. No Hilt/Koin unless the task needs it.

## Compose

- Blur with Haze: one `HazeState` per blurred backdrop, `hazeSource` on content, `hazeEffect` on the overlay; keep the source and effect in the same window.

## UI interaction quality

- Hit area ≥ 44dp even when the visual is smaller — expand touch, not visuals.
- Haptics on meaningful boundaries (selection change, threshold cross).
- Prefer spring animations; linear only for cross-fades. Animations must be interruptible.
- Support predictive back.

## Code style

- No comments or docs in code. Exception: critical info with a link to an issue or source.
- No XML layouts, no AppCompat, no Material Components (views).

## Commands

```
./gradlew assembleDebug testDebugUnitTest lint 2>&1 | tee /tmp/gradle-build.log | tail -40
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
- Commit finished, verified work, scoped so history reads like a changelog. Messages are one line, lowercase, high-level outcome, e.g. `added the timer screen`, `fixed blur flicker on scroll`.
- Stage explicit paths; never `git add .` or `git add -A`; don't commit unrelated changes.
- Merging into `develop` happens only after the user's review. Push or open PRs only on explicit request, or with an explicit ok when it's the only step left.
- PR description: one-sentence outcome, plus risks or critical info.

## Meta rules

- Suggest reasonable updates to AGENTS.md as the project evolves.
