<p align="center"><img width="600" height="auto" alt="logo" src="logo.png" /></p>

#
<p align="center">English / <a href="readme_cn.md">简体中文</a></p>

ECShopX Java backend: a Spring Boot 3 + Java 17 server implementation built as a multi-module architecture (1 Bundle = 1 Maven Module). It covers the full business stack—from products, orders, members, and marketing to payments and third-party integrations—and works with the ECShopX frontend to quickly build an official commerce foundation across multiple channels and business models.

## Project Overview
`ecshopx-java` is the Java server-side implementation of the ECShopX commerce system. It keeps business semantics aligned with the legacy PHP version (see the repository root [`README`](../readme_cn.md)) and maps configuration items one-to-one, while reorganizing the codebase into a highly modular, multi-Bundle architecture following modern Java engineering practices. Each business domain (products, orders, marketing, members, payments, third-party integrations, etc.) is split into an independent Maven module and managed under the parent project `cn.shopex:ecshopx`. At runtime, `ecshopx-bootstrap` serves as the Spring Boot entrypoint and assembles all Bundles on demand.

## Use Cases
* **B2C private-domain brand store**: a unified backend for official mini-programs, apps, PC websites, H5, and other DTC commerce channels.
* **B2C employee purchase benefits platform**: supports employee & family/friends internal-purchase programs for multi-brand groups.
* **B2B2C multi-merchant platform**: builds a JD.com/Meituan-style online platform with self-operated stores plus multi-merchant onboarding.
* **S2B2C supply chain collaboration**: connects brands, distributors, and end stores on one supply chain platform.
* **O2O brand cloud store + instant retail**: online ordering with nearby-store pickup and instant delivery.
* **O2O dealer cloud store**: aggregates dealer store resources to support online ordering with store fulfillment and pickup.

## Key Features
### Modular Architecture
* **1 Bundle = 1 Maven Module**: 50+ business Bundles (`ecshopx-goods`, `ecshopx-orders`, `ecshopx-promotions`, `ecshopx-members`, `ecshopx-payment`, etc.) are packaged and evolve independently and can be included as needed.
* **Clear layering**: each Bundle follows a lightweight `controller / service / mapper / integration / port` structure, and Bundles are decoupled through Port interfaces to avoid circular dependencies.
* **Unified startup**: business Bundles only expose capabilities; `ecshopx-bootstrap` wires everything together and runs as a single Spring Boot application.

### Business Capabilities
* **Products / Orders / After-sales**: multi-level product categories, SKUs, specifications, inventory, market price / sale price; a complete order state machine; after-sales orders and refunds.
* **Marketing**: coupons (`ecshopx-kaquan`), points mall (`ecshopx-pointsmall`), group buying / flash sales / tiered discounts (`ecshopx-promotions`), membership levels (`ecshopx-members`), distribution and promotions (`ecshopx-distribution` / `ecshopx-popularize`).
* **Multi-merchant & stores**: merchant onboarding (`ecshopx-merchant`), store reservations (`ecshopx-reservation`), sales associates (`ecshopx-salesperson`), and self-service ordering (`ecshopx-selfservice`).
* **Payments & reconciliation**: built-in channels including WeChat Pay, Alipay, ChinaUMS (`ecshopx-chinaums-pay`), AdaPay (`ecshopx-adapay`), and Huifu (`ecshopx-hfpay`).
* **Third-party integrations**: WeChat Open Platform / Mini Program / Official Account (`ecshopx-wechat`), WeCom (`ecshopx-work-wechat`), Ali / Shuyun / Jushuitan / Youshu (`ecshopx-ali` / `ecshopx-shuyun` / `ecshopx-system-link` / `ecshopx-youshu`).
* **OpenAPI & cross-border**: an Open API gateway (`ecshopx-openapi`) and cross-border commerce (`ecshopx-cross-border`).

### Engineering Highlights
* **Spring Boot 3.5 + Java 17**: uses Jakarta EE 9+ namespaces and the Spring 6 programming model.
* **MyBatis-Plus 3.5**: ships a custom `FqcnMapperBeanNameGenerator` so mappers with the same name don't collide across Bundles.
* **Embedded Undertow**: enables `allow-unescaped-characters-in-url` by default for better compatibility with legacy PHP URLs; the POST body limit is 64 MB.
* **XXL-JOB scheduling**: all scheduled jobs register through the XXL-JOB Executor, with the admin console deployed separately.
* **Multiple Redis logical databases**: default / companys / prism / datacube / deposit, isolated by business area.

## System Requirements
* **JDK** ≥ 17 (Eclipse Temurin 17 recommended)
* **Maven** ≥ 3.9 (use the bundled `./mvnw` wrapper in the repository root)
* **MySQL** ≥ 5.7 (8.0 recommended, `utf8mb4` charset, `Asia/Shanghai` timezone)
* **Redis** ≥ 4.0
* **XXL-JOB Admin** (optional, but required for scheduled jobs; see the deployment manifest in `../docs/migration/infra/xxl-job/`)

## Project Structure
```
ecshopx-java/
├── pom.xml                      # Parent POM; manages versions for 50+ business modules
├── docker-compose.yml           # One-command orchestration: mysql + redis + xxl-job-admin + ecshopx-java
├── .dockerignore                # Reduces the image build context
├── docker/
│   ├── Dockerfile               # Multi-stage build (builder → runtime)
│   ├── ecshopx.sql              # Business schema + demo data (imported automatically on compose startup)
│   └── tables_xxl_job.sql       # XXL-Job scheduling database
├── ecshopx-bom/                 # Dependency BOM
├── ecshopx-common/              # Shared utilities: exceptions / helpers / MyBatis extensions
├── ecshopx-dispatch/            # Cross-Bundle scheduling / event dispatch
├── ecshopx-bootstrap/           # Spring Boot entrypoint (main class)
├── ecshopx-goods/               # Product domain
├── ecshopx-orders/              # Order domain
├── ecshopx-aftersales/          # After-sales domain
├── ecshopx-promotions/          # Marketing domain
├── ecshopx-members/             # Membership domain
├── ecshopx-payment/             # Payment aggregation
├── ecshopx-wechat/              # WeChat ecosystem
├── ecshopx-openapi/             # Public OpenAPI
├── ...                          # 30+ additional business Bundles
└── logs/                        # Runtime logs (including xxl-job execution logs)
```
Main class: `cn.shopex.ecshopx.EcshopxApplication` (located in `ecshopx-bootstrap`).

## Database Migrations (Flyway)

The Java backend uses Flyway to manage incremental SQL migrations. Migration files live in:

```bash
ecshopx-bootstrap/src/main/resources/db/migration
```

Automatic migrations at application startup are enabled by default (`spring.flyway.enabled=true`). Full/lite Docker installations wait for the Java service to become ready after compose startup; Flyway runs during that startup. For local manual runs, you can still use the script shortcuts. Execution history is recorded in `flyway_schema_history`. Existing databases are baselined at version `0` via `baselineOnMigrate=true`, which avoids re-running historical schema scripts when Flyway is first introduced.

### Generate Migration Files

The project provides a `bin/make-migration` helper for generating Flyway-compliant SQL files:

```bash
# From the ecshopx-java directory
bin/make-migration add_order_extra_index
```

The default flow mirrors the core logic of the PHP project's `php artisan doctrine:migrations:diff`: it connects to the MySQL database pointed to by `spring.datasource.*` and reads the current schema as the "from" schema, scans the local MyBatis-Plus domains (`@TableName` / `@TableId` / `@TableField`) to derive the target schema as the "to" schema, then generates SQL to migrate from the from-schema to the to-schema. Use `--filter-expression` to limit which tables are compared. By default, DROP statements are not generated for tables or columns that exist in the database but not in the domains, unless `--allow-drop` is explicitly passed. The script compares the base column structure and primary keys inferred from `@TableId`; ordinary secondary indexes are not generated out of thin air because MyBatis-Plus domains have no standard index metadata source. Since MyBatis-Plus annotations do not carry as complete metadata as Doctrine ORM (column length, precision, nullability, ordinary indexes, etc.), the generated SQL must be manually reviewed for SQL types, default values, NULL allowance, comments, indexes, and column order.

You can override the connection configuration or narrow the comparison scope with these options:

```bash
bin/make-migration --profile local add_order_extra_index
bin/make-migration --filter-expression '^items$' sync_items
bin/make-migration --changed-only add_order_extra_index
bin/make-migration --allow-drop sync_domain_schema
bin/make-migration --db-url "jdbc:mysql://127.0.0.1:3306/ecshopx" --db-user ecshopx --db-password ecshopx add_order_extra_index
```

If you only need an empty template:

```bash
bin/make-migration --empty manual_data_fix
```

Generated files use the `VyyyyMMddHHmmss__description.sql` format, for example:

```text
ecshopx-bootstrap/src/main/resources/db/migration/V20260709153000__add_order_extra_index.sql
```

### Run Migrations

After generating and reviewing a migration file, update the database manually with the shortcut command:

```bash
bin/make-migration migrate
bin/make-migration migrate --profile local
bin/make-migration migrate --db-url "jdbc:mysql://127.0.0.1:3306/ecshopx" --db-user ecshopx --db-password ecshopx
```

This command invokes the Flyway Maven plugin's `migrate` goal. The migration directory is fixed at:

```bash
ecshopx-bootstrap/src/main/resources/db/migration
```

## Installation Modes

This repository supports three deployment paths. Choose one based on your scenario:

| Mode | Entry point | Use case |
|------|------|----------|
| **Full development** | `bash install.sh --full` or `./dev-setup.sh` | Local development with four frontends + `docker-compose.dev.yml` |
| **Lite offline** | `bash install.sh --lite` or `docker-lite/deploy.sh` | Offline deployment on customer machines with a prebuilt jar + frontend artifacts |
| **One-click demo** | `docker compose up -d` (repository root) | Quick evaluation; Maven builds automatically inside containers |

See [`docker-lite/README.md`](docker-lite/README.md) for lite installation details. The release archive `ecshopx-java-<version>.tar.gz` is generated on the build machine by `docker-lite/pack.sh` (or `./pack.sh` at the repository root).

## Docker Deployment (Recommended)

The repository ships a complete `docker-compose.yml` that starts four containers—**mysql + redis + xxl-job-admin + ecshopx-java**—with one command. It automates the full path from source code → executable jar → runtime image → startup → data import, so there is no need to install Java / Maven / MySQL / Redis on the host.

### Prerequisites
- Docker 24+ and Docker Compose v2 (Docker Desktop recommended on macOS; enable BuildKit to take advantage of Maven cache layers)
- At least 4 GB of available memory (recommend allocating ≥ 6 GB to Docker Desktop)
- Available ports: `18080`, `19999`, `9080`, `3306`, `6379`

### One-Click Startup
```bash
cd ecshopx-java
docker compose up -d              # First run builds the image + imports SQL; takes 5–15 minutes
docker compose logs -f ecshopx-java
```

`docker compose up -d` will:
1. Start `mysql:8.0` and import `docker/tables_xxl_job.sql` followed by `docker/ecshopx.sql` in order (business tables + demo data);
2. Start `redis:7-alpine` (password `redispassword`, AOF persistence);
3. Start `xuxueli/xxl-job-admin:2.5.0` and use the `xxl_job` database in the `mysql` service;
4. Build the `ecshopx-java:latest` image from `docker/Dockerfile` (multi-stage, final image based on `eclipse-temurin:17-jre-alpine`) and wait for dependencies to be ready via `depends_on: condition: service_healthy` before starting.

The four containers share a custom bridge network named `ecshopx` and reach each other by service name (`mysql` / `redis` / `xxl-job-admin`). Business parameters are injected in Spring Boot CLI style through `APP_ARGS`, preserving the semantics of case-sensitive properties such as `xxl.job.accessToken`.

### Access URLs
| Service | URL / Port | Default credentials |
|---|---|---|
| ecshopx-java API | http://localhost:18080/api/ | — |
| ecshopx-java static resources | http://localhost:18080/storage/ | — |
| WeChat callback | http://localhost:18080/wechatAuth/ | — |
| XXL-Job console | http://localhost:9080/xxl-job-admin | `admin` / `123456` |
| MySQL | `localhost:3306` | business `ecshopx` / `ecshopx`; root `root` / `rootpassword` |
| Redis | `localhost:6379` | password `redispassword` |

The `/api/`, `/storage/`, and `/wechatAuth/` paths keep the same semantics as the PHP version and integrate seamlessly with the frontend projects (`ecshopx-admin` / `ecshopx-vshop`).

### Common Compose Commands
```bash
# List containers and health status
docker compose ps

# Tail logs for a single service
docker compose logs -f ecshopx-java
docker compose logs -f xxl-job-admin

# Rebuild and hot-swap the Java service after code changes
docker compose up -d --build ecshopx-java

# Enter a container for troubleshooting
docker compose exec ecshopx-java sh
docker compose exec mysql mysql -uecshopx -pecshopx ecshopx

# Stop all services (keep mysql/redis volumes)
docker compose down

# Remove everything, including database / Redis AOF data
docker compose down -v
```

### Build / Run the Image Without Compose
If you already provide MySQL / Redis / XXL-Job Admin, you can build and run only the Java image:
```bash
cd ecshopx-java
docker build -f docker/Dockerfile -t ecshopx-java:latest .

docker run --rm -p 18080:18080 -p 19999:19999 \
  -e JAVA_OPTS="-Xms512m -Xmx1024m" \
  -e APP_ARGS="--spring.datasource.url=jdbc:mysql://<host>:3306/ecshopx \
               --spring.datasource.username=ecshopx \
               --spring.datasource.password=ecshopx \
               --spring.data.redis.host=<host> \
               --spring.data.redis.password=redispassword \
               --xxl.job.admin.addresses=http://<host>:9080/xxl-job-admin \
               --xxl.job.accessToken=ecshopx-cron-dev" \
  ecshopx-java:latest
```
> The Dockerfile passes through `JAVA_OPTS` (JVM options) and `APP_ARGS` (Spring Boot CLI-style property overrides) via `ENTRYPOINT`; both can be injected as needed with `docker run -e`.

## Self-Hosted Deployment (Local Development)

### 1. Configure `application-local.properties`
The project already includes `ecshopx-bootstrap/src/main/resources/application-local.properties` as a local development sample. Update it as needed:
* Database: `spring.datasource.url` / `username` / `password`
* Redis: `spring.data.redis.host` / `port` / `password`
* JWT: `JWT_SECRET` (generate a 32-byte key with `openssl rand -base64 32`)
* Object storage: `ecshopx.storage.driver` (`local` / `oss` / `qiniu` / `aws` / `cosv5`) and the corresponding credentials
* XXL-JOB: `xxl.job.admin.addresses` / `xxl.job.accessToken`
* Third-party integrations (WeChat, Alipay, ChinaUMS, Prism, Shuyun, etc.) as needed

### 2. Build
```bash
# From the ecshopx-java directory
./mvnw clean install -DskipTests
```
The first build pulls dependencies from the Aliyun Maven mirror (the `aliyun-public` repository is preconfigured in the parent POM).

### 3. Run
The entrypoint is `cn.shopex.ecshopx.EcshopxApplication` in the `ecshopx-bootstrap` module. Choose any of the following methods:

#### Method A: Maven plugin (recommended during development)
```bash
# From the ecshopx-java directory
./mvnw -pl ecshopx-bootstrap -am spring-boot:run \
  -Dspring-boot.run.profiles=local
```
`-am` (also-make) also builds all Bundles that bootstrap depends on. It is recommended on the first startup or after pulling new code; you can omit it on subsequent runs when dependencies haven't changed to speed things up.

Additional JVM / Spring options:
```bash
./mvnw -pl ecshopx-bootstrap spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8" \
  -Dspring-boot.run.arguments="--server.port=18081"
```

#### Method B: Executable jar
```bash
# Package first
./mvnw -pl ecshopx-bootstrap -am package -DskipTests

# Then run
java -jar ecshopx-bootstrap/target/ecshopx-bootstrap-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local
```
Common optional arguments:
```bash
# Custom JVM memory and encoding, override the port, and add external config
java -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 \
  -jar ecshopx-bootstrap/target/ecshopx-bootstrap-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local \
  --server.port=18081 \
  --spring.config.additional-location=file:./config/
```

#### Method C: Run directly from the IDE
* In IntelliJ IDEA / VS Code, right-click `EcshopxApplication` → Run/Debug.
* VM options: `-Dfile.encoding=UTF-8` (add `-Xms512m -Xmx1024m` if needed)
* Program arguments: `--spring.profiles.active=local`
* Active profiles: `local`
* Working directory: `$MODULE_WORKING_DIR$` (the `ecshopx-bootstrap` directory)

#### Method D: Remote debugging
```bash
# Maven plugin (listens on 5005; connect as soon as the app starts)
./mvnw -pl ecshopx-bootstrap spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Executable jar
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar ecshopx-bootstrap/target/ecshopx-bootstrap-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local
```

#### Method E: Run in the background (Linux/macOS)
```bash
nohup java -jar ecshopx-bootstrap/target/ecshopx-bootstrap-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local \
  > logs/ecshopx.out 2>&1 &
echo $! > ecshopx.pid

# Stop
kill "$(cat ecshopx.pid)"
```

After a successful startup, the application listens on `server.port=18080` by default (see `ecshopx-bootstrap/src/main/resources/application.properties`) and exposes:

* API: http://localhost:18080/api/
* Static resources: http://localhost:18080/storage/
* WeChat callback: http://localhost:18080/wechatAuth/

### 4. Database Initialization
The repository's `docker/ecshopx.sql` already contains the business schema and a demo dataset:
- **Docker Compose mode**: imported automatically by the MySQL container on the first `docker compose up -d`; no manual steps required.
- **Local MySQL mode**:
  ```bash
  mysql -h 127.0.0.1 -P 3306 -uroot -p \
    -e "CREATE DATABASE IF NOT EXISTS ecshopx DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
  mysql -h 127.0.0.1 -P 3306 -uroot -p ecshopx < docker/ecshopx.sql
  ```
  Import `docker/tables_xxl_job.sql` for the XXL-Job scheduling database (it includes `CREATE/USE xxl_job`).

The default admin credentials carry over from the PHP version:

> Username: `admin`  
> Password: `Shopex123`

### 5. NGINX Reverse Proxy (Optional)
To expose the API and frontend under a unified domain, refer to the NGINX template in the repository root [`readme_cn.md`](../readme_cn.md) and point `proxy_pass` to `http://localhost:18080`.

## Common Development Commands

### Maven
```bash
# Build a single module with its dependencies
./mvnw -pl ecshopx-orders -am clean install -DskipTests

# Run tests for one Bundle only
./mvnw -pl ecshopx-orders test

# Run a specific unit test class
./mvnw -pl ecshopx-orders test -Dtest=OrderXxxTest

# Inspect the dependency tree (troubleshoot conflicts)
./mvnw -pl ecshopx-bootstrap dependency:tree

# Skip tests, compile, and package the executable jar
./mvnw -pl ecshopx-bootstrap -am package -DskipTests
```

### Docker Compose
```bash
# One-click startup / run in the background
docker compose up -d

# Check service status and health
docker compose ps

# Follow logs
docker compose logs -f ecshopx-java

# Rebuild and replace only the business container after Java code changes
docker compose up -d --build ecshopx-java

# Stop everything (keep volumes)
docker compose down

# Remove everything, including database / Redis data
docker compose down -v
```

## Notes
* The first build takes 5–15 minutes (image pull + 50+ module compilation + SQL import). Subsequent builds are much faster thanks to the Maven local cache (the Dockerfile declares `--mount=type=cache` cache layers).
* Port conflicts: make sure `18080` (application HTTP), `19999` (XXL-Job Executor), `9080` (XXL-Job Admin), `3306` (MySQL), and `6379` (Redis) are not already in use.
* `docker compose down` keeps the `ecshopx-mysql-data` / `ecshopx-redis-data` / `ecshopx-app-logs` named volumes so the next startup can reuse them. Use `docker compose down -v` if you need SQL to be re-imported.
* Same-name Mapper collisions across modules are already handled by `FqcnMapperBeanNameGenerator`. When adding new mappers, use the `cn.shopex.ecshopx.**.mapper` package path directly.
* The credentials in `application-local.properties` and the compose file (`rootpassword` / `redispassword` / `JWT_SECRET`, etc.) are for local development only. **In production, always inject real secrets via environment variables or an external configuration center**; do not reuse the default values from the repository.

## License
This project is licensed under the Apache-2.0 open source license.  
Every ECShopX source file included in this distribution is licensed under the Apache 2.0 open source license.

See the root `LICENSE.txt` for the full text of the Apache 2.0 license.

## Contributing
We welcome contributions of all kinds!  
Read the repository root [`CONTRIBUTING.md`](../CONTRIBUTING.md) to learn how to get involved. Before submitting Java-side changes:
1. Keep analysis / plan / test artifacts under `docs/migration/` (when related to PHP → Java migration);
2. Make sure `./mvnw -pl <module> -am verify` passes;
3. Follow the repository's existing package structure and layering conventions (`controller` / `service` / `mapper` / `integration` / `port`).

## Support
* 📖 Documentation: check the [official documentation](https://doc.shopex.cn/ecshopx/docs/readme.html) first
* 🐛 Issues: submit them in [Issues]

## 💬 Community
Scan the QR code to join the ECShopX open-source community for updates, discussions, and troubleshooting.
<p align="center"><img width="300" height="auto" alt="workwechat" src="workwechat.png" /></p>

## Acknowledgments
Thanks to all the developers, users, and Shopex's global brand customers who have contributed to ECShopX!
