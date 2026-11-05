# 外部集成

**分析日期:** 2026-07-08

## API 与外部服务

**微信生态:**
- 微信小程序 - 用户登录、订阅消息、模板管理
  - SDK: `com.github.binarywang:weixin-java-miniapp` 4.7.6
  - 模块: `ecshopx-wechat/`
  - 认证: `ecshopx.wechat.open.component-appid`, `ecshopx.wechat.open.component-appsecret`
- 微信公众号 - 消息推送、用户管理
  - SDK: `com.github.binarywang:weixin-java-mp` 4.7.6
  - 模块: `ecshopx-wechat/`
- 微信开放平台（第三方平台） - 第三方授权、小程序发布
  - SDK: `com.github.binarywang:weixin-java-open` 4.7.6
  - 模块: `ecshopx-wechat/`
  - 配置: `ecshopx.wechat.open.*` 属性
- 微信支付 - 支付、退款
  - SDK: `com.github.binarywang:weixin-java-pay` 4.7.6
  - 模块: `ecshopx-orders/`
  - 回调地址: `ecshopx.payment.wechat.notify-url`
- 企业微信 - 企业消息推送、通讯录
  - SDK: `com.github.binarywang:weixin-java-cp` 4.7.6
  - 模块: `ecshopx-work-wechat/`, `ecshopx-third-party/`
  - 配置: `ecshopx.work-wechat.dis-workwechat-h5-base-uri`

**支付宝生态:**
- 支付宝支付 - 支付、退款、生命周期管理
  - SDK: `com.alipay.sdk:alipay-sdk-java` 4.39.134.ALL
  - 模块: `ecshopx-ali/`, `ecshopx-payment/`
  - 回调地址: `ecshopx.payment.alipay.notify-url`
- Alipay Easy SDK - 简化支付集成
  - SDK: `com.alipay.sdk:alipay-easysdk` 2.2.3
  - 模块: `ecshopx-ali/`

**聚合支付:**
- AdaPay（汇付斗拱） - 聚合支付、商户进件
  - SDK: `com.huifu.adapay:adapay-java-sdk` 1.2.10
  - 模块: `ecshopx-adapay/`, `ecshopx-payment/`
- BSPay（汇付天下） - 银行级支付
  - SDK: `com.huifu.bspay.sdk:dg-java-sdk` 3.0.26
  - 模块: `ecshopx-bspay/`, `ecshopx-payment/`
  - 配置: `application-bspay.yml`
- HFPay（汇付） - 支付处理、分销商提现
  - 模块: `ecshopx-hfpay/`
  - 加密: BouncyCastle (`bcpkix-jdk18on` 1.79) 用于签名验证
  - 配置: `ecshopx.payment.hfpay-is-open`
- 银联商务 - 银联商户服务
  - 模块: `ecshopx-chinaums-pay/`
  - 配置: `ecshopx.ums.notify-md5-key`（SHA256 签名验证）

**短信服务商:**
- 阿里云短信
  - SDK: `com.aliyun:dysmsapi20170525` 4.1.1
  - 模块: `ecshopx-aliyunsms/`
- 商派短信网关
  - 自定义 HTTP 集成
  - 配置: `ecshopx.operator.sms.shopex.*` (api-url, ent-id, license, secret)

**地图服务:**
- 高德地图 - 轨迹追踪 API、地理围栏
  - 自定义 HTTP 客户端
  - 配置: `ecshopx.map.amap.track-base-uri=https://tsapi.amap.com`
  - 认证: `ecshopx.map.amap.app-key`, `ecshopx.map.amap.app-secret`

**同城配送:**
- 达达 - 同城配送
  - 配置: `common.local-delivery-dirver=dada`
- 闪送 - 同城配送
  - 配置: `common.local-delivery-dirver=shansong`

**ERP / 订单管理:**
- 聚水潭 - ERP 集成、订单/库存同步
  - 模块: `ecshopx-system-link/`
  - 配置: `ecshopx.jushuitan.app-key`, `ecshopx.jushuitan.api-base-url`, `ecshopx.jushuitan.oauth-base-url`
- OME / Shopex ERP - OpenAPI 网关
  - 配置: `ecshopx.ome.openapi-url`

**商派平台服务:**
- Shopex 用户中心 - 协议获取、文档、补丁
  - 配置: `espier.shopex-usercenter.base-uri=https://www.shopex.cn`
  - 路径: `/api/usercenter/open/ecx/agreement`, `/api/usercenter/open/ecx/docs` 等
- Prism Core - OAuth 令牌交换、证书签名
  - 配置: `ecshopx.thirdparty.prism-core.base-url`, `app-key`, `app-secret`
- Prism Ishopex - 线索管理、云起账户验证、在线激活
  - 配置: `ecshopx.thirdparty.prism-ishopex.base-url`, `app-key`, `app-secret`
- Shopex CRM - 单会员同步
  - 配置: `ecshopx.thirdparty.shopex-crm.sync-single-member.*`

**第三方 CRM / 营销:**
- 数云 - CRM 用户信息、OEM 回调、会员修改/解绑
  - 模块: `ecshopx-shuyun/`
  - 配置: `ecshopx.thirdparty.shuyun.base-url`, `app-key`, `app-secret`
  - 功能开关: `ecshopx.thirdparty.shuyun.member-modify.http-enabled`
- 达摩 CRM / Hope - 会员积分明细、会员信息更新
  - 配置: `ecshopx.thirdparty.dm-crm.point-detail-list.http-enabled`
  - 基于 Redis 的配置: `DmCrmSetting:{sha1(companyId)}`

**家居设计:**
- 酷家乐 - 3D 家居设计集成
  - 模块: `ecshopx-kujiale/`

**数据分析:**
- 有数 - 数据分析/埋点追踪
  - 模块: `ecshopx-youshu/`

**直播:**
- 微信直播组件 - 小程序直播电商
  - 配置: `ecshopx.wechat.live-player-plugin.version=1.3.5`

## 数据存储

**数据库:**
- MySQL（主关系型存储）
  - 连接: `spring.datasource.url`（通过环境变量注入）
  - 驱动: `com.mysql.cj.jdbc.Driver`
  - 客户端: MyBatis-Plus 3.5.16
  - Mapper 路径: `classpath*:mapper/**/*.xml`

**文件存储:**
- `ecshopx-espier/` 模块中的多驱动抽象
  - 驱动选择: `ecshopx.storage.driver` (local/oss/qiniu/aws/cosv5)
  - 本地: 文件系统 (`storage/app/public`)
  - 阿里云 OSS: `com.aliyun.oss:aliyun-sdk-oss` 3.17.4
  - 七牛: `com.qiniu:qiniu-java-sdk` 7.15.1
  - AWS S3: `software.amazon.awssdk:s3` 2.25.27
  - 腾讯 COS: `com.qcloud:cos_api` 5.6.227
- 导入/导出对象存储: 独立的 S3 兼容配置 (`ecshopx.storage.import-file.*`)

**缓存:**
- Redis (Spring Data Redis)
  - 按业务域划分多个逻辑数据库
  - 用途: 会话、JWT 黑名单、调度队列、限流、配置缓存

## 认证与身份

**管理员/运营者认证:**
- Spring Security + JWT (jjwt 0.12.6)
- 模块: `ecshopx-espier/`（安全过滤器链）
- JWT 密钥: Base64 编码的 32 字节密钥
- 有效期: 可配置（默认 120 分钟），刷新窗口（默认 20160 分钟）
- 令牌撤销的黑名单宽限期

**H5 / 小程序认证:**
- 基于 JWT（使用相同的 jjwt 库）
- 商户小程序 JWT: 独立的签发者/密钥/有效期配置
- 模块: `ecshopx-members/` 使用 `nimbus-jose-jwt` 9.37.3

**微信 OAuth:**
- 通过 WxJava SDK（开放平台第三方授权流程）
- 模块: `ecshopx-wechat/`

**OpenAPI 认证:**
- 基于令牌 (`openapi.common-token`)
- 签名验证（可配置调试模式: `openapi.debug=0`）
- 应用密钥生成加盐: `common.openapi-rand-salt`

**SaaS 证书中心:**
- 从 `common.certi-base-url` 进行 OAuth + 证书拉取
- 门店密钥认证: `common.store-key`

## 监控与可观测性

**错误追踪:**
- 未检测到（无 Sentry/Bugsnag 等依赖）

**日志:**
- MyBatis SQL 日志: `StdOutImpl`（开发环境标准输出）
- XXL-JOB 执行日志: `logs/xxl-job`（7 天保留）
- 调度结构化日志: `DispatchStructuredLogger.java`
- 标准 Spring Boot 日志（Logback 可用: 存在 `logback-spring.xml`）

## CI/CD 与部署

**部署方式:**
- 单一 fat JAR 部署（所有模块打包在一个 `ecshopx-bootstrap` 构件中）
- 端口: 18080

**CI 流水线:**
- 仓库中未检测到（可能为外部管理）

**任务调度:**
- XXL-JOB Admin: 独立部署 (`http://127.0.0.1:9080/xxl-job-admin`)
- 执行器: `ecshopx-executor`，端口 19999

## 消息队列 / 事件总线

**自定义调度系统:**
- 模块: `ecshopx-dispatch/`
- 门面: `DispatchFacade.java` - 统一发布 API
- 驱动:
  - SYNC: 进程内同线程执行 (`SyncDispatchDriver.java`)
  - REDIS: 基于 Redis List/Stream，通过有序集合实现延迟调度 (`redis/RedisDispatchDriver.java`)
  - RABBITMQ: Spring AMQP，自动创建拓扑 (`rabbit/RabbitDispatchDriver.java`)
- 功能: 重试策略、扇出规划、失败任务记录到数据库
- 消费者运行时: `DispatchConsumerRuntime.java`
- 集成事件: `HfpayRefundSuccessEvent`, `HfpayDistributorWithdrawSuccessEvent`, `OrderProcessLog`

**RabbitMQ:**
- Spring Boot AMQP starter 在 bootstrap 和 dispatch 模块中引入
- 作为调度总线的后端之一使用

## 环境配置

**必需的环境变量（生产环境关键配置）:**
- `spring.datasource.url` / `username` / `password` - MySQL 连接
- `spring.data.redis.host` / `port` / `password` - Redis 连接
- `JWT_SECRET` - 令牌签名密钥
- `ecshopx.wechat.open.component-appid` / `component-appsecret` - 微信平台
- `ecshopx.storage.driver` + 对应驱动凭证
- RabbitMQ 连接（Spring AMQP 自动配置: `spring.rabbitmq.*`）

**密钥存储位置:**
- 环境变量注入（`${ENV_VAR:default}` 模式）
- `.env` 文件在 `.gitignore` 中声明（不会提交到仓库）
- 按部署覆盖，通过 `application-local.properties`

## Webhook 与回调

**入站（本系统暴露的回调地址）:**
- 微信支付回调: `ecshopx.payment.wechat.notify-url`（如 `/wechatAuth/wxpay/notify`）
- 支付宝回调: `ecshopx.payment.alipay.notify-url`（如 `/api/alipay/notify`）
- 银联商务回调: 通过 `ecshopx.ums.notify-md5-key` 进行 SHA256 签名验证
- 微信开放平台: 第三方平台验证票据回调
- 聚水潭 OAuth 回调: `/api/systemlink/jushuitan/{companyId}`
- OpenAPI 外部回调: `common.external-baseuri`

**出站（本系统调用的外部 Webhook）:**
- Shopex CRM 单会员同步: POST 到可配置端点
- 数云会员修改/解绑: 启用时进行 HTTP 调用
- 达摩 CRM 积分明细/会员更新: 启用时进行 HTTP 调用
- Prism Ishopex: 在线激活回调

## 内部模块依赖

**基础层（被大多数模块依赖）:**
- `ecshopx-common` - 基类、响应封装、分页、AOP、MyBatis-Plus 配置
- `ecshopx-dispatch` - 异步事件总线（Redis/RabbitMQ/Sync 驱动）

**平台层:**
- `ecshopx-super-admin` - 超级管理员/平台级配置
- `ecshopx-companys` - 多租户公司管理
- `ecshopx-merchant` - 商户/门店管理
- `ecshopx-espier` - 安全（JWT）、文件存储、Excel 导入导出

**业务领域模块:**
- `ecshopx-members` - 会员/用户管理（依赖: companys, wechat, work-wechat, third-party, dispatch）
- `ecshopx-goods` - 商品目录（依赖: companys, wechat, promotions, orders, members, distribution, espier, merchant, supplier, cross-border, tdkset, popularize, salesperson）
- `ecshopx-orders` - 订单管理（依赖: dispatch, work-wechat, third-party, supplier, reservation, espier, members, companys, point, aftersales, payment, chinaums-pay, deposit, hfpay, popularize）
- `ecshopx-payment` - 支付编排（依赖: common, chinaums-pay, point）
- `ecshopx-distribution` - 分销/佣金（依赖: companys, third-party, espier, wechat, members, orders, popularize, hfpay, aftersales, payment, merchant）
- `ecshopx-data-cube` - 数据分析/报表（依赖: companys, employee-purchase, distribution, orders, aftersales, wechat, espier, goods, members, merchant）

**集成模块:**
- `ecshopx-wechat` - 微信小程序/公众号/开放平台
- `ecshopx-work-wechat` - 企业微信
- `ecshopx-ali` - 支付宝小程序和支付
- `ecshopx-aliyunsms` - 阿里云短信
- `ecshopx-third-party` - 第三方网关（Prism、数云、Shopex CRM、达摩 CRM）
- `ecshopx-system-link` - ERP 连接器（聚水潭）
- `ecshopx-openapi` - OpenAPI 接口层

**支付模块:**
- `ecshopx-adapay` - AdaPay 集成
- `ecshopx-bspay` - BSPay 集成
- `ecshopx-hfpay` - HFPay 集成
- `ecshopx-chinaums-pay` - 银联商务集成

**专业模块:**
- `ecshopx-promotions` - 促销引擎
- `ecshopx-aftersales` - 售后/退货
- `ecshopx-deposit` - 储值/充值
- `ecshopx-point` / `ecshopx-pointsmall` - 积分系统
- `ecshopx-kaquan` - 卡券/优惠券
- `ecshopx-reservation` - 预约管理
- `ecshopx-community` - 社区功能
- `ecshopx-popularize` - 营销/推广工具
- `ecshopx-salesperson` - 导购员管理
- `ecshopx-cross-border` - 跨境电商
- `ecshopx-shopex-ai` - AI 功能
- `ecshopx-im` - 即时通讯
- `ecshopx-form` - 自定义表单
- `ecshopx-theme` - 店铺主题
- `ecshopx-tdkset` - SEO（标题/描述/关键词）
- `ecshopx-tb-items` - 淘宝商品导入
- `ecshopx-wsugc` - UGC 内容
- `ecshopx-employee-purchase` - 内购
- `ecshopx-selfservice` - 自助服务功能
- `ecshopx-one-code` - 一码通功能
- `ecshopx-shopmenu-border` - 店铺菜单/边框
- `ecshopx-kujiale` - 酷家乐 3D 设计
- `ecshopx-youshu` - 有数分析
- `ecshopx-shuyun` - 数云 CRM

---
*集成审计: 2026-07-08*
