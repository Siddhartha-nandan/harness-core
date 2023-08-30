## Describe your changes

### 1. Why we made the change ? 

### 2. What is the change ?

### 3. What will be the impact ?

### 4. Is there a Design Doc / Tech Spec for feature attached in the PR Description?

## Testing

- [ ]  Have you added Unit Tests for the Changes

- [ ]  Have you added Automation for the Changes

- [ ]  Have you run Sanity for existing features

- [ ]  Single PR may contain changes in multiple msvc, have you tested for inter-operability and backward compatibility?


###  Share critical test scenarios.

### Optional: Screenshots if applicable.


## Feature Flags

- [ ]  Have you added / removed a feature flag in the code base

- [ ]  Have you ensured functionality is not breaking with or without Enabling FF

- [ ]  Have you created ticket for removing FF once GA’d.

## Checklist
- [ ] I've documented the changes in the PR description.
- [ ] I've tested this change either in PR or local environment.
- [ ] If hashcheck failed I followed [the checklist](https://harness.atlassian.net/wiki/spaces/DEL/pages/21016838831/PR+Codebasehash+Check+merge+checklist) and updated this PR description with my answers to make sure I'm not introducing any breaking changes.

## Comment Triggers
<details>
  <summary>Build triggers</summary>

- Feature build: `trigger feature-build`
-
Specific builds
    - `trigger manager`
    - `trigger dms`
    - `trigger ng_manager`
    - `trigger cvng `
    - `trigger cmdlib`
    - `trigger template_svc`
    - `trigger events_fmwrk_monitor`
    - `trigger event_server`
    - `trigger change_data_capture`
    -  Trigger multiple builds together: For eg: `trigger dms, manager`
- Immutable delegate `trigger publish-delegate`
</details>

## [Latest PR Check Triggers](https://github.com/harness/harness-core/blob/develop/.github/pull_request_template.md)

<details>
  <summary>PR Check triggers</summary>
You can run multiple PR check triggers by comma separating them in a single comment. e.g. `trigger ut0, ut1`

- Compile: `trigger compile`
- CodeformatCheckstyle: `trigger checkstylecodeformat`
    - CodeFormat: `trigger codeformat`
    - Checkstyle: `trigger checkstyle`
- MessageMetadata: `trigger messagecheck`
- File-Permission-Check: `trigger checkpermission`
- Recency: `trigger recency`
- BuildNumberMetadata: `trigger buildnum`
- Trigger CommonChecks: `trigger commonchecks`
- PMD: `trigger pmd`
- Copyright Check: `trigger copyrightcheck`
- Feature Name Check: `trigger featurenamecheck`
- UnitTests-ALL: `trigger uts`
- UnitTests-0: `trigger ut0`
- UnitTests-1: `trigger ut1`
- UnitTests-2: `trigger ut2`
- UnitTests-3: `trigger ut3`
- UnitTests-4: `trigger ut4`
- UnitTests-5: `trigger ut5`
- UnitTests-6: `trigger ut6`
- UnitTests-7: `trigger ut7`
- UnitTests-8: `trigger ut8`
- UnitTests-9: `trigger ut9`
- CodeBaseHash: `trigger codebasehash`
- CodeFormatCheckstyle: `trigger checkstylecodeformat`
- SonarScan: `trigger ss`
- GitLeaks: `trigger gitleaks`
- Trigger all Checks: `trigger smartchecks`
- Go Build: `trigger gobuild`
- Validate_Reviews: `trigger review`
- Module Dependency Check: `trigger mdc`
</details>

## PR check failures and solutions
https://harness.atlassian.net/wiki/spaces/BT/pages/21106884744/PR+Checks+-+Failures+and+Solutions


## [Contributor license agreement](https://github.com/harness/harness-core/blob/develop/CONTRIBUTOR_LICENSE_AGREEMENT.md)
