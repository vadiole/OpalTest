# Claim reveal

Full-screen reveal that lifts a claimed gem out of its card and returns it. All code lives in `feature/claimreveal/`; the referral screen opens it when a MileStone claim succeeds. The tuning demo is a hidden debug activity:

```
adb shell am start -n vadiole.opaltest.debug/vadiole.opaltest.feature.claimreveal.demo.ClaimRevealDemoActivity
```

## Plugging it into a screen

1. Wrap the screen content in `ClaimRevealHost(state = rememberClaimRevealState())`.
2. Add `Modifier.claimRevealSource(state, key)` to the gem thumbnail, and `ClaimedGemGlow` behind it for the claimed glow.
3. Keys must be saveable in a `Bundle` (an enum or a string), so a reveal survives rotation and process death.
4. When a claim succeeds, call `state.open(key, RevealContent(gem = stone.revealImage, title = name))`. Show the card as claimed while `!state.isHolding(key)`.

## Before shipping

- Each reveal plays a random `RevealSound`; pick one and delete the other `claim_reveal_shine_*` files, or keep the set and load them lazily.
- Move the glow and blur constants into the design system if other screens need them.
- Drop sources when their thumbnail leaves composition (needed for lazy lists).
