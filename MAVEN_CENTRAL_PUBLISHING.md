# Maven Central Publishing Guide

This project is configured to publish releases through the Sonatype Central Portal instead of JitPack.

## Prerequisites

Before publishing the first release, make sure all of the following are in place:

1. **Claim the namespace in the Central Portal**
   - Current coordinates use `com.raditha:hql-parser`.
   - You must be able to verify ownership of the `com.raditha` namespace in the Sonatype Central Portal.
   - If that namespace cannot be verified, update the project's `groupId` to a namespace you control before attempting a release.

2. **Create a Sonatype Central Portal user token**
   - Sign in to <https://central.sonatype.com/>.
   - Generate a user token from the account page.
   - Keep both the token username and token password.

3. **Create an ASCII-armored GPG private key**
   - Maven Central requires signed release artifacts.
   - Export the private key in ASCII-armored form so it can be stored as a GitHub secret.

## Repository Configuration

The repository now includes:

- Maven Central metadata in `pom.xml` (`url`, license, developers, SCM)
- Source JAR generation via `maven-source-plugin`
- Javadoc JAR generation via `maven-javadoc-plugin`
- Release signing via `maven-gpg-plugin` in the `release` profile
- Sonatype Central Portal publishing via `org.sonatype.central:central-publishing-maven-plugin`
- GitHub Actions workflow at `.github/workflows/publish-maven-central.yml`

## Required GitHub Secrets

Add these repository secrets before using the release workflow:

- `CENTRAL_TOKEN_USERNAME`
- `CENTRAL_TOKEN_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

## Release Workflow

The GitHub Actions workflow supports two modes:

- **Automatic publishing on version tags**: pushing a tag like `v0.0.16` performs a real Maven Central release
- **Manual validation on demand**: running the workflow from the GitHub Actions UI only verifies the build and release profile; it does not publish anything

For publishing, the workflow runs when a version tag is pushed:

- Tag format: `v<project-version>`
- Example: `v0.0.16`

For tag-based releases, the workflow checks that the tag version matches `project.version` in `pom.xml` before deploying.

## Typical Release Steps

1. Update `pom.xml` to the release version.
2. Commit the release changes.
3. Create and push a matching Git tag.

```bash
git tag v0.0.16
git push origin v0.0.16
```

4. GitHub Actions builds, signs, and publishes the release to Maven Central.

## Manual Validation Run

If you trigger the workflow manually from GitHub Actions, it will run:

```bash
mvn --batch-mode clean verify
mvn --batch-mode -P release -Dgpg.skip=true verify
```

This path is intended for validating release readiness without uploading artifacts to Sonatype Central.

## Local Release Command

If you want to publish manually from a local machine instead of GitHub Actions:

1. Configure `~/.m2/settings.xml` with a server entry whose id is `central`.
2. Make sure your GPG key is available locally.
3. Run:

```bash
mvn -P release deploy -Dgpg.passphrase=YOUR_GPG_PASSPHRASE
```

Your Maven `settings.xml` should contain credentials similar to:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_CENTRAL_TOKEN_USERNAME</username>
      <password>YOUR_CENTRAL_TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

## Troubleshooting

### HTTP 401 when uploading to Maven Central

If the publish job fails with an error like:

```
[INFO] Using Usertoken auth, with namecode:
[ERROR] Unable to upload bundle for deployment: Deployment
java.lang.RuntimeException: Invalid request. Status: 401 Response body:
```

The blank `namecode:` value means Maven is sending empty credentials to the Sonatype Central Portal.

**Root cause**: The `CENTRAL_TOKEN_USERNAME` or `CENTRAL_TOKEN_PASSWORD` repository secrets are missing or empty.

**Fix**:
1. Sign in to <https://central.sonatype.com/>.
2. Go to **Account** → **Generate User Token**.
3. Copy the generated token username and token password.
4. In the GitHub repository, go to **Settings** → **Secrets and variables** → **Actions**.
5. Create (or update) the `CENTRAL_TOKEN_USERNAME` secret with the token username.
6. Create (or update) the `CENTRAL_TOKEN_PASSWORD` secret with the token password.

Note: These are *token* credentials generated in the Central Portal, not your Sonatype account login credentials.

## Local Verification

Before creating a release tag, verify the build locally:

```bash
mvn clean verify
```

For a full release-style build including signing, use:

```bash
mvn -P release verify -Dgpg.passphrase=YOUR_GPG_PASSPHRASE
```
