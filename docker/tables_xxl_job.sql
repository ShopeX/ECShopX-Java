#
# XXL-JOB
# Copyright (c) 2015-present, xuxueli.

CREATE database if NOT EXISTS `xxl_job` default character set utf8mb4 collate utf8mb4_unicode_ci;
use `xxl_job`;

SET NAMES utf8mb4;

CREATE TABLE `xxl_job_info`
(
    `id`                        int(11)      NOT NULL AUTO_INCREMENT,
    `job_group`                 int(11)      NOT NULL COMMENT '执行器主键ID',
    `job_desc`                  varchar(255) NOT NULL,
    `add_time`                  datetime              DEFAULT NULL,
    `update_time`               datetime              DEFAULT NULL,
    `author`                    varchar(64)           DEFAULT NULL COMMENT '作者',
    `alarm_email`               varchar(255)          DEFAULT NULL COMMENT '报警邮件',
    `schedule_type`             varchar(50)  NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
    `schedule_conf`             varchar(128)          DEFAULT NULL COMMENT '调度配置，值含义取决于调度类型',
    `misfire_strategy`          varchar(50)  NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
    `executor_route_strategy`   varchar(50)           DEFAULT NULL COMMENT '执行器路由策略',
    `executor_handler`          varchar(255)          DEFAULT NULL COMMENT '执行器任务handler',
    `executor_param`            varchar(512)          DEFAULT NULL COMMENT '执行器任务参数',
    `executor_block_strategy`   varchar(50)           DEFAULT NULL COMMENT '阻塞处理策略',
    `executor_timeout`          int(11)      NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
    `executor_fail_retry_count` int(11)      NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `glue_type`                 varchar(50)  NOT NULL COMMENT 'GLUE类型',
    `glue_source`               mediumtext COMMENT 'GLUE源代码',
    `glue_remark`               varchar(128)          DEFAULT NULL COMMENT 'GLUE备注',
    `glue_updatetime`           datetime              DEFAULT NULL COMMENT 'GLUE更新时间',
    `child_jobid`               varchar(255)          DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
    `trigger_status`            tinyint(4)   NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
    `trigger_last_time`         bigint(13)   NOT NULL DEFAULT '0' COMMENT '上次调度时间',
    `trigger_next_time`         bigint(13)   NOT NULL DEFAULT '0' COMMENT '下次调度时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_log`
(
    `id`                        bigint(20) NOT NULL AUTO_INCREMENT,
    `job_group`                 int(11)    NOT NULL COMMENT '执行器主键ID',
    `job_id`                    int(11)    NOT NULL COMMENT '任务，主键ID',
    `executor_address`          varchar(255)        DEFAULT NULL COMMENT '执行器地址，本次执行的地址',
    `executor_handler`          varchar(255)        DEFAULT NULL COMMENT '执行器任务handler',
    `executor_param`            varchar(512)        DEFAULT NULL COMMENT '执行器任务参数',
    `executor_sharding_param`   varchar(20)         DEFAULT NULL COMMENT '执行器任务分片参数，格式如 1/2',
    `executor_fail_retry_count` int(11)    NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `trigger_time`              datetime            DEFAULT NULL COMMENT '调度-时间',
    `trigger_code`              int(11)    NOT NULL COMMENT '调度-结果',
    `trigger_msg`               text COMMENT '调度-日志',
    `handle_time`               datetime            DEFAULT NULL COMMENT '执行-时间',
    `handle_code`               int(11)    NOT NULL COMMENT '执行-状态',
    `handle_msg`                text COMMENT '执行-日志',
    `alarm_status`              tinyint(4) NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
    PRIMARY KEY (`id`),
    KEY `I_trigger_time` (`trigger_time`),
    KEY `I_handle_code` (`handle_code`),
    KEY `I_jobid_jobgroup` (`job_id`,`job_group`),
    KEY `I_job_id` (`job_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_log_report`
(
    `id`            int(11) NOT NULL AUTO_INCREMENT,
    `trigger_day`   datetime         DEFAULT NULL COMMENT '调度-时间',
    `running_count` int(11) NOT NULL DEFAULT '0' COMMENT '运行中-日志数量',
    `suc_count`     int(11) NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
    `fail_count`    int(11) NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
    `update_time`   datetime         DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_logglue`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT,
    `job_id`      int(11)      NOT NULL COMMENT '任务，主键ID',
    `glue_type`   varchar(50) DEFAULT NULL COMMENT 'GLUE类型',
    `glue_source` mediumtext COMMENT 'GLUE源代码',
    `glue_remark` varchar(128) NOT NULL COMMENT 'GLUE备注',
    `add_time`    datetime    DEFAULT NULL,
    `update_time` datetime    DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_registry`
(
    `id`             int(11)      NOT NULL AUTO_INCREMENT,
    `registry_group` varchar(50)  NOT NULL,
    `registry_key`   varchar(255) NOT NULL,
    `registry_value` varchar(255) NOT NULL,
    `update_time`    datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_g_k_v` (`registry_group`, `registry_key`, `registry_value`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_group`
(
    `id`           int(11)     NOT NULL AUTO_INCREMENT,
    `app_name`     varchar(64) NOT NULL COMMENT '执行器AppName',
    `title`        varchar(12) NOT NULL COMMENT '执行器名称',
    `address_type` tinyint(4)  NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
    `address_list` text COMMENT '执行器地址列表，多地址逗号分隔',
    `update_time`  datetime             DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_user`
(
    `id`         int(11)     NOT NULL AUTO_INCREMENT,
    `username`   varchar(50) NOT NULL COMMENT '账号',
    `password`   varchar(50) NOT NULL COMMENT '密码',
    `role`       tinyint(4)  NOT NULL COMMENT '角色：0-普通用户、1-管理员',
    `permission` varchar(255) DEFAULT NULL COMMENT '权限：执行器ID列表，多个逗号分割',
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_username` (`username`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_lock`
(
    `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
    PRIMARY KEY (`lock_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


## —————————————————————— init data ——————————————————
INSERT INTO `xxl_job_group`(`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`)
VALUES (1, 'ecshopx-executor', 'ecshopx执行器', 0, NULL, '2018-11-03 22:21:31');

INSERT INTO `xxl_job_info`(`id`, `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
                           `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
                           `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
                           `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
                           `child_jobid`, `trigger_status`) VALUES 
(1,1,'智能模板定时启用','2026-04-23 20:33:48','2026-05-07 17:51:02','ecshopx','','CRON','0 */5 * * * ? *','DO_NOTHING','FIRST','enable-theme-template','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-23 20:33:48','',1),
(2,1,'商家后台当日汇总统计','2026-04-24 10:56:11','2026-05-08 12:07:09','ecshopx','','CRON','0 30 1 * * ? *','DO_NOTHING','FIRST','daily-statistics','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'GLUE代码初始化','2026-04-24 10:56:11','',1),
(3,1,'实体订单取消订单','2026-04-24 13:05:06','2026-05-08 10:46:06','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','cancel-orders','','SERIAL_EXECUTION',0,0,'BEAN','',NULL,'2026-04-24 13:05:06','',1),
(4,1,'自动取消未支付线下转账支付方式的订单','2026-04-24 14:06:05','2026-05-08 10:46:51','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','cancel-offlinepay-orders','','SERIAL_EXECUTION',0,0,'BEAN','',NULL,'2026-04-24 14:06:05','',1),
(5,1,'自动取消未支付实体拼团订单','2026-04-24 17:58:28','2026-05-08 10:47:31','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','cancel-group-orders','','SERIAL_EXECUTION',0,0,'BEAN','',NULL,'2026-04-24 17:58:28','',1),
(6,1,'自动取消未支付服务类拼团订单','2026-04-24 18:28:41','2026-05-08 10:48:15','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','cancel-service-group-orders','','SERIAL_EXECUTION',0,0,'BEAN','',NULL,'2026-04-24 18:28:41','',1),
(7,1,'自动取消已支付未成功拼团','2026-04-24 19:10:16','2026-05-08 10:48:52','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','fail-group','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-24 19:10:16','',1),
(8,1,'自动完成拼团','2026-04-24 19:53:15','2026-05-08 10:49:31','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','done-group','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-24 19:53:15','',1),
(9,1,'库存为0时自动完成拼团','2026-04-24 20:58:18','2026-05-08 12:07:37','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','done-nostore-group','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-24 20:58:18','',1),
(10,1,'自动取消未支付秒杀订单','2026-04-24 21:27:16','2026-05-08 12:10:34','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','cancel-seckill-orders','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-24 21:27:16','',1),
(11,1,'自动取消未支付积分商城订单','2026-04-24 21:47:03','2026-05-08 12:10:20','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','cancel-pointsmall-orders','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-24 21:47:03','',1),
(12,1,'自动取消用户选择后过期的兑换券','2026-04-24 22:08:51','2026-05-08 12:10:05','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','cancel-expired-card','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-24 22:08:51','',1),
(13,1,'自动过期定向促销','2026-04-24 22:20:49','2026-05-08 12:09:47','ecshopx','','CRON','0 */5 * * * ? *','DO_NOTHING','FIRST','expire-specific-crowd-discount','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-24 22:20:49','',1),
(14,1,'自动驳回售后','2026-04-24 22:51:26','2026-05-08 12:09:30','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','refuse-aftersales','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-24 22:51:26','',1),
(15,1,'定时结算分佣','2026-04-24 23:11:31','2026-05-08 12:09:21','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','settle-brokerage','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-24 23:11:31','',1),
(16,1,'自动关闭订单售后','2026-04-24 23:42:09','2026-05-08 12:09:09','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','close-order-item-aftersales','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-24 23:42:09','',1),
(17,1,'定时结算导购佣金','2026-04-24 23:56:36','2026-05-08 12:08:58','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','settle-order-profit','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-24 23:56:36','',1),
(18,1,'订单分账','2026-04-25 00:36:22','2026-05-08 12:08:36','ecshopx','','CRON','0 */5 * * * ? *','DO_NOTHING','FIRST','share-order-profit','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-25 00:36:22','',1),
(19,1,'删除上传到期文件','2026-04-25 00:59:52','2026-05-08 12:08:23','ecshopx','','CRON','0 0 0 * * ? *','DO_NOTHING','FIRST','delete-error-file','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 00:59:52','',1),
(20,1,'自动关闭到期驳回售后','2026-04-25 01:27:07','2026-05-08 12:10:53','ecshopx','','CRON','0 0 0 * * ? *','DO_NOTHING','FIRST','done-aftersales','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 01:27:07','',1),
(21,1,'为会员增加积分','2026-04-25 01:56:24','2026-05-08 12:11:16','ecshopx','','CRON','0 0 0 * * ? *','DO_NOTHING','FIRST','send-member-point','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 01:56:24','',1),
(22,1,'各种定时统计','2026-04-25 02:33:07','2026-05-08 12:12:47','ecshopx','','CRON','0 0 1 * * ? *','DO_NOTHING','FIRST','record-statistics','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 02:33:07','',1),
(23,1,'各种定时统计','2026-04-25 03:04:35','2026-05-08 13:43:25','ecshopx','','CRON','0 0 1 * * ? *','DO_NOTHING','FIRST','record-active-article-statistics','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 03:04:35','',1),
(24,1,'各种定时统计','2026-04-25 03:52:26','2026-05-08 15:09:28','ecshopx','','CRON','0 0 1 * * ? *','DO_NOTHING','FIRST','record-commission-statistics','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 03:52:26','',1),
(25,1,'各种定时统计','2026-04-25 04:10:57','2026-05-08 15:09:59','ecshopx','','CRON','0 0 1 * * ? *','DO_NOTHING','FIRST','record-popularize-statistics','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 04:10:57','',1),
(26,1,'各种定时统计','2026-04-25 04:31:56','2026-05-08 15:10:35','ecshopx','','CRON','0 0 1 * * ? *','DO_NOTHING','FIRST','record-give-coupons-statistics','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 04:31:56','',1),
(27,1,'生成结算单','2026-04-25 05:08:38','2026-05-08 15:11:11','ecshopx','','CRON','0 0 1 * * ? *','DO_NOTHING','FIRST','generate-statements','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 05:08:38','',1),
(28,1,'过期营销活动失效','2026-04-25 05:21:36','2026-05-08 15:13:37','ecshopx','','CRON','0 0 1 * * ? *','DO_NOTHING','FIRST','invalid-promotion-activity','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 05:21:36','',1),
(29,1,'触发营销活动','2026-04-25 06:00:18','2026-05-08 15:14:23','ecshopx','','CRON','0 0 1 * * ? *','DO_NOTHING','FIRST','trigger-promotion-activity','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 06:00:18','',1),
(30,1,'内购活动交易统计','2026-04-25 06:20:32','2026-05-08 15:43:53','ecshopx','','CRON','0 30 1 * * ? *','DO_NOTHING','FIRST','employee-purchase-daily-statistic','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 06:20:32','',1),
(31,1,'商户后台当日汇总统计','2026-04-25 06:36:30','2026-05-08 15:44:27','ecshopx','','CRON','0 30 1 * * ? *','DO_NOTHING','FIRST','merchant-daily-statistic','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 06:36:30','',1),
(32,1,'汇付店铺分账数据统计','2026-04-25 08:54:42','2026-05-08 15:45:03','ecshopx','','CRON','0 30 1 * * ? *','DO_NOTHING','FIRST','hfpay-distributor-daily-statistics','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-25 08:54:42','',1),
(33,1,'汇付平台分账数据统计','2026-04-25 09:14:01','2026-05-08 15:45:59','ecshopx','','CRON','0 30 1 * * ? *','DO_NOTHING','FIRST','hfpay-company-daily-statistics','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-25 09:14:01','',1),
(34,1,'商品数据统计','2026-04-25 09:33:26','2026-05-08 15:46:39','ecshopx','','CRON','0 0 2 * * ? *','DO_NOTHING','FIRST','goods-daily-statistics','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-25 09:33:26','',1),
(35,1,'内购活动商品统计','2026-04-25 09:50:23','2026-05-08 15:47:13','ecshopx','','CRON','0 0 2 * * ? *','DO_NOTHING','FIRST','employee-purchase-goods-daily-statistics','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-25 09:50:23','',1),
(36,1,'删除操作日志','2026-04-25 10:04:53','2026-05-08 15:47:47','ecshopx','','CRON','0 30 2 * * ? *','DO_NOTHING','FIRST','delete-operator-logs','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-25 10:04:53','',1),
(37,1,'店铺每日数据统计','2026-04-25 11:20:47','2026-05-08 15:48:27','ecshopx','','CRON','0 45 2 * * ? *','DO_NOTHING','FIRST','distributor-daily-statistic','','SERIAL_EXECUTION',0,0,'BEAN',NULL,'','2026-04-25 11:20:47','',1),
(38,1,'删除历史导出文件列表','2026-04-25 11:37:22','2026-05-08 15:49:11','ecshopx','','CRON','0 0 3 * * ? *','DO_NOTHING','FIRST','delete-history-file','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 11:37:22','',1),
(39,1,'银联商务支付，划付，上传文件','2026-04-25 12:13:09','2026-05-08 15:49:55','ecshopx','','CRON','0 0 18 * * ? *','DO_NOTHING','FIRST','chinaums-division-transfer-sftp','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 12:13:09','',1),
(40,1,'银联商务支付，划付重新提交，上传文件','2026-04-25 12:32:37','2026-05-08 15:50:56','ecshopx','','CRON','0 0 18 * * ? *','DO_NOTHING','FIRST','chinaums-division-resubmit-sftp','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 12:32:37','',1),
(41,1,'银联商务支付，划付，处理回盘数据','2026-04-25 13:07:33','2026-05-08 15:52:31','ecshopx','','CRON','0 0 3 * * ? *','DO_NOTHING','FIRST','chinaums-division-download-sftp','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 13:07:33','',1),
(42,1,'定时退款','2026-04-25 13:34:16','2026-05-08 15:52:42','ecshopx','','CRON','0 */10 * * * ? *','DO_NOTHING','FIRST','schedule-refund','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 13:34:16','',1),
(43,1,'微信商户打款批次状态查询','2026-04-25 13:53:53','2026-05-08 15:53:40','ecshopx','','CRON','0 */10 * * * ? *','DO_NOTHING','FIRST','wechat-query-merchant-payment','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 13:53:53','',1),
(44,1,'会员升级','2026-04-25 14:20:57','2026-05-08 15:54:17','ecshopx','','CRON','0 30 3 * * ? *','DO_NOTHING','FIRST','consumption-orders','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 14:20:57','',1),
(45,1,'实体订单完成订单','2026-04-25 14:46:15','2026-05-08 15:55:00','ecshopx','','CRON','0 0 3 * * ? *','DO_NOTHING','FIRST','finish-orders','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 14:46:15','',1),
(46,1,'每天过期或失效用户权益','2026-04-25 19:45:39','2026-05-08 15:55:39','ecshopx','','CRON','0 30 3 * * ? *','DO_NOTHING','FIRST','update-rights-status','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 19:45:39','',1),
(47,1,'每天11点上传腾讯有数，微信数据','2026-04-25 20:04:14','2026-05-08 15:56:25','ecshopx','','CRON','0 0 11 * * ? *','DO_NOTHING','FIRST','youshu-add-wxapp-visit-page','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 20:04:14','',1),
(48,1,'每天11点上传腾讯有数，微信数据','2026-04-25 20:16:54','2026-05-08 15:57:39','ecshopx','','CRON','0 0 11 * * ? *','DO_NOTHING','FIRST','youshu-add-wxapp-visit-distribution','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 20:16:54','',1),
(49,1,'汇付提现状态查询','2026-04-25 20:40:45','2026-05-08 15:58:36','ecshopx','','CRON','0 0 23 * * ? *','DO_NOTHING','FIRST','hypay-check-cash','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 20:40:45','',1),
(50,1,'每天5点上传腾讯有数，订单汇总信息','2026-04-25 20:56:02','2026-05-08 15:59:26','ecshopx','','CRON','0 0 5 * * ? *','DO_NOTHING','FIRST','youshu-order-sum','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 20:56:02','',1),
(51,1,'大转盘活动结束时清空抽奖次数','2026-04-25 21:25:25','2026-05-08 16:00:00','ecshopx','','CRON','0 30 3 * * ? *','DO_NOTHING','FIRST','clear-turntable-times-over','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 21:25:25','',1),
(52,1,'定向促销 更新自然月的周期开始和结束时间','2026-04-25 21:37:18','2026-05-08 16:00:36','ecshopx','','CRON','0 0 0 1 * ? *','DO_NOTHING','FIRST','expire-crowd-month','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 21:37:18','',1),
(53,1,'店铺提现','2026-04-25 21:54:06','2026-05-08 16:03:47','ecshopx','','CRON','0 0 18 * * ? *','DO_NOTHING','FIRST','hypay-distributor-withdraw','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 21:54:06','',1),
(54,1,'定时查询adapay总商户提交证照审核状态','2026-04-25 22:11:39','2026-05-08 16:04:40','ecshopx','','CRON','0 */10 * * * ? *','DO_NOTHING','FIRST','adapay-get-submit-license-audit','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 22:11:39','',1),
(55,1,'adapay支付确认重试','2026-04-25 22:37:30','2026-05-08 16:05:13','ecshopx','','CRON','0 */10 * * * ? *','DO_NOTHING','FIRST','adapay-confirm-retry','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 22:37:30','',1),
(56,1,'斗拱支付确认重试','2026-04-25 23:14:00','2026-05-08 16:05:45','ecshopx','','CRON','0 */10 * * * ? *','DO_NOTHING','FIRST','bspay-confirm-retry','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 23:14:00','',1),
(57,1,'活动开始提醒','2026-04-25 23:43:20','2026-05-08 16:06:20','ecshopx','','CRON','0 0 * * * ? *','DO_NOTHING','FIRST','send-wx-remind-msg','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-25 23:43:20','',1),
(58,1,'adapay自动提现','2026-04-26 00:04:11','2026-05-08 16:09:30','ecshopx','','CRON','0 0 * * * ? *','DO_NOTHING','FIRST','adapy-draw-cash-queue','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-26 00:04:11','',1),
(59,1,'阿里云短信签名审核状态查询','2026-04-26 00:24:46','2026-05-08 16:10:06','ecshopx','','CRON','0 */5 * * * ? *','DO_NOTHING','FIRST','aliyunsms-query-sign-audit-status','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-26 00:24:46','',1),
(60,1,'阿里云短信模板审核状态查询','2026-04-26 01:57:52','2026-05-08 16:10:52','ecshopx','','CRON','0 */5 * * * ? *','DO_NOTHING','FIRST','aliyunsms-query-template-audit-status','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-26 01:57:52','',1),
(61,1,'阿里云短信发送结果查询','2026-04-26 02:38:03','2026-05-08 16:12:07','migration','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','aliyunsms-query-sms-send-detail','','SERIAL_EXECUTION',0,0,'BEAN','','GLUE代码初始化','2026-04-26 02:38:03','',1),
(62,1,'阿里云短信群发任务状态更新','2026-04-26 08:57:14','2026-05-08 16:12:48','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','aliyunsms-update-task','','SERIAL_EXECUTION',0,0,'BEAN',NULL,NULL,'2026-04-26 08:57:14','',1),
(63,1,'阿里云短信群发任务执行','2026-04-26 09:25:59','2026-05-08 16:14:10','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','aliyunsms-run-task','','SERIAL_EXECUTION',0,0,'BEAN',NULL,NULL,'2026-04-26 09:25:59','',1),
(64,1,'定时开票任务','2026-04-26 09:49:01','2026-05-08 16:14:51','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','create-invoice','','SERIAL_EXECUTION',0,0,'BEAN',NULL,NULL,'2026-04-26 09:49:01','',1),
(65,1,'定时查询开票结果任务','2026-04-26 10:31:58','2026-05-08 16:15:32','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','query-invoice','','SERIAL_EXECUTION',0,0,'BEAN',NULL,NULL,'2026-04-26 10:31:58','',1),
(66,1,'红冲定时查询任务','2026-04-26 10:42:17','2026-05-08 16:17:49','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','query-invoice-red','','SERIAL_EXECUTION',0,0,'BEAN',NULL,NULL,'2026-04-26 10:42:17','',1),
(67,1,'活动时间截止时自动成团或解散','2026-04-26 11:17:02','2026-05-08 16:18:22','ecshopx','','CRON','0 * * * * ? *','DO_NOTHING','FIRST','finish-community-activity','','SERIAL_EXECUTION',0,0,'BEAN',NULL,NULL,'2026-04-26 11:17:02','',1),
(68,1,'旺店通定时同步订单发货','2026-04-26 11:49:45','2026-05-08 16:18:55','ecshopx','','CRON','0 */5 * * * ? *','DO_NOTHING','FIRST','sync-wdt-logistics','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-26 11:49:45','',1),
(69,1,'旺店通定时同步货品库存','2026-04-26 12:14:23','2026-05-08 16:19:32','ecshopx','','CRON','0 */5 * * * ? *','DO_NOTHING','FIRST','sync-wdt-inventory','','SERIAL_EXECUTION',0,0,'BEAN','','','2026-04-26 12:14:23','',1);

INSERT INTO `xxl_job_user`(`id`, `username`, `password`, `role`, `permission`)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL);

INSERT INTO `xxl_job_lock` (`lock_name`)
VALUES ('schedule_lock');
