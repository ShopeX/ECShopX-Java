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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷（订单关联单对象，35 键），来源
 * {@code OrderAssociationAssociationDataAssembler#toAssociationDataMap}（发货后重读 orders_associations）。
 */
@Schema(name = "EcxOrderDeliverData", description = "发货后的订单关联信息（orders_associations 快照）")
public class EcxOrderDeliverDataDoc {

	@Schema(description = "订单 ID", example = "10001")
	public Long order_id;

	@Schema(description = "公众号授权 appid", nullable = true)
	public String authorizer_appid;

	@Schema(description = "小程序 appid", nullable = true)
	public String wxa_appid;

	@Schema(description = "订单标题", nullable = true)
	public String title;

	@Schema(description = "订单金额（分）", nullable = true)
	public Long total_fee;

	@Schema(description = "租户/公司 ID", example = "100")
	public Long company_id;

	@Schema(description = "门店 ID", nullable = true)
	public Long shop_id;

	@Schema(description = "门店名称", nullable = true)
	public String store_name;

	@Schema(description = "会员 ID", nullable = true)
	public Long user_id;

	@Schema(description = "导购员 ID", example = "0")
	public Long salesman_id;

	@Schema(description = "推广员会员 ID", nullable = true)
	public Long promoter_user_id;

	@Schema(description = "推广员店铺 ID", nullable = true)
	public Long promoter_shop_id;

	@Schema(description = "来源渠道 ID", nullable = true)
	public Long source_id;

	@Schema(description = "来源监控 ID", nullable = true)
	public Long monitor_id;

	@Schema(description = "收货手机号", nullable = true)
	public String mobile;

	@Schema(description = "订单种类（normal/groups/community 等）", example = "normal")
	public String order_class;

	@Schema(description = "订单类型", nullable = true)
	public String order_type;

	@Schema(description = "订单状态（全部发货后 WAIT_BUYER_CONFIRM）", example = "WAIT_BUYER_CONFIRM")
	public String order_status;

	@Schema(description = "下单时间（Unix 秒）", nullable = true)
	public Integer create_time;

	@Schema(description = "更新时间（Unix 秒，发货时刷新）", nullable = true)
	public Integer update_time;

	@Schema(description = "是否分销订单", example = "false")
	public Boolean is_distribution;

	@Schema(description = "返利总额", example = "0")
	public Integer total_rebate;

	@Schema(description = "快递公司编码（本次发货写入）", example = "SF")
	public String delivery_corp;

	@Schema(description = "快递单号（本次发货写入）", example = "SFTRACK001")
	public String delivery_code;

	@Schema(description = "会员优惠金额（分）", example = "0")
	public Integer member_discount;

	@Schema(description = "优惠券优惠金额（分）", example = "0")
	public Integer coupon_discount;

	@Schema(description = "优惠券优惠明细（JSON 解码；空/非法为 []）")
	public Object coupon_discount_desc;

	@Schema(description = "会员优惠明细（JSON 解码；空/非法为 []）")
	public Object member_discount_desc;

	@Schema(description = "发货状态：PENDING 未发；PARTAIL 部分发货（库值拼写）；DONE 全部发货", example = "DONE")
	public String delivery_status;

	@Schema(description = "发货时间（Unix 秒；全部发货时写入）", nullable = true)
	public Integer delivery_time;

	@Schema(description = "取消状态", example = "NO_APPLY_CANCEL")
	public String cancel_status;

	@Schema(description = "订单结束时间（Unix 秒）", nullable = true)
	public Long end_time;

	@Schema(description = "币种", example = "CNY")
	public String fee_type;

	@Schema(description = "汇率", example = "1.0")
	public Float fee_rate;

	@Schema(description = "货币符号", example = "￥")
	public String fee_symbol;
}