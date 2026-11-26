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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源 {@code DistributorController#download}
 * 读取的全部 key（originalString / mergeString），定位语义见
 * {@code OpenapiThirdApiV2DistributorDownloadService}。
 */
@Schema(
		name = "EcxDistributorDownloadBody",
		description =
				"店铺码参数（body 与 query 同名 key 双通道：shop_code/distributor_name/status 取原始值不 trim，"
						+ "template_name 走 mergeString）。三个筛选均可选、组合定位店铺（LIMIT 1，无排序）；"
						+ " 全不提供时取租户任意一家店铺。")
public class EcxDistributorDownloadBodyDoc {

	@Schema(description = "店铺号精确过滤；可选，非空才生效", example = "S001")
	public String shop_code;

	@Schema(description = "店铺名称模糊过滤（LIKE）；可选，非空才生效", example = "新门店名")
	public String distributor_name;

	@Schema(
			description = "状态过滤：0→is_valid=delete / 1→true / 2→false（PHP 数值宽松解析，含小数取整）；其他值忽略不过滤",
			allowableValues = {"0", "1", "2"},
			example = "1")
	public String status;

	@Schema(
			description =
					"小程序模板名；可选，缺省空串（按 template_name=\"\" 查授权记录，通常查无 → E5700「没有开通此小程序」）；"
							+ "常规传 yykweishop",
			example = "yykweishop")
	public String template_name;
}