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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；data 为 {@code OpenapiThirdApiV2OrderDetailFormatSupport#formatOrderInfoStruct}
 * 输出的 42 固定键（LinkedHashMap 插入序）。数据源 {@code AdminNormalOrderDetailService#buildOrderBundle}
 * （checkaftersales=false，detailFrom=api）——响应不含售后列表/交易列表等 bundle 其余槽位。
 * 字符串键 null 一律转 ""（shop_code 例外：无店铺时为 null）；金额单位分（整型）；
 * 时间键为 yyyy-MM-dd HH:mm:ss（系统时区），epoch ≤0 转 ""。
 */
@Schema(name = "EcxOrderGetData")
public class EcxOrderGetDataDoc {

	@Schema(description = "订单号（字符串回显）", example = "100")
	public String order_id;

	@Schema(description = "成功交易单号（无成功交易/空/\"0\" 时为 \"-\"；示例为格式演示值）", example = "-")
	public String trade_no;

	@Schema(description = "订单标题（null 转空串）", example = "")
	public String title;

	@Schema(
			description = "店铺编码（distributor_id>0 时查店铺 shop_code；平台单/查不到为 null——本键唯一可 null 字符串）",
			nullable = true)
	public String shop_code;

	@Schema(description = "订单总金额，单位分（整型，null/非法转 0）", example = "8000")
	public Integer total_fee;

	@Schema(description = "下单手机号（订单表存储原值透传，null 转空串）", example = "")
	public String mobile;

	@Schema(description = "运费，单位分（整型）", example = "0")
	public Integer freight_fee;

	@Schema(description = "商品金额，单位分（整型）", example = "8000")
	public Integer item_fee;

	@Schema(
			description = "收货方式：logistics 物流 / ziti 店铺自提 / dada 达达同城配 / merchant 商家自配",
			example = "logistics")
	public String receipt_type;

	@Schema(
			description = "订单状态：DONE 完成 / PAYED 已支付 / NOTPAY 未支付 / CANCEL 已取消 / WAIT_BUYER_CONFIRM 待收货",
			example = "PAYED")
	public String order_status;

	@Schema(
			description = "支付状态：NOTPAY 未支付 / PAYED 已支付 / ADVANCE_PAY 预付款完成 / TAIL_PAY 支付尾款中",
			example = "PAYED")
	public String pay_status;

	@Schema(
			description =
					"支付时间（yyyy-MM-dd HH:mm:ss，系统时区；取 SUCCESS 交易 time_expire，>9999999999 视为毫秒除 1000；"
							+ "无成功交易为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String pay_time;

	@Schema(
			description =
					"快递公司编码（null 转空串；无旧式单号时回填首张发货单 orders_delivery 的编码）",
			example = "SF")
	public String delivery_corp;

	@Schema(description = "快递单号（null 转空串；无旧式单号时回填首张发货单的单号）", example = "SFTRACK001")
	public String delivery_code;

	@Schema(
			description = "发货时间（yyyy-MM-dd HH:mm:ss，系统时区；epoch ≤0/null 转空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String delivery_time;

	@Schema(
			description = "订单完成时间（yyyy-MM-dd HH:mm:ss，系统时区；epoch ≤0/null 转空串）",
			example = "")
	public String end_time;

	@Schema(
			description = "发货状态：PENDING 待发货 / PARTAIL 部分发货（代码拼写）/ DONE 已发货",
			example = "DONE")
	public String delivery_status;

	@Schema(
			description =
					"取消状态：NO_APPLY_CANCEL 未申请 / WAIT_PROCESS 等待审核 / REFUND_PROCESS 退款处理 /"
							+ " SUCCESS 取消成功 / FAILS 取消失败",
			example = "NO_APPLY_CANCEL")
	public String cancel_status;

	@Schema(description = "收货人姓名（null 转空串）", example = "")
	public String receiver_name;

	@Schema(description = "收货人手机号（null 转空串）", example = "")
	public String receiver_mobile;

	@Schema(description = "收货邮编（null 转空串）", example = "")
	public String receiver_zip;

	@Schema(description = "收货省（null 转空串）", example = "")
	public String receiver_state;

	@Schema(description = "收货市（null 转空串）", example = "")
	public String receiver_city;

	@Schema(description = "收货区（null 转空串）", example = "")
	public String receiver_district;

	@Schema(description = "收货详细地址（null 转空串）", example = "")
	public String receiver_address;

	@Schema(description = "会员折扣金额，单位分（整型）", example = "0")
	public Integer member_discount;

	@Schema(description = "优惠券抵扣金额，单位分（整型）", example = "0")
	public Integer coupon_discount;

	@Schema(description = "优惠总额，单位分（整型；库值 0/null 时回退 会员折扣+优惠券）", example = "0")
	public Integer discount_fee;

	@Schema(description = "订单优惠明细数组（解析失败/无优惠为空数组；行 5 固定键）")
	public List<DiscountInfoDoc> discount_info;

	@Schema(
			description = "订单创建时间（yyyy-MM-dd HH:mm:ss，系统时区；epoch ≤0/null 转空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String create_time;

	@Schema(
			description = "订单更新时间（yyyy-MM-dd HH:mm:ss，系统时区；epoch ≤0/null 转空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String update_time;

	@Schema(description = "支付方式（订单表原值，如 wxpay/alipay/offline_pay；null 转空串）", example = "")
	public String pay_type;

	@Schema(description = "买家备注（null 转空串）", example = "")
	public String remark;

	@Schema(description = "店铺备注（null 转空串）", example = "")
	public String distributor_remark;

	@Schema(description = "积分抵扣使用的积分数（整型透传，可 null）", nullable = true, example = "0")
	public Integer point_use;

	@Schema(description = "积分抵扣金额，单位分（整型）", example = "0")
	public Integer point_fee;

	@Schema(description = "订单商品行数组（订单行 ID 升序；行 14 固定键）")
	public List<ItemDoc> items;

	@Schema(description = "用户 ID（整型透传，可 null）", nullable = true, example = "50")
	public Long user_id;

	@Schema(description = "会员用户名（members_info.username；无会员/无记录为空串）", example = "")
	public String username;

	@Schema(
			description =
					"微信 unionid（members_associations.unionid + wechat_users 按会员小程序 appid 匹配；"
							+ "任一环节缺失为空串）",
			example = "")
	public String unionid;

	@Schema(description = "微信 openid（同 unionid 匹配链路；缺失为空串）", example = "")
	public String openid;

	@Schema(
			description =
					"订单种类：normal 普通 / groups 拼团 / community 社区活动 / bargain 助力 / seckill 秒杀 /"
							+ " shopguide 导购 / pointsmall 积分商城 / excard 兑换券",
			example = "normal")
	public String order_class;

	/** 优惠明细行（5 固定键，parseDiscountInfoList：字符串键缺省 ""，discount_fee 缺省 0）。 */
	@Schema(name = "EcxOrderGetDiscountInfo")
	public static class DiscountInfoDoc {

		@Schema(description = "优惠活动/券 ID（字符串，缺省空串）", example = "")
		public String id;

		@Schema(description = "优惠类型（如 coupon_discount/member_discount 及活动类型；缺省空串）", example = "coupon_discount")
		public String type;

		@Schema(description = "优惠说明（字符串，缺省空串）", example = "")
		public String info;

		@Schema(description = "优惠规则（字符串，缺省空串）", example = "")
		public String rule;

		@Schema(description = "优惠金额，单位分（数值，缺省 0）", example = "0")
		public Number discount_fee;
	}

	/** 商品行（14 固定键，formatItems）。 */
	@Schema(name = "EcxOrderGetItem")
	public static class ItemDoc {

		@Schema(description = "商品 SPU ID（整型；按 item_id 反查 goods，查不到为 0）", example = "1")
		public Integer goods_id;

		@Schema(description = "商品编码/货号（null 转空串）", example = "")
		public String item_bn;

		@Schema(description = "商品名称（null 转空串）", example = "Item-A")
		public String item_name;

		@Schema(description = "购买数量（整型）", example = "1")
		public Integer num;

		@Schema(description = "单价，单位分（整型）", example = "8000")
		public Integer price;

		@Schema(description = "行总金额，单位分（整型）", example = "8000")
		public Integer total_fee;

		@Schema(description = "行商品金额，单位分（整型）", example = "8000")
		public Integer item_fee;

		@Schema(description = "行优惠明细数组（结构同订单级 discount_info，5 固定键）")
		public List<DiscountInfoDoc> discount_info;

		@Schema(description = "行分摊积分抵扣数（取订单行 share_points，整型透传，可 null）", nullable = true, example = "0")
		public Integer point_use;

		@Schema(description = "行积分抵扣金额，单位分（整型）", example = "0")
		public Integer point_fee;

		@Schema(description = "商品规格描述（null 转空串）", example = "")
		public String item_spec_desc;

		@Schema(description = "商品体积（整型透传，默认 0，可 null）", nullable = true, example = "0")
		public Integer volume;

		@Schema(description = "商品重量（浮点透传，默认 0.0，可 null）", nullable = true, example = "0.0")
		public Float weight;

		@Schema(description = "订单商品类型：normal 正常商品 / gift 赠品 / plus_buy 加价购商品", example = "normal")
		public String item_type;
	}
}