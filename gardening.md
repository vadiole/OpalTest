# Gardening ledger

Maintained by the gardener agent. See [WORKFLOW.md](WORKFLOW.md).

## Agreements

- 2026-10-30: Ledger is tracked in the repo on `gardening/ledger` and lands through PRs.
- 2026-10-30: Sub-agents take one bounded task each in a `../OpalTest-<topic>` worktree, at most 2 in parallel, and each opens a PR.
- 2026-10-30: The main agent session is `opaltest-f3`.

## Reviewed

| Date | Range | Notes |
|---|---|---|
| 2026-10-30 | `d36907c..eeb7ab9` | Project skeleton only. No feature code yet, so nothing to garden. |
| 2026-10-30 | `eeb7ab9..4501dbc` | Referral data layer, fake backend, Nav3 with Console and Referral routes. Clean: typed errors, rethrows CancellationException, DTO→domain mapping drops invalid tiers, tested. The screens are placeholders and the main agent is still working on them. No action needed. |

## Active

- 2026-10-30: Read-only review of #4 (design system, console) and #6 (console save errors) is running.

- 2026-10-30 PR #3 update: fixed the push replaying on back and the outgoing screen vanishing on open. Root cause of the vanishing: a no-op NavDisplay ContentTransform has zero duration, so the exiting entry was disposed after one frame. The user decided to block navigation mid-transition through the `navigate`/`navigateBack` helpers, and to make the window and screens Opal black (the app is always dark). Merged develop (#4, #6) and resolved the AppNavigation conflict.
- 2026-10-30 `gardening/nav-transitions` → PR #3, merged as 78ae1a9: Port the Unicode app's `Nav.kt` push, pop and predictive-back transitions to Nav3 (requested by the user). Tokens are in `ui/theme/OpalMotion.kt`. A `NavEntryDecorator` in `ui/common/StackTransitions.kt` draws predictive back, while Nav3's predictive spec is a timed no-op. Known gaps: Nav3 shortens the commit fade and the cancel duration in proportion to back progress, push during a back commit snaps, and there's no RTL handling. Risk: it relies on Nav3/navigationevent predictive-back internals. The user checked the animations on their phone.

## Findings

## Done

- PR #3, Nav3 transitions: see the entries under Active.

## Deferred

- `AppContainer.referralDebugControls` exposes the fake backend through the app-wide container. A real backend would need a no-op or debug-only binding. Minor; revisit if a real API ever lands.

## Patterns

- Sub-agents must not wait on or launch emulators. The `Shared_API_37` qemu can run without being attached to adb, so a wait loop hangs forever. Use the user's phone or skip device verification and say so.
