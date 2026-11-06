## Before committing

Run `./gradlew :core:ktlintFormat :plugin:ktlintFormat` and then `./gradlew :core:ktlintCheck :plugin:ktlintCheck` before committing or opening a PR. CI fails on ktlint violations. `.githooks/pre-commit` does this automatically once `git config core.hooksPath .githooks` is set; do not rely on it being set.

Open a PR for changes instead of committing directly to `main`.

## Agent skills

### Issue tracker

Issues and specs are local markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-label vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
