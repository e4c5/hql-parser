# AGENTS.md

This file provides repository-specific guidance for coding agents working in this project.

## Project Overview

- Project type: single-module Java library built with Maven
- Java version: 21
- Packaging: `jar`
- Publishing target: Maven Central through the Sonatype Central Portal

## Repository Layout

- `pom.xml` - Maven build and publishing configuration
- `src/main/java/` - main library sources
- `src/main/antlr4/` - ANTLR grammar definitions
- `src/test/java/` - test sources
- `.github/workflows/publish-maven-central.yml` - CI workflow for release validation and publishing
- `MAVEN_CENTRAL_PUBLISHING.md` - detailed release and publishing guide

## Build and Test Commands

Use Maven for all build and test operations.

- Standard verification:

```bash
mvn clean verify
```

- Run tests only:

```bash
mvn test
```

- Verify release configuration without signing:

```bash
mvn -P release -Dgpg.skip=true verify
```

## Release Behavior

The GitHub Actions workflow has three modes:

1. **Tag push publishes to Maven Central**
   - Pushing a tag matching `v*` (e.g. `v1.0.2`) or a bare version tag (e.g. `1.0.2`) triggers a real release
   - The tag version must match `project.version` in `pom.xml` (the leading `v` is stripped before comparing)

2. **Manual workflow run validates only (default)**
   - Triggering `workflow_dispatch` without enabling the `publish` input runs build and release-profile verification only

3. **Manual workflow run publishes**
   - Triggering `workflow_dispatch` with the `publish` input set to `true` runs the full publish job, bypassing the tag-version check

Do not change this behavior unless explicitly requested.

## Maven Central Notes

- Current coordinates are `com.raditha:hql-parser`
- The `com.raditha` namespace is intentionally retained because the repository owner controls the `raditha.com` domain
- Release publishing uses:
  - sources JAR
  - javadoc JAR
  - GPG signing via the `release` Maven profile
  - Sonatype Central Portal publishing plugin

## Editing Guidance

- Keep Java compatibility aligned with Java 21
- Prefer updating Maven configuration in `pom.xml` rather than introducing alternate build tools
- Do not remove Maven Central metadata from the POM (`url`, `licenses`, `developers`, `scm`)
- Do not switch publishing back to JitPack unless explicitly requested

## ANTLR Notes

- Grammar files live under `src/main/antlr4/`
- Generated ANTLR sources are produced during the Maven build into `target/generated-sources/antlr4`
- Do not commit generated sources unless the repository is intentionally changed to track them

## Documentation Expectations

When release or publishing behavior changes, keep these files in sync:

- `README.md`
- `MAVEN_CENTRAL_PUBLISHING.md`
- `.github/workflows/publish-maven-central.yml`
- `pom.xml`

## Environment Notes

- Maven is required for routine work in this repository
- If a cloud agent environment is missing `mvn`, use the environment setup flow rather than encoding machine-specific installation steps in repository files
