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
 * {@code OpenapiThirdApiV1MemberBrowseHistoryService#executeOpenapiMemberBrowseHistory}：
 * 足迹按 updated 倒序取页后关联商品行（默认语言覆盖 item_name），再按 updated 倒序输出。
 */
@Schema(
		name = "EcxMemberBrowserHistoryData",
		description = "浏览足迹：count 为本页返回条数（非总数）；无足迹/商品均已删除时 count=0、list=[]",
		example =
				"{\"list\":[{\"item_id\":1001,\"item_name\":\"示例商品\",\"price\":\"99.00\","
						+ "\"updated\":1764115200,\"pic\":\"https://example.test/key.jpg\"}],\"count\":1}")
public class EcxMemberBrowserHistoryDataDoc {

	@ArraySchema(arraySchema = @Schema(description = "足迹商品列表，按浏览时间 updated 倒序"))
	public List<HistoryItemDoc> list;

	@Schema(description = "本页返回条数（等于 list.size，非符合条件总数）", example = "1")
	public Integer count;

	@Schema(name = "EcxMemberBrowserHistoryItem", description = "单条足迹商品")
	public static class HistoryItemDoc {

		@Schema(description = "商品/SKU ID", example = "1001")
		public Long item_id;

		@Schema(description = "商品名称（默认语言分表覆盖）", nullable = true)
		public String item_name;

		@Schema(description = "售价，元，两位小数字符串（分/100，HALF_UP）", example = "99.00")
		public String price;

		@Schema(description = "最近浏览时间（Unix 秒，member_browse_history.updated）", example = "1764115200")
		public Long updated;

		@Schema(description = "商品首图 URL（pics 首个元素；无图为空串）", example = "https://example.test/key.jpg")
		public String pic;
	}
}