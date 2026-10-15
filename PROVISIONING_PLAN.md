# One-command group provisioning — plan

**Goal.** `tnra-cli provision <group> … --apply` creates a working TNRA group end to end, from the
deploy host, with no manual steps: database, Keycloak realm (on the central server), tunnel route +
DNS, billing registration, container, admin seed, verification, one-page handoff. Two admins are
created: the founder as a **temporary admin** (billing-exempt; deactivated by the real admin once the
group is running) and the group's **real admin**. Command line first; a UI can wrap the same code later.

Status: **planned** (2026-10-15). Prerequisites done: Keycloak externalized (dogeared/keycloak);
`cloudflared` runs in Docker on `tnra-shared` (10.7.1) so tunnel routes use container names.

## The steps the command executes (idempotent; re-runnable after a partial failure)

| # | Step | How | Checked before doing |
|---|---|---|---|
| 1 | Register with billing; exempt the temp admin | existing `POST /api/admin/groups` + **new** member-exempt admin endpoint | group already registered |
| 2 | Create DB + user on the shared MySQL | `docker compose exec -T mysql mysql …` (root from `.env`) | database exists |
| 3 | Create the realm on central Keycloak | `POST /admin/realms` with the rendered `realm.json.tmpl` (two users) | realm exists |
| 4 | Tunnel route + DNS | Cloudflare API: tunnel configuration (ingress) + proxied CNAME | route / record exists |
| 5 | Billing token → `.env`; start container | append `TNRA_BILLING_API_TOKEN_<GROUP>=…`; `docker compose … up -d --build tnra-<group>` | line present; container up |
| 6 | Seed both admin rows | poll `/actuator/health` on the host port, then `seed-admin.sql` | rows exist |
| 7 | Verify + hand off | realm GET, DB ping, health, `https://<group>.<domain>` reachable (DNS may lag — retry) | — |

Omitting `--apply` keeps today's generate-only behaviour.

## Barriers and their fixes

- **Keycloak admin API credentials.** Dedicated client in the central server's `master` realm with a
  service account + `realm-management`/`create-realm`; its secret in `.env` (`KEYCLOAK_ADMIN_URL`,
  `KEYCLOAK_ADMIN_CLIENT_ID`, `KEYCLOAK_ADMIN_CLIENT_SECRET`). Documented in dogeared/keycloak.
- **Cloudflare route + DNS.** API token scoped `Account → Cloudflare Tunnel → Edit` and
  `Zone → DNS → Edit`, plus `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_ZONE_ID`, `CLOUDFLARE_TUNNEL_ID` in
  `.env`. Route target is the container name (`http://tnra-<group>:8080`) now that `cloudflared` is in Docker.
- **No health endpoint on the group app.** Add `spring-boot-starter-actuator`, expose only `health`,
  permit it anonymously (as billing does). The CLI polls it on the host port before seeding.
- **Exempting the temp admin.** Billing has member-level `exempt` in the model but no API: add
  `PUT /api/admin/groups/{slug}/members/{email}` `{exempt: true|false}` (X-Admin-Token). Everyone else
  gets the normal trial. The real admin later deactivates the temp admin (Admin → Members).
- **MySQL from Java.** Shell out to `docker compose exec` (the CLI must run on the host anyway); no JDBC driver.
- **Production jar.** `up --build` bakes `tnra-app/target/*.jar`; the CLI refuses with the build command
  if it is missing or not the checked-out version, instead of silently building a stale image.
- **Secrets.** All read from the deploy directory's `.env` (`--env-file` to override).
- **Forgot-password mail.** Default the realm SMTP block from Mailgun SMTP creds in `.env`
  (`MAILGUN_SMTP_USER` / `MAILGUN_SMTP_PASSWORD` on `mail.tnra.app`) unless `--smtp-*` given.
- **Where it runs.** On the deploy host, from the deploy directory (Docker, `.env`, built jar). Java 21 is there.

## Delivery

1. 10.8.0-pre: actuator health on `tnra-app`; billing member-exempt endpoint (both small, tested).
2. 10.8.0: `--apply` in `ProvisionCommand` with one class per step (`DatabaseStep`, `KeycloakRealmStep`,
   `CloudflareRouteStep`, `ContainerStep`, `SeedStep`, `VerifyStep`), each `check()`/`apply()`, unit-tested
   against stubbed HTTP (`HttpServer`) and a fake shell; `--temp-admin-*` options; handoff output.
3. dogeared/keycloak: README section for the service-account client.
4. Later: self-serve flow calls the same steps as a service (see TODOS "Self-serve onboarding").
