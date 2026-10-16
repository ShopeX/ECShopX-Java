-- Flyway migration: V00000000000002
-- Created at: 2026-10-16 17:46:12 +0800

CREATE TABLE `member_signin_activity` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `name` varchar(50) NOT NULL COMMENT '活动名称',
  `begin_time` bigint NOT NULL COMMENT '开始时间(秒级Unix)',
  `end_time` bigint NOT NULL COMMENT '结束时间(秒级Unix)',
  `config_version` bigint NOT NULL DEFAULT 1 COMMENT '配置乐观锁版本',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_member_signin_activity_company` (`company_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员签到累计活动';

CREATE TABLE `member_signin_activity_progress` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `progress_count` int NOT NULL DEFAULT 0 COMMENT '当前累计次数',
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_signin_activity_progress` (`company_id`, `user_id`, `activity_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员签到活动进度';

CREATE TABLE `member_signin_activity_tier` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `tier_count` int NOT NULL COMMENT '累计签到次数',
  `points_reward` int DEFAULT NULL COMMENT '积分奖励',
  `coupon_id` bigint DEFAULT NULL COMMENT '优惠券ID',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `ix_member_signin_activity_tier_company` (`company_id`),
  UNIQUE KEY `uk_member_signin_activity_tier` (`activity_id`, `tier_count`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员签到活动阶梯奖励';

CREATE TABLE `member_signin_daily_points` (
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `points_mon` int NOT NULL DEFAULT 0 COMMENT '周一积分',
  `points_tue` int NOT NULL DEFAULT 0 COMMENT '周二积分',
  `points_wed` int NOT NULL DEFAULT 0 COMMENT '周三积分',
  `points_thu` int NOT NULL DEFAULT 0 COMMENT '周四积分',
  `points_fri` int NOT NULL DEFAULT 0 COMMENT '周五积分',
  `points_sat` int NOT NULL DEFAULT 0 COMMENT '周六积分',
  `points_sun` int NOT NULL DEFAULT 0 COMMENT '周日积分',
  `rule_desc` longtext NOT NULL COMMENT '签到规则说明',
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`company_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员签到周一至周日日常积分配置';

CREATE TABLE `member_signin_record` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sign_date` date NOT NULL COMMENT '签到自然日',
  `weekday` int NOT NULL COMMENT '1=Mon … 7=Sun',
  `daily_points` int NOT NULL DEFAULT 0 COMMENT '实际发放日常积分',
  `activity_id` bigint DEFAULT NULL COMMENT '计入的活动ID',
  `activity_seq` int DEFAULT NULL COMMENT '活动内第几次签到',
  `created` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_member_signin_record_user` (`company_id`, `user_id`),
  UNIQUE KEY `uk_member_signin_record` (`company_id`, `user_id`, `sign_date`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员签到记录';

CREATE TABLE `member_signin_reminder` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户开关',
  `subscribe_accepted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否持有有效微信订阅授权',
  `subscribe_template_id` varchar(128) DEFAULT NULL COMMENT '最近一次授权模板ID',
  `last_push_date` date DEFAULT NULL COMMENT '最近一次推送自然日',
  `next_push_time` int DEFAULT NULL COMMENT '已排定的下一次推送时刻(epoch秒), NULL=未排定',
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_signin_reminder` (`company_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员签到漏签提醒';

CREATE TABLE `member_signin_reward_grant` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_type` varchar(16) NOT NULL COMMENT 'daily/tier',
  `activity_id` bigint DEFAULT NULL COMMENT '活动ID(tier必填)',
  `tier_count` int DEFAULT NULL COMMENT '阶梯累计次数(tier必填)',
  `sign_date` date DEFAULT NULL COMMENT '签到自然日(daily)',
  `title` varchar(64) DEFAULT NULL COMMENT 'C端展示标题',
  `status` varchar(16) NOT NULL COMMENT 'pending/granted/failed',
  `reward_snapshot` longtext DEFAULT NULL COMMENT '发放内容快照',
  `error_message` varchar(512) DEFAULT NULL COMMENT '失败原因',
  `signin_record_id` bigint DEFAULT NULL COMMENT '触发签到的记录ID',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_member_signin_reward_grant_user` (`company_id`, `user_id`, `activity_id`),
  UNIQUE KEY `uk_member_signin_reward_grant_daily` (`company_id`, `user_id`, `sign_date`, `record_type`),
  UNIQUE KEY `uk_member_signin_reward_grant_tier` (`company_id`, `user_id`, `activity_id`, `tier_count`, `record_type`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员签到领奖流水';
