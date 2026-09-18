-- Flyway migration: V00000000000001
-- Created at: 2026-09-18 19:50:17 +0800

CREATE TABLE `company_shuyun_open_platform_config` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `auth_value` varchar(128) DEFAULT NULL,
  `plat_code` varchar(64) DEFAULT NULL,
  `app_id` varchar(64) DEFAULT NULL,
  `app_secret` varchar(512) DEFAULT NULL,
  `access_token` varchar(255) DEFAULT NULL,
  `is_over_due` varchar(8) DEFAULT NULL,
  `is_enabled` int NOT NULL,
  `created` int NOT NULL COMMENT '添加时间',
  `updated` int DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shuyun_op_auth_value` (`auth_value`),
  UNIQUE KEY `uk_shuyun_op_company_id` (`company_id`),
  UNIQUE KEY `uk_shuyun_op_app_id` (`app_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '数云开放网关租户配置';

CREATE TABLE `employee_purchase_activity_enterprise_behavior_log` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `activity_id` bigint NOT NULL,
  `enterprise_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `behavior_type` varchar(32) NOT NULL,
  `result_status` varchar(16) DEFAULT NULL,
  `visitor_key` varchar(64) DEFAULT NULL,
  `ref_id` bigint DEFAULT NULL,
  `extra` json DEFAULT NULL,
  `created` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_behavior_type` (`behavior_type`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '内购活动企业行为流水';

CREATE TABLE `employee_purchase_activity_enterprise_participate_user` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `activity_id` bigint NOT NULL,
  `enterprise_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  UNIQUE KEY `uk_company_activity_enterprise_user` (`company_id`, `activity_id`, `enterprise_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '内购活动参与名额已占用用户';

CREATE TABLE `employee_purchase_activity_passphrase_enterprises` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `activity_id` bigint NOT NULL,
  `enterprise_id` bigint NOT NULL,
  `participate_quota` int NOT NULL COMMENT '可参与名额',
  `passphrase_limitfee` int NOT NULL COMMENT '口令通道额度（分）',
  `passphrase_code` varchar(64) NOT NULL COMMENT '口令编码',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  UNIQUE KEY `uk_activity_enterprise` (`activity_id`, `enterprise_id`),
  UNIQUE KEY `uk_activity_code` (`activity_id`, `passphrase_code`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '内购活动口令企业配置';

CREATE TABLE `employee_purchase_store_home_page` (
  `id` bigint unsigned AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `distributor_id` int NOT NULL DEFAULT 0 COMMENT '门店ID',
  `template_name` varchar(64) DEFAULT NULL COMMENT '小程序模板名称',
  `page_name` varchar(255) NOT NULL COMMENT '页面名称',
  `page_description` varchar(500) NOT NULL COMMENT '页面描述',
  `page_share_title` varchar(255) DEFAULT NULL COMMENT '分享标题',
  `page_share_desc` varchar(500) DEFAULT NULL COMMENT '分享描述',
  `page_share_imageUrl` varchar(500) DEFAULT NULL COMMENT '分享图片',
  `is_open` int NOT NULL DEFAULT 1 COMMENT '是否开启',
  `weapp_customize_page_id` bigint unsigned DEFAULT NULL,
  `created` int DEFAULT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_distributor_id` (`distributor_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '内购模版（门店首页配置）';

CREATE TABLE `goods_recommend_display_setting` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `detail_enabled` int NOT NULL COMMENT '商品详情页开关',
  `cart_enabled` int NOT NULL COMMENT '购物车页开关',
  `checkout_enabled` int NOT NULL COMMENT '结算页开关',
  `order_detail_enabled` int NOT NULL COMMENT '订单详情页开关',
  `detail_limit` int NOT NULL COMMENT '详情页展示数量',
  `cart_limit` int NOT NULL COMMENT '购物车页展示数量',
  `checkout_limit` int NOT NULL COMMENT '结算页展示数量',
  `order_detail_limit` int NOT NULL COMMENT '订单详情页展示数量',
  `detail_sort` varchar(16) NOT NULL COMMENT '详情页排序',
  `cart_sort` varchar(16) NOT NULL COMMENT '购物车页排序',
  `checkout_sort` varchar(16) NOT NULL COMMENT '结算页排序',
  `order_detail_sort` varchar(16) NOT NULL COMMENT '订单详情页排序',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_recommend_display_company` (`company_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '商品推荐四页展示设置';

CREATE TABLE `goods_recommend_rule` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `rule_name` varchar(50) NOT NULL,
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_goods_recommend_rule_company_created` (`company_id`, `created`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '商品推荐规则';

CREATE TABLE `goods_recommend_rule_main_item` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `rule_id` bigint NOT NULL,
  `goods_id` bigint NOT NULL,
  `distributor_id` bigint NOT NULL,
  `created` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_recommend_main_company_goods` (`company_id`, `goods_id`),
  KEY `ix_goods_recommend_main_rule` (`rule_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '商品推荐规则主商品';

CREATE TABLE `goods_recommend_rule_recommend_item` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `rule_id` bigint NOT NULL,
  `goods_id` bigint NOT NULL,
  `sort` int NOT NULL,
  `created` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_recommend_recommend_rule_goods` (`rule_id`, `goods_id`),
  KEY `ix_goods_recommend_recommend_company` (`company_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '商品推荐规则推荐商品';

CREATE TABLE `member_email_activation_tokens` (
  `id` bigint AUTO_INCREMENT NOT NULL COMMENT '主键',
  `company_id` bigint NOT NULL COMMENT '公司 ID',
  `user_id` bigint NOT NULL COMMENT '会员 user_id',
  `token_hash` varchar(64) NOT NULL COMMENT 'SHA-256 哈希',
  `expires_at` int NOT NULL COMMENT '过期时间戳',
  `used_at` int DEFAULT NULL COMMENT '使用时间戳',
  `created_at` int NOT NULL COMMENT '创建时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_company_user` (`company_id`, `user_id`),
  KEY `idx_token_hash` (`token_hash`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员邮箱激活链接令牌';

CREATE TABLE `member_password_reset_tokens` (
  `id` bigint AUTO_INCREMENT NOT NULL COMMENT '主键',
  `company_id` bigint NOT NULL COMMENT '公司 ID',
  `user_id` bigint NOT NULL COMMENT '会员 user_id',
  `token_hash` varchar(64) NOT NULL COMMENT 'SHA-256 哈希',
  `expires_at` int NOT NULL COMMENT '过期时间戳',
  `used_at` int DEFAULT NULL COMMENT '使用时间戳',
  `created_at` int NOT NULL COMMENT '创建时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_company_user` (`company_id`, `user_id`),
  KEY `idx_token_hash` (`token_hash`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '会员邮箱找回密码令牌';

CREATE TABLE `promotions_turntable_prize_day_stock` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `act_id` bigint NOT NULL COMMENT '活动ID',
  `prize_id` varchar(64) NOT NULL COMMENT '奖项稳定ID',
  `day_key` varchar(16) NOT NULL COMMENT '业务日YYYY-MM-DD',
  `reserved_count` bigint NOT NULL COMMENT '已占用日库存',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_turntable_prize_day_stock` (`act_id`, `prize_id`, `day_key`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '大转盘奖项日库存占用';

CREATE TABLE `promotions_turntable_user_count` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `act_id` bigint NOT NULL COMMENT '活动ID',
  `total_count` bigint NOT NULL COMMENT '已占用总次数',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_turntable_user_count` (`company_id`, `user_id`, `act_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '大转盘用户活动总次数';

CREATE TABLE `promotions_turntable_user_day_count` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `act_id` bigint NOT NULL COMMENT '活动ID',
  `day_key` varchar(16) NOT NULL COMMENT '业务日YYYY-MM-DD',
  `day_count` bigint NOT NULL COMMENT '当日已占用次数',
  `created` int NOT NULL,
  `updated` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_turntable_user_day_count` (`company_id`, `user_id`, `act_id`, `day_key`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '大转盘用户每日次数';

CREATE TABLE `shuyun_offline_benefit` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `client_id` varchar(128) DEFAULT NULL,
  `benefit_id` varchar(128) NOT NULL,
  `benefit_name` varchar(512) DEFAULT NULL,
  `effective_start` int DEFAULT NULL COMMENT '权益生效起(秒)',
  `effective_end` int DEFAULT NULL COMMENT '权益生效止(秒)',
  `claim_start` int DEFAULT NULL COMMENT '领取起(秒)',
  `claim_end` int DEFAULT NULL COMMENT '领取止(秒)',
  `condition_limits_json` varchar(255) DEFAULT NULL,
  `local_card_id` bigint DEFAULT NULL COMMENT '本地券模板/活动键',
  `created` int NOT NULL COMMENT '添加时间',
  `updated` int DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shuyun_offline_benefit_company_benefit` (`company_id`, `benefit_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '数云线下权益档案';

CREATE TABLE `shuyun_offline_benefit_send_batch` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `request_id` varchar(128) NOT NULL,
  `benefit_id` varchar(128) NOT NULL,
  `send_kind` varchar(16) NOT NULL COMMENT 'single|batch',
  `send_time` int DEFAULT NULL,
  `expire_time` int DEFAULT NULL,
  `send_remark` varchar(512) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `total_count` int DEFAULT NULL,
  `success_count` int DEFAULT NULL,
  `failure_count` int DEFAULT NULL,
  `report_pushed_at` int DEFAULT NULL,
  `report_last_error` varchar(255) DEFAULT NULL,
  `report_retry_count` int NOT NULL DEFAULT 0,
  `created` int NOT NULL COMMENT '添加时间',
  `updated` int DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shuyun_offline_benefit_batch_company_request` (`company_id`, `request_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '数云线下权益发送批次';

CREATE TABLE `shuyun_offline_benefit_send_item` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `batch_id` bigint NOT NULL,
  `customer_id` varchar(128) NOT NULL,
  `member_user_id` bigint DEFAULT NULL,
  `benefit_code` varchar(256) DEFAULT NULL,
  `fail_reason` varchar(255) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `send_time` int DEFAULT NULL COMMENT '实际发送时间(秒)',
  `send_reason` varchar(512) DEFAULT NULL,
  `detail_pushed_at` int DEFAULT NULL,
  `last_consume_status` varchar(32) DEFAULT NULL COMMENT 'USED|NOT_USED 等',
  `last_consume_push_at` int DEFAULT NULL,
  `local_order_id` bigint DEFAULT NULL,
  `created` int NOT NULL COMMENT '添加时间',
  `updated` int DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `IDX_41573DEDF39EBE7A` (`batch_id`),
  UNIQUE KEY `uk_shuyun_offline_benefit_item_batch_customer` (`batch_id`, `customer_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '数云线下权益发送明细';

CREATE TABLE `shuyun_open_platform_traffic_audit` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL,
  `direction` varchar(16) NOT NULL,
  `correlation_id` varchar(128) NOT NULL,
  `http_verb` varchar(16) NOT NULL,
  `action_method` varchar(255) DEFAULT NULL,
  `http_status` int DEFAULT NULL,
  `outcome` varchar(32) NOT NULL,
  `request_headers_json` varchar(255) NOT NULL,
  `request_body` varchar(255) DEFAULT NULL,
  `response_body` varchar(255) DEFAULT NULL,
  `error_message` varchar(1024) DEFAULT NULL,
  `created` int NOT NULL COMMENT '添加时间',
  `updated` int DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shuyun_op_traffic_company_created` (`company_id`, `created`),
  KEY `idx_shuyun_op_traffic_correlation` (`correlation_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = '数云开放网关/回调排障审计（轻量）';

CREATE TABLE `web_menu_items` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `menu_id` bigint NOT NULL COMMENT '所属菜单id',
  `company_id` bigint NOT NULL COMMENT '公司id',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父菜单项id，0=顶级',
  `name` varchar(100) NOT NULL COMMENT '菜单项显示名称',
  `image_url` varchar(500) DEFAULT NULL COMMENT '菜单项图片',
  `link_type` varchar(50) NOT NULL DEFAULT 'url' COMMENT '链接类型',
  `link_value` varchar(500) DEFAULT NULL COMMENT '关联目标值',
  `link_extra` longtext DEFAULT NULL COMMENT '链接扩展信息',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` int NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_menu_id` (`menu_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = 'Web端商城-菜单项表';

CREATE TABLE `web_menus` (
  `id` bigint AUTO_INCREMENT NOT NULL,
  `company_id` bigint NOT NULL COMMENT '公司id',
  `name` varchar(100) NOT NULL COMMENT '菜单名称',
  `key` varchar(100) NOT NULL COMMENT '菜单标识符',
  `status` int NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_key` (`company_id`, `key`)
) ENGINE = InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` COMMENT = 'Web端商城-菜单主表';

ALTER TABLE `business_representative`
  CHANGE `is_active` `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否在职';

ALTER TABLE `chinaumspay_division_error_log`
  CHANGE `is_resubmit` `is_resubmit` tinyint(1) DEFAULT 0 COMMENT '是否重新提交';

ALTER TABLE `community_chief_ziti`
  CHANGE `is_default` `is_default` tinyint(1) DEFAULT 0 COMMENT '是否默认';

ALTER TABLE `companys_article`
  CHANGE `release_status` `release_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '文章发布状态',
  CHANGE `is_ai` `is_ai` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否AI生成，0表示人工创建，1表示AI生成';

ALTER TABLE `distribution_distribute_logs`
  CHANGE `is_close` `is_close` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已结算';

ALTER TABLE `distribution_distributor`
  CHANGE `is_distributor` `is_distributor` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否是主店铺',
  CHANGE `auto_sync_goods` `auto_sync_goods` tinyint(1) NOT NULL DEFAULT 0 COMMENT '自动同步总部商品',
  CHANGE `is_audit_goods` `is_audit_goods` tinyint(1) DEFAULT 0 COMMENT '是否审核店铺商品',
  CHANGE `is_ziti` `is_ziti` tinyint(1) DEFAULT 0 COMMENT '是否支持自提',
  CHANGE `is_delivery` `is_delivery` tinyint(1) DEFAULT 1 COMMENT '是否支持配送',
  CHANGE `is_self_delivery` `is_self_delivery` tinyint(1) DEFAULT 0 COMMENT '是否支持自配送',
  CHANGE `is_open_salesman` `is_open_salesman` tinyint(1) DEFAULT 0 COMMENT '是否开启业务员',
  ADD COLUMN `show_salesperson` smallint NOT NULL DEFAULT 1 COMMENT '是否展示导购 0:不展示 1:展示固定URL 2:展示归属导购',
  ADD COLUMN `fixed_salesperson_qrcode_url` varchar(255) DEFAULT NULL COMMENT '导购固定码URL',
  CHANGE `review_status` `review_status` tinyint(1) DEFAULT 0 COMMENT '入驻审核状态，0未审核，1已审核',
  CHANGE `is_require_subdistrict` `is_require_subdistrict` tinyint(1) DEFAULT 0 COMMENT '下单是否需要选择街道社区',
  CHANGE `is_require_building` `is_require_building` tinyint(1) DEFAULT 0 COMMENT '下单是否需要填写楼栋门牌号';

ALTER TABLE `distribution_distributor_items`
  CHANGE `is_show` `is_show` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否为列表默认展示',
  CHANGE `is_total_store` `is_total_store` tinyint(1) DEFAULT 1 COMMENT '是否为总部库存',
  CHANGE `is_can_sale` `is_can_sale` tinyint(1) DEFAULT 1 COMMENT '是否在本店可售',
  CHANGE `goods_can_sale` `goods_can_sale` tinyint(1) DEFAULT 1 COMMENT '商品是否可售，有一个sku可售，那么商品就可售',
  CHANGE `is_self_delivery` `is_self_delivery` tinyint(1) DEFAULT 0 COMMENT '是否开启自提配送',
  CHANGE `is_express_delivery` `is_express_delivery` tinyint(1) DEFAULT 0 COMMENT '是否开启快递配送';

ALTER TABLE `distribution_distributor_white_list`
  CHANGE `distributor_id` `distributor_id` bigint NOT NULL DEFAULT 1 COMMENT '店铺id';

ALTER TABLE `distribution_shopscreen_advertisement`
  CHANGE `release_status` `release_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '发布状态';

ALTER TABLE `distribution_shopscreen_slider`
  CHANGE `desc_status` `desc_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '图片描述状态11';

ALTER TABLE `employee_purchase_activities`
  ADD COLUMN `list_pic` varchar(255) NOT NULL COMMENT '活动列表海报',
  CHANGE `if_relative_join` `if_relative_join` tinyint(1) NOT NULL DEFAULT 0 COMMENT '亲友是否参与',
  CHANGE `if_share_limitfee` `if_share_limitfee` tinyint(1) DEFAULT 0 COMMENT '亲友是否共享员工额度',
  CHANGE `if_share_store` `if_share_store` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否共享库存',
  CHANGE `is_discount_description_enabled` `is_discount_description_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '优惠说明开关',
  ADD COLUMN `is_passphrase_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启口令通道',
  ADD COLUMN `purchase_mode` varchar(32) DEFAULT NULL COMMENT '购买方式 cash/prepaid_point；旧活动为空';

ALTER TABLE `employee_purchase_activity_enterprises`
  ADD COLUMN `per_capita_limitfee` int DEFAULT NULL COMMENT '人均可购买额度/预充点数（分）；新活动必填';

ALTER TABLE `employee_purchase_activity_items`
  ADD COLUMN `shelf_status` smallint NOT NULL DEFAULT 1 COMMENT '上下架状态:1上架,0下架';

ALTER TABLE `employee_purchase_enterprises`
  CHANGE `is_employee_check_enabled` `is_employee_check_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否验证员工白名单';

ALTER TABLE `employee_purchase_orders_rel_activity`
  CHANGE `if_share_store` `if_share_store` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否共享库存',
  ADD COLUMN `participate_quota_order_consumed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '本单是否扣过口令名额',
  ADD COLUMN `purchase_mode` varchar(32) DEFAULT NULL COMMENT '下单时购买方式快照',
  ADD COLUMN `prepaid_payable_fee` int NOT NULL DEFAULT 0 COMMENT '预充点本单扣减点数（分）',
  ADD COLUMN `restored_prepaid_fee` int NOT NULL DEFAULT 0 COMMENT '预充点本单已还点数累计（分）';

ALTER TABLE `items`
  CHANGE `is_default` `is_default` tinyint(1) DEFAULT 1 COMMENT '商品是否为默认商品',
  CHANGE `is_show_specimg` `is_show_specimg` tinyint(1) NOT NULL DEFAULT 0 COMMENT '详情页是否显示规格图片',
  CHANGE `enable_agreement` `enable_agreement` tinyint(1) NOT NULL DEFAULT 0 COMMENT '开启购买协议',
  CHANGE `is_point` `is_point` tinyint(1) DEFAULT 0 COMMENT '是否积分兑换 true可以 false不可以',
  CHANGE `is_profit` `is_profit` tinyint(1) DEFAULT 0 COMMENT '是否支持分润',
  CHANGE `is_gift` `is_gift` tinyint(1) DEFAULT 0 COMMENT '是否为赠品',
  CHANGE `is_package` `is_package` tinyint(1) DEFAULT 0 COMMENT '是否为打包产品';

ALTER TABLE `items_category`
  CHANGE `is_main_category` `is_main_category` tinyint(1) DEFAULT 0 COMMENT '是否为商品主类目';

ALTER TABLE `kaquan_rel_items`
  CHANGE `is_show` `is_show` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否为列表默认展示';

ALTER TABLE `kaquan_user_discount`
  CHANGE `is_give_by_friend` `is_give_by_friend` tinyint(1) DEFAULT 0 COMMENT '是否为转赠领取',
  CHANGE `is_return_back` `is_return_back` tinyint(1) DEFAULT 0 COMMENT '转赠时是否退回',
  CHANGE `is_chat_room` `is_chat_room` tinyint(1) DEFAULT 0 COMMENT '是否群转赠';

ALTER TABLE `kaquan_vip_grade`
  CHANGE `is_default` `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '购买引导文本',
  CHANGE `default_grade` `default_grade` tinyint(1) DEFAULT 0 COMMENT '是否默认等级',
  CHANGE `is_disabled` `is_disabled` tinyint(1) DEFAULT 0 COMMENT '是否禁用';

ALTER TABLE `logistics`
  CHANGE `custom` `custom` tinyint(1) DEFAULT 0 COMMENT '是否自定义';

ALTER TABLE `lucky_draw_activity`
  ADD COLUMN `config_version` bigint NOT NULL COMMENT '影响抽奖的配置版本，乐观锁';

ALTER TABLE `membercard_grade`
  CHANGE `default_grade` `default_grade` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认等级';

ALTER TABLE `members`
  ADD COLUMN `login_email` varchar(255) DEFAULT NULL COMMENT '登录邮箱(小写)',
  ADD COLUMN `email_verified_at` int DEFAULT NULL COMMENT '邮箱验证时间戳',
  ADD COLUMN `shuyun_open_online_wxapp_sync_at` int DEFAULT NULL COMMENT '数云 OPEN 线上 wxapp 同步成功时间（Unix）；NULL 未成功',
  ADD COLUMN `offline_reg_distributor` int DEFAULT NULL COMMENT '店务 OFFLINE member.register 成功时写入的分销商 ID；NULL 未写入',
  ADD KEY `idx_company_id_user_id` (`company_id`, `user_id`),
  ADD UNIQUE KEY `login_email_company` (`login_email`, `company_id`);

ALTER TABLE `members_associations`
  CHANGE `unionid` `unionid` varchar(128) NOT NULL COMMENT '第三方unionid',
  CHANGE `user_type` `user_type` varchar(30) NOT NULL COMMENT '用户类型，可选值有 wechat:微信;ali:支付宝;apple;google;facebook;line',
  ADD KEY `idx_company_userid_usertype` (`company_id`, `user_id`, `user_type`);

ALTER TABLE `members_info`
  CHANGE `have_consume` `have_consume` tinyint(1) DEFAULT 0 COMMENT '是否有消费';

ALTER TABLE `members_wechat_fans`
  CHANGE `tagpop` `tagpop` tinyint(1) NOT NULL DEFAULT 0 COMMENT '列表标签弹出框所需字段',
  CHANGE `remarkpop` `remarkpop` tinyint(1) NOT NULL DEFAULT 0 COMMENT '列表页备注弹出框所需字段';

ALTER TABLE `members_wechatusers`
  ADD KEY `idx_company_unionid` (`company_id`, `unionid`);

ALTER TABLE `merchant_settlement_apply`
  CHANGE `is_agree_agreement` `is_agree_agreement` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否同意入驻协议';

ALTER TABLE `merchant_type`
  CHANGE `is_show` `is_show` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否显示';

ALTER TABLE `onecode_batchs`
  CHANGE `show_trace` `show_trace` tinyint(1) NOT NULL DEFAULT 1 COMMENT '前台是否可以查看流通信息';

ALTER TABLE `operators`
  ADD COLUMN `shopex_bind_account` varchar(255) DEFAULT NULL COMMENT '可选绑定 Shopex 登录账号';

ALTER TABLE `orders_associations`
  CHANGE `is_distribution` `is_distribution` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否是分销订单';

ALTER TABLE `orders_cart`
  CHANGE `is_checked` `is_checked` tinyint(1) NOT NULL DEFAULT 1 COMMENT '购物车是否选中',
  CHANGE `is_plus_buy` `is_plus_buy` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是加价购商品';

ALTER TABLE `orders_normal_orders`
  CHANGE `is_distribution` `is_distribution` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否分销订单',
  CHANGE `is_online_order` `is_online_order` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否为线上订单',
  CHANGE `is_shopscreen` `is_shopscreen` tinyint(1) DEFAULT 0 COMMENT '是否门店订单',
  CHANGE `is_logistics` `is_logistics` tinyint(1) DEFAULT 0 COMMENT '门店缺货商品总部快递发货';

ALTER TABLE `orders_normal_orders_items`
  CHANGE `is_total_store` `is_total_store` tinyint(1) DEFAULT 1 COMMENT '是否是总部库存(true:总部库存，false:店铺库存)',
  CHANGE `is_logistics` `is_logistics` tinyint(1) DEFAULT 0 COMMENT '门店缺货商品总部快递发货';

ALTER TABLE `orders_process_log`
  CHANGE `is_show` `is_show` tinyint(1) DEFAULT 0 COMMENT 'C端是否可见';

ALTER TABLE `orders_rights`
  CHANGE `can_reservation` `can_reservation` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否可预约';

ALTER TABLE `pages_side_bar`
  CHANGE `disabled` `disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否禁用';

ALTER TABLE `pointsmall_items`
  CHANGE `is_default` `is_default` tinyint(1) DEFAULT 1 COMMENT '商品是否为默认商品',
  CHANGE `is_show_specimg` `is_show_specimg` tinyint(1) NOT NULL DEFAULT 0 COMMENT '详情页是否显示规格图片',
  CHANGE `enable_agreement` `enable_agreement` tinyint(1) NOT NULL DEFAULT 0 COMMENT '开启购买协议';

ALTER TABLE `popularize_brokerage`
  CHANGE `is_close` `is_close` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已结算';

ALTER TABLE `promotion_groups_activity`
  CHANGE `free_post` `free_post` tinyint(1) DEFAULT 1 COMMENT '是否包邮',
  CHANGE `rig_up` `rig_up` tinyint(1) DEFAULT 1 COMMENT '是否展示开团列表',
  CHANGE `robot` `robot` tinyint(1) DEFAULT 1 COMMENT '成团机器人',
  CHANGE `disabled` `disabled` tinyint(1) DEFAULT 0 COMMENT '是否禁用 true=禁用,false=启用';

ALTER TABLE `promotion_groups_rel_goods`
  CHANGE `is_show` `is_show` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否展示';

ALTER TABLE `promotion_groups_team`
  CHANGE `disabled` `disabled` tinyint(1) DEFAULT 0 COMMENT '是否禁用 true=禁用,false=启用';

ALTER TABLE `promotion_groups_team_member`
  CHANGE `disabled` `disabled` tinyint(1) DEFAULT 0 COMMENT '是否禁用 true=禁用,false=启用';

ALTER TABLE `promotions_marketing_activity`
  CHANGE `in_proportion` `in_proportion` tinyint(1) DEFAULT 0 COMMENT '是否按比例多次赠送',
  CHANGE `canjoin_repeat` `canjoin_repeat` tinyint(1) DEFAULT 0 COMMENT '是否上不封顶',
  CHANGE `free_postage` `free_postage` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否免邮';

ALTER TABLE `promotions_marketing_activity_items`
  CHANGE `is_show` `is_show` tinyint(1) NOT NULL DEFAULT 1 COMMENT '列表页是否显示',
  CHANGE `status` `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否生效中';

ALTER TABLE `promotions_marketing_gift_items`
  CHANGE `without_return` `without_return` tinyint(1) NOT NULL DEFAULT 0 COMMENT '退货无需退回赠品';

ALTER TABLE `promotions_seckill_activity`
  CHANGE `is_activity_rebate` `is_activity_rebate` tinyint(1) NOT NULL DEFAULT 0 COMMENT '秒杀活动是否返佣',
  CHANGE `is_free_shipping` `is_free_shipping` tinyint(1) NOT NULL DEFAULT 0 COMMENT '秒杀活动是否包邮';

ALTER TABLE `promotions_seckill_rel_goods`
  CHANGE `is_show` `is_show` tinyint(1) NOT NULL DEFAULT 1 COMMENT '查询列表是否显示';

ALTER TABLE `promotions_turntable_log`
  ADD COLUMN `request_id` varchar(64) DEFAULT NULL COMMENT '客户端幂等号',
  ADD COLUMN `status` varchar(32) DEFAULT NULL COMMENT '抽奖主状态',
  ADD COLUMN `process_step` varchar(64) DEFAULT NULL COMMENT 'PROCESSING子进度',
  ADD COLUMN `config_version` bigint DEFAULT NULL COMMENT '抽奖时活动配置版本',
  ADD COLUMN `prize_id` varchar(64) DEFAULT NULL COMMENT '稳定奖项标识',
  ADD COLUMN `sector_index` int DEFAULT NULL COMMENT '转盘扇区索引',
  ADD COLUMN `random_value` int DEFAULT NULL COMMENT '抽奖随机数1-100',
  ADD COLUMN `cost_points` bigint DEFAULT NULL COMMENT '本次扣除会员积分',
  ADD COLUMN `original_prize_id` varchar(64) DEFAULT NULL COMMENT '原命中奖项',
  ADD COLUMN `grant_no` varchar(128) DEFAULT NULL COMMENT '发奖外部单号',
  ADD COLUMN `error_code` varchar(64) DEFAULT NULL COMMENT '业务错误码',
  ADD COLUMN `error_message` varchar(512) DEFAULT NULL COMMENT '错误信息',
  ADD KEY `idx_turntable_log_status_updated` (`status`, `updated`),
  ADD KEY `idx_turntable_log_act_status` (`act_id`, `status`),
  ADD KEY `idx_turntable_log_user_act_status` (`user_id`, `act_id`, `status`),
  ADD UNIQUE KEY `uk_turntable_req` (`company_id`, `user_id`, `act_id`, `request_id`);

ALTER TABLE `promotions_user_bargains`
  CHANGE `is_ordered` `is_ordered` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已下单';

ALTER TABLE `refund_error_logs`
  CHANGE `is_resubmit` `is_resubmit` tinyint(1) DEFAULT 0 COMMENT '是否重新提交';

ALTER TABLE `salesperson_task`
  CHANGE `use_all_distributor` `use_all_distributor` tinyint(1) DEFAULT 0 COMMENT '是否是全部店铺';

ALTER TABLE `selfservice_form_setting`
  CHANGE `is_required` `is_required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否必填';

ALTER TABLE `selfservice_registration_activity`
  CHANGE `is_sms_notice` `is_sms_notice` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否短信通知',
  CHANGE `is_wxapp_notice` `is_wxapp_notice` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否小程序模板通知';

ALTER TABLE `shipping_templates`
  CHANGE `protect` `protect` tinyint(1) DEFAULT 0 COMMENT '物流保价',
  CHANGE `status` `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否开启';

ALTER TABLE `shop_comments`
  CHANGE `is_reply` `is_reply` tinyint(1) DEFAULT 0 COMMENT '评论是否回复',
  CHANGE `stuck` `stuck` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  CHANGE `hid` `hid` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否隐藏';

ALTER TABLE `super_admin_accounts`
  CHANGE `super` `super` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否超级管理员',
  CHANGE `status` `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用';

ALTER TABLE `superadmin_wxapp_template`
  CHANGE `is_only` `is_only` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为唯一属性，如果为唯一属性那么当前模版只能绑定一个小程序',
  CHANGE `is_disabled` `is_disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否禁用';

ALTER TABLE `supplier_items`
  CHANGE `is_default` `is_default` tinyint(1) DEFAULT 1 COMMENT '商品是否为默认商品',
  CHANGE `is_show_specimg` `is_show_specimg` tinyint(1) NOT NULL DEFAULT 0 COMMENT '详情页是否显示规格图片',
  CHANGE `enable_agreement` `enable_agreement` tinyint(1) NOT NULL DEFAULT 0 COMMENT '开启购买协议',
  CHANGE `is_point` `is_point` tinyint(1) DEFAULT 0 COMMENT '是否积分兑换 true可以 false不可以',
  CHANGE `is_profit` `is_profit` tinyint(1) DEFAULT 0 COMMENT '是否支持分润',
  CHANGE `is_gift` `is_gift` tinyint(1) DEFAULT 0 COMMENT '是否为赠品',
  CHANGE `is_package` `is_package` tinyint(1) DEFAULT 0 COMMENT '是否为打包产品';

ALTER TABLE `supplier_order`
  CHANGE `is_distribution` `is_distribution` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否分销订单',
  CHANGE `is_settled` `is_settled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否分账(斗拱)';

ALTER TABLE `theme_pc_template`
  ADD COLUMN `distributor_id` int NOT NULL DEFAULT 0 COMMENT '店铺ID',
  CHANGE `page_type` `page_type` varchar(15) DEFAULT 'index' COMMENT '页面类型 index 首页 custom product_list',
  ADD KEY `idx_company_distributor` (`company_id`, `distributor_id`);

ALTER TABLE `trade`
  CHANGE `is_settled` `is_settled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否分账';

ALTER TABLE `transcripts`
  CHANGE `transcript_status` `transcript_status` varchar(255) NOT NULL DEFAULT 0 COMMENT '状态';

ALTER TABLE `work_wechat_message_manager_template`
  CHANGE `disabled` `disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '模版是否禁用';

ALTER TABLE `work_wechat_message_template`
  CHANGE `disabled` `disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '模版是否开启',
  CHANGE `emphasis_first_item` `emphasis_first_item` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否放大第一个';

ALTER TABLE `wsugc_comment`
  CHANGE `disabled` `disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否无效';

ALTER TABLE `wsugc_comment_like`
  CHANGE `disabled` `disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否无效';

ALTER TABLE `wsugc_follower`
  CHANGE `disabled` `disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否无效';

ALTER TABLE `wsugc_message`
  CHANGE `hasread` `hasread` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读.';

ALTER TABLE `wsugc_post_favorite`
  CHANGE `disabled` `disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否无效';

ALTER TABLE `wsugc_post_image`
  CHANGE `is_sms_notice` `is_sms_notice` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否短信通知',
  CHANGE `is_wxapp_notice` `is_wxapp_notice` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否小程序模板通知';

ALTER TABLE `wsugc_post_like`
  CHANGE `disabled` `disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否无效';

ALTER TABLE `wxshops`
  CHANGE `is_default` `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否是默认门店',
  CHANGE `is_open` `is_open` tinyint(1) DEFAULT 1 COMMENT '是否开启 1:开启,0:关闭';
