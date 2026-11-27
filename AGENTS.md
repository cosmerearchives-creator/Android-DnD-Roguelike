# Forests of Hell — permanent development contract

Repository: https://github.com/cosmerearchives-creator/Android-DnD-Roguelike; branch `main`; existing Godot project in `godot/`; Android and Windows PC. Rules: 2014 D&D 5e except explicitly approved custom rules. Do not recreate the game, change its application ID `com.cosmerearchives.dndroguelike`, or change signing identity. These instructions supersede older recommendations in historical docs where they conflict, without erasing history.

## Modes and authorization
Only **Planning** (discuss, no implementation) and **Publishing** (implement, test, commit, version, build, release and verify). An instruction to add, fix, modify or implement authorizes execution. Do not repeatedly request permission. Never claim work was done if it was only discussed. Respect tooling and permission restrictions.

## Mandatory asset gates — stop work on failures
- **User supplies every necessary skeleton/rig.** Do not create, generate, infer, approximate, substitute, repair or modify one independently. Validate identity, hierarchy, rest pose, compatibility, mapping and deformation before integration. An absent, incomplete or incompatible appropriate skeleton stops **all development** until resolved by the user; no temporary rigs or silent workarounds.
- **Always import every provided bone map in full.** Preserve names, all bones (even presently unused), mappings, hierarchy and source references. Validate the complete import. Missing mappings are blockers, not grounds to omit bones.
- Preserve every supplied animation. Reuse an existing animation only after verifying compatibility with the correct user-supplied skeleton. Do not invent replacement clips to disguise missing assets.
- **All textures arrive at development quality.** Import and retain the original source files, resolution, material channels and fidelity. Do not compress, transcode with quality loss, downscale, resize or downgrade textures unless the user specifically authorizes that optimization. Audit Godot's actual import configuration and generated results, not only source files. Do not silently omit normal, metallic/roughness, alpha, emission or other material maps. Missing material dependencies block the asset's completion.
- Check actual geometry, scale, feet, rig, all bone maps, animations, textures/materials and in-game appearance. Never publish incomplete asset integrations. Historical generated rigs and compression tooling are **not** permission to run them again.

## Continuity and implementation
Inspect `HANDOFF.md`, `IMPLEMENTATION_STATUS.md`, `ROADMAP.md`, `DESIGN_DECISIONS.md`, `ASSET_MANIFEST.md`, `KNOWN_ISSUES.md`, recent commits, and relevant `docs/` BEFORE making changes. The repository and measured tests govern **implementation**, confirmed GitHub artifacts govern **publication**; neither old conversation claims nor historical docs automatically prove the current state. Reconcile discrepancies without discarding history.
Preserve established mechanics, approved visuals, save compatibility, existing assets, PC/Android controls, camera, combat, enemies, inventory, races, clock/lighting, updater and release pipeline. Reuse code and verified-compatible assets instead of recreating functioning features. Implement all connections (UI, gameplay, state, tests), not just database entries or demonstrations.
Use 2014 5e as default and explicitly record any real-time/test-only deviations. Never silently simplify an ability, rule or player-facing feature.

## Testing and publication
For each change: define acceptance criteria; validate affected Godot scene/script imports and applicable smoke/regression tests; review affected device-specific behavior. Failures and incomplete tests must be disclosed. Optimize architecture, caching and loading before quality reduction. No asset quality reduction without explicit authorization.
In Publishing mode commit to `main` only; increment `version.properties` on gameplay releases, use existing CI/signing secrets, verify APK and PC ZIP and update manifests on the published release, and verify auto-update/downgrade paths where applicable. Never commit credentials. A commit, green local command or workflow trigger alone is **not** a verified published release. A documentation-only task explicitly limited to docs does not require a new binary release.
Update all relevant persistent records after each session. Mark proposed, approved, in development, partially implemented, implemented/unverified, tested, published or blocked **separately**. Distinguish source presence, local smoke tests, device validation and release confirmation.

## Blockers and completion reports
Investigate failures and perform permitted repairs without repeated approval requests; stop all development for the mandatory skeleton blocker. Otherwise continue unaffected authorized work, subject to restrictions. Never fake tests, access or builds. Summarize exactly what changed, evidence/tests run or skipped, commit/release/versions, unresolved blockers, regressions, and links to builds or local launchers when available. Preserve pre-existing local untracked files; stage only intentional files.

## Documentation contract
`AGENTS.md` rules; `ROADMAP.md` priorities and acceptance; `IMPLEMENTATION_STATUS.md` evidence-qualified status; `DESIGN_DECISIONS.md` approved decisions; `ASSET_MANIFEST.md` per-asset source/rig/maps/material and gates; `CHANGELOG.md` chronological releases; `KNOWN_ISSUES.md` actionable defects; `TEST_CHECKLIST.md` gates; `HANDOFF.md` verified resume point. Existing `docs/` are historical and topic-specific references, not to be deleted or rewritten indiscriminately.
