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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，5 键与 query 同名双通道，来源
 * {@code OpenapiMemberCardV2UpdateHandlerParams#collectPresentFields}
 * （按键存在性 PATCH：body 含键即 present，值 null 转空串、非字符串 String.valueOf；
 * query 非 null 即 present，原样不 trim）。
 * 落库见 {@code OpenapiThirdApiV2MemberCardUpdateService}（upsert，无必填校验）。
 */
@Schema(
		name = "EcxMemberCardUpdateBody",
		description =
				"修改会员卡基础设置参数（5 键，body 含键优先否则 query 同名键，均按键存在性 PATCH）。"
						+ " 全部键缺席时无操作成功；任一键存在即 upsert membercard 行"
						+ "（无行新建、有行仅更新 present 键 + updated）。"
						+ " 与 admin 端不同：本接口不校验必填，空串/null（转空串）可写入清空。")
public class EcxMemberCardUpdateBodyDoc {

	@Schema(
			description = "商户名称；键存在即更新（body null 转空串可清空；不校验必填）",
			example = "FixtureBrand")
	public String brand_name;

	@Schema(
			description = "商户 logo URL；键存在即更新（body null 转空串可清空；不校验必填；示例为格式演示值）",
			example = "https://example.test/key.jpg")
	public String logo_url;

	@Schema(
			description = "会员卡名称；键存在即更新（body null 转空串可清空；不校验必填）",
			example = "Gold")
	public String title;

	@Schema(
			description =
					"会员卡背景颜色（微信卡券色值枚举，如 Color010）；键存在即更新"
							+ "（body null 转空串可清空；不校验枚举）",
			example = "Color010")
	public String color;

	@Schema(
			description = "会员卡背景图 URL；键存在即更新（body null 转空串可清空；示例为格式演示值）",
			example = "https://example.test/key.jpg")
	public String background_pic_url;
}