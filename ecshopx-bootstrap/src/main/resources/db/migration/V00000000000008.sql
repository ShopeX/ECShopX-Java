-- Flyway migration: V00000000000008
-- Created at: 2026-11-13T07:51:06.780Z
-- Release: 5.6.0

-- Flyway migration: release
-- Created at: 2026-11-13 15:51:06 +0800

CREATE TABLE `goods_search_synonyms` (
  `id` bigint AUTO_INCREMENT NOT NULL COMMENT 'id',
  `company_id` bigint NOT NULL COMMENT '公司id',
  `group_key` varchar(64) NOT NULL COMMENT '同义词组键',
  `words` longtext NOT NULL COMMENT '同义词 JSON 数组',
  `created` bigint NOT NULL,
  `updated` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_company_id` (`company_id`),
  UNIQUE KEY `uk_company_group_key` (`company_id`, `group_key`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '商品搜索同义词';

CREATE TABLE `member_task` (
  `task_id` bigint AUTO_INCREMENT NOT NULL COMMENT '会员任务ID',
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `name` varchar(100) NOT NULL COMMENT '任务名称',
  `type_category` varchar(32) NOT NULL COMMENT '任务一级类型码',
  `type_detail` varchar(32) NOT NULL COMMENT '任务二级类型码',
  `begin_time` int NOT NULL DEFAULT 0 COMMENT '开始时间',
  `end_time` int NOT NULL DEFAULT 0 COMMENT '结束时间',
  `published` int NOT NULL DEFAULT 0 COMMENT '是否已发布',
  `off_shelf` int NOT NULL DEFAULT 0 COMMENT '是否下架',
  `frequency_type` varchar(16) NOT NULL COMMENT '频次',
  `frequency_period` varchar(16) DEFAULT NULL COMMENT '周期',
  `audience_mode` varchar(16) NOT NULL COMMENT '人群模式',
  `audience_tag_ids` longtext DEFAULT NULL COMMENT '人群标签',
  `audience_tag_combine` varchar(16) DEFAULT NULL,
  `task_rule` longtext NOT NULL COMMENT '任务规则',
  `reward_config` longtext NOT NULL COMMENT '奖励配置 JSON',
  `rule_config` longtext DEFAULT NULL COMMENT '类型规则 JSON',
  `jump_page` varchar(512) DEFAULT NULL COMMENT '跳转',
  `config_version` bigint NOT NULL DEFAULT 1 COMMENT '乐观锁',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  KEY `idx_member_task_company` (`company_id`, `task_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员任务活动';

CREATE TABLE `member_task_completion` (
  `completion_id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `cycle_key` varchar(32) NOT NULL,
  `completed_at` int NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `invitee_user_id` bigint NOT NULL DEFAULT 0,
  `voided` int NOT NULL DEFAULT 0,
  `voided_at` int NOT NULL DEFAULT 0,
  `void_reason` varchar(64) DEFAULT NULL,
  `created` int NOT NULL DEFAULT 0,
  `updated` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`completion_id`),
  KEY `idx_mtc_task_user_cycle` (`task_id`, `user_id`, `cycle_key`, `voided`),
  KEY `idx_mtc_task_completed` (`task_id`, `voided`, `completed_at`),
  KEY `idx_mtc_task_order` (`task_id`, `order_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员任务完成记录';

CREATE TABLE `member_task_reward_grant` (
  `grant_id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `completion_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` varchar(16) NOT NULL,
  `error_message` varchar(512) DEFAULT NULL,
  `reward_snapshot` longtext DEFAULT NULL,
  `granted_at` int NOT NULL DEFAULT 0,
  `retry_count` int NOT NULL DEFAULT 0,
  `created` int NOT NULL DEFAULT 0,
  `updated` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`grant_id`),
  KEY `idx_mtrg_task_status` (`task_id`, `status`),
  UNIQUE KEY `uk_mtrg_completion` (`completion_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员任务发奖流水';

CREATE TABLE `member_task_user_progress` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `amount_fen` bigint NOT NULL DEFAULT 0,
  `updated` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mtup_task_user` (`task_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员任务累计进度';

ALTER TABLE `orders_normal_orders`
  ADD COLUMN `member_task_id` bigint NOT NULL DEFAULT 0 COMMENT '会员任务归因',
  ADD COLUMN `member_task_sharer_user_id` bigint NOT NULL DEFAULT 0 COMMENT '会员任务分享人';
