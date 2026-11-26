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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，8 键与 query 同名双通道
 * （{@code OpenapiRequestParams#originalString}：body 含键优先、值 null 视为未传、不 trim），
 * 分页解析见 {@code OpenapiThirdApiV2NormalItemsEntityListParams#resolve}，
 * 过滤语义见 {@code OpenapiThirdApiV2NormalItemsEntityListService#executeOpenapiGetEntityList}。
 */
@Schema(
		name = "EcxItemsEntityListBody",
		description =
				"平台商品 SPU 列表过滤参数（8 键，body 含键优先否则 query 同名键）。"
						+ " 固定过滤 item_type=normal 且 is_default=1（每 SPU 默认 SKU 一行）；"
						+ " 除 is_self 按键存在性判断外，其余过滤键按 PHP truthy 判断（空串/\"0\" 视为未传）。")
public class EcxItemsEntityListBodyDoc {

	@Schema(
			description =
					"页码；默认 1。PHP 式解析（空取默认、纯空白/非法→0、小数截断取整），再取 max(…,1)，"
							+ "因此负数/0 最终按 1 处理；「当前页面最小值为1」分支不可达",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。PHP 式解析后先校验原始值 ≥500 报 E5002「每页的大小最大为500！」（500 本身也报错），"
							+ "再取 max(…,20)，实际生效范围 [20,499]；「每页显示数量1-500」分支不可达",
			example = "20")
	public String page_size;

	@Schema(
			description =
					"商品状态过滤（onsale/instock/offline_sale/only_show）；truthy 时按 trim 值过滤："
							+ "EXISTS 同 goods_id 的任一 SKU 行 approve_status 等于该值（外层默认 SKU 行状态可不同）；"
							+ "空串/\"0\" 忽略",
			example = "onsale")
	public String approve_status;

	@Schema(
			description =
					"品牌 ID 过滤（整数字符串，须可 parseInt）；truthy 且解析非 0 时 brand_id 等值过滤；"
							+ "空串/\"0\" 忽略；非数字字符串触发 E5000（NumberFormatException message 透传）",
			example = "3")
	public String brand_id;

	@Schema(
			description =
					"分类 ID；PHP 旧契约遗留缺陷：truthy 时生成 items.category_id 等值过滤，"
							+ "该列不存在，任何非空非\"0\"取值都会触发 SQL 错误 → E5000（SQL 异常 message 透传）；空串/\"0\" 忽略",
			example = "1")
	public String category_id;

	@Schema(
			description =
					"更新时间下界（items.updated ≥，Unix 秒换算）；格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd（当日零点），"
							+ "系统时区解析；无法解析按 epoch 0（等于不过滤）；空串/\"0\" 忽略；"
							+ "与 time_end 同时 truthy 且开始>结束报 E5320「开始时间不能大于结束时间」（示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String time_begin;

	@Schema(
			description =
					"更新时间上界（items.updated ≤）；格式与解析同 time_begin；空串/\"0\" 忽略（示例为格式演示值）",
			example = "2026-12-31 23:59:59")
	public String time_end;

	@Schema(
			description =
					"自营/店铺过滤；按键存在性判断（query 出现或 body 含键即生效，与值无关）："
							+ "原始值恰为字符串 \"true\" → distributor_id=0（自营）；"
							+ "其他任何值（含空串、null、\"false\"）→ distributor_id>0（店铺商品）；键不存在则不过滤",
			example = "true")
	public String is_self;
}