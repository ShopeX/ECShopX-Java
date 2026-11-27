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
 * 仅 OpenAPI 文档；POST 请求 JSON body，4 键与 query 同名双通道。
 * page/page_size 走 {@code OpenapiGoodsListParams#resolveInt}（body 值非 null 优先，否则 query trim 非空；
 * 数字直取 intValue，字符串 Integer.parseInt，非法/空白回退默认——无上下限校验）；
 * time_begin/time_end 走 {@code originalString}（body 含键优先、null 视为未传，不 trim）。
 * 过滤见 {@code OpenapiThirdApiV2ProductGoodsListService#executeOpenapiProductGoodsList}
 * （items.updated 窗口，仅 company_id 过滤——含全部上下架状态与店铺商品，无 ORDER BY）。
 */
@Schema(
		name = "EcxProductGoodsListBody",
		description =
				"导购云店商品列表参数（4 键，body 含键优先否则 query 同名键）。"
						+ " 按 items.updated 时间窗增量拉取商品 SKU 行（含店铺商品，is_self 区分），无排序保证。")
public class EcxProductGoodsListBodyDoc {

	@Schema(
			description =
					"页码；默认 1。整数解析（非法/空白回退 1），无下限校验——"
							+ "page≤0 时 offset 为负 → SQL 错误 → HTTP 500",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 100。整数解析（非法/空白回退 100），无上限校验；"
							+ "**≤0 时全量返回（不分页，无 LIMIT）**",
			example = "100")
	public String page_size;

	@Schema(
			description =
					"更新时间窗口起点（items.updated ≥）；PHP truthy（非 null/空串/\"0\"）才生效。"
							+ "仅接受 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd（系统时区，日期取 00:00:00）；"
							+ "其他格式静默解析为 0——解析值 ≤0 时起点过滤直接跳过（等于不设下限）。"
							+ "与 time_end 同时生效且起点>终点 → E5320「开始时间不能大于结束时间」",
			example = "2026-01-01 00:00:00")
	public String time_begin;

	@Schema(
			description =
					"更新时间窗口终点（items.updated ≤）；语义同 time_begin；"
							+ "格式非法静默为 0 → 过滤 updated≤0 通常查空（终点无 >0 才生效的保护）",
			example = "2026-01-02 00:00:00")
	public String time_end;
}