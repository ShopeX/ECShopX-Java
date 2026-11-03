-- Flyway migration: V00000000000005
-- Created at: 2026-11-02T10:38:36.591Z
-- Release: 5.4.4

-- Flyway migration: release
-- Created at: 2026-11-02 18:38:36 +0800

CREATE TABLE `digios_api_log` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `direction` varchar(16) NOT NULL,
  `method` varchar(128) DEFAULT NULL,
  `http_status` smallint DEFAULT NULL,
  `outcome` varchar(32) DEFAULT NULL,
  `runtime_ms` int DEFAULT NULL,
  `request_body` longtext DEFAULT NULL,
  `response_body` longtext DEFAULT NULL,
  `error_message` varchar(1024) DEFAULT NULL,
  `created` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_digios_log_company_created` (`company_id`, `created`),
  KEY `idx_digios_log_method` (`method`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = 'DigiOS直连API日志';

CREATE TABLE `digios_company_config` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `is_enabled` smallint NOT NULL,
  `base_url` varchar(512) DEFAULT NULL,
  `app_key` varchar(128) DEFAULT NULL,
  `app_secret` varchar(512) DEFAULT NULL,
  `extra_json` longtext DEFAULT NULL,
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_digios_company_id` (`company_id`),
  UNIQUE KEY `uk_digios_app_key` (`app_key`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = 'DigiOS直连公司配置';

CREATE TABLE `digios_store_mapping` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `distributor_id` bigint NOT NULL,
  `shop_code` varchar(128) NOT NULL,
  `digios_store_id` bigint NOT NULL,
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = 'DigiOS门店档案ID映射';

CREATE TABLE `pos_store_account` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司id',
  `employee_no` varchar(30) NOT NULL COMMENT '员工号（全局唯一）',
  `name` varchar(30) NOT NULL COMMENT '姓名',
  `email` varchar(255) NOT NULL COMMENT '邮箱',
  `mobile` varchar(20) NOT NULL COMMENT '手机号',
  `role` varchar(32) NOT NULL COMMENT 'store_manager/supervisor/clerk',
  `distributor_id` bigint NOT NULL COMMENT '归属门店',
  `status` varchar(16) NOT NULL COMMENT 'enabled/disabled/resigned',
  `password_cipher` varchar(512) NOT NULL COMMENT '可逆密文',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_company_shop_status` (`company_id`, `distributor_id`, `status`),
  KEY `idx_company_role` (`company_id`, `role`),
  UNIQUE KEY `uk_employee_no` (`employee_no`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '门店POS账号';

ALTER TABLE `orders_normal_orders`
  ADD COLUMN `pos_store_account_id` bigint NOT NULL DEFAULT 0 COMMENT 'POS经手账号';

ALTER TABLE `supplier_items`
  ADD COLUMN `is_sync_platform` int NOT NULL DEFAULT 0 COMMENT '是否已同步到平台商品库：0未同步，1已同步';
