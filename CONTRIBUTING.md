# Contributing to auralogs-java

Thanks for your interest in improving the Auralogs Java SDK!

## Scope

This repo is the **Java SDK** only. For issues with the Auralogs service itself (ingest, dashboard, analysis, billing), head to [auralogs.ai](https://auralogs.ai) or the [docs](https://docs.auralogs.ai).

## Reporting bugs

Open a [bug report](https://github.com/auralogs-ai/auralogs-java/issues/new?template=bug_report.yml). Include SDK version, JDK version, minimal repro, expected vs. actual.

## Security issues

**Do not open public issues for vulnerabilities.** See [SECURITY.md](./SECURITY.md).

## Development

Requirements: JDK 11 or later (any distribution).

```bash
git clone https://github.com/auralogs-ai/auralogs-java.git
cd auralogs-java
./gradlew check
```

`check` runs Spotless format check, all unit tests, and Javadoc. Run `./gradlew spotlessApply` to auto-fix formatting.

## Commit messages

We follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`, `chore:`. Scope by module when relevant: `feat(core):`, `fix(slf4j):`.

## Releases

Maintainers publish via GitHub Releases. Tagging `vX.Y.Z` and publishing a release triggers the `Release` workflow, which builds + signs + publishes to Maven Central via OIDC.

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](./LICENSE).
