-- Flyway migration: goods_search_synonyms
-- Created at: 2026-11-10 15:28:43 +0800

CREATE TABLE `goods_search_synonyms` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司id',
  `group_key` varchar(64) NOT NULL COMMENT '同义词组键',
  `words` longtext NOT NULL COMMENT '同义词 JSON 数组',
  `created` bigint NOT NULL COMMENT '创建时间',
  `updated` bigint NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_group_key` (`company_id`, `group_key`),
  KEY `ix_company_id` (`company_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '商品搜索同义词';
