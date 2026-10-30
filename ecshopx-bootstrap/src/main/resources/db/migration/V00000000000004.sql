-- Flyway migration: V00000000000004
-- Created at: 2026-10-30T02:26:38.877Z
-- Release: 5.4.0

ALTER TABLE `items`
  ADD COLUMN `other_product_materials` longtext DEFAULT NULL COMMENT '其他产品资料';

ALTER TABLE `supplier_items`
  ADD COLUMN `other_product_materials` longtext DEFAULT NULL COMMENT '其他产品资料';