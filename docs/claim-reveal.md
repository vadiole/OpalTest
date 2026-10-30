# Claim reveal

Full-screen reveal that lifts a claimed gem out of its card and returns it. All code lives in `feature/claimreveal/`; the referral screen opens it when a MileStone claim succeeds. The tuning demo is a hidden debug activity:

```
adb shell am start -n vadiole.opaltest.debug/vadiole.opaltest.feature.claimreveal.demo.ClaimRevealDemoActivity
```

## Plugging it into a screen

1. Wrap the screen content in `ClaimRevealHost(state = rememberClaimRevealState())`. While a reveal is open, the host hides the content from accessibility and keyboard focus, and moves focus to Continue.
2. Add `Modifier.claimRevealSource(state, key)` to the gem thumbnail, and `ClaimedGemGlow` behind it for the claimed glow. Sources unregister when their thumbnail leaves composition; if the source is gone when the reveal closes, the gem fades out toward its last known position.
3. Keys must be saveable in a `Bundle` (an enum or a string), so a reveal survives rotation and process death.
4. When a claim starts, call `state.prefetch(stone.revealImage)` so the large image is decoded before the reveal opens. Without it the reveal still works: the scrim and back handling start at once, and the flight waits up to a second for the image.
5. When a claim succeeds, call `state.open(key, RevealContent(gem = stone.revealImage, title = R.string.…))`. The title is a string resource, resolved at render time. Show the card as claimed while `!state.isHolding(key)`.

## Decided

- Each reveal plays a random `RevealSound` from the five `claim_reveal_shine_*` files, on purpose. All five load into a `SoundPool` when the host enters composition (about 216 KB).
- Once revealed, the gem reacts to a press 16dp beyond the stone's edges. It ticks (`SegmentTick`) on press and on lift, but not when a drag or scroll cancels the press. Reduced motion turns off the squish but keeps the ticks.
- Once the gem settles (`RevealPhase.Revealed`), faint stars drift up and out from behind it, never down toward the title (`GemSparkles`, an AGSL `RuntimeShader`). They fade in over 1.2 s and fade out in 220 ms when the reveal closes or a back gesture commits. The frame loop runs only while `state.sparkle` is above zero. The look is tuned with the `SPARKLE_*` constants in `GemSparkles.kt`. Reduced motion hides them entirely: they are decoration, and a static sparkle adds nothing.
- `claim_reveal_title` has a `\n` after the gem name, so translators decide where the line breaks and screen readers still read the title as one phrase. At large font sizes the name can wrap onto more lines.

## Before shipping

- Move the glow and blur constants into the design system if other screens need them.
