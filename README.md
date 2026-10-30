# Opal Referrals

Referral rewards screen for Opal: share an invite link, track successful referrals, and claim tiered rewards. See the [brief](docs/BRIEF.md) and [design](docs/design/referral-screen.png).

## Run

Install the prebuilt release: [opal-referrals.apk](opal-referrals.apk). Or build from source:

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

- Keep tuning the haptics. They work, but getting them perfect takes many more iterations.
- Spend much more time polishing all the transitions and animations, the gem reveal most of all. Each one takes many iterations to get right.
- Improve the horizontal layout. Right now it's a simple adaptation, and I think it can be better.
- Add claim logic for the other rewards. The 2-year, lifetime and mystery rewards ask the user to contact us, and how they get claimed still needs to be specified.
- Clean up the codebase. I put most of the time into making the visuals look right, so some of the code is rougher than I'd like.

## How AI agents were used

Agent instructions live in [AGENTS.md](AGENTS.md) and [WORKFLOW.md](WORKFLOW.md).
