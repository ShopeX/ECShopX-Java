# 技术栈

**分析日期:** 2026-07-08

## 编程语言

**主要语言:**
- Java 17 - 所有应用代码，涵盖 50+ 模块

**辅助语言:**
- XML - MyBatis mapper 文件 (`classpath*:mapper/**/*.xml`)、Maven POM 文件
- Properties - Spring Boot 配置文件 (`application.properties`)

## 运行时环境

**环境:**
- JDK 17（通过根 `pom.xml` 中的 `<java.version>17</java.version>` 指定）
- Undertow 内嵌服务器（在 `ecshopx-bootstrap/pom.xml` 中排除了 Tomcat）

**包管理器:**
- Apache Maven（多模块 reactor 构建）
- 锁文件：不适用（Maven 通过 `pom.xml` 进行版本固定）

## 框架

**核心框架:**
- Spring Boot 3.5.12 - 应用框架（父 POM: `org.springframework.boot:spring-boot-starter-parent:3.5.12`）
- Spring MVC - REST API 层 (`spring-boot-starter-web`)
- Spring Security - 认证授权，用于 `ecshopx-espier`、`ecshopx-companys` 模块
- Spring AOP - 横切关注点 (`spring-boot-starter-aop`，位于 `ecshopx-common`)
- Spring Validation - Bean 校验 (`spring-boot-starter-validation`)
- Spring Data Redis - 缓存/会话/消息分发 (`spring-boot-starter-data-redis`)
- Spring AMQP - RabbitMQ 消息通信 (`spring-boot-starter-amqp`，位于 bootstrap/dispatch)
- Spring Mail - 邮件发送 (`spring-boot-starter-mail`，位于 orders/companys)

**ORM/持久化:**
- MyBatis-Plus 3.5.16 - ORM 层 (`mybatis-plus-spring-boot3-starter`)
- MyBatis-Plus JSqlParser - SQL 解析/分页 (`mybatis-plus-jsqlparser`)

**测试:**
- Spring Boot Test - 集成测试 (`spring-boot-starter-test`)
- H2 Database - 内存测试数据库（test scope，位于 `ecshopx-goods`、`ecshopx-members`、`ecshopx-third-party`）

**构建/开发:**
- Maven（reactor，多模块）
- Spring Boot Maven Plugin - Fat JAR 打包（仅在 `ecshopx-bootstrap` 中启用）
- Lombok 1.18.36 - 减少样板代码（provided scope，所有模块）

## 关键依赖

**核心依赖:**
- `com.github.binarywang:weixin-java-*` 4.7.6 - 微信生态（小程序、公众号、开放平台、企业微信、支付）
- `com.alipay.sdk:alipay-sdk-java` 4.39.134.ALL - 支付宝支付
- `com.alipay.sdk:alipay-easysdk` 2.2.3 - 支付宝简化 SDK (`ecshopx-ali`)
- `io.jsonwebtoken:jjwt-*` 0.12.6 - JWT 令牌生成/验证
- `com.nimbusds:nimbus-jose-jwt` 9.37.3 - JWT（用于 `ecshopx-members`）
- `com.xuxueli:xxl-job-core` 2.5.0 - 分布式任务调度

**支付 SDK:**
- `com.huifu.adapay:adapay-java-sdk` 1.2.10 - AdaPay（汇付斗拱）支付 (`ecshopx-payment`)
- `com.huifu.bspay.sdk:dg-java-sdk` 3.0.26 - BSPay（汇付）支付 (`ecshopx-payment`)
- `org.bouncycastle:bcpkix-jdk18on` 1.79 - 汇付支付签名加密 (`ecshopx-hfpay`)

**文件存储:**
- `software.amazon.awssdk:s3` 2.25.27 - AWS S3 兼容存储 (`ecshopx-espier`)
- `com.aliyun.oss:aliyun-sdk-oss` 3.17.4 - 阿里云 OSS (`ecshopx-espier`)
- `com.qiniu:qiniu-java-sdk` 7.15.1 - 七牛云存储 (`ecshopx-espier`)
- `com.qcloud:cos_api` 5.6.227 - 腾讯云 COS 存储 (`ecshopx-espier`)

**短信:**
- `com.aliyun:dysmsapi20170525` 4.1.1 - 阿里云短信 (`ecshopx-aliyunsms`)

**工具类:**
- `com.google.zxing:core` + `javase` 3.5.3 - 二维码生成 (`ecshopx-goods`、`ecshopx-members`)
- `org.apache.poi:poi-ooxml` 5.2.5 - Excel 导入/导出 (`ecshopx-espier`)
- `org.apache.commons:commons-compress` 1.26.2 - 压缩包处理 (`ecshopx-espier`)
- `commons-codec:commons-codec` - 哈希/编码 (`ecshopx-common`)
- `org.hashids:hashids` 1.0.3 - 短 ID 生成 (`ecshopx-wechat`)
- `com.belerweb:pinyin4j` 2.5.1 - 中文拼音转换 (`ecshopx-distribution`)
- `com.fasterxml.jackson.core:jackson-databind` - JSON 序列化（Spring 托管版本）

**基础设施:**
- `com.mysql:mysql-connector-j` - MySQL JDBC 驱动（runtime scope，仅 bootstrap）

## 配置

**环境配置:**
- 主配置文件: `ecshopx-bootstrap/src/main/resources/application.properties`
- 基于 Profile 的覆盖: `application-local.properties`（本地开发）
- 导入的配置: `application-request-field.yml`、`application-bspay.yml`
- 环境变量注入模式: 配置文件中使用 `${ENV_VAR:default}`
- 多租户（SaaS）模式通过 `common.system-is-saas` 切换

**主要配置区域:**
- 数据源: MySQL (`spring.datasource.*`)
- Redis: 多个逻辑数据库（db 0 默认、db 1 deposit、db 2 prism）
- JWT: Base64 编码的 32 字节密钥 (`JWT_SECRET`)，可配置过期时间
- 文件存储: 基于驱动 (`local` / `oss` / `qiniu` / `aws` / `cosv5`)
- XXL-JOB: 执行器配置，用于分布式调度
- 微信开放平台: 第三方平台 appid/secret
- 支付回调 URL: 微信和支付宝回调端点

**构建:**
- 根 `pom.xml` - 父/BOM，依赖管理，模块列表
- `ecshopx-bootstrap/pom.xml` - 可执行 JAR 组装（Spring Boot 插件 `skip=false`）
- 所有其他模块: `spring-boot-maven-plugin` 跳过（仅生成库 JAR）
- Maven 仓库: 阿里云公共镜像 (`https://maven.aliyun.com/repository/public`)

## 数据库

**主数据库:**
- MySQL（通过 `com.mysql:mysql-connector-j`）
- 连接: `jdbc:mysql://...?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai`
- ORM: MyBatis-Plus，支持下划线转驼峰映射

**缓存/消息总线:**
- Redis（通过 `spring-boot-starter-data-redis`）
- 按业务域划分多个逻辑数据库（companys=0、deposit=1、prism=2、datacube=0）

**消息队列:**
- RabbitMQ（通过 `spring-boot-starter-amqp`）
- 自定义消息分发总线，支持 Redis 和 RabbitMQ 驱动 (`ecshopx-dispatch`)

**搜索引擎:**
- Elasticsearch: 未检测到

## 异步/任务调度

**消息分发总线:**
- 自定义事件分发系统（`ecshopx-dispatch` 模块）
- 三种驱动模式: SYNC、REDIS、RABBITMQ (`DispatchDriverType.java`)
- Redis 驱动: 支持延迟分发，使用有序集合轮询
- RabbitMQ 驱动: 基于 Topic 的交换机，自动创建拓扑
- 支持重试策略、扇出规划、失败任务记录

**定时任务:**
- XXL-JOB 2.5.0（分布式定时任务）
- 管理后台: `http://127.0.0.1:9080/xxl-job-admin`
- 执行器端口: 19999

## 平台要求

**开发环境:**
- JDK 17+
- MySQL 5.7+ / 8.x
- Redis 6+
- RabbitMQ（可选，可使用 SYNC/REDIS 分发驱动替代）
- XXL-JOB Admin（可选，用于定时任务）

**生产环境:**
- 基于 Undertow 的 Fat JAR（单一可部署包）
- 端口: 18080（可配置）
- 所有 50+ 模块组装为一个 bootstrap 应用

---
*技术栈分析: 2026-07-08*
