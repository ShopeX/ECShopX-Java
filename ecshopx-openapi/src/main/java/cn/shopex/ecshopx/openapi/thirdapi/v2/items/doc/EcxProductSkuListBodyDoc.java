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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；POST 请求 JSON body，5 键与 query 同名双通道。
 * page/page_size 走 {@code OpenapiPaginationParams}（body 值非 null 优先，否则 query trim 非空；
 * **两键均必填**——缺失/非法 → E0001；page 须 ≥1；page_size 服务内归一：≤0→100、>2000→2000）；
 * country_code 走 {@code mergeCountryCode}（body 含键优先——null/空白归一 null 且不回退 query）；
 * time_begin/time_end 走 {@code originalString}。
 * 过滤见 {@code OpenapiThirdApiV2ProductSkuListService#executeOpenapiProductSkuList}
 * （item_type=normal + items.updated 窗口，item_id 降序）。
 */
@Schema(
		name = "EcxProductSkuListBody",
		description =
				"商品 SKU 列表参数（5 键，body 优先否则 query 同名键）。"
						+ " 按 items.updated 时间窗拉取普通类型（item_type=normal）SKU 行（含店铺商品，is_self 区分），"
						+ " item_id 降序。")
public class EcxProductSkuListBodyDoc {

	@Schema(
			description =
					"页码；**必填**（缺失/null/非整数/≤0 → E0001「page参数必填」）。服务内再 max(…,1)",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；**必填**（缺失/null/非整数 → E0001「page_size参数必填」；0/负数合法）。"
							+ "服务内归一：≤0 → 100；>2000 → 2000（上限截断）；生效值回显于 data.page_size",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "100")
	public String page_size;

	@Schema(
			description =
					"更新时间窗口起点（items.updated ≥）；PHP truthy（非 null/空串/\"0\"）才生效。"
							+ "仅接受 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd（系统时区，日期取 00:00:00）；"
							+ "其他格式静默解析为 0——解析值 ≤0 时起点过滤跳过。"
							+ "与 time_end 同时生效且起点>终点 → E5320「开始时间不能大于结束时间」",
			example = "2026-01-01 00:00:00")
	public String time_begin;

	@Schema(
			description =
					"更新时间窗口终点（items.updated ≤）；语义同 time_begin；格式非法静默为 0 → 过滤 updated≤0 通常查空",
			example = "2026-01-02 00:00:00")
	public String time_end;

	@Schema(
			description =
					"多语言代码（如 zh-CN/en-US）；trim 后生效，空白/缺失默认 zh-CN。"
							+ "按语言分表覆盖 name（item_name）；body 含键但值 null/空白时归一为默认（不回退 query）",
			example = "zh-CN")
	public String country_code;
}