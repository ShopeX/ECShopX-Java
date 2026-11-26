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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code DiscountCardController#getDiscountCardList}
 * 经 {@code OpenapiRequestParams#mergeString} 读取的全部 key；筛选语义见
 * {@code OpenapiDiscountCardListFilterBuilder#build}，分页见 execute 的 parseIntFlexible。
 */
@Schema(
		name = "EcxDiscountcardListBody",
		description =
				"卡券列表查询参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）。"
						+ " 注意分页键为 pageSize（驼峰），非 page_size")
public class EcxDiscountcardListBodyDoc {

	@Schema(description = "页码，从 1 起；非整数/缺省按 1", defaultValue = "1", example = "1")
	public String page;

	@Schema(description = "每页条数；非整数/缺省按 20（驼峰键名）", defaultValue = "20", example = "20")
	public String pageSize;

	@Schema(description = "卡券名，模糊匹配", example = "满减券")
	public String title;

	@Schema(
			description = "卡券类型精确匹配：cash 代金券；discount 折扣券；gift 兑换券；缺省限定这三类（不含 new_gift）",
			allowableValues = {"cash", "discount", "gift", "new_gift"},
			example = "cash")
	public String card_type;

	@Schema(description = "卡券 ID 列表，逗号分隔；非整数项忽略；全部非法时不过滤", example = "3,4")
	public String card_ids;

	@Schema(
			description = "仅取字符串 \"true\" 时过滤未过期券（end_date>now 或 =0 或 null）；其他值不过滤",
			example = "true")
	public String is_valid;

	@Schema(
			description = "卡券状态精确匹配（PHP-truthy 才生效，\"0\" 不生效）：1 暂停；2 关闭；非整数按 -1（匹配不到）",
			example = "1")
	public String status;
}