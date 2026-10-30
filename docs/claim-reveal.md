# Claim reveal

Full-screen reveal that lifts a claimed gem out of its card and returns it. All code lives in `feature/claimreveal/`; the demo is a debug-only launcher activity (`ClaimRevealDemoActivity`).

## Plugging it into a screen

1. Wrap the screen content in `ClaimRevealHost(state = rememberClaimRevealState())`.
2. Add `Modifier.claimRevealSource(state, key)` to the gem thumbnail, and `ClaimedGemGlow` behind it for the claimed glow.
3. Keys must be saveable in a `Bundle` (an enum or a string), so a reveal survives rotation and process death.
4. When a claim succeeds, call `state.open(key, RevealContent(gem = stone.revealImage, title = name))`. Show the card as claimed while `!state.isHolding(key)`.

## Before shipping

- Pick one `RevealSound` and delete the other `claim_reveal_shine_*` files.
- Move the glow and blur constants into the design system if other screens need them.
- Drop sources when their thumbnail leaves composition (needed for lazy lists).
