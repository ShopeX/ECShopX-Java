-- Flyway migration: V00000000000009
-- Created at: 2026-11-27T09:15:40.195Z
-- Release: 5.8.0

-- Flyway migration: release
-- Created at: 2026-11-27 17:15:40 +0800

CREATE TABLE `search_hotword` (
  `id` bigint AUTO_INCREMENT NOT NULL COMMENT 'id',
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `content` varchar(100) NOT NULL COMMENT '热词',
  `type` varchar(10) NOT NULL DEFAULT 'text' COMMENT '内容形态 text/image',
  `text_color` varchar(16) DEFAULT NULL COMMENT '文案颜色 #RRGGBB',
  `background_color` varchar(16) DEFAULT NULL COMMENT '文案背景色 #RRGGBB',
  `icon_url` varchar(512) DEFAULT NULL COMMENT '文案图标地址',
  `image_url` varchar(512) DEFAULT NULL COMMENT '图片地址',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值,越大越靠前',
  `start_time` int NOT NULL COMMENT '展示开始时间(epoch秒,含)',
  `end_time` int NOT NULL COMMENT '展示结束时间(epoch秒,含)',
  `channel` varchar(32) NOT NULL DEFAULT 'mobile' COMMENT '展示渠道 mobile/pc',
  `jump_page` varchar(512) DEFAULT NULL COMMENT '跳转配置JSON',
  `created` int NOT NULL COMMENT '创建时间(epoch秒)',
  `updated` int DEFAULT NULL COMMENT '更新时间(epoch秒)',
  PRIMARY KEY (`id`),
  KEY `ix_search_hotword_company_start_end` (`company_id`, `start_time`, `end_time`),
  KEY `ix_search_hotword_company_sort` (`company_id`, `sort`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '搜索热词';
