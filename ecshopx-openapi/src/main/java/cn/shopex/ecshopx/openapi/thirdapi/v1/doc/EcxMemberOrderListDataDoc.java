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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiThirdApiV1MemberOrderListService#executeOpenapiMemberOrderLists}：
 * 固定过滤 order_type=normal 且 order_status ∉ {NOTPAY,CANCEL}，create_time 倒序分页；
 * 订单行裁剪为 11 键（items 仅首条订单行，见 {@code selectFirstItemByOrderId LIMIT 1}）。
 */
@Schema(name = "EcxMemberOrderListData", description = "会员订单分页 + 消费汇总")
public class EcxMemberOrderListDataDoc {

	@Schema(description = "分页信息")
	public PagerDoc pager;

	@ArraySchema(arraySchema = @Schema(description = "订单列表，create_time 倒序"))
	public List<OrderRowDoc> list;

	@Schema(description = "符合条件的订单总数（与 pager.count 同值）", example = "1")
	public Long total_count;

	@Schema(description = "该会员符合条件订单的累计金额（分）", example = "9900")
	public Long total_amount;

	@Schema(description = "平均订单金额（分；total_amount/total_count 向下取整，任一为 0 时 0）", example = "9900")
	public Long avg_amount;

	@Schema(name = "EcxMemberOrderListPager", description = "分页信息")
	public static class PagerDoc {

		@Schema(description = "符合条件的订单总数", example = "1")
		public Long count;

		@Schema(description = "当前页码", example = "1")
		public Integer page_no;

		@Schema(description = "每页条数", example = "10")
		public Integer page_size;

		@Schema(
				description = "按 user_id 分组的累计金额映射（键为 user_id，值为分；本接口单会员，仅一项或空对象）",
				example = "{\"5\":9900}")
		public Map<String, Long> total_amount;
	}

	@Schema(name = "EcxMemberOrderListOrderRow", description = "单笔订单（裁剪 11 键）")
	public static class OrderRowDoc {

		@Schema(description = "订单 ID", example = "10001")
		public Long order_id;

		@Schema(description = "订单状态（如 ACTIVE/DONE 等，经展示层映射）", nullable = true)
		public String order_status;

		@Schema(description = "订单状态文案", nullable = true)
		public String order_status_msg;

		@Schema(description = "订单状态描述", nullable = true)
		public String order_status_des;

		@Schema(description = "支付状态（如 PAYED）", nullable = true)
		public String pay_status;

		@Schema(description = "订单金额（分，库字符串原值）", example = "9900")
		public String total_fee;

		@Schema(description = "会员 ID", example = "5")
		public Long user_id;

		@Schema(
				description = "支付时间 yyyy-MM-dd HH:mm:ss（注意：实际由订单 create_time 格式化，非真实支付时间；≤0 为空串）",
				example = "2026-11-26 12:00:00")
		public String pay_time;

		@Schema(description = "购买件数（items 首条订单行 num 之和）", example = "1")
		public Long buy_num;

		@Schema(description = "含赠品标记：gift（首条订单行为赠品）/ normal", example = "normal")
		public String gift;

		@ArraySchema(arraySchema = @Schema(description = "订单商品行（仅首条，LIMIT 1；无行为空数组）"))
		public List<OrderItemDoc> items;
	}

	@Schema(name = "EcxMemberOrderListItem", description = "订单商品行（裁剪 12 键）")
	public static class OrderItemDoc {

		@Schema(description = "订单行 ID", example = "20001")
		public Long id;

		@Schema(description = "订单 ID（主单回填）", example = "10001")
		public Long order_id;

		@Schema(description = "会员 ID", example = "5")
		public Long user_id;

		@Schema(description = "商品/SKU ID", example = "1001")
		public Long item_id;

		@Schema(description = "货品编码", nullable = true)
		public String item_bn;

		@Schema(description = "商品名称", nullable = true)
		public String item_name;

		@Schema(description = "商品图", nullable = true)
		public String pic;

		@Schema(description = "购买数量", example = "1")
		public Long num;

		@Schema(description = "单价（分，库原值）", example = "9900")
		public Object price;

		@Schema(description = "行金额（分，库原值）", example = "9900")
		public Object item_fee;

		@Schema(description = "订单行类型：normal 普通 / gift 赠品", example = "normal")
		public String order_item_type;

		@Schema(
				description = "规格值（item_spec_desc 按首个冒号切分取后段；无冒号为 null；描述为空时空串）",
				nullable = true,
				example = "红")
		public String item_spec;
	}
}