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

package cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源
 * {@code OpenapiThirdApiV2DiscountCardUserDiscountListParams#validate}：
 * plat_account/code 经 {@code OpenapiRequestParams#mergeString}（body 含键取原始值，不回退 query），
 * page/page_size 经 {@code OpenapiThirdApiV2DiscountCardListParams#resolve}
 * （{@code OpenapiRequestParams#originalString}）。
 */
@Schema(
		name = "EcxUserdiscountListBody",
		description =
				"会员优惠券列表参数（body 与 query 同名 key 双通道：body 含键时优先取原始值，"
						+ "null/空白不回退 query）")
public class EcxUserdiscountListBodyDoc {

	@Schema(
			description =
					"商派会员 ID（user_id 整数）；必填非空白，缺失报 E5001「商派会员Id参数错误」；"
							+ "非整数按 -1 查会员，查无报 E5501「商派会员Id错误，未查询到会员信息」",
			example = "5")
	public String plat_account;

	@Schema(
			description =
					"券码精确过滤；可选；PHP truthy（trim 后非空且不为 \"0\"）才生效，"
							+ "否则不过滤（\"0\" 视为未传）",
			example = "000000000000")
	public String code;

	@Schema(description = "页码；PHP 整型解析（含小数取整、非法按 0），缺省 1，小于 1 归一为 1", example = "1")
	public String page;

	@Schema(
			description =
					"每页条数；缺省 20；解析后 ≥500 报 E5002「每页的大小最大为500！」（500 本身也报错）；"
							+ "小于 20 一律归一为 20（PHP 旧契约）",
			example = "20")
	public String page_size;
}