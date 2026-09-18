# docker-lite — offline / fast release pack and deploy

Operator guide for building a customer-facing offline tarball (`pack.sh`) and deploying with **docker compose** (`deploy.sh`). This is the **lite / fast** path: prebuilt frontend artifacts + packaged `ecshopx-bootstrap.jar`, minimal compose stack.

For a full local development install, use `dev-setup.sh` + `docker-compose.dev.yml` instead.

## docker-compose.dev.yml vs docker-lite

| | **Full dev** (`docker-compose.dev.yml`) | **docker-lite** |
|---|---|---|
| Purpose | Full local development | Offline / fast customer deploy |
| Builds on deploy | Yes (Maven in `ecshopx-app` image, Node 20 frontends via `dev-setup.sh`) | No — jar + prebuilt `dist-*`, `.output` ship in the tarball |
| Java runtime | Built into `ecshopx-app` image | Packaged `docker-lite/app/ecshopx-bootstrap.jar`, bind-mounted as `app.jar` |
| Compose file | `docker-compose.dev.yml` | `docker-lite/docker-compose.yml` |
| Operator entry | `dev-setup.sh` / `install.sh --full` | `docker-lite/pack.sh`, `docker-lite/deploy.sh` / `install.sh --lite` |

## Architecture (full dev and lite deploy)

Four services under `docker-compose.dev.yml` / `docker-lite/docker-compose.yml`:

| Service | Role |
|---------|------|
| **mysql** | MySQL 8 + init SQL |
| **redis** | Redis 7 |
| **xxl-job-admin** | XXL-Job Admin (host port **8080** by default) |
| **ecshopx-app** | Java (:18080) + OpenResty (:80, Host-based vhosts) + Nuxt SSR (:3000) in one container |

Removed: separate `gateway` and `ecshopx-web-frontend` services. Nginx runs inside `ecshopx-app` and proxies to `127.0.0.1:18080` (Java) and `127.0.0.1:3000` (Nuxt).

Host extract root (parent of `ECShopX-Java/`) is mounted to `/data/httpd`:

```text
/data/httpd/ECShopX-Java
/data/httpd/ECShopX-Java_Admin
/data/httpd/ECShopX-Java_Mobile
/data/httpd/ECShopX-Java_Web
```

Default publish: HTTP **80** (domains `admin.ecshopx.test` / `h5.ecshopx.test` / `www.ecshopx.test`), XXL-Job **8080**, Java API direct **18080**. Override with `--http-port` / `--admin-host` / `--h5-host` / `--pc-host`. If names do not resolve, add them to local hosts pointing at `127.0.0.1`.

## Pack (`pack.sh`)

Prerequisites on the build machine:

- **Jar**: `./mvnw` + JDK 17, **or** Docker (uses `docker/Dockerfile.app` builder stage when host Maven/Java unavailable) — jar **is included** in the tarball as `docker-lite/app/ecshopx-bootstrap.jar`
- **Frontends**: Node **20.19** for admin, mobile, and PC (pnpm **10.13.0** for PC)
- `tar` (and `rsync` recommended)

```bash
cd ECShopX-Java/docker-lite
./pack.sh
./pack.sh --with-images
```

Root wrappers from `ECShopX-Java/`:

```bash
cd ECShopX-Java
./pack.sh
./pack.sh --with-images
```

`--with-images` downloads image tars into `docker-lite/images/` (needs URLs in `images.env`).

**Build steps:**

1. Bootstrap jar → `docker-lite/app/ecshopx-bootstrap.jar` (+ `app.jar` alias)
2. Admin: `npm run build:b2c` / `build:bbc` → `dist-b2c` / `dist-bbc`
3. Mobile H5: b2c/bbc builds → `dist-b2c/h5` / `dist-bbc/h5`
4. PC: `pnpm build` → `.output` (keeps `.output/server/node_modules`)
5. Tarball `ecshopx-java-<version>.tar.gz` with four project roots **including** the jar

**Excluded from tarball:** `.git`, project-root `node_modules/`, Maven `target/`, `.env`, prior `ecshopx-*.tar.gz`. **Kept:** `ECShopX-Java_Web/.output/server/node_modules` (required for Nuxt SSR).

## Configure image tars

Edit `docker-lite/images.env` (see `images.env.example`):

```bash
APP_IMAGE=ecshopx-app:latest
APP_IMAGE_TAR_URL=https://your-cdn/ecshopx-app.tar
MYSQL_IMAGE=mysql:8.0
# ...
```

If a tar already exists under `docker-lite/images/<basename>`, deploy skips download and loads it directly.

## Deploy (`deploy.sh`)

```bash
tar xzf ecshopx-java-0.0.1-SNAPSHOT.tar.gz
cd ecshopx-java-0.0.1-SNAPSHOT/ECShopX-Java/docker-lite
# fill images.env tags first (APP_IMAGE points at registry)
./deploy.sh --mode b2c \
  --http-port 80 \
  --admin-host admin.example.com \
  --h5-host h5.example.com \
  --pc-host www.example.com
```

Root wrappers from extracted `ECShopX-Java/` also work: `./deploy.sh ...`.

**Jar**: deploy always uses packaged `docker-lite/app/ecshopx-bootstrap.jar` (copied to `app.jar` for compose). No remote jar download.

What it does:

1. Validate prebuilt frontend `dist-*`, PC `.output`, and packaged jar
2. Copy packaged `ecshopx-bootstrap.jar` → `app.jar`
3. Activate `dist-b2c` or `dist-bbc` for admin/mobile
4. Ensure runtime images (`docker pull`)
5. `docker compose -f docker-lite/docker-compose.yml up -d`

Does **not** run Maven, npm, pnpm, or frontend builds.

## Related scripts

| Script | Audience |
|--------|----------|
| `docker-lite/pack.sh` (or `ECShopX-Java/pack.sh`) | Release: build jar + frontends + tarball |
| `docker-lite/deploy.sh` (or `ECShopX-Java/deploy.sh`) | Customer: use packaged jar + pull images + compose up |
| `dev-setup.sh` | Local full stack (not used by offline deploy) |
| `install.sh --lite` | Download tarball + run deploy |

## Self-check smoke (operators / CI)

Run from `ECShopX-Java/` when Docker is available:

```bash
docker compose -f docker-compose.dev.yml config
docker compose -f docker-lite/docker-compose.yml --env-file docker-lite/images.env.example config
bash tests/install/run.sh
```

Contract tests cover `install.sh` mode prompts, full/lite handoff paths, compose validity, and pack/deploy CLI guards. `LITE_PACKAGE_URL` may remain a placeholder until the CDN publishes a fresh tarball that includes the jar.
