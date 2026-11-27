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
 * 仅 OpenAPI 文档；data 为发货后重读 orders_associations 行的
 * {@code OrderAssociationAssociationDataAssembler#toAssociationDataMap}（35 固定键，LinkedHashMap 插入序）。
 * 注意：本次写入的 delivery_corp/delivery_code 只更新 normal_orders，不更新 orders_associations——
 * 响应中这两个键为关联表既有值（通常 null），不是本次入参回显；delivery_status/delivery_time/order_status
 * 已更新（全部发完 DONE/WAIT_BUYER_CONFIRM，否则 PARTAIL，代码拼写即 PARTAIL）。
 * coupon_discount_desc/member_discount_desc 默认空数组，库内 JSON 文本非空时解析覆盖（解析失败回退空数组，
 * 内部结构由存储内容决定，无固定叶子键）。
 */
@Schema(name = "EcxOrderDeliverData")
public class EcxOrderDeliverDataDoc {

	@Schema(description = "订单号（整型）", example = "100")
	public Long order_id;

	@Schema(description = "公众号 appid（可 null）", nullable = true)
	public String authorizer_appid;

	@Schema(description = "小程序 appid（可 null）", nullable = true)
	public String wxa_appid;

	@Schema(description = "订单标题（可 null）", nullable = true)
	public String title;

	@Schema(description = "订单金额，单位分（整型，可 null）", nullable = true, example = "8000")
	public Long total_fee;

	@Schema(description = "公司（租户）ID（整型）", example = "1")
	public Long company_id;

	@Schema(description = "店铺 ID（整型，可 null）", nullable = true)
	public Long shop_id;

	@Schema(description = "店铺名称（可 null）", nullable = true)
	public String store_name;

	@Schema(description = "用户 ID（整型，可 null）", nullable = true, example = "50")
	public Long user_id;

	@Schema(description = "导购员 ID（整型，默认 0）", example = "0")
	public Long salesman_id;

	@Schema(description = "推广员 user_id（整型，可 null）", nullable = true)
	public Long promoter_user_id;

	@Schema(description = "推广员店铺 id，实际为推广员的 user_id（整型，可 null）", nullable = true)
	public Long promoter_shop_id;

	@Schema(description = "订单来源 id（整型，可 null）", nullable = true)
	public Long source_id;

	@Schema(description = "订单监控页面 id（整型，可 null）", nullable = true)
	public Long monitor_id;

	@Schema(description = "手机号（关联表存储原值透传，可 null）", nullable = true)
	public String mobile;

	@Schema(
			description =
					"订单种类：normal 普通 / groups 拼团 / community 社区活动 / bargain 助力 / seckill 秒杀 / shopguide 导购",
			example = "normal")
	public String order_class;

	@Schema(
			description = "订单类型：service 服务业 / bargain 砍价 / distribution 分销 / normal 普通实体（可 null）",
			nullable = true,
			example = "normal")
	public String order_type;

	@Schema(
			description =
					"订单状态：DONE 完成 / PAYED 已支付 / NOTPAY 未支付 / CANCEL 已取消 / WAIT_BUYER_CONFIRM 待用户收货"
							+ "（全部发完时本接口置 WAIT_BUYER_CONFIRM）",
			example = "WAIT_BUYER_CONFIRM")
	public String order_status;

	@Schema(
			description = "订单创建时间（Unix 秒，整型；示例为格式演示值）",
			example = "1767225600")
	public Integer create_time;

	@Schema(description = "订单更新时间（Unix 秒，整型，本次发货刷新；可 null；示例为格式演示值）", nullable = true, example = "1767225600")
	public Integer update_time;

	@Schema(description = "是否分销订单（布尔，默认 false）", example = "false")
	public Boolean is_distribution;

	@Schema(description = "订单总分销金额，单位分（整型，默认 0）", example = "0")
	public Integer total_rebate;

	@Schema(
			description = "快递公司（关联表既有值——本次入参不回写本表，通常 null；不是本次 lc_code 回显）",
			nullable = true)
	public String delivery_corp;

	@Schema(
			description = "快递单号（关联表既有值——本次入参不回写本表，通常 null；不是本次 l_code 回显）",
			nullable = true)
	public String delivery_code;

	@Schema(description = "会员折扣金额，单位分（整型，默认 0）", example = "0")
	public Integer member_discount;

	@Schema(description = "优惠券抵扣金额，单位分（整型，默认 0）", example = "0")
	public Integer coupon_discount;

	@Schema(
			description =
					"优惠券使用详情：默认空数组；库内 JSON 文本非空时解析为对应 JSON 值（解析失败回退空数组，"
							+ "结构由存储内容决定，无固定叶子键）")
	public List<Object> coupon_discount_desc;

	@Schema(
			description =
					"会员折扣使用详情：默认空数组；库内 JSON 文本非空时解析为对应 JSON 值（解析失败回退空数组，"
							+ "结构由存储内容决定，无固定叶子键）")
	public List<Object> member_discount_desc;

	@Schema(
			description =
					"发货状态（本次刷新）：DONE 已发货 / PARTAIL 部分发货（代码拼写即 PARTAIL；表注释中的"
							+ " PARTAIL_DELIVERY 非实际写入值）/ PENDING 待发货",
			example = "DONE")
	public String delivery_status;

	@Schema(
			description = "发货时间（Unix 秒，整型，全部发完时本次写入；可 null；示例为格式演示值）",
			nullable = true,
			example = "1767225600")
	public Integer delivery_time;

	@Schema(
			description =
					"取消状态：NO_APPLY_CANCEL 未申请 / WAIT_PROCESS 等待审核 / REFUND_PROCESS 退款处理 /"
							+ " SUCCESS 取消成功 / FAILS 取消失败",
			example = "NO_APPLY_CANCEL")
	public String cancel_status;

	@Schema(description = "订单完成时间（Unix 秒，整型，未完成为 null）", nullable = true)
	public Long end_time;

	@Schema(description = "货币类型（默认 CNY）", example = "CNY")
	public String fee_type;

	@Schema(description = "货币汇率（浮点，默认 1.0）", example = "1.0")
	public Float fee_rate;

	@Schema(description = "货币符号（默认 ￥）", example = "￥")
	public String fee_symbol;
}