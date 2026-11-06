-- Flyway migration: V00000000000007
-- Created at: 2026-11-06T09:15:37.275Z
-- Release: 5.5.0

-- Flyway migration: release
-- Created at: 2026-11-06 17:15:37 +0800

ALTER TABLE `merchant`
  ADD COLUMN `contact_name` varchar(255) DEFAULT NULL COMMENT '联系人姓名',
  ADD COLUMN `main_category_selection` longtext DEFAULT NULL COMMENT '商户类目选择 JSON',
  ADD COLUMN `license_registered_address` varchar(255) DEFAULT NULL COMMENT '营业执照注册地址',
  ADD COLUMN `license_legal_representative` varchar(255) DEFAULT NULL COMMENT '营业执照法定代表人';

ALTER TABLE `merchant_settlement_apply`
  ADD COLUMN `user_id` bigint DEFAULT NULL COMMENT '商城会员user_id',
  ADD COLUMN `contact_name` varchar(255) DEFAULT NULL COMMENT '联系人姓名',
  ADD COLUMN `main_category_selection` longtext DEFAULT NULL COMMENT '商户类目选择 JSON',
  ADD COLUMN `license_registered_address` varchar(255) DEFAULT NULL COMMENT '营业执照注册地址',
  ADD COLUMN `license_legal_representative` varchar(255) DEFAULT NULL COMMENT '营业执照法定代表人',
  ADD UNIQUE KEY `uk_company_user` (`company_id`, `user_id`);
