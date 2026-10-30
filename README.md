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

- Pro users don't see the free year of Pro, since only Free users can get it. A user who claimed it while on Free still sees it as claimed.
- Claiming the free year upgrades the user to Pro right away.
- A claimed reward stays claimed, even if the referral count later drops.
- The first milestone unlocks at 1 friend. The design doesn't show its threshold.
- The app opens on a demo console. From there you can switch between Free and Pro, change the number of friends, make the network slow or fail, and reset everything. Progress is kept between launches.
- On tablets and other wide screens, the code and Share button sit on the left and the rewards list on the right.

## With more time

## How AI agents were used

Agent instructions live in [AGENTS.md](AGENTS.md) and [WORKFLOW.md](WORKFLOW.md).
