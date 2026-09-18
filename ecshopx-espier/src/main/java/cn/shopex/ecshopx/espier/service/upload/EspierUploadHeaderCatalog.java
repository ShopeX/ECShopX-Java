/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.espier.service.upload;

import cn.shopex.ecshopx.espier.service.upload.headerinfo.AdapayTradedataHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.CommunityChiefHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.DiscountGoodsHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.DistributorInfoHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.EmployeePurchaseActivityItemsHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.EmployeePurchaseActivityItemsSortHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.EmployeePurchaseEmployeesHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.LimitSaleItemHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.MarketingGoodsHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.MemberConsumeHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.MemberInfoHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.MemberUpdateHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalEpidemicGoodsHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalGoodsHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalGoodsProfitHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalGoodsStoreHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalGoodsTagHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalOrdersCancelHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalOrdersHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalPointsmallGoodsHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalPointsmallGoodsStoreHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.PurchaseGoodsHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.SelformRegistrationRecordHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.SupplierGoodsHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.UpdateDistributionItemHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.UploadDistributorWhiteHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.UploadTbItemsHeaderInfo;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.WhitelistCreateHeaderInfo;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EspierUploadHeaderCatalog {
	private EspierUploadHeaderCatalog() {}

	private static final Map<String, String> HDR_ADAPAY_TRADEDATA;
	static {
		HDR_ADAPAY_TRADEDATA = new LinkedHashMap<>();
		HDR_ADAPAY_TRADEDATA.put("订单号", "order_id");
		HDR_ADAPAY_TRADEDATA.put("交易单号", "trade_id");
		HDR_ADAPAY_TRADEDATA.put("是否分账", "adapay_div_status");
	}

	private static final Map<String, String> NEED_ADAPAY_TRADEDATA;
	static {
		NEED_ADAPAY_TRADEDATA = new LinkedHashMap<>();
		NEED_ADAPAY_TRADEDATA.put("订单号", "order_id");
		NEED_ADAPAY_TRADEDATA.put("交易单号", "trade_id");
		NEED_ADAPAY_TRADEDATA.put("是否分账", "adapay_div_status");
	}

	public static UploadHeaderTitle ADAPAY_TRADEDATA() {
		return new UploadHeaderTitle(HDR_ADAPAY_TRADEDATA, NEED_ADAPAY_TRADEDATA, AdapayTradedataHeaderInfo.map());
	}

	private static final Map<String, String> HDR_COMMUNITY_CHIEF;
	static {
		HDR_COMMUNITY_CHIEF = new LinkedHashMap<>();
		HDR_COMMUNITY_CHIEF.put("团长手机号码", "mobile");
		HDR_COMMUNITY_CHIEF.put("店铺ID", "did");
	}

	private static final Map<String, String> NEED_COMMUNITY_CHIEF;
	static {
		NEED_COMMUNITY_CHIEF = new LinkedHashMap<>();
		NEED_COMMUNITY_CHIEF.put("团长手机号码", "mobile");
		NEED_COMMUNITY_CHIEF.put("店铺ID", "did");
	}

	public static UploadHeaderTitle COMMUNITY_CHIEF() {
		return new UploadHeaderTitle(HDR_COMMUNITY_CHIEF, NEED_COMMUNITY_CHIEF, CommunityChiefHeaderInfo.map());
	}

	private static final Map<String, String> HDR_DISCOUNT_GOODS;
	static {
		HDR_DISCOUNT_GOODS = new LinkedHashMap<>();
		HDR_DISCOUNT_GOODS.put("商品编码-精确到规格", "item_bn");
		HDR_DISCOUNT_GOODS.put("商品名称", "item_name");
		HDR_DISCOUNT_GOODS.put("商品可兑换上限", "limit_num");
	}

	private static final Map<String, String> NEED_DISCOUNT_GOODS;
	static {
		NEED_DISCOUNT_GOODS = new LinkedHashMap<>();
		NEED_DISCOUNT_GOODS.put("商品编码-精确到规格", "item_bn");
		NEED_DISCOUNT_GOODS.put("商品名称", "item_name");
		NEED_DISCOUNT_GOODS.put("商品可兑换上限", "limit_num");
	}

	public static UploadHeaderTitle DISCOUNT_GOODS() {
		return new UploadHeaderTitle(HDR_DISCOUNT_GOODS, NEED_DISCOUNT_GOODS, DiscountGoodsHeaderInfo.map());
	}

	private static final Map<String, String> HDR_DISTRIBUTOR_INFO;
	static {
		HDR_DISTRIBUTOR_INFO = new LinkedHashMap<>();
		HDR_DISTRIBUTOR_INFO.put("店铺类型", "distribution_type");
		HDR_DISTRIBUTOR_INFO.put("店铺号", "shop_code");
		HDR_DISTRIBUTOR_INFO.put("店铺名称", "name");
		HDR_DISTRIBUTOR_INFO.put("门店分类", "distributor_category");
		HDR_DISTRIBUTOR_INFO.put("联系人姓名", "contact");
		HDR_DISTRIBUTOR_INFO.put("联系方式", "contract_phone");
		HDR_DISTRIBUTOR_INFO.put("店铺所在省市区", "addr1");
		HDR_DISTRIBUTOR_INFO.put("店铺详细地址", "addr2");
		HDR_DISTRIBUTOR_INFO.put("店铺详细地址门牌号", "addr3");
		HDR_DISTRIBUTOR_INFO.put("经营开始时间", "hour1");
		HDR_DISTRIBUTOR_INFO.put("经营结束时间", "hour2");
		HDR_DISTRIBUTOR_INFO.put("开启快递配送", "is_delivery");
		HDR_DISTRIBUTOR_INFO.put("自动同步商品", "auto_sync_goods");
		HDR_DISTRIBUTOR_INFO.put("店铺LOGO", "logo");
		HDR_DISTRIBUTOR_INFO.put("店铺背景", "banner");
		HDR_DISTRIBUTOR_INFO.put("旺店通ERP店铺号", "wdt_shop_no");
		HDR_DISTRIBUTOR_INFO.put("聚水潭店铺编号", "jst_shop_id");
	}

	private static final Map<String, String> NEED_DISTRIBUTOR_INFO;
	static {
		NEED_DISTRIBUTOR_INFO = new LinkedHashMap<>();
		NEED_DISTRIBUTOR_INFO.put("店铺类型", "distribution_type");
		NEED_DISTRIBUTOR_INFO.put("店铺号", "shop_code");
		NEED_DISTRIBUTOR_INFO.put("联系人姓名", "contact");
		NEED_DISTRIBUTOR_INFO.put("联系方式", "contract_phone");
		NEED_DISTRIBUTOR_INFO.put("店铺所在省市区", "addr1");
		NEED_DISTRIBUTOR_INFO.put("店铺详细地址", "addr2");
		NEED_DISTRIBUTOR_INFO.put("开启快递配送", "is_delivery");
		NEED_DISTRIBUTOR_INFO.put("自动同步商品", "auto_sync_goods");
		NEED_DISTRIBUTOR_INFO.put("店铺名称", "name");
	}

	public static UploadHeaderTitle DISTRIBUTOR_INFO() {
		return new UploadHeaderTitle(HDR_DISTRIBUTOR_INFO, NEED_DISTRIBUTOR_INFO, DistributorInfoHeaderInfo.map());
	}

	/** 默认按非共享库存（含「活动库存」列）；实际下载/导入由 Handler 按活动 if_share_store 动态生成。 */
	public static UploadHeaderTitle EMPLOYEE_PURCHASE_ACTIVITY_ITEMS() {
		return EmployeePurchaseActivityItemsHeaderInfo.title(false);
	}

	public static UploadHeaderTitle EMPLOYEE_PURCHASE_ACTIVITY_ITEMS(boolean ifShareStore) {
		return EmployeePurchaseActivityItemsHeaderInfo.title(ifShareStore);
	}

	private static final Map<String, String> HDR_EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT;
	static {
		HDR_EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT = new LinkedHashMap<>();
		HDR_EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT.put("SPU编码", "goods_bn");
		HDR_EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT.put("排序值", "sort");
	}

	private static final Map<String, String> NEED_EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT;
	static {
		NEED_EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT = new LinkedHashMap<>();
		NEED_EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT.put("SPU编码", "goods_bn");
	}

	public static UploadHeaderTitle EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT() {
		return new UploadHeaderTitle(
				HDR_EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT,
				NEED_EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT,
				EmployeePurchaseActivityItemsSortHeaderInfo.map());
	}

	private static final Map<String, String> HDR_EMPLOYEE_PURCHASE_EMPLOYEES;
	static {
		HDR_EMPLOYEE_PURCHASE_EMPLOYEES = new LinkedHashMap<>();
		HDR_EMPLOYEE_PURCHASE_EMPLOYEES.put("企业编码", "enterprise_sn");
		HDR_EMPLOYEE_PURCHASE_EMPLOYEES.put("姓名", "name");
		HDR_EMPLOYEE_PURCHASE_EMPLOYEES.put("手机号码", "mobile");
		HDR_EMPLOYEE_PURCHASE_EMPLOYEES.put("账号", "account");
		HDR_EMPLOYEE_PURCHASE_EMPLOYEES.put("校验密码", "auth_code");
		HDR_EMPLOYEE_PURCHASE_EMPLOYEES.put("邮箱", "email");
	}

	private static final Map<String, String> NEED_EMPLOYEE_PURCHASE_EMPLOYEES;
	static {
		NEED_EMPLOYEE_PURCHASE_EMPLOYEES = new LinkedHashMap<>();
		NEED_EMPLOYEE_PURCHASE_EMPLOYEES.put("企业编码", "enterprise_sn");
		NEED_EMPLOYEE_PURCHASE_EMPLOYEES.put("姓名", "name");
	}

	public static UploadHeaderTitle EMPLOYEE_PURCHASE_EMPLOYEES() {
		return new UploadHeaderTitle(HDR_EMPLOYEE_PURCHASE_EMPLOYEES, NEED_EMPLOYEE_PURCHASE_EMPLOYEES, EmployeePurchaseEmployeesHeaderInfo.map());
	}

	private static final Map<String, String> HDR_LIMIT_SALE_ITEM;
	static {
		HDR_LIMIT_SALE_ITEM = new LinkedHashMap<>();
		HDR_LIMIT_SALE_ITEM.put("店铺号", "shop_code");
		HDR_LIMIT_SALE_ITEM.put("商品货号", "item_bn");
		HDR_LIMIT_SALE_ITEM.put("限购数量", "limit_num");
	}

	private static final Map<String, String> NEED_LIMIT_SALE_ITEM;
	static {
		NEED_LIMIT_SALE_ITEM = new LinkedHashMap<>();
		NEED_LIMIT_SALE_ITEM.put("店铺号", "shop_code");
		NEED_LIMIT_SALE_ITEM.put("商品货号", "item_bn");
		NEED_LIMIT_SALE_ITEM.put("限购数量", "limit_num");
	}

	public static UploadHeaderTitle LIMIT_SALE_ITEM() {
		return new UploadHeaderTitle(HDR_LIMIT_SALE_ITEM, NEED_LIMIT_SALE_ITEM, LimitSaleItemHeaderInfo.map());
	}

	private static final Map<String, String> HDR_MARKETING_GOODS;
	static {
		HDR_MARKETING_GOODS = new LinkedHashMap<>();
		HDR_MARKETING_GOODS.put("商品编码", "item_bn");
		HDR_MARKETING_GOODS.put("商品名称", "item_name");
		HDR_MARKETING_GOODS.put("活动价", "activity_price");
		HDR_MARKETING_GOODS.put("库存(非秒杀活动可为0)", "activity_store");
		HDR_MARKETING_GOODS.put("每人限购", "limit_num");
		HDR_MARKETING_GOODS.put("排序", "sort");
	}

	private static final Map<String, String> NEED_MARKETING_GOODS;
	static {
		NEED_MARKETING_GOODS = new LinkedHashMap<>();
		NEED_MARKETING_GOODS.put("商品编码", "item_bn");
		NEED_MARKETING_GOODS.put("商品名称", "item_name");
		NEED_MARKETING_GOODS.put("活动价", "activity_price");
		NEED_MARKETING_GOODS.put("每人限购", "limit_num");
	}

	public static UploadHeaderTitle MARKETING_GOODS() {
		return new UploadHeaderTitle(HDR_MARKETING_GOODS, NEED_MARKETING_GOODS, MarketingGoodsHeaderInfo.map());
	}

	private static final Map<String, String> HDR_MEMBER_CONSUME;
	static {
		HDR_MEMBER_CONSUME = new LinkedHashMap<>();
		HDR_MEMBER_CONSUME.put("手机号码", "mobile");
		HDR_MEMBER_CONSUME.put("消费金额", "consumption");
	}

	private static final Map<String, String> NEED_MEMBER_CONSUME;
	static {
		NEED_MEMBER_CONSUME = new LinkedHashMap<>();
		NEED_MEMBER_CONSUME.put("手机号码", "mobile");
		NEED_MEMBER_CONSUME.put("消费金额", "consumption");
	}

	public static UploadHeaderTitle MEMBER_CONSUME() {
		return new UploadHeaderTitle(HDR_MEMBER_CONSUME, NEED_MEMBER_CONSUME, MemberConsumeHeaderInfo.map());
	}

	private static final Map<String, String> HDR_MEMBER_INFO;
	static {
		HDR_MEMBER_INFO = new LinkedHashMap<>();
		HDR_MEMBER_INFO.put("会员手机号", "mobile");
		HDR_MEMBER_INFO.put("原实体卡号", "offline_card_code");
		HDR_MEMBER_INFO.put("姓名", "username");
		HDR_MEMBER_INFO.put("性别", "sex");
		HDR_MEMBER_INFO.put("会员等级", "grade_name");
		HDR_MEMBER_INFO.put("生日", "birthday");
		HDR_MEMBER_INFO.put("入会日期", "created");
		HDR_MEMBER_INFO.put("邮箱", "email");
		HDR_MEMBER_INFO.put("地址", "address");
		HDR_MEMBER_INFO.put("标签", "tags");
		HDR_MEMBER_INFO.put("积分", "point");
	}

	private static final Map<String, String> NEED_MEMBER_INFO;
	static {
		NEED_MEMBER_INFO = new LinkedHashMap<>();
		NEED_MEMBER_INFO.put("会员手机号", "mobile");
		NEED_MEMBER_INFO.put("姓名", "username");
		NEED_MEMBER_INFO.put("性别", "sex");
		NEED_MEMBER_INFO.put("会员等级", "grade_name");
		NEED_MEMBER_INFO.put("入会日期", "created");
	}

	public static UploadHeaderTitle MEMBER_INFO() {
		return new UploadHeaderTitle(HDR_MEMBER_INFO, NEED_MEMBER_INFO, MemberInfoHeaderInfo.map());
	}

	private static final Map<String, String> HDR_MEMBER_UPDATE;
	static {
		HDR_MEMBER_UPDATE = new LinkedHashMap<>();
		HDR_MEMBER_UPDATE.put("手机号码", "mobile");
		HDR_MEMBER_UPDATE.put("姓名", "username");
		HDR_MEMBER_UPDATE.put("性别", "sex");
		HDR_MEMBER_UPDATE.put("会员等级", "grade_name");
		HDR_MEMBER_UPDATE.put("邮箱", "email");
		HDR_MEMBER_UPDATE.put("标签", "tags");
		HDR_MEMBER_UPDATE.put("禁用", "disabled");
		HDR_MEMBER_UPDATE.put("积分", "point");
	}

	private static final Map<String, String> NEED_MEMBER_UPDATE;
	static {
		NEED_MEMBER_UPDATE = new LinkedHashMap<>();
		NEED_MEMBER_UPDATE.put("手机号码", "mobile");
	}

	public static UploadHeaderTitle MEMBER_UPDATE() {
		return new UploadHeaderTitle(HDR_MEMBER_UPDATE, NEED_MEMBER_UPDATE, MemberUpdateHeaderInfo.map());
	}

	private static final Map<String, String> HDR_NORMAL_EPIDEMIC_GOODS;
	static {
		HDR_NORMAL_EPIDEMIC_GOODS = new LinkedHashMap<>();
		HDR_NORMAL_EPIDEMIC_GOODS.put("商品条形码", "barcode");
		HDR_NORMAL_EPIDEMIC_GOODS.put("商品编号", "item_bn");
		HDR_NORMAL_EPIDEMIC_GOODS.put("是否设为疫情商品", "is_epidemic");
	}

	private static final Map<String, String> NEED_NORMAL_EPIDEMIC_GOODS;
	static {
		NEED_NORMAL_EPIDEMIC_GOODS = new LinkedHashMap<>();
		NEED_NORMAL_EPIDEMIC_GOODS.put("商品条形码", "barcode");
		NEED_NORMAL_EPIDEMIC_GOODS.put("商品编号", "item_bn");
		NEED_NORMAL_EPIDEMIC_GOODS.put("是否设为疫情商品", "is_epidemic");
	}

	public static UploadHeaderTitle NORMAL_EPIDEMIC_GOODS() {
		return new UploadHeaderTitle(HDR_NORMAL_EPIDEMIC_GOODS, NEED_NORMAL_EPIDEMIC_GOODS, NormalEpidemicGoodsHeaderInfo.map());
	}

	private static final Map<String, String> HDR_NORMAL_GOODS;
	static {
		HDR_NORMAL_GOODS = new LinkedHashMap<>();
		HDR_NORMAL_GOODS.put("管理分类", "item_main_category");
		HDR_NORMAL_GOODS.put("商品名称", "item_name");
		HDR_NORMAL_GOODS.put("SPU编码", "goods_bn");
		HDR_NORMAL_GOODS.put("SKU编码", "item_bn");
		HDR_NORMAL_GOODS.put("简介", "brief");
		HDR_NORMAL_GOODS.put("销售价", "price");
		HDR_NORMAL_GOODS.put("市场价", "market_price");
		HDR_NORMAL_GOODS.put("成本价", "cost_price");
		HDR_NORMAL_GOODS.put("起订量", "start_num");
		HDR_NORMAL_GOODS.put("会员价", "member_price");
		HDR_NORMAL_GOODS.put("审核状态", "audit_status");
		HDR_NORMAL_GOODS.put("库存", "store");
		HDR_NORMAL_GOODS.put("头图", "pics");
		HDR_NORMAL_GOODS.put("详情图", "intro");
		HDR_NORMAL_GOODS.put("规格图", "item_spec_pics");
		HDR_NORMAL_GOODS.put("视频", "videos");
		HDR_NORMAL_GOODS.put("品牌", "goods_brand");
		HDR_NORMAL_GOODS.put("运费模板", "templates_id");
		HDR_NORMAL_GOODS.put("销售分类", "item_category");
		HDR_NORMAL_GOODS.put("重量", "weight");
		HDR_NORMAL_GOODS.put("条形码", "barcode");
		HDR_NORMAL_GOODS.put("单位", "item_unit");
		HDR_NORMAL_GOODS.put("规格值", "item_spec");
		HDR_NORMAL_GOODS.put("参数值", "item_params");
		HDR_NORMAL_GOODS.put("发货时间", "delivery_time");
		HDR_NORMAL_GOODS.put("是否支持分润", "is_profit");
		HDR_NORMAL_GOODS.put("分润类型", "profit_type");
		HDR_NORMAL_GOODS.put("拉新分润", "profit");
		HDR_NORMAL_GOODS.put("推广分润", "popularize_profit");
		HDR_NORMAL_GOODS.put("商品状态", "approve_status");
	}

	private static final Map<String, String> NEED_NORMAL_GOODS;
	static {
		NEED_NORMAL_GOODS = new LinkedHashMap<>();
		NEED_NORMAL_GOODS.put("管理分类", "item_main_category");
		NEED_NORMAL_GOODS.put("商品名称", "item_name");
		NEED_NORMAL_GOODS.put("销售价", "price");
		NEED_NORMAL_GOODS.put("库存", "store");
		NEED_NORMAL_GOODS.put("运费模板", "templates_id");
		NEED_NORMAL_GOODS.put("销售分类", "item_category");
		NEED_NORMAL_GOODS.put("分润类型", "profit_type");
		NEED_NORMAL_GOODS.put("发货时间", "delivery_time");
	}

	public static UploadHeaderTitle NORMAL_GOODS() {
		return new UploadHeaderTitle(HDR_NORMAL_GOODS, NEED_NORMAL_GOODS, NormalGoodsHeaderInfo.map());
	}

	private static final Map<String, String> HDR_NORMAL_GOODS_PROFIT;
	static {
		HDR_NORMAL_GOODS_PROFIT = new LinkedHashMap<>();
		HDR_NORMAL_GOODS_PROFIT.put("商品编码", "item_bn");
		HDR_NORMAL_GOODS_PROFIT.put("分润类型", "profit_type");
		HDR_NORMAL_GOODS_PROFIT.put("拉新分润", "profit");
		HDR_NORMAL_GOODS_PROFIT.put("推广分润", "popularize_profit");
	}

	private static final Map<String, String> NEED_NORMAL_GOODS_PROFIT;
	static {
		NEED_NORMAL_GOODS_PROFIT = new LinkedHashMap<>();
		NEED_NORMAL_GOODS_PROFIT.put("商品编码", "item_bn");
		NEED_NORMAL_GOODS_PROFIT.put("分润类型", "profit_type");
		NEED_NORMAL_GOODS_PROFIT.put("拉新分润", "profit");
		NEED_NORMAL_GOODS_PROFIT.put("推广分润", "popularize_profit");
	}

	public static UploadHeaderTitle NORMAL_GOODS_PROFIT() {
		return new UploadHeaderTitle(HDR_NORMAL_GOODS_PROFIT, NEED_NORMAL_GOODS_PROFIT, NormalGoodsProfitHeaderInfo.map());
	}

	private static final Map<String, String> HDR_NORMAL_GOODS_STORE;
	static {
		HDR_NORMAL_GOODS_STORE = new LinkedHashMap<>();
		HDR_NORMAL_GOODS_STORE.put("店铺ID", "did");
		HDR_NORMAL_GOODS_STORE.put("商品编码", "item_bn");
		HDR_NORMAL_GOODS_STORE.put("库存", "store");
	}

	private static final Map<String, String> NEED_NORMAL_GOODS_STORE;
	static {
		NEED_NORMAL_GOODS_STORE = new LinkedHashMap<>();
		NEED_NORMAL_GOODS_STORE.put("店铺ID", "did");
		NEED_NORMAL_GOODS_STORE.put("商品编码", "item_bn");
		NEED_NORMAL_GOODS_STORE.put("库存", "store");
	}

	public static UploadHeaderTitle NORMAL_GOODS_STORE() {
		return new UploadHeaderTitle(HDR_NORMAL_GOODS_STORE, NEED_NORMAL_GOODS_STORE, NormalGoodsStoreHeaderInfo.map());
	}

	private static final Map<String, String> HDR_NORMAL_GOODS_TAG;
	static {
		HDR_NORMAL_GOODS_TAG = new LinkedHashMap<>();
		HDR_NORMAL_GOODS_TAG.put("商品货号", "item_bn");
		HDR_NORMAL_GOODS_TAG.put("标签名称", "tag_name");
	}

	private static final Map<String, String> NEED_NORMAL_GOODS_TAG;
	static {
		NEED_NORMAL_GOODS_TAG = new LinkedHashMap<>();
		NEED_NORMAL_GOODS_TAG.put("商品货号", "item_bn");
		NEED_NORMAL_GOODS_TAG.put("标签名称", "tag_name");
	}

	public static UploadHeaderTitle NORMAL_GOODS_TAG() {
		return new UploadHeaderTitle(HDR_NORMAL_GOODS_TAG, NEED_NORMAL_GOODS_TAG, NormalGoodsTagHeaderInfo.map());
	}

	private static final Map<String, String> HDR_NORMAL_ORDERS;
	static {
		HDR_NORMAL_ORDERS = new LinkedHashMap<>();
		HDR_NORMAL_ORDERS.put("订单号", "order_id");
		HDR_NORMAL_ORDERS.put("快递单号", "delivery_code");
		HDR_NORMAL_ORDERS.put("快递公司", "delivery_corp_name");
	}

	private static final Map<String, String> NEED_NORMAL_ORDERS;
	static {
		NEED_NORMAL_ORDERS = new LinkedHashMap<>();
		NEED_NORMAL_ORDERS.put("订单号", "order_id");
		NEED_NORMAL_ORDERS.put("快递单号", "delivery_code");
		NEED_NORMAL_ORDERS.put("快递公司", "delivery_corp_name");
	}

	public static UploadHeaderTitle NORMAL_ORDERS() {
		return new UploadHeaderTitle(HDR_NORMAL_ORDERS, NEED_NORMAL_ORDERS, NormalOrdersHeaderInfo.map());
	}

	private static final Map<String, String> HDR_NORMAL_ORDERS_CANCEL;
	static {
		HDR_NORMAL_ORDERS_CANCEL = new LinkedHashMap<>();
		HDR_NORMAL_ORDERS_CANCEL.put("订单号", "order_id");
		HDR_NORMAL_ORDERS_CANCEL.put("取消原因", "cancel_reason");
	}

	private static final Map<String, String> NEED_NORMAL_ORDERS_CANCEL;
	static {
		NEED_NORMAL_ORDERS_CANCEL = new LinkedHashMap<>();
		NEED_NORMAL_ORDERS_CANCEL.put("订单号", "order_id");
		NEED_NORMAL_ORDERS_CANCEL.put("取消原因", "cancel_reason");
	}

	public static UploadHeaderTitle NORMAL_ORDERS_CANCEL() {
		return new UploadHeaderTitle(HDR_NORMAL_ORDERS_CANCEL, NEED_NORMAL_ORDERS_CANCEL, NormalOrdersCancelHeaderInfo.map());
	}

	private static final Map<String, String> HDR_NORMAL_POINTSMALL_GOODS;
	static {
		HDR_NORMAL_POINTSMALL_GOODS = new LinkedHashMap<>();
		HDR_NORMAL_POINTSMALL_GOODS.put("管理分类", "item_main_category");
		HDR_NORMAL_POINTSMALL_GOODS.put("商品名称", "item_name");
		HDR_NORMAL_POINTSMALL_GOODS.put("商品编码", "item_bn");
		HDR_NORMAL_POINTSMALL_GOODS.put("简介", "brief");
		HDR_NORMAL_POINTSMALL_GOODS.put("商品价格", "price");
		HDR_NORMAL_POINTSMALL_GOODS.put("市场价", "market_price");
		HDR_NORMAL_POINTSMALL_GOODS.put("成本价", "cost_price");
		HDR_NORMAL_POINTSMALL_GOODS.put("积分价格", "point");
		HDR_NORMAL_POINTSMALL_GOODS.put("库存", "store");
		HDR_NORMAL_POINTSMALL_GOODS.put("图片", "pics");
		HDR_NORMAL_POINTSMALL_GOODS.put("视频", "videos");
		HDR_NORMAL_POINTSMALL_GOODS.put("品牌", "goods_brand");
		HDR_NORMAL_POINTSMALL_GOODS.put("运费模板", "templates_id");
		HDR_NORMAL_POINTSMALL_GOODS.put("分类", "item_category");
		HDR_NORMAL_POINTSMALL_GOODS.put("重量", "weight");
		HDR_NORMAL_POINTSMALL_GOODS.put("条形码", "barcode");
		HDR_NORMAL_POINTSMALL_GOODS.put("单位", "item_unit");
		HDR_NORMAL_POINTSMALL_GOODS.put("规格值", "item_spec");
		HDR_NORMAL_POINTSMALL_GOODS.put("参数值", "item_params");
	}

	private static final Map<String, String> NEED_NORMAL_POINTSMALL_GOODS;
	static {
		NEED_NORMAL_POINTSMALL_GOODS = new LinkedHashMap<>();
		NEED_NORMAL_POINTSMALL_GOODS.put("商品名称", "item_name");
		NEED_NORMAL_POINTSMALL_GOODS.put("商品价格", "price");
		NEED_NORMAL_POINTSMALL_GOODS.put("积分价格", "point");
		NEED_NORMAL_POINTSMALL_GOODS.put("库存", "store");
		NEED_NORMAL_POINTSMALL_GOODS.put("运费模板", "templates_id");
		NEED_NORMAL_POINTSMALL_GOODS.put("分类", "item_category");
		NEED_NORMAL_POINTSMALL_GOODS.put("管理分类", "item_main_category");
		NEED_NORMAL_POINTSMALL_GOODS.put("图片", "pics");
	}

	public static UploadHeaderTitle NORMAL_POINTSMALL_GOODS() {
		return new UploadHeaderTitle(HDR_NORMAL_POINTSMALL_GOODS, NEED_NORMAL_POINTSMALL_GOODS, NormalPointsmallGoodsHeaderInfo.map());
	}

	private static final Map<String, String> HDR_NORMAL_POINTSMALL_GOODS_STORE;
	static {
		HDR_NORMAL_POINTSMALL_GOODS_STORE = new LinkedHashMap<>();
		HDR_NORMAL_POINTSMALL_GOODS_STORE.put("商品编码", "item_bn");
		HDR_NORMAL_POINTSMALL_GOODS_STORE.put("库存", "store");
	}

	private static final Map<String, String> NEED_NORMAL_POINTSMALL_GOODS_STORE;
	static {
		NEED_NORMAL_POINTSMALL_GOODS_STORE = new LinkedHashMap<>();
		NEED_NORMAL_POINTSMALL_GOODS_STORE.put("商品编码", "item_bn");
		NEED_NORMAL_POINTSMALL_GOODS_STORE.put("库存", "store");
	}

	public static UploadHeaderTitle NORMAL_POINTSMALL_GOODS_STORE() {
		return new UploadHeaderTitle(HDR_NORMAL_POINTSMALL_GOODS_STORE, NEED_NORMAL_POINTSMALL_GOODS_STORE, NormalPointsmallGoodsStoreHeaderInfo.map());
	}

	private static final Map<String, String> HDR_PURCHASE_GOODS;
	static {
		HDR_PURCHASE_GOODS = new LinkedHashMap<>();
		HDR_PURCHASE_GOODS.put("商品编码", "item_bn");
		HDR_PURCHASE_GOODS.put("商品名称", "item_name");
		HDR_PURCHASE_GOODS.put("每人限购", "limit_num");
		HDR_PURCHASE_GOODS.put("每人限额", "limit_fee");
	}

	private static final Map<String, String> NEED_PURCHASE_GOODS;
	static {
		NEED_PURCHASE_GOODS = new LinkedHashMap<>();
		NEED_PURCHASE_GOODS.put("商品编码", "item_bn");
		NEED_PURCHASE_GOODS.put("商品名称", "item_name");
		NEED_PURCHASE_GOODS.put("每人限购", "limit_num");
		NEED_PURCHASE_GOODS.put("每人限额", "limit_fee");
	}

	public static UploadHeaderTitle PURCHASE_GOODS() {
		return new UploadHeaderTitle(HDR_PURCHASE_GOODS, NEED_PURCHASE_GOODS, PurchaseGoodsHeaderInfo.map());
	}

	private static final Map<String, String> HDR_SELFORM_REGISTRATION_RECORD;
	static {
		HDR_SELFORM_REGISTRATION_RECORD = new LinkedHashMap<>();
		HDR_SELFORM_REGISTRATION_RECORD.put("报名申请编号", "record_id");
		HDR_SELFORM_REGISTRATION_RECORD.put("会员手机号码", "mobile");
		HDR_SELFORM_REGISTRATION_RECORD.put("审核结果", "review_result");
		HDR_SELFORM_REGISTRATION_RECORD.put("拒绝原因", "reason");
	}

	private static final Map<String, String> NEED_SELFORM_REGISTRATION_RECORD;
	static {
		NEED_SELFORM_REGISTRATION_RECORD = new LinkedHashMap<>();
		NEED_SELFORM_REGISTRATION_RECORD.put("会员手机号码", "mobile");
		NEED_SELFORM_REGISTRATION_RECORD.put("报名申请编号", "record_id");
		NEED_SELFORM_REGISTRATION_RECORD.put("审核结果", "review_result");
	}

	public static UploadHeaderTitle SELFORM_REGISTRATION_RECORD() {
		return new UploadHeaderTitle(HDR_SELFORM_REGISTRATION_RECORD, NEED_SELFORM_REGISTRATION_RECORD, SelformRegistrationRecordHeaderInfo.map());
	}

	private static final Map<String, String> HDR_SUPPLIER_GOODS;
	static {
		HDR_SUPPLIER_GOODS = new LinkedHashMap<>();
		HDR_SUPPLIER_GOODS.put("管理分类", "item_main_category");
		HDR_SUPPLIER_GOODS.put("商品名称", "item_name");
		HDR_SUPPLIER_GOODS.put("SPU编码", "goods_bn");
		HDR_SUPPLIER_GOODS.put("SKU编码", "item_bn");
		HDR_SUPPLIER_GOODS.put("供应商货号", "supplier_goods_bn");
		HDR_SUPPLIER_GOODS.put("简介", "brief");
		HDR_SUPPLIER_GOODS.put("销售价", "price");
		HDR_SUPPLIER_GOODS.put("市场价", "market_price");
		HDR_SUPPLIER_GOODS.put("成本价", "cost_price");
		HDR_SUPPLIER_GOODS.put("起订量", "start_num");
		HDR_SUPPLIER_GOODS.put("库存", "store");
		HDR_SUPPLIER_GOODS.put("头图", "pics");
		HDR_SUPPLIER_GOODS.put("详情图", "intro");
		HDR_SUPPLIER_GOODS.put("规格图", "item_spec_pics");
		HDR_SUPPLIER_GOODS.put("视频", "videos");
		HDR_SUPPLIER_GOODS.put("品牌", "goods_brand");
		HDR_SUPPLIER_GOODS.put("运费模板", "templates_id");
		HDR_SUPPLIER_GOODS.put("销售分类", "item_category");
		HDR_SUPPLIER_GOODS.put("重量", "weight");
		HDR_SUPPLIER_GOODS.put("条形码", "barcode");
		HDR_SUPPLIER_GOODS.put("单位", "item_unit");
		HDR_SUPPLIER_GOODS.put("规格值", "item_spec");
		HDR_SUPPLIER_GOODS.put("参数值", "item_params");
		HDR_SUPPLIER_GOODS.put("供应状态", "is_market");
	}

	private static final Map<String, String> NEED_SUPPLIER_GOODS;
	static {
		NEED_SUPPLIER_GOODS = new LinkedHashMap<>();
		NEED_SUPPLIER_GOODS.put("管理分类", "item_main_category");
		NEED_SUPPLIER_GOODS.put("商品名称", "item_name");
		NEED_SUPPLIER_GOODS.put("销售价", "price");
		NEED_SUPPLIER_GOODS.put("库存", "store");
		NEED_SUPPLIER_GOODS.put("运费模板", "templates_id");
		NEED_SUPPLIER_GOODS.put("销售分类", "item_category");
	}

	public static UploadHeaderTitle SUPPLIER_GOODS() {
		return new UploadHeaderTitle(HDR_SUPPLIER_GOODS, NEED_SUPPLIER_GOODS, SupplierGoodsHeaderInfo.map());
	}

	private static final Map<String, String> HDR_UPDATE_DISTRIBUTION_ITEM;
	static {
		HDR_UPDATE_DISTRIBUTION_ITEM = new LinkedHashMap<>();
		HDR_UPDATE_DISTRIBUTION_ITEM.put("店铺ID", "distribution_id");
		HDR_UPDATE_DISTRIBUTION_ITEM.put("店铺号", "shop_code");
		HDR_UPDATE_DISTRIBUTION_ITEM.put("商品SPU", "spu_bn");
		HDR_UPDATE_DISTRIBUTION_ITEM.put("商品货号", "item_bn");
		HDR_UPDATE_DISTRIBUTION_ITEM.put("是否上架", "is_onsale");
		HDR_UPDATE_DISTRIBUTION_ITEM.put("是否总部发货", "is_total_store");
		HDR_UPDATE_DISTRIBUTION_ITEM.put("商品库存", "item_store");
		HDR_UPDATE_DISTRIBUTION_ITEM.put("商品价格", "item_price");
	}

	private static final Map<String, String> NEED_UPDATE_DISTRIBUTION_ITEM;
	static {
		NEED_UPDATE_DISTRIBUTION_ITEM = new LinkedHashMap<>();
		NEED_UPDATE_DISTRIBUTION_ITEM.put("商品SPU", "spu_bn");
		NEED_UPDATE_DISTRIBUTION_ITEM.put("商品货号", "item_bn");
	}

	public static UploadHeaderTitle UPDATE_DISTRIBUTION_ITEM() {
		return new UploadHeaderTitle(HDR_UPDATE_DISTRIBUTION_ITEM, NEED_UPDATE_DISTRIBUTION_ITEM, UpdateDistributionItemHeaderInfo.map());
	}

	private static final Map<String, String> HDR_UPLOAD_DISTRIBUTOR_WHITE;
	static {
		HDR_UPLOAD_DISTRIBUTOR_WHITE = new LinkedHashMap<>();
		HDR_UPLOAD_DISTRIBUTOR_WHITE.put("手机号", "mobile");
		HDR_UPLOAD_DISTRIBUTOR_WHITE.put("姓名", "username");
		HDR_UPLOAD_DISTRIBUTOR_WHITE.put("店铺号", "distributor_no");
	}

	private static final Map<String, String> NEED_UPLOAD_DISTRIBUTOR_WHITE;
	static {
		NEED_UPLOAD_DISTRIBUTOR_WHITE = new LinkedHashMap<>();
		NEED_UPLOAD_DISTRIBUTOR_WHITE.put("手机号", "mobile");
		NEED_UPLOAD_DISTRIBUTOR_WHITE.put("姓名", "username");
		NEED_UPLOAD_DISTRIBUTOR_WHITE.put("店铺号", "distributor_no");
	}

	public static UploadHeaderTitle UPLOAD_DISTRIBUTOR_WHITE() {
		return new UploadHeaderTitle(HDR_UPLOAD_DISTRIBUTOR_WHITE, NEED_UPLOAD_DISTRIBUTOR_WHITE, UploadDistributorWhiteHeaderInfo.map());
	}

	private static final Map<String, String> HDR_UPLOAD_TB_ITEMS;
	static {
		HDR_UPLOAD_TB_ITEMS = new LinkedHashMap<>();
		HDR_UPLOAD_TB_ITEMS.put("商品链接", "item_url");
		HDR_UPLOAD_TB_ITEMS.put("类目ID", "category_id");
	}

	private static final Map<String, String> NEED_UPLOAD_TB_ITEMS;
	static {
		NEED_UPLOAD_TB_ITEMS = new LinkedHashMap<>();
	}

	public static UploadHeaderTitle UPLOAD_TB_ITEMS() {
		return new UploadHeaderTitle(HDR_UPLOAD_TB_ITEMS, NEED_UPLOAD_TB_ITEMS, UploadTbItemsHeaderInfo.map());
	}

	private static final Map<String, String> HDR_WHITELIST_CREATE;
	static {
		HDR_WHITELIST_CREATE = new LinkedHashMap<>();
		HDR_WHITELIST_CREATE.put("手机号码", "mobile");
		HDR_WHITELIST_CREATE.put("姓名", "name");
	}

	private static final Map<String, String> NEED_WHITELIST_CREATE;
	static {
		NEED_WHITELIST_CREATE = new LinkedHashMap<>();
		NEED_WHITELIST_CREATE.put("手机号码", "mobile");
		NEED_WHITELIST_CREATE.put("姓名", "name");
	}

	public static UploadHeaderTitle WHITELIST_CREATE() {
		return new UploadHeaderTitle(HDR_WHITELIST_CREATE, NEED_WHITELIST_CREATE, WhitelistCreateHeaderInfo.map());
	}

	public static UploadHeaderTitle forFileType(String fileType) {
		return switch (fileType) {
			case "adapay_tradedata" -> ADAPAY_TRADEDATA();
			case "community_chief" -> COMMUNITY_CHIEF();
			case "discount_goods" -> DISCOUNT_GOODS();
			case "distributor_info" -> DISTRIBUTOR_INFO();
			case "employee_purchase_activity_items" -> EMPLOYEE_PURCHASE_ACTIVITY_ITEMS();
			case "employee_purchase_activity_items_sort" -> EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT();
			case "employee_purchase_employees" -> EMPLOYEE_PURCHASE_EMPLOYEES();
			case "limit_sale_item" -> LIMIT_SALE_ITEM();
			case "marketing_goods" -> MARKETING_GOODS();
			case "member_consume" -> MEMBER_CONSUME();
			case "member_info" -> MEMBER_INFO();
			case "member_update" -> MEMBER_UPDATE();
			case "normal_epidemic_goods" -> NORMAL_EPIDEMIC_GOODS();
			case "normal_goods" -> NORMAL_GOODS();
			case "normal_goods_profit" -> NORMAL_GOODS_PROFIT();
			case "normal_goods_store" -> NORMAL_GOODS_STORE();
			case "normal_goods_tag" -> NORMAL_GOODS_TAG();
			case "normal_orders" -> NORMAL_ORDERS();
			case "normal_orders_cancel" -> NORMAL_ORDERS_CANCEL();
			case "normal_pointsmall_goods" -> NORMAL_POINTSMALL_GOODS();
			case "normal_pointsmall_goods_store" -> NORMAL_POINTSMALL_GOODS_STORE();
			case "purchase_goods" -> PURCHASE_GOODS();
			case "selform_registration_record" -> SELFORM_REGISTRATION_RECORD();
			case "supplier_goods" -> SUPPLIER_GOODS();
			case "update_distribution_item" -> UPDATE_DISTRIBUTION_ITEM();
			case "upload_distributor_white" -> UPLOAD_DISTRIBUTOR_WHITE();
			case "upload_tb_items" -> UPLOAD_TB_ITEMS();
			case "whitelist_create" -> WHITELIST_CREATE();
			default -> throw new IllegalArgumentException("unknown file_type: " + fileType);
		};
	}
}