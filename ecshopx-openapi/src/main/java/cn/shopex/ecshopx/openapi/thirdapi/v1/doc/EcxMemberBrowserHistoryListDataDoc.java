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

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code MemberBrowseHistoryListService#getBrowseHistory}（total_count + 原始行）经
 * {@code OpenapiThirdApiV1MemberBrowseHistoryListService#trimBrowseHistoryForOpenApi}：
 * 每行追加 create_time（created 格式化），itemData 裁剪为 5 键子集（无商品行时为 []）。
 */
@Schema(
		name = "EcxMemberBrowserHistoryListData",
		description = "浏览足迹分页：total_count 为符合条件总数；list 按 updated 倒序",
		example =
				"{\"total_count\":1,\"list\":[{\"history_id\":9,\"company_id\":100,\"user_id\":5,"
						+ "\"item_id\":1001,\"created\":1764115200,\"updated\":1764115200,"
						+ "\"create_time\":\"2026-11-26 12:00:00\",\"itemData\":{\"item_id\":1001,"
						+ "\"item_bn\":\"A1\",\"item_name\":\"示例商品\",\"pics\":[\"https://example.test/key.jpg\"],"
						+ "\"price\":9900}}]}")
public class EcxMemberBrowserHistoryListDataDoc {

	@Schema(description = "符合条件的足迹总数", example = "1")
	public Long total_count;

	@ArraySchema(arraySchema = @Schema(description = "足迹列表，按 updated 倒序分页"))
	public List<HistoryRowDoc> list;

	@Schema(name = "EcxMemberBrowserHistoryListRow", description = "单条足迹（原始行 + create_time + 裁剪 itemData）")
	public static class HistoryRowDoc {

		@Schema(description = "足迹记录 ID（null 时 0）", example = "9")
		public Long history_id;

		@Schema(description = "租户/公司 ID", example = "100")
		public Long company_id;

		@Schema(description = "会员 ID", example = "5")
		public Long user_id;

		@Schema(description = "商品/SKU ID", nullable = true)
		public Long item_id;

		@Schema(description = "首次浏览时间（Unix 秒）", nullable = true)
		public Long created;

		@Schema(description = "最近浏览时间（Unix 秒）", nullable = true)
		public Long updated;

		@Schema(description = "created 格式化 yyyy-MM-dd HH:mm:ss；≤0/非法为空串", example = "2026-11-26 12:00:00")
		public String create_time;

		@Schema(description = "商品信息 5 键子集；商品不存在/已删除时为空数组 []")
		public ItemDataDoc itemData;
	}

	@Schema(name = "EcxMemberBrowserHistoryListItemData", description = "商品信息子集（默认语言分表覆盖 item_name）")
	public static class ItemDataDoc {

		@Schema(description = "商品/SKU ID（列表行格式，数值或字符串）", example = "1001")
		public Object item_id;

		@Schema(description = "货品编码", nullable = true)
		public String item_bn;

		@Schema(description = "商品名称（默认语言分表覆盖）", nullable = true)
		public String item_name;

		@Schema(description = "商品图片：JSON 数组或原样字符串；空/非法为 []")
		public Object pics;

		@Schema(description = "售价（分，列表行原值未转元）", example = "9900")
		public Object price;
	}
}