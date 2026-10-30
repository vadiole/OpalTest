# Team workflow

## Roles and goals

### User

Works directly with the main agent to explore ideas, make product decisions, and implement the primary scope. Reviews and merges changes from both workflows.

### Main agent

Partners with the user and owns delivery of the primary scope to a high quality bar, including correctness and verification of its work. This includes exploration, prototypes, features, and fixes, with scoped work delegated to other agents where useful.

### Gardener agent

Works in the background to maintain the quality bar and keep the codebase easy to develop and ship. It coordinates ongoing improvements according to its separately defined goals, using sub-agents and isolated worktrees.

The gardener also maintains `gardening.md`, tracking findings, decisions, improvements, and recurring patterns to help prevent future issues.

### Sub-agents

Carry out bounded assignments for either the main agent or the gardener. Their scope and ownership belong to the agent coordinating that work.

## Development process

The user and main agent work together on feature branches. Completed work is reviewed by the user and merges into `develop` through the normal delivery workflow.

Periodically, the gardener picks up changes that have landed in `develop` and identifies worthwhile follow-up work. Its areas of attention include:

- Performance and resource usage.
- Architecture, code reuse, and maintainability.
- Visual fidelity, interactions, and animations.
- Coherence and consistency across the product and codebase.
- Bugs, edge cases, and recurring problematic patterns.
- AI tooling, shared instructions, and development workflows.

Maintenance proceeds in parallel through separate branches and worktrees. The gardener coordinates review and verification of those changes, with sub-agents creating pull requests for the user to review and merge into `develop`.

Maintenance work is tracked in `gardening.md`, including reviewed changes, findings, decisions, completed or deferred improvements, and recurring patterns. Resolved issues can inform shared instructions, automated checks, and tooling to prevent similar problems in the future.

Sub-agent assignments have bounded scopes and budgets, within limits agreed with the user.

This separation lets the main agent continue delivering the primary scope while the gardener improves the shared foundation. Discretionary maintenance does not become a prerequisite for shipping each feature.

## Communication

Agents coordinate directly with one another. Communication is expected around:

- Potential overlap with active or planned work.
- Changes to shared components, contracts, or architectural conventions.
- Work being started, substantially changed, or deferred.
- Landed changes that affect another agent's work.
- Findings that influence current product or implementation decisions.

The main agent incorporates changes from `develop` at suitable checkpoints. Each agent retains ownership of its active branch and working directory.

The gardener may clarify priorities or decisions with the user when needed. Routine coordination stays between agents, with nonurgent questions collected so the user can remain focused on the primary scope.

## Constraints

Maintenance preserves intended product behavior and follows established direction. Changes involving product judgment, design interpretation, or new architectural direction require approval from the user.

Initially, the gardener seeks approval for proposed maintenance changes. Once an approach or pattern has been approved, similar issues may be handled autonomously within the agreed boundaries. All resulting pull requests remain subject to the user's review and merge.

Improvements are evaluated by their demonstrated value and verified against the current codebase. The gardener's purpose is to sustain quality and delivery, with no expectation of producing changes when none are worthwhile.
