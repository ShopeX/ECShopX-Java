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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷。
 *
 * <p>字段来源：{@code OpenapiThirdApiV1CompanyGetInfoService#getCompanyInfo} 把 Redis
 * {@code wxShopsSetting:sha1(companyId)} 中当前语言切片的内层 Map 原样透传并追加 {@code company_id}。
 * 内层固定键由写侧 {@code WxShopsSettingSetService#setWxShopsSetting} 决定（logo / intro /
 * brand_name / background）；历史扁平格式（无语言包装）时可能包含写侧以外的历史键，原样透传。
 */
@Schema(
		name = "EcxCompanyInfoData",
		description =
				"云店基本信息：当前语言的门店展示配置 + company_id。"
						+ " Redis 未命中、JSON 非法或无该语言切片时，仅返回 company_id。",
		example =
				"{\"logo\":\"/fixtures/logo.png\",\"intro\":\"hi\",\"brand_name\":\"FixtureBrand\","
						+ "\"background\":\"/bg.png\",\"company_id\":88001}")
public class EcxCompanyInfoDataDoc {

	@Schema(
			description = "门店 logo 图片路径；写侧未提供时为 null",
			example = "/fixtures/logo.png",
			nullable = true)
	public String logo;

	@Schema(description = "门店简介；写侧未提供时为 null", example = "hi", nullable = true)
	public String intro;

	@Schema(description = "品牌名称；写侧未提供时为 null", example = "FixtureBrand", nullable = true)
	public String brand_name;

	@Schema(description = "背景图路径；写侧缺省为空字符串", example = "/bg.png")
	public String background;

	@Schema(description = "租户/公司 ID，来自 OpenAPI 签名鉴权上下文（auth.company_id）", example = "88001")
	public Long company_id;
}