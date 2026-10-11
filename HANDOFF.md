# FocusGuardServer cloud handoff

This commit preserves the current local backend development state for Codex Cloud.

- Backend repository: https://github.com/Yilinluo2006/FocusGuardServer
- Android client repository: https://github.com/Yilinluo2006/FocusGuard
- Original backend module directory: `E:\FocusGuardServer\FocusGuardServer`.

Read `docs/` and the client repository's `HANDOFF.md` before continuing cross-repository work. The snapshot includes account, phone/WeChat authentication, backup, compliance, device synchronization, and temporary sleep access code and tests. Verify current behavior rather than assuming every feature is complete.

The project declares Java 21 in `pom.xml`. Prepare dependencies, then run `sh mvnw test` with isolated test configuration. The ignored local `.env` is not uploaded; use `.env.example` and `application.properties` to identify required settings. Provide cloud credentials through environment configuration, not committed source files.

This upload operation did not rerun application builds or tests. Local Android installation and real-device validation remain separate steps.
