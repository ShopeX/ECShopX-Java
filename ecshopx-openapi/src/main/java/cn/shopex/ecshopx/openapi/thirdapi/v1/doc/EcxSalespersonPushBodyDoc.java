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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ShopSalespersonController#pushSalesperson}
 * 读取的全部 key（mergeString / originalString / isParamPresent / isPhpTruthy），
 * 校验与落库语义见 {@code OpenapiThirdApiV1SalespersonPushService}。
 */
@Schema(
		name = "EcxSalespersonPushBody",
		description =
				"新增/编辑导购员同步参数。双通道两种语义："
						+ " mergeString 字段——body 含键时以 body 为准（null/空白视为未提供且不回退 query），body 无键读 query 并 trim；"
						+ " originalString 字段（mobile/salesperson_avatar/NewUserID）——body 含键时取原始值（不 trim），否则用 query。"
						+ " 按 company_id + salesperson_type=shopping_guide + work_userid 判断已存在则更新、否则新建。")
public class EcxSalespersonPushBodyDoc {

	@Schema(
			description = "导购名称；必填非空白（trim 后），否则「请填写导购名称」；加密存储，响应返回明文",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "导购员甲")
	public String salesperson_name;

	@Schema(
			description = "员工类型；必填，仅 \"1\"（员工）/\"2\"（编外），否则「请填写正确的员工类型」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			allowableValues = {"1", "2"},
			example = "1")
	public String employee_status;

	@Schema(
			description =
					"手机号；可选，但键存在（body 含 mobile 键——含 null 值——或 query 提供）即视为要修改，"
							+ "必须匹配 ^1[3456789][0-9]{9}$，否则「请填写正确的手机号」；"
							+ "键完全不提供则新建不写手机号、更新不改手机号。原始值不 trim 判存在，校验时 trim。加密存储",
			example = "13800000000")
	public String mobile;

	@Schema(
			description =
					"企业微信 userid；必填非空白，否则「请填写企业微信userid」；"
							+ "作为 upsert 匹配键（租户 + shopping_guide + work_userid，LIMIT 1）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "work-u")
	public String work_userid;

	@Schema(
			description =
					"是否启用；必填，仅 \"0\"/\"1\"/\"2\"，否则「请填写是否启用」；"
							+ "\"1\" 置 is_valid=true，\"0\"/\"2\" 置 is_valid=false",
			requiredMode = Schema.RequiredMode.REQUIRED,
			allowableValues = {"0", "1", "2"},
			example = "1")
	public String salesperson_status;

	@Schema(
			description =
					"店铺编号（distribution_distributor.shop_code）；可选。非空白时按租户查有效店铺"
							+ "（is_valid=true，created 倒序），查无报「店铺code未查询到正在开启的店铺信息」；"
							+ "命中则绑定全部匹配店铺（shop_rel_salesperson，store_type=distributor；更新时先删旧关联再插入）。"
							+ "缺省/空白不校验也不改动店铺关联",
			example = "S01")
	public String store_bn;

	@Schema(description = "职务；可选，缺省空串", example = "店长")
	public String salesperson_job;

	@Schema(
			description = "企业微信头像 URL；可选，原始值不 trim，原样落库（可为 null）",
			example = "https://example.test/avatar.jpg")
	public String salesperson_avatar;

	@Schema(description = "企业微信联系我配置 ID；可选，缺省空串")
	public String work_qrcode_configid;

	@Schema(
			description =
					"新的企业微信 userid（改名）；可选，仅更新分支生效：键存在且 PHP-truthy（非空且非 \"0\"）"
							+ "且与 work_userid 不同才把 work_userid 改写为本值（原始值不 trim）；新建分支忽略",
			example = "work-z")
	public String NewUserID;
}