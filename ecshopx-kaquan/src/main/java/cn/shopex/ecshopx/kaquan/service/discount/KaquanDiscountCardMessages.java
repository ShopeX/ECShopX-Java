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

package cn.shopex.ecshopx.kaquan.service.discount;

/** 卡券模块创建/校验等业务提示文案常量 */
public final class KaquanDiscountCardMessages {

	public static final String CARD_TYPE_REQUIRED = "请填写卡券类型";
	public static final String TITLE_REQUIRED = "请填写卡券标题";
	public static final String COLOR_REQUIRED = "请填写卡券颜色";
	public static final String DESCRIPTION_REQUIRED = "请填写卡券使用说明";
	public static final String QUANTITY_RANGE_ERROR = "填写的卡券数量范围超出范围";
	public static final String BEGIN_TIME_REQUIRED = "请填写卡券开始时间";
	public static final String DATE_TYPE_REQUIRED = "请填写卡券日期类型";
	public static final String LEAST_COST_NUMERIC = "起用金额只能填写数字";
	public static final String REDUCE_COST_NUMERIC = "减免金额只能填写数字";
	public static final String DISCOUNT_NUMERIC = "折扣只能填写数字";
	public static final String ADD_CARD_VALIDITY_ERROR = "添加卡券出错,有效期数据填写有误,请仔细检查";
	public static final String ADD_CARD_TYPE_ERROR = "添加卡券出错, 活动类型错误";
	public static final String ADD_CARD_GENERAL_ERROR = "添加卡券出错.";
	public static final String CASH_REDUCE_MIN = "代金券减免金额必须小于最低消费金额";
	public static final String DISCOUNT_MIN_COST_NUMERIC = "折扣券最低消费金额必须为数字";
	public static final String DISCOUNT_MAX_LIMIT_NUMERIC = "折扣券最高限额必须为数字";
	public static final String DISCOUNT_MIN_COST_GTE_ZERO = "折扣券最低消费金额必须大于等于0";
	public static final String DISCOUNT_MAX_LIMIT_GT_MIN = "折扣券最高限额必须大于最低消费金额";
	public static final String MONEY_REDUCE_MIN = "现金券减免金额必须小于最低消费金额";
	public static final String APPLICABLE_STORES_REQUIRED = "适用门店必填";
	public static final String APPLICABLE_SHOPS_REQUIRED = "适用店铺必填";
	public static final String END_TIME_GT_TODAY = "有效期结束时间必须大于今天";
	public static final String END_TIME_GT_START_TIME = "有效期结束时间不可小于等于开始时间";
	public static final String END_DATE_EXPIRED = "结束日期已过期，请重新确认";
	public static final String QUANTITY_MIN_ONE = "发放数量至少为1份";
	public static final String COUPON_RULE_ID_DUPLICATE = "优惠券规则ID重复";
	public static final String DATA_CREATE_FAILED_RETRY = "数据创建失败，请稍后重试";
	public static final String PLEASE_SELECT_MAIN_CATEGORY = "请选择主分类";
	public static final String PLEASE_SELECT_TAGS = "请选择标签";
	public static final String PLEASE_SELECT_BRAND = "请选择品牌";
	public static final String NO_PRODUCTS_UNDER_OPTION = "该选项下没有商品，请重新选择";
	public static final String LOCK_TIME_EXCEED_USAGE = "锁定时间不能大于券使用时间";

	public static final String UPDATE_CARD_NO_ID = "修改卡券出错. 没有指定 card_id";
	public static final String UPDATE_CARD_ERROR = "修改卡券出错.";
	public static final String COUPON_INVALID = "该优惠券已失效";
	public static final String CARD_TYPE_ERROR = "卡券类型错误";
	public static final String PAUSED_CARD_CANNOT_REACTIVATE = "暂停的卡券不允许重新启用";
	public static final String REDUCE_QUANTITY_CANNOT_LESS_THAN_REMAINING = "减少数量不可少于目前剩余优惠券 %s";
	public static final String MEMBERS_ONLY_EXPAND_NOT_REDUCE_SCOPE = "指定会员只可扩大领用范围，不可缩小";
	public static final String LOCK_TIME_CANNOT_EXCEED_USAGE_TIME = "锁定时间不能大于券使用时间";
	public static final String UPDATE_CARD_START_TIME_ERROR = "修改卡券出错,有效期开始时间必须小于等于上次提交的开始时间";
	public static final String UPDATE_CARD_END_TIME_ERROR = "修改卡券出错,有效期结束时间必须大于等于上次提交的结束时间";
	public static final String DATA_UPDATE_FAILED_RETRY = "数据更新失败，请稍后重试";
	public static final String NO_UPDATE_DATA_FOUND = "未查询到更新数据";

	public static final String PACKAGE_ID_REQUIRED = "卡券包ID必填";
	public static final String PAGE_REQUIRED = "查询页码必填";
	public static final String PAGE_SIZE_REQUIRED = "每页条数必填且必须大于0";
	public static final String RECEIVE_TYPE_TEMPLATE = "模板领取送优惠券包";
	public static final String RECEIVE_TYPE_GRADE = "等级会员送优惠券包";
	public static final String RECEIVE_TYPE_VIP_GRADE = "购买会员送优惠券包";
	public static final String RECEIVE_TYPE_UNKNOWN = "未知";
	public static final String PACKAGE_ID_LIST_REQUIRED = "卡券包ID列表必填";
	public static final String SET_TYPE_REQUIRED = "卡券包ID必填";
	public static final String GRADE_ID_REQUIRED = "等级ID必填";
	public static final String PACKAGE_NOT_FOUND = "未找到该卡券包信息";
	public static final String PACKAGE_TITLE_REQUIRED = "券包标题必填且最多10字";
	public static final String PACKAGE_DESCRIBE_MAX = "券包描述最多20字";

	public static final String STOCK_FIELD_EXCEEDS_MAX = "库存字段超出最大值";

	public static final String EXCEED_LIMIT_COUNT = "已超过限领次数";
	public static final String RECEIVE_COUPON_EXPIRED = "优惠券已过期";
	public static final String RECEIVE_COUPON_OUT_OF_STOCK = "优惠券已领完";
	public static final String COUPON_NOT_FRONTEND_RECEIVABLE = "该优惠券不支持前台领取";
	public static final String COUPON_STATUS_ABNORMAL = "优惠券状态异常";
	public static final String MEMBER_GRADE_NOT_MATCH = "会员等级不符合领取条件";
	public static final String VIP_GRADE_NOT_MATCH_NOT_OPEN = "付费会员功能未开启或不可用";
	public static final String VIP_GRADE_NOT_MATCH = "付费会员等级不符合领取条件";
	public static final String USER_EXCEED_COUPON_LIMIT = "已超过个人限领数量";
	public static final String ALREADY_RECEIVED_COUPON = "已领取过该优惠券";
	public static final String FAILED_TO_RECEIVE_COUPON = "领取优惠券失败";

	public static final String NOT_MEMBER_CANNOT_RECEIVE = "您还不是会员，无法领取优惠券";
	public static final String GUIDE_COUPON_OVER_LIMIT = "超过该券导购可领取的最大数量";
	public static final String COUPON_RECORD_ADD_FAILED = "优惠券记录添加失败";

	private KaquanDiscountCardMessages() {}
}
