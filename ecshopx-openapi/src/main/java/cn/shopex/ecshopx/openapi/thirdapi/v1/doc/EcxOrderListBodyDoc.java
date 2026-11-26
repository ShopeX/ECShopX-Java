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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code OrderController#list} 与
 * {@code OpenapiOrderListParams#resolve} / {@code OpenapiThirdApiV1OrderListService#validateParams}
 * 读取的全部 key。
 */
@Schema(
		name = "EcxOrderListBody",
		description =
				"会员订单列表参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白不回退 query）。mobile 与 unionid 二选一必填（PHP-empty 视为未提供）；"
						+ "mobile PHP-truthy 优先按手机号定位，否则按 unionid")
public class EcxOrderListBodyDoc {

	@Schema(
			description = "微信 unionid（wechat 关联）；必须为字符串类型；查无关联报「会员信息获取失败」；与 mobile 二选一",
			example = "union-test")
	public String unionid;

	@Schema(
			description = "手机号；提供（键存在）时必须匹配 ^1[3-9]\\d{9}$；查无会员报「会员信息获取失败」；优先于 unionid",
			example = "13800000000")
	public String mobile;

	@Schema(
			description =
					"页码；PHP-falsy（空/\"0\"）时固定返回前 10 条（LIMIT 10 OFFSET 0）；"
							+ "truthy 时按本页码分页（非整数按 1）",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页条数；仅 page truthy 时生效：缺失/PHP-empty/非整数时不分页（返回该会员全部订单）；"
							+ "page falsy 时固定 10",
			example = "10")
	public String page_size;
}