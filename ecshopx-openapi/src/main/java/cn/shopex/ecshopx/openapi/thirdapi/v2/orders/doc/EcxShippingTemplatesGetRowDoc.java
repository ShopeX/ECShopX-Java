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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；运费模板行（13 固定键 + 条件键 name_lang）。
 * {@code OpenapiThirdApiV2ShippingTemplatesListService#toLangMergeRow} +
 * {@code formatOpenApiTemplateRow}；查询 {@code buildOpenApiListWrapper}：
 * company_id 等值 + distributor_id=0（总部模板），create_time 倒序，不过滤 status/supplier_id。
 * fee_conf/nopost_conf/free_conf 为库内 TEXT 列 JSON 字符串**原样透传**（非嵌套对象），
 * 内部键集由写入路径 {@code ShippingTemplateAdminCreateService} 证实，见各字段说明与示例。
 */
@Schema(name = "EcxShippingTemplatesGetRow")
public class EcxShippingTemplatesGetRowDoc {

	@Schema(description = "运费模板 id（数值）", example = "1")
	public Long template_id;

	@Schema(description = "供应商 id（数值，0 表示非供应商模板）", example = "0")
	public Long supplier_id;

	@Schema(
			description = "运费模板名称（最长 50；zh-CN 语言分片有非空覆盖名时为覆盖后的值）",
			example = "全国包邮模板")
	public String name;

	@Schema(
			description = "是否包邮（字符串形态：\"0\" 不包邮 / \"1\" 包邮）",
			allowableValues = {"0", "1"},
			example = "0")
	public String is_free;

	@Schema(
			description =
					"运费计算方式（字符串形态）：1 按重量 / 2 按件数 / 3 按金额 / 4 按体积；"
							+ "决定 fee_conf/free_conf 内部结构（见各字段说明）",
			allowableValues = {"1", "2", "3", "4"},
			example = "1")
	public String valuation;

	@Schema(description = "是否物流保价（布尔，库内可 null）", nullable = true, example = "false")
	public Boolean protect;

	@Schema(
			description = "保价费率（字符串，固定 3 位小数 HALF_UP；库内 null 时返回 null）",
			nullable = true,
			example = "0.000")
	public String protect_rate;

	@Schema(
			description = "保价费最低值（字符串，固定 2 位小数 HALF_UP，单位元；库内 null 时返回 null）",
			nullable = true,
			example = "0.00")
	public String minprice;

	@Schema(description = "是否开启（布尔；本接口不过滤停用模板）", example = "true")
	public Boolean status;

	@Schema(
			description =
					"运费配置：库内 JSON 数组字符串原样透传（可 null）。valuation=1/2/4 时首项为全国默认"
							+ " {start_standard 首费标准, start_fee 首费(元), add_standard 续费标准, add_fee 续费(元)}，"
							+ "后续项另含 area（省/市/区 id 数组，整型或字符串均可）；valuation=3 时每项为"
							+ " {rules:[{up 金额上限, down 金额下限, basefee 运费(元)}]}（非首项另含 area）。"
							+ "键集证据：ShippingTemplateAdminCreateService#fillFeeConfWeightNumberVolume/fillFeeConfMoney；"
							+ "数值语义证据：NormalOrderShippingFreightCountService（元、重量/件数/体积标准）",
			nullable = true,
			example =
					"[{\"start_standard\":\"1\",\"start_fee\":\"10\",\"add_standard\":\"1\",\"add_fee\":\"5\"},"
							+ "{\"area\":[110000],\"start_standard\":\"1\",\"start_fee\":\"15\","
							+ "\"add_standard\":\"1\",\"add_fee\":\"8\"}]")
	public String fee_conf;

	@Schema(
			description =
					"不配送地区：库内 JSON 数组字符串原样透传（默认 \"[]\"，可 null）；元素为省/市/区 id"
							+ "（整型或字符串均可）。证据：写入路径 nopost JSON 序列化 + "
							+ "NormalOrderShippingFreightCountService#checkNopost",
			example = "[110000]")
	public String nopost_conf;

	@Schema(
			description =
					"指定包邮条件：库内 JSON 数组字符串原样透传（可 null）。仅 valuation=1/2/4 写入；首项为全国默认，"
							+ "非首项另含 area（省/市/区 id 数组）。freetype：1 满足重量/件数/体积 / 2 满足金额 / 3 两者同时满足；"
							+ "valuation=1 行键 {freetype,inweight 重量上限,upmoney 金额上限(元)}；"
							+ "valuation=2 行键 {freetype,upquantity 件数上限,upmoney}；"
							+ "valuation=4 行键 {freetype,upmoney,upvolume 体积上限}。"
							+ "键集证据：ShippingTemplateAdminCreateService#fillFreeConfWeight/Number/Volume；"
							+ "数值语义证据：NormalOrderShippingFreightCountService",
			nullable = true,
			example = "[{\"freetype\":3,\"inweight\":\"2\",\"upmoney\":\"99\"}]")
	public String free_conf;

	@Schema(
			description =
					"最后修改时间（yyyy-MM-dd HH:mm:ss，系统时区；库内 null/≤0 时为空串 \"\"——非 epoch 0 文案）",
			example = "2023-11-15 08:00:01")
	public String update_time;

	@Schema(
			description =
					"多语言名称（**条件键**：仅 zh-CN 语言分片存在非空覆盖名时携带，单键 \"zh-CN\"；无覆盖时不出现）",
			implementation = EcxShippingTemplatesGetNameLangDoc.class)
	public EcxShippingTemplatesGetNameLangDoc name_lang;
}