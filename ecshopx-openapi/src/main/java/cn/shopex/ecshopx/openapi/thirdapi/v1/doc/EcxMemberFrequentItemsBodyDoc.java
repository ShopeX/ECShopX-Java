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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberController#frequentItems} 与
 * {@code OpenapiThirdApiV1MemberFrequentItemsService#validateParams} 读取的全部 key。
 */
@Schema(
		name = "EcxMemberFrequentItemsBody",
		description =
				"会员常购清单参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白不回退 query）。定位优先级：mobile（PHP-truthy）> external_member_id > unionid；"
						+ "三者 PHP-empty 时报「unionid或者手机号必填」")
public class EcxMemberFrequentItemsBodyDoc {

	@Schema(
			description = "手机号；提供（键存在）时必须匹配 ^1[3-9]\\d{9}$；查无会员报「参数无效」",
			example = "13800000000")
	public String mobile;

	@Schema(
			description = "微信 unionid（wechat 关联）；必须为字符串类型；查无关联报「参数无效」",
			example = "union-test")
	public String unionid;

	@Schema(description = "会员 ID（user_id）；mobile 未提供且本值 PHP-truthy 时按其定位；查无会员报「参数无效」", example = "5")
	public String external_member_id;

	@Schema(
			description =
					"统计时间段；必填：\"0\" 近一年；\"1\" 近半年；\"2\" 近三个月；"
							+ "缺失/空/非字符串报「请填写时间段」；其他值报 status=fail 且 code=E0000「时间段值无效」（PHP 旧契约）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			allowableValues = {"0", "1", "2"},
			example = "2")
	public String timeRange;
}