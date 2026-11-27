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
 * 仅 OpenAPI 文档；data 结构（2 固定键，键序 delivery_num/list），来源
 * {@code OpenapiThirdApiV2OrderDeliverListService#executeGetOrderDeliveryListV2}。
 * list 为真实发货单行（orders_delivery_id 升序，status=1「已发货」）+ 可选末尾一条未发货合成行
 * （status=0「未发货」，delivery_id/corp/corp_name/code/time 均空字符串）。
 */
@Schema(name = "EcxOrderDeliverGetData")
public class EcxOrderDeliverGetDataDoc {

	@Schema(description = "真实发货单数量（整型，不含未发货合成行；无发货单为 0）", example = "1")
	public Integer delivery_num;

	@Schema(description = "发货单行数组（发货单 ID 升序；如有待发余量末尾附未发货合成行；无数据为空数组）")
	public List<RowDoc> list;

	/** 发货单行（9 固定键）。 */
	@Schema(name = "EcxOrderDeliverGetRow")
	public static class RowDoc {

		@Schema(
				description = "发货单 ID（整型；未发货合成行为空字符串 \"\"——类型为整型或字符串）",
				example = "1")
		public Object delivery_id;

		@Schema(description = "快递公司编码（null 转空串；未发货合成行为空串）", example = "SF")
		public String delivery_corp;

		@Schema(description = "快递公司显示名（null 转空串；发货时未匹配物流公司为「其他」；未发货合成行为空串）", example = "顺丰快递")
		public String delivery_corp_name;

		@Schema(description = "快递单号（null 转空串；未发货合成行为空串）", example = "SFTRACK001")
		public String delivery_code;

		@Schema(
				description =
						"发货时间（yyyy-MM-dd HH:mm:ss，固定 Asia/Shanghai 时区；库值 null/0 格式化为"
								+ " 1970-01-01 08:00:00；未发货合成行为空串；示例为格式演示值）",
				example = "2026-01-01 08:00:00")
		public String delivery_time;

		@Schema(description = "商品行数组（发货明细 ID 升序；未发货合成行为订单待发行，订单行 ID 升序）")
		public List<ItemDoc> items;

		@Schema(
				description =
						"数量合计（整型）：已发货行=明细 num 之和；未发货合成行=各行待发余量"
								+ "（num-已取消-已发）之和（注意其 items[].num 是订单行总 num，非余量）",
				example = "1")
		public Integer items_num;

		@Schema(description = "状态文案：已发货 / 未发货", example = "已发货")
		public String status_msg;

		@Schema(description = "状态（整型）：1 已发货；0 未发货（合成行）", example = "1")
		public Integer status;
	}

	/** 商品行（5 固定键）。 */
	@Schema(name = "EcxOrderDeliverGetItem")
	public static class ItemDoc {

		@Schema(description = "订单商品行 ID（整型；已发货行取发货明细的 order_items_id，未发货行取订单行 id）", example = "1")
		public Long order_items_id;

		@Schema(description = "货品 ID（整型，两表均非空列）", example = "1")
		public Long item_id;

		@Schema(
				description = "数量（整型，null 转 0）：已发货行=本次发货数量；未发货行=订单行总购买数量（非待发余量）",
				example = "1")
		public Integer num;

		@Schema(description = "商品名称（null 转空串）", example = "Item-A")
		public String item_name;

		@Schema(
				description = "商品图片（null 转空串；示例取物流公司 seed 中真实图片 URL，仅作格式演示）",
				example =
						"https://b-img-cdn.yuanyuanke.cn/image/21/2021/05/21/f425f5ae2e6032eb6fded1015fc979e4xPndgZMDIyfeGTWsMJOTcmWvaPQjWPbL")
		public String pic;
	}
}