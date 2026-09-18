<p align="center"><img width="600" height="auto" alt="logo" src="logo.png" /></p>

# 
<p align="center"><a href="readme.md">English</a> / 简体中文</p>

ECShopX-Java 的 Java 后端工程：一套基于 Spring Boot 3 + Java 17 的多模块（1 Bundle = 1 Maven Module）服务端实现，承载从商品、订单、会员、营销到支付与第三方集成的完整业务能力，配合 ECShopX-Java 前端可快速构建多端、多模式的官方商城基座。

## 项目介绍
`ecshopx-java` 是 ECShopX-Java 商城系统的 Java 服务端，与历史 PHP 版（参见仓库根目录readme_cn）业务语义对齐、配置项一一映射，并按现代 Java 工程实践重新组织为高度模块化的多 Bundle 架构。各业务域（商品、订单、营销、会员、支付、第三方对接等）被拆分为独立 Maven Module，统一在父工程 `cn.shopex:ecshopx` 下管理；运行时由 `ecshopx-bootstrap` 作为 Spring Boot 启动入口，按需聚合所有 Bundle。

## 适用场景
* **B2C 品牌私域商城**：作为官方小程序、APP、PC 官网、H5 等多端 DTC 商城的统一后端。
* **B2C 员工内购福利平台**：支持多品牌集团开展「员工 & 亲友内购」业务。
* **B2B2C 多商户平台**：打造类似京东、美团的「自营 + 多商户入驻」在线平台。
* **S2B2C 供应链协同**：连接品牌、经销商与终端门店的供应链平台。
* **O2O 品牌云店 + 即时零售**：线上下单、附近门店自提与即时配送。
* **O2O 经销商云店**：聚合所有经销商门店资源，实现「线上下单、门店发货 / 自提」。

## 核心特性
### 模块化架构
* **1 Bundle = 1 Maven Module**：50+ 业务 Bundle（`ecshopx-goods`、`ecshopx-orders`、`ecshopx-promotions`、`ecshopx-members`、`ecshopx-payment` 等）独立打包、独立演进，按需引入。
* **分层清晰**：每个 Bundle 内部遵循 `controller / service / mapper / integration / port` 的轻量分层，跨 Bundle 通过 Port 接口解耦，避免循环依赖。
* **统一启动**：业务 Bundle 仅暴露能力，`ecshopx-bootstrap` 负责装配并以单个 Spring Boot 应用运行。

### 业务能力
* **商品 / 订单 / 售后**：商品多级类目、SKU、规格、库存、市场价 / 销售价；订单全状态机；售后单与退款。
* **营销**：优惠券（`ecshopx-kaquan`）、积分商城（`ecshopx-pointsmall`）、拼团 / 秒杀 / 满减（`ecshopx-promotions`）、会员等级（`ecshopx-members`）、分销与推广（`ecshopx-distribution` / `ecshopx-popularize`）。
* **多商户与门店**：商户入驻（`ecshopx-merchant`）、门店预约（`ecshopx-reservation`）、导购（`ecshopx-salesperson`）、自助点单（`ecshopx-selfservice`）。
* **支付与对账**：内置 微信支付、支付宝、银联商务（`ecshopx-chinaums-pay`）、AdaPay（`ecshopx-adapay`）、汇付（`ecshopx-hfpay`）等多通道。
* **第三方集成**：微信开放平台 / 小程序 / 公众号（`ecshopx-wechat`）、企业微信（`ecshopx-work-wechat`）、阿里 / 数云 / 聚水潭 / 友数等（`ecshopx-ali` / `ecshopx-shuyun` / `ecshopx-system-link` / `ecshopx-youshu`）。
* **OpenAPI 与跨境**：开放 API 网关（`ecshopx-openapi`）、跨境业务（`ecshopx-cross-border`）。

### 工程特性
* **Spring Boot 3.5 + Java 17**：使用 Jakarta EE 9+ 命名空间、Spring 6 编程模型。
* **MyBatis-Plus 3.5**：搭配自定义 `FqcnMapperBeanNameGenerator`，多 Bundle 下同名 Mapper 不冲突。
* **Undertow 内嵌容器**：默认开启 `allow-unescaped-characters-in-url`，提升对历史 PHP URL 的兼容性；POST Body 上限 64MB。
* **XXL-JOB 调度**：所有定时任务统一通过 XXL-JOB Executor 注册，admin 独立部署。
* **多 Redis 逻辑库**：默认 / companys / prism / datacube / deposit 等按业务隔离。

## 系统要求
* **JDK** ≥ 17（推荐 Eclipse Temurin 17）
* **Maven** ≥ 3.9（推荐使用根目录自带的 `./mvnw`）
* **MySQL** ≥ 5.7（建议 8.0，字符集 `utf8mb4`，时区 `Asia/Shanghai`）
* **Redis** ≥ 4.0
* **XXL-JOB Admin**（可选，启用定时任务时必需，部署清单见 `docs/migration/infra/xxl-job/`）

## 工程结构
```
ECShopX-Java/
├── pom.xml                      # 父 POM，统一管理 50+ 业务模块版本
├── install.sh                   # 远程一键安装入口（--full / --lite）
├── dev-setup.sh                 # 全量开发安装
├── deploy.sh / pack.sh          # 极速离线入口（转发到 docker-lite/）
├── docker-compose.dev.yml       # 全量编排：mysql + redis + xxl + ecshopx-app
├── docker/
│   ├── Dockerfile.app           # 业务镜像（基于 runtime 基础镜像）
│   ├── Dockerfile.runtime       # 基础镜像：JRE17 + Node20 + OpenResty
│   ├── install-secrets.sh       # 安装时密码 / JWT
│   ├── ecshopx.sql              # 业务表结构 + 演示数据
│   └── tables_xxl_job.sql       # XXL-Job 调度库
├── docker-lite/                 # 极速离线 pack / deploy
├── ecshopx-bootstrap/           # Spring Boot 启动入口
├── ...                          # 其余业务 Bundle
└── logs/
```
启动主类：`cn.shopex.ecshopx.EcshopxApplication`（位于 `ecshopx-bootstrap`）。

## 数据库迁移（Flyway）

Java 后端使用 Flyway 管理增量 SQL 迁移，迁移文件目录：

```bash
ecshopx-bootstrap/src/main/resources/db/migration
```

默认已开启应用启动自动执行迁移（`spring.flyway.enabled=true`）。Docker 完整/极速安装会在 compose 启动后等待 Java 就绪（启动过程中完成 Flyway）。本地若需手动执行，仍可使用脚本快捷命令；执行记录写入 `flyway_schema_history`。已有数据库会通过 `baselineOnMigrate=true` 从版本 `0` 建立基线，避免首次接入 Flyway 时重跑历史建库 SQL。

### 生成迁移文件

项目提供 `bin/make-migration` 辅助命令生成符合 Flyway 命名规范的 SQL 文件：

```bash
# 在 ecshopx-java 目录下
bin/make-migration add_order_extra_index
```

默认流程对齐 PHP 项目 `php artisan doctrine:migrations:diff` 的核心逻辑：连接 `spring.datasource.*` 指向的 MySQL 读取当前数据库结构作为 from schema，扫描本地 MyBatis-Plus domain（`@TableName` / `@TableId` / `@TableField`）推导目标结构作为 to schema，然后生成从 from schema 迁移到 to schema 的 SQL。可用 `--filter-expression` 限定表名；默认不会为数据库中存在但 domain 中不存在的表/列生成 DROP，除非显式传入 `--allow-drop`。脚本会对比基础列结构与 `@TableId` 推导出的主键；普通二级索引由于 MyBatis-Plus domain 没有标准索引元数据来源，不会凭空生成。由于 MyBatis-Plus 注解没有 Doctrine ORM 那样完整的列长度、精度、nullable、普通索引等元数据，生成后必须人工检查 SQL 类型、默认值、是否允许 NULL、注释、索引和列顺序。

可通过参数覆盖连接配置或缩小对比范围：

```bash
bin/make-migration --profile local add_order_extra_index
bin/make-migration --filter-expression '^items$' sync_items
bin/make-migration --changed-only add_order_extra_index
bin/make-migration --allow-drop sync_domain_schema
bin/make-migration --db-url "jdbc:mysql://127.0.0.1:3306/ecshopx" --db-user ecshopx --db-password ecshopx add_order_extra_index
```

若只需要空模板，可使用：

```bash
bin/make-migration --empty manual_data_fix
```

生成文件格式为 `VyyyyMMddHHmmss__description.sql`，例如：

```text
ecshopx-bootstrap/src/main/resources/db/migration/V20260709153000__add_order_extra_index.sql
```

### 执行迁移

生成并 review 迁移文件后，使用快捷命令手动更新数据库：

```bash
bin/make-migration migrate
bin/make-migration migrate --profile local
bin/make-migration migrate --db-url "jdbc:mysql://127.0.0.1:3306/ecshopx" --db-user ecshopx --db-password ecshopx
```

该命令会调用 Flyway Maven 插件执行 `migrate`，迁移目录固定为：

```bash
ecshopx-bootstrap/src/main/resources/db/migration
```

## 安装模式

本仓库支持两种部署路径，可按场景选择：

| 模式 | 入口 | 适用场景 |
|------|------|----------|
| **全量开发** | `bash install.sh --full` 或 `./dev-setup.sh` | 本地开发，四端前端 + `docker-compose.dev.yml`（`ecshopx-app` 一体化容器） |
| **极速离线** | `bash install.sh --lite` 或 `./deploy.sh` | 客户机离线部署，发行包内含 jar + 预构建前端产物 |

### 运行时拓扑（全量开发 / 极速离线）

全量开发与极速离线共用同一 compose 形态：**mysql + redis + xxl-job-admin + ecshopx-app**（无独立 `gateway` / `ecshopx-web-frontend` 服务）。

| 服务 | 角色 |
|------|------|
| **mysql** | MySQL 8 + 初始化 SQL |
| **redis** | Redis 7 |
| **xxl-job-admin** | XXL-Job 调度控制台（宿主机默认 **8080**） |
| **ecshopx-app** | 一体化应用容器：Java (:18080) + OpenResty (:80，按域名分流) + Nuxt SSR (:3000) |

`ecshopx-app` 内 nginx 监听 **80**，按 `Host` 区分：

| 默认域名 | 服务 |
|----------|------|
| `admin.ecshopx.test` | 管理后台 + `/api` `/storage` `/wechatAuth` → Java |
| `h5.ecshopx.test` | H5 静态 |
| `www.ecshopx.test` | PC Nuxt SSR |

宿主机默认映射：业务 HTTP **80**、XXL-Job **8080**、Java 直连调试 **18080**。可用 `--http-port` / `--*-host` 覆盖；若域名无法解析，请在本机 hosts 添加 `127.0.0.1 admin.ecshopx.test h5.ecshopx.test www.ecshopx.test`。

**全量开发**（`dev-setup.sh`）：

- 前端编译统一使用 **Node 20**（`node:20.19.0-alpine`）构建 admin / mobile / PC 四端。
- 开发容器名 `ecshopx-dev-app`；与 mysql、redis、xxl 共四个业务容器。
- 编排文件：`docker-compose.dev.yml`；镜像构建：`docker/Dockerfile.app`。

**极速离线**（`docker-lite/deploy.sh`）：

- 发行包内含 `docker-lite/app/ecshopx-bootstrap.jar`（部署时直接使用，同步为 `app.jar` 供 compose 挂载）。
- 只需下载一次 `ecshopx-java-*.tar.gz`，无需再单独下载 jar。

```bash
bash docker-lite/deploy.sh --mode b2c \
  --http-port 80 \
  --admin-host admin.ecshopx.test \
  --h5-host h5.ecshopx.test \
  --pc-host www.ecshopx.test
```

极速安装详情见 [`docker-lite/README.md`](docker-lite/README.md)。发行包由 `docker-lite/pack.sh`（或根目录 `./pack.sh`）在构建机上生成 `ecshopx-java-<version>.tar.gz`（含 jar 与前端产物）。

## Docker 部署方式（推荐）

全量开发请使用 `./dev-setup.sh`（编排文件 `docker-compose.dev.yml`，镜像 `docker/Dockerfile.app`，基础镜像 `Dockerfile.runtime`）。

极速离线请使用 `./deploy.sh` / `docker-lite/deploy.sh`（见 [`docker-lite/README.md`](docker-lite/README.md)）。

### 前置要求
- Docker 24+ 与 Docker Compose v2
- 至少 4 GB 可用内存（建议 ≥ 6 GB）
- 端口可用：业务 HTTP `80`、XXL `8080`、Java `18080`、以及 `19999` / `3306` / `6379`（可按需改端口）

### 全量开发一键启动
```bash
cd ECShopX-Java
./dev-setup.sh --mode b2c
# 或
docker compose -f docker-compose.dev.yml up -d --build
docker compose -f docker-compose.dev.yml logs -f ecshopx-app
```

`dev-setup.sh` / compose 会完成：
1. 拉起 `mysql:8.0`，导入 `docker/tables_xxl_job.sql` 与 `docker/ecshopx.sql`；
2. 拉起 `redis:7-alpine`；
3. 拉起 `xuxueli/xxl-job-admin:2.5.0`；
4. 基于 `docker/Dockerfile.app` 构建 `ecshopx-app:latest`（依赖私有仓基础镜像 `registry.cn-hangzhou.aliyuncs.com/shopex_company/ecshopx-java:17-node20-openresty`）。

### 访问地址
| 服务 | URL / 端口 | 默认账号 |
|---|---|---|
| 管理后台 | http://admin.ecshopx.test/ | 安装时设置的 admin 密码 |
| API | http://admin.ecshopx.test/api/v1/ | — |
| H5 | http://h5.ecshopx.test/ | — |
| PC | http://www.ecshopx.test/ | — |
| Java 直连 | http://localhost:18080/ | — |
| XXL-Job 控制台 | http://localhost:8080/xxl-job-admin | `admin` / `123456` |
| MySQL | `localhost:3306` | `ecshopx` / `ecshopx`；root `rootpassword` |
| Redis | `localhost:6379` | `redispassword` |

### 常用编排命令
```bash
docker compose -f docker-compose.dev.yml ps
docker compose -f docker-compose.dev.yml logs -f ecshopx-app
docker compose -f docker-compose.dev.yml up -d --build ecshopx-app
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.dev.yml down -v
```

### 运行时基础镜像
```bash
./docker/build-runtime-base.sh          # 构建并打仓库 tag
./docker/build-runtime-base.sh --push   # 推送到阿里云仓库
./docker/build-runtime-base.sh --save docker/images/ecshopx-java-17-node20-openresty.tar
```

## 自行部署（本地开发）

### 1. 配置 `application-local.properties`
工程已自带 `ecshopx-bootstrap/src/main/resources/application-local.properties` 作为本地开发样例，按需修改：
* 数据库：`spring.datasource.url` / `username` / `password`
* Redis：`spring.data.redis.host` / `port` / `password`
* JWT：`JWT_SECRET`（建议 `openssl rand -base64 32` 生成 32 字节密钥）
* 对象存储：`ecshopx.storage.driver`（`local` / `oss` / `qiniu` / `aws` / `cosv5`）及对应密钥
* XXL-JOB：`xxl.job.admin.addresses` / `xxl.job.accessToken`
* 第三方集成（微信、支付宝、银联、Prism、数云等）按需填充

### 2. 编译
```bash
# 在 ecshopx-java 目录下
./mvnw clean install -DskipTests
```
首次构建会从阿里云 Maven 镜像拉取依赖（已在父 POM 中预配置 `aliyun-public` 仓库）。

### 3. 启动
启动入口位于 `ecshopx-bootstrap` 模块的 `cn.shopex.ecshopx.EcshopxApplication`，可任选下列方式之一：

#### 方式 A：Maven 插件（开发期推荐）
```bash
# 在 ecshopx-java 目录下
./mvnw -pl ecshopx-bootstrap -am spring-boot:run \
  -Dspring-boot.run.profiles=local
```
`-am`（also-make）会同时编译 bootstrap 依赖的所有 Bundle，首次启动或拉到新代码后建议带上；后续无依赖改动可省略以提速。

附加 JVM / Spring 参数：
```bash
./mvnw -pl ecshopx-bootstrap spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8" \
  -Dspring-boot.run.arguments="--server.port=18081"
```

#### 方式 B：可执行 jar
```bash
# 先打包
./mvnw -pl ecshopx-bootstrap -am package -DskipTests

# 再启动
java -jar ecshopx-bootstrap/target/ecshopx-bootstrap-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local
```
常见可选参数：
```bash
# 自定义 JVM 内存与编码、覆盖端口、追加外部配置
java -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 \
  -jar ecshopx-bootstrap/target/ecshopx-bootstrap-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local \
  --server.port=18081 \
  --spring.config.additional-location=file:./config/
```

#### 方式 C：IDE 直接运行
* 在 IntelliJ IDEA / VS Code 中右键 `EcshopxApplication` → Run/Debug。
* VM options：`-Dfile.encoding=UTF-8`（按需追加 `-Xms512m -Xmx1024m`）
* Program arguments：`--spring.profiles.active=local`
* Active profiles：`local`
* Working directory：`$MODULE_WORKING_DIR$`（即 `ecshopx-bootstrap` 目录）

#### 方式 D：远程调试启动
```bash
# Maven 插件（监听 5005，应用一启动即可连接）
./mvnw -pl ecshopx-bootstrap spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# 可执行 jar
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar ecshopx-bootstrap/target/ecshopx-bootstrap-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local
```

#### 方式 E：后台运行（Linux/macOS）
```bash
nohup java -jar ecshopx-bootstrap/target/ecshopx-bootstrap-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local \
  > logs/ecshopx.out 2>&1 &
echo $! > ecshopx.pid

# 停止
kill "$(cat ecshopx.pid)"
```

启动成功后默认监听 `server.port=18080`（见 `ecshopx-bootstrap/src/main/resources/application.properties`），访问：

* API 接口：http://localhost:18080/api/v1/
* 静态资源：http://localhost:18080/storage/
* 微信回调：http://localhost:18080/wechatAuth/

### 4. 数据库初始化
本仓库 `docker/ecshopx.sql` 已包含业务表结构与一份演示数据：
- **Docker Compose 模式**：首次 `docker compose -f docker-compose.dev.yml up -d` 时由 MySQL 容器自动导入，无需手工操作。
- **本地 MySQL 模式**：
  ```bash
  mysql -h 127.0.0.1 -P 3306 -uroot -p \
    -e "CREATE DATABASE IF NOT EXISTS ecshopx DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
  mysql -h 127.0.0.1 -P 3306 -uroot -p ecshopx < docker/ecshopx.sql
  ```
  XXL-Job 调度库则导入 `docker/tables_xxl_job.sql`（自带 `CREATE/USE xxl_job`）。

默认管理员账号沿用 PHP 版：

> 用户名：`admin`  
> 密码：`Shopex123`

### 5. NGINX 反向代理（可选）
若需以统一域名对外暴露 API 与前端，可参PHP版本考根目录readme_cn.md 中的 NGINX 模板，将 `proxy_pass` 指向 `http://localhost:18080`。

## 常用开发命令

### Maven
```bash
# 单模块构建（带依赖）
./mvnw -pl ecshopx-orders -am clean install -DskipTests

# 仅跑单 Bundle 的测试
./mvnw -pl ecshopx-orders test

# 运行单元测试（按类）
./mvnw -pl ecshopx-orders test -Dtest=OrderXxxTest

# 查看依赖树（排查冲突）
./mvnw -pl ecshopx-bootstrap dependency:tree

# 跳过测试 + 编译并打可执行 jar
./mvnw -pl ecshopx-bootstrap -am package -DskipTests
```

### Docker Compose
```bash
# 全量开发栈
docker compose -f docker-compose.dev.yml up -d --build
docker compose -f docker-compose.dev.yml ps
docker compose -f docker-compose.dev.yml logs -f ecshopx-app
docker compose -f docker-compose.dev.yml up -d --build ecshopx-app
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.dev.yml down -v
```

## 注意事项
* 首次构建需 5–15 分钟（拉镜像 + 编译 50+ 模块 + 导入 SQL）；后续基于 Maven 本地缓存（Dockerfile 已声明 `--mount=type=cache` 缓存层）显著加速。
* 端口冲突：确认 `18080`（应用 HTTP）、`19999`（XXL-Job Executor）、`8080`（XXL-Job Admin）、`3306`（MySQL）、`6379`（Redis）未被占用；一体化栈还需宿主机 HTTP `80`（可用 `--http-port` 改）。
* `docker compose down` 默认保留 `ecshopx-mysql-data` / `ecshopx-redis-data` / `ecshopx-app-logs` 三个命名卷，下次启动可继续使用；若需重新导入 SQL 请用 `docker compose down -v`。
* 多模块下 Mapper 同名问题已由 `FqcnMapperBeanNameGenerator` 解决，新增 Mapper 时直接使用 `cn.shopex.ecshopx.**.mapper` 包路径即可。
* `application-local.properties` 与 compose 中的密码（`rootpassword` / `redispassword` / `JWT_SECRET` 等）仅供本地开发使用，**生产环境务必通过环境变量或外部配置中心注入真实密钥**，不要直接复用仓库内的默认值。

## 许可证
本项目采用 Apache-2.0 开源许可证。  
每个包含在本发行版中的 ECShopX-Java 源文件，均依据 Apache 2.0 开源许可证进行授权。

开源软件许可协议（Apache 2.0） —— 请参阅根目录 `LICENSE.txt` 以获取 Apache 2.0 协议的完整文本。

## 贡献
我们欢迎所有形式的贡献！  
请阅读根目录 [`CONTRIBUTING.md`](CONTRIBUTING.md) 了解如何参与；提交 Java 侧变更前请：
1. 在 `docs/migration/` 下保留分析 / 计划 / 测试工件（如涉及 PHP → Java 迁移）；
2. `./mvnw -pl <module> -am verify` 通过；
3. 遵循本仓库现有的包结构与分层约定（`controller` / `service` / `mapper` / `integration` / `port`）。

## 支持
* 📖 文档：请首先查阅 [官方文档](https://doc.shopex.cn/ecshopx/docs/readme.html)
* 🐛 问题反馈：请在 [Issues] 中提交

## 💬 开源社群
欢迎扫码加入 ECShopX-Java 开源交流社群，获取更新动态、使用交流与问题互助。
<p align="center"><img width="300" height="auto" alt="workwechat" src="workwechat.png" /></p>

## 致谢
感谢所有为 ECShopX-Java 做出贡献的开发者、用户以及商派背后的全球品牌客户们！
