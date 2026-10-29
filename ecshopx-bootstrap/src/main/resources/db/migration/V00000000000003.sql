-- Flyway migration: V00000000000003
-- Created at: 2026-10-29 09:40:02 +0800

ALTER TABLE `distribution_distributor`
  ADD COLUMN `is_platform_store_buy` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启云仓可购买（店务立即购买等）';

ALTER TABLE `orders_normal_orders`
  ADD COLUMN `uses_platform_item_stock` smallint NOT NULL DEFAULT 0 COMMENT '是否云仓/平台SKU库存履约（店务立即购买等）';

CREATE TABLE `item_multi_lang_mod_lang_zhTW` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `company_id` bigint NOT NULL COMMENT '公司id',
  `data_id` bigint NOT NULL DEFAULT '0' COMMENT '业务id字段',
  `table_name` varchar(255) NOT NULL DEFAULT '' COMMENT '表名',
  `field` varchar(255) NOT NULL DEFAULT '' COMMENT 'field,多语言对应字段',
  `module_name` varchar(255) NOT NULL DEFAULT '' COMMENT 'module_name,模块名',
  `lang` varchar(255) NOT NULL DEFAULT '' COMMENT '语言',
  `attribute_value` text NOT NULL COMMENT '多语言值',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多语言字典库';

CREATE TABLE `item_multi_lang_mod_lang_jaJP` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `company_id` bigint NOT NULL COMMENT '公司id',
  `data_id` bigint NOT NULL DEFAULT '0' COMMENT '业务id字段',
  `table_name` varchar(255) NOT NULL DEFAULT '' COMMENT '表名',
  `field` varchar(255) NOT NULL DEFAULT '' COMMENT 'field,多语言对应字段',
  `module_name` varchar(255) NOT NULL DEFAULT '' COMMENT 'module_name,模块名',
  `lang` varchar(255) NOT NULL DEFAULT '' COMMENT '语言',
  `attribute_value` text NOT NULL COMMENT '多语言值',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多语言字典库';

CREATE TABLE `item_multi_lang_mod_lang_ruRU` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `company_id` bigint NOT NULL COMMENT '公司id',
  `data_id` bigint NOT NULL DEFAULT '0' COMMENT '业务id字段',
  `table_name` varchar(255) NOT NULL DEFAULT '' COMMENT '表名',
  `field` varchar(255) NOT NULL DEFAULT '' COMMENT 'field,多语言对应字段',
  `module_name` varchar(255) NOT NULL DEFAULT '' COMMENT 'module_name,模块名',
  `lang` varchar(255) NOT NULL DEFAULT '' COMMENT '语言',
  `attribute_value` text NOT NULL COMMENT '多语言值',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多语言字典库';

CREATE TABLE `outside_item_multi_lang_mod_lang_zhTW` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `company_id` bigint NOT NULL COMMENT '公司id',
  `data_id` bigint NOT NULL DEFAULT '0' COMMENT '业务id字段',
  `table_name` varchar(255) NOT NULL DEFAULT '' COMMENT '表名',
  `field` varchar(255) NOT NULL DEFAULT '' COMMENT 'field,多语言对应字段',
  `module_name` varchar(255) NOT NULL DEFAULT '' COMMENT 'module_name,模块名',
  `lang` varchar(255) NOT NULL DEFAULT '' COMMENT '语言',
  `attribute_value` text NOT NULL COMMENT '多语言值',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多语言字典库';

CREATE TABLE `outside_item_multi_lang_mod_lang_jaJP` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `company_id` bigint NOT NULL COMMENT '公司id',
  `data_id` bigint NOT NULL DEFAULT '0' COMMENT '业务id字段',
  `table_name` varchar(255) NOT NULL DEFAULT '' COMMENT '表名',
  `field` varchar(255) NOT NULL DEFAULT '' COMMENT 'field,多语言对应字段',
  `module_name` varchar(255) NOT NULL DEFAULT '' COMMENT 'module_name,模块名',
  `lang` varchar(255) NOT NULL DEFAULT '' COMMENT '语言',
  `attribute_value` text NOT NULL COMMENT '多语言值',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多语言字典库';

CREATE TABLE `outside_item_multi_lang_mod_lang_ruRU` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `company_id` bigint NOT NULL COMMENT '公司id',
  `data_id` bigint NOT NULL DEFAULT '0' COMMENT '业务id字段',
  `table_name` varchar(255) NOT NULL DEFAULT '' COMMENT '表名',
  `field` varchar(255) NOT NULL DEFAULT '' COMMENT 'field,多语言对应字段',
  `module_name` varchar(255) NOT NULL DEFAULT '' COMMENT 'module_name,模块名',
  `lang` varchar(255) NOT NULL DEFAULT '' COMMENT '语言',
  `attribute_value` text NOT NULL COMMENT '多语言值',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多语言字典库';
