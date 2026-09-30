# Repository instructions

- Read README.md, docs/architecture.md and docs/page-standard.md first.
- Keep Vue frontend in the pnpm workspace and Java in backend/. Do not add React Ant Design to the Vue runtime.
- Keep TypeScript contracts, Java PageSchema and spec/page.schema.json aligned; add regression tests for validation changes.
- Never execute model output or repository scripts as part of planning. Write generated files to a new review directory, not over existing source or schemas.
- Preserve Session / CSRF, backend roles, optimistic locking, module isolation and transactional audit.
- Never put secrets in source, browser bundles, logs or fixtures.
- Run pnpm test, pnpm typecheck, pnpm build and mvn -f backend/pom.xml verify when dependencies are available. Report what actually ran.
- Do not add fake dashboard data or present keyword rules as an LLM.
