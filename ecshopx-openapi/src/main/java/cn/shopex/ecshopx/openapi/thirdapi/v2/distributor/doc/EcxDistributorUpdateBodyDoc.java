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
 * 仅 OpenAPI 文档；PATCH 请求可选 JSON body，字段来源 {@code DistributorController#update}
 * 读取的全部 key（shop_code originalString，其余 presentOptionalString 键存在语义），
 * 校验与部分更新见 {@code OpenapiThirdApiV2DistributorUpdateService}。
 */
@Schema(
		name = "EcxDistributorUpdateBody",
		description =
				"更新店铺参数（部分更新：可选字段键不存在则不改动；键存在值为 null 时字符串类跳过、0/1 开关类跳过、"
						+ "status 为 null 跳过）。shop_code 为定位键兼可改字段（originalString，body 含键取原始值）；"
						+ " 其余键存在语义：body 含键（含 null）或 query 提供即视为提交。"
						+ " 无任何有效补丁字段时不写库、直接返回当前行。")
public class EcxDistributorUpdateBodyDoc {

	@Schema(
			description =
					"店铺号（定位键，精确匹配）；必填非空且 ^[A-Za-z0-9-]+$（缺失/空 E5401「店铺找不到」，"
							+ "格式非法 E5001「店铺号参数错误」）；与其他店铺重复 E5402「店铺号已存在」；"
							+ "本值同时作为新 shop_code 写入（改号即传新值会定位失败——定位与改号共用同一键，PHP 旧契约）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "S001")
	public String shop_code;

	@Schema(description = "店铺名称；可选；租户内与其他非删除店铺重名 E5402「店铺名称已存在」；更新后重算首字母", example = "新门店名")
	public String distributor_name;

	@Schema(description = "联系人姓名；可选；加密存储", example = "王五")
	public String contact_username;

	@Schema(
			description =
					"联系人手机；可选；非空时须 ^1[3456789]\\d{9}$（否则 E5001「联系方式参数错误」）；"
							+ "与其他店铺重复（密文）E5402「店铺手机号已存在」；加密存储",
			example = "13912345678")
	public String contact_mobile;

	@Schema(description = "营业时间描述；可选（无格式校验）", example = "08:30 - 21:30")
	public String hour;

	@Schema(
			description = "是否支持自提；可选，键存在非 null 时仅 0/1（空串/非法 E5001「是否支持自提参数错误」）",
			allowableValues = {"0", "1"},
			example = "1")
	public String is_ziti;

	@Schema(
			description = "是否支持快递配送；可选，仅 0/1（否则 E5001「是否支持快递参数错误」）",
			allowableValues = {"0", "1"},
			example = "1")
	public String is_delivery;

	@Schema(
			description = "是否自动同步总部商品；可选，仅 0/1（否则 E5001「是否自动同步总部商品参数错误」）",
			allowableValues = {"0", "1"},
			example = "1")
	public String is_auto_sync_goods;

	@Schema(
			description = "是否开启同城配；可选，仅 0/1（否则 E5001「是否开启同城配参数错误」）；更新不触发远程建店",
			allowableValues = {"0", "1"},
			example = "0")
	public String is_dada;

	@Schema(
			description = "是否默认店铺；可选，仅 0/1（否则 E5001「是否默认店铺参数错误」）",
			allowableValues = {"0", "1"},
			example = "0")
	public String is_default;

	@Schema(description = "店铺 Logo URL；可选（无格式校验）", example = "/x/logo.jpg")
	public String logo;

	@Schema(
			description =
					"店铺状态；可选，键存在非 null 时仅 0/1/2（空串/非法 E5001「店铺状态参数错误」）："
							+ "0→is_valid=delete（删除）/ 1→true（启用）/ 2→false（禁用）",
			allowableValues = {"0", "1", "2"},
			example = "1")
	public String status;

	@Schema(description = "省；可选", example = "北京市")
	public String province;

	@Schema(description = "市；可选", example = "北京市")
	public String city;

	@Schema(description = "区；可选", example = "海淀区")
	public String area;

	@Schema(description = "详细地址；可选", example = "中关村大街1号")
	public String address;

	@Schema(
			description = "经度；可选，非空时须数值（否则 E5001「经度参数错误」）",
			example = "116.310003")
	public String lng;

	@Schema(
			description = "纬度；可选，非空时须数值（否则 E5001「纬度参数错误」）",
			example = "39.992806")
	public String lat;
}