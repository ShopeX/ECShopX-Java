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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code OpenapiWxappShopListParams#parse}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key（原始值不 trim）。
 */
@Schema(
		name = "EcxWxappShopListBody",
		description =
				"微信店铺数据查询参数（body 与 query 同名 key 双通道：body 含键时取 body 原始值——不 trim，"
						+ "body 无该键时用 query）。page/pageSize 校验失败统一报「参数不正确.」"
						+ "（PHP 旧契约：status=fail 但 code=E0000）。")
public class EcxWxappShopListBodyDoc {

	@Schema(
			description = "页码；必填整数字符串（不含小数点、不 trim）且 ≥1，否则「参数不正确.」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String page;

	@Schema(
			description = "每页条数；必填整数字符串且 1–50（超上限直接报错，不封顶），否则「参数不正确.」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "50")
	public String pageSize;

	@Schema(
			description =
					"更新时间下界（updated 严格大于）；可选，PHP-truthy（非空且非 \"0\"）才生效；"
							+ "格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd（当日 00:00:00，系统时区）转 Unix 秒；"
							+ "无法解析按 0（不报错，等效无下界过滤）",
			example = "2020-09-13")
	public String start_time;

	@Schema(
			description =
					"更新时间上界（updated 严格小于）；可选，语义同 start_time；无法解析按 0（等效查不到数据）",
			example = "2023-11-15")
	public String end_time;
}