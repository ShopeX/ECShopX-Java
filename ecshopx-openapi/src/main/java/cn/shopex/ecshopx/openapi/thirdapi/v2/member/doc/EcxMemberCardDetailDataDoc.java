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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；data 会员卡基础设置（7 固定键），来源
 * {@code OpenapiThirdApiV2MemberCardDetailService#executeOpenapiDetail}
 * （membercard 表按 company_id 主键查询；company_id/code_type 不透出）。
 * 无设置行时不报错，7 键全部为空串。
 */
@Schema(name = "EcxMemberCardDetailData")
public class EcxMemberCardDetailDataDoc {

	@Schema(description = "商户名称（null/无设置行为空串）", example = "FixtureBrand")
	public String brand_name;

	@Schema(description = "商户 logo URL（null/无设置行为空串；示例为格式演示值）", example = "https://example.test/key.jpg")
	public String logo_url;

	@Schema(description = "会员卡名称（null/无设置行为空串）", example = "Gold")
	public String title;

	@Schema(description = "会员卡背景颜色（微信卡券色值枚举，如 Color010；null/无设置行为空串）", example = "Color010")
	public String color;

	@Schema(description = "会员卡背景图 URL（null/无设置行为空串；示例为格式演示值）", example = "https://example.test/key.jpg")
	public String background_pic_url;

	@Schema(
			description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；无设置行/非数值为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String created;

	@Schema(
			description = "更新时间（yyyy-MM-dd HH:mm:ss，系统时区；无设置行/非数值为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String updated;
}