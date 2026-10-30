# Opal Referrals

Referral rewards screen for Opal: share an invite link, track successful referrals, and claim tiered rewards. See the [brief](docs/BRIEF.md) and [design](docs/design/referral-screen.png).

Install: [opal-referrals.apk](opal-referrals.apk)

> A more polished version, made after the 4-hour time slot: [overtime-polish branch](https://github.com/vadiole/OpalTest/tree/overtime-polish) · [opal-referrals-extra.apk](opal-referrals-extra.apk)

<img width="250" alt="Screenshot_20261030-222504" src="https://github.com/user-attachments/assets/08ca56fa-ac04-4ac5-846c-bd44ae23b4ee" />
<img width="250" alt="Screenshot_20261030-222544" src="https://github.com/user-attachments/assets/1e8fddef-cef6-4909-b777-9ddf35b2c604" />
<img width="250" alt="Screenshot_20261030-222610" src="https://github.com/user-attachments/assets/9b983a23-1982-4f24-815c-d32cf2cc0d71" />

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
- On landscape phones and other short windows (at least 600dp wide, under 480dp tall), the code and Share button sit on the left without scrolling, and a compact invite card and the rewards list sit on the right. Switching layouts keeps the item you were looking at on screen.

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
