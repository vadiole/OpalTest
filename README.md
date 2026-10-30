# Opal Referrals

Referral rewards screen for Opal: share an invite link, track successful referrals, and claim tiered rewards. See the [brief](docs/BRIEF.md) and [design](docs/design/referral-screen.png).

<img width="250" alt="Screenshot_20261030-162228" src="https://github.com/user-attachments/assets/0a087018-b3ba-4d2c-b5dc-a2ee9835456a" />
<img width="250" alt="Screenshot_20261030-162240" src="https://github.com/user-attachments/assets/785bb408-8afd-474e-a419-6ea284af2b77" />
<img width="250" alt="Screenshot_20261030-162247" src="https://github.com/user-attachments/assets/8c965d2f-0f8a-473c-8a72-7866d4bb181d" />
<img width="759" alt="Screenshot_20261030-162314" src="https://github.com/user-attachments/assets/0a12769d-c838-4eac-9c45-344a74e868e3" />


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
- If the refresh after a successful claim fails, the app shows no error. The claim already went through and the screen is correct, and the next load fetches fresh data.
- The first milestone unlocks at 1 friend. The design doesn't show its threshold.
- The app opens on a demo console. From there you can switch between Free and Pro, change the number of friends, make the network slow or fail, and reset everything. Progress is kept between launches.
- On tablets and other wide screens, the code and Share button sit on the left and the rewards list on the right.
- On landscape phones and other short windows (at least 600dp wide, under 480dp tall), the code and Share button sit on the left and the invite card and rewards on the right, so Share and the first reward show without scrolling. Switching layouts keeps the item you were looking at on screen.

## With more time

- Keep tuning the haptics. They work, but getting them perfect takes many more iterations.
- Spend much more time polishing all the transitions and animations, the gem reveal most of all. Each one takes many iterations to get right.
- Make claiming even more satisfying: shaders, animations, better sound. Make it so people screenshot and post it on socials:)
- Improve the horizontal layout. Right now it's a simple adaptation, and I think it can be better.
- Add claim logic for the other rewards. The 2-year, lifetime and mystery rewards ask the user to contact us, and how they get claimed still needs to be specified.
- Clean up the codebase. I put most of the time into making the visuals look right, so some of the code is rougher than I'd like.
- Invest more time into things that matter long term, screenshot tests, architecture, etc.

## How AI agents were used

Agent instructions live in [AGENTS.md](AGENTS.md) and [WORKFLOW.md](WORKFLOW.md).
