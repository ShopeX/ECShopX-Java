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
 * {@code OpenapiThirdApiV1MemberFrequentItemsService#executeOpenapiMemberFrequentItems}：
 * 已支付订单按 item_id 聚合（COUNT 订单行数 / SUM 数量），关联商品行后按 item_id 倒序输出。
 */
@Schema(
		name = "EcxMemberFrequentItemsData",
		description = "常购清单：count 为返回条数（非总数字段）；无购买记录/商品均已删除时 count=0、list=[]",
		example =
				"{\"list\":[{\"item_name\":\"示例商品\",\"price\":\"99.00\",\"buy_num\":3,"
						+ "\"pic\":\"https://example.test/key.jpg\",\"sales_num\":6}],\"count\":1}")
public class EcxMemberFrequentItemsDataDoc {

	@ArraySchema(arraySchema = @Schema(description = "常购商品列表，按 item_id 倒序"))
	public List<FrequentItemDoc> list;

	@Schema(description = "返回条数（等于 list.size）", example = "1")
	public Integer count;

	@Schema(name = "EcxMemberFrequentItemsItem", description = "单条常购商品")
	public static class FrequentItemDoc {

		@Schema(description = "商品名称（默认语言分表覆盖）", nullable = true)
		public String item_name;

		@Schema(description = "售价，元，两位小数字符串（分/100，HALF_UP）", example = "99.00")
		public String price;

		@Schema(description = "购买次数（时间段内已支付订单含该商品的订单行数 COUNT）", example = "3")
		public Long buy_num;

		@Schema(description = "商品首图 URL（pics 首个元素；无图为空串）", example = "https://example.test/key.jpg")
		public String pic;

		@Schema(description = "累计购买件数（SUM(num)）", example = "6")
		public Long sales_num;
	}
}