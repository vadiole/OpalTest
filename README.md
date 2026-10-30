# Opal Referrals

Referral rewards screen for Opal: share an invite link, track successful referrals, and claim tiered rewards. See the [brief](docs/BRIEF.md) and [design](docs/design/referral-screen.png).

## Run

```
./gradlew installDebug
./gradlew spotlessCheck testDebugUnitTest lintDebug
```

## Architecture

- Jetpack Compose, MVI per screen (`Contract` → `ViewModel` → stateless `Content`).
- Manual DI through `AppContainer`; mock repositories sit behind interfaces, so a real backend replaces one binding.

## Decisions

## With more time

## How AI agents were used

Agent instructions live in [AGENTS.md](AGENTS.md) and [WORKFLOW.md](WORKFLOW.md).
