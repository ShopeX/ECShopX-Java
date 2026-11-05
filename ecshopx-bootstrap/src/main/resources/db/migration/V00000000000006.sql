-- Flyway migration: V00000000000006
-- Created at: 2026-11-05T10:26:28.505Z
-- Release: 5.4.7

-- Flyway migration: release
-- Created at: 2026-11-05 18:26:28 +0800

CREATE TABLE `members_rel_tags_audit` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司id',
  `user_id` bigint NOT NULL COMMENT '会员id',
  `tag_id` bigint NOT NULL COMMENT '标签id',
  `action` varchar(16) NOT NULL COMMENT 'attach|detach',
  `source` varchar(32) NOT NULL COMMENT '来源',
  `rule_version` int NOT NULL DEFAULT 1 COMMENT '规则版本',
  `reason` varchar(255) DEFAULT NULL COMMENT '原因',
  `event_id` varchar(64) DEFAULT NULL COMMENT '事件或任务id',
  `created` bigint NOT NULL COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_company_user_tag` (`company_id`, `user_id`, `tag_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员标签关系审计';

ALTER TABLE `members_rel_tags`
  ADD COLUMN `source` varchar(32) NOT NULL DEFAULT 'manual' COMMENT '关系来源',
  ADD COLUMN `rule_version` int NOT NULL DEFAULT 1 COMMENT '规则版本',
  ADD COLUMN `effective_at` bigint NOT NULL DEFAULT 0 COMMENT '生效时间',
  ADD COLUMN `updated_at` bigint NOT NULL DEFAULT 0 COMMENT '更新时间';

ALTER TABLE `members_tags`
  ADD COLUMN `tag_code` varchar(64) DEFAULT NULL COMMENT '标签编码，系统标签必填',
  ADD COLUMN `tag_kind` varchar(16) NOT NULL DEFAULT 'manual' COMMENT '标签种类 manual|system',
  ADD COLUMN `rule_version` int NOT NULL DEFAULT 1 COMMENT '规则版本',
  ADD UNIQUE KEY `uk_company_tag_code` (`company_id`, `tag_code`);

ALTER TABLE `promotions_marketing_activity`
  ADD COLUMN `member_tag_ids` longtext DEFAULT NULL COMMENT '会员人群标签ID JSON数组',
  ADD COLUMN `member_tag_mode` varchar(16) DEFAULT NULL COMMENT 'include适用 exclude不适用';

ALTER TABLE `promotions_seckill_activity`
  ADD COLUMN `member_tag_ids` longtext DEFAULT NULL COMMENT '会员人群标签ID JSON数组',
  ADD COLUMN `member_tag_mode` varchar(16) DEFAULT NULL COMMENT 'include适用 exclude不适用';
