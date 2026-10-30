-- Flyway migration: V00000000000004
-- Created at: 2026-10-30T02:26:38.877Z
-- Release: 5.4.0

ALTER TABLE `distribution_distributor`
  CHANGE `is_distributor` `is_distributor` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否是主店铺',
  CHANGE `auto_sync_goods` `auto_sync_goods` tinyint(1) NOT NULL DEFAULT 0 COMMENT '自动同步总部商品',
  CHANGE `is_audit_goods` `is_audit_goods` tinyint(1) DEFAULT 0 COMMENT '是否审核店铺商品',
  CHANGE `is_ziti` `is_ziti` tinyint(1) DEFAULT 0 COMMENT '是否支持自提',
  CHANGE `is_platform_store_buy` `is_platform_store_buy` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启云仓可购买（店务立即购买等）',
  CHANGE `is_delivery` `is_delivery` tinyint(1) DEFAULT 1 COMMENT '是否支持配送',
  CHANGE `is_self_delivery` `is_self_delivery` tinyint(1) DEFAULT 0 COMMENT '是否支持自配送',
  CHANGE `is_open_salesman` `is_open_salesman` tinyint(1) DEFAULT 0 COMMENT '是否开启业务员',
  CHANGE `review_status` `review_status` tinyint(1) DEFAULT 0 COMMENT '入驻审核状态，0未审核，1已审核',
  CHANGE `is_require_subdistrict` `is_require_subdistrict` tinyint(1) DEFAULT 0 COMMENT '下单是否需要选择街道社区',
  CHANGE `is_require_building` `is_require_building` tinyint(1) DEFAULT 0 COMMENT '下单是否需要填写楼栋门牌号';

ALTER TABLE `employee_purchase_activities`
  CHANGE `if_relative_join` `if_relative_join` tinyint(1) NOT NULL DEFAULT 0 COMMENT '亲友是否参与',
  CHANGE `if_share_limitfee` `if_share_limitfee` tinyint(1) DEFAULT 0 COMMENT '亲友是否共享员工额度',
  CHANGE `if_share_store` `if_share_store` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否共享库存',
  CHANGE `is_discount_description_enabled` `is_discount_description_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '优惠说明开关',
  CHANGE `is_passphrase_enabled` `is_passphrase_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启口令通道';

ALTER TABLE `employee_purchase_orders_rel_activity`
  CHANGE `if_share_store` `if_share_store` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否共享库存',
  CHANGE `participate_quota_order_consumed` `participate_quota_order_consumed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '本单是否扣过口令名额';

ALTER TABLE `items`
  CHANGE `is_default` `is_default` tinyint(1) DEFAULT 1 COMMENT '商品是否为默认商品',
  ADD COLUMN `other_product_materials` longtext DEFAULT NULL COMMENT '其他产品资料',
  CHANGE `is_show_specimg` `is_show_specimg` tinyint(1) NOT NULL DEFAULT 0 COMMENT '详情页是否显示规格图片',
  CHANGE `enable_agreement` `enable_agreement` tinyint(1) NOT NULL DEFAULT 0 COMMENT '开启购买协议',
  CHANGE `is_point` `is_point` tinyint(1) DEFAULT 0 COMMENT '是否积分兑换 true可以 false不可以',
  CHANGE `is_profit` `is_profit` tinyint(1) DEFAULT 0 COMMENT '是否支持分润',
  CHANGE `is_gift` `is_gift` tinyint(1) DEFAULT 0 COMMENT '是否为赠品',
  CHANGE `is_package` `is_package` tinyint(1) DEFAULT 0 COMMENT '是否为打包产品';

ALTER TABLE `supplier_items`
  CHANGE `is_default` `is_default` tinyint(1) DEFAULT 1 COMMENT '商品是否为默认商品',
  ADD COLUMN `other_product_materials` longtext DEFAULT NULL COMMENT '其他产品资料',
  CHANGE `is_show_specimg` `is_show_specimg` tinyint(1) NOT NULL DEFAULT 0 COMMENT '详情页是否显示规格图片',
  CHANGE `enable_agreement` `enable_agreement` tinyint(1) NOT NULL DEFAULT 0 COMMENT '开启购买协议',
  CHANGE `is_point` `is_point` tinyint(1) DEFAULT 0 COMMENT '是否积分兑换 true可以 false不可以',
  CHANGE `is_profit` `is_profit` tinyint(1) DEFAULT 0 COMMENT '是否支持分润',
  CHANGE `is_gift` `is_gift` tinyint(1) DEFAULT 0 COMMENT '是否为赠品',
  CHANGE `is_package` `is_package` tinyint(1) DEFAULT 0 COMMENT '是否为打包产品';
