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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code DistributorController#create}
 * 读取的全部 key（originalString / mergeString），校验见
 * {@code OpenapiThirdApiV2DistributorCreateService#validateParams}。
 */
@Schema(
		name = "EcxDistributorCreateBody",
		description =
				"创建店铺参数。双通道：多数 originalString（body 含键取原始值不 trim，否则用 query）；"
						+ " hour/logo 走 mergeString（body 含键优先，null/空白不回退 query）。"
						+ " 新建店铺恒为禁用态（is_valid=false → status=2）、source_from=3，省市区/地址/坐标为空。")
public class EcxDistributorCreateBodyDoc {

	@Schema(
			description =
					"店铺号；必填非空且匹配 ^[A-Za-z0-9-]+$，否则 E5001「店铺号参数错误」；"
							+ "租户内重复（非 delete）E5402「店铺号已存在」；is_dada=1 时被同城配返回的 originShopId 覆盖",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "S001")
	public String shop_code;

	@Schema(
			description = "店铺名称；必填非空否则 E5001「店铺名称参数错误」；租户内重名（非 delete）E5402「店铺名称已存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "新门店名")
	public String distributor_name;

	@Schema(
			description = "联系人姓名；必填非空否则 E5001「联系人姓名参数错误」；加密存储，响应解密明文",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "王五")
	public String contact_username;

	@Schema(
			description =
					"联系人手机；必填且匹配 ^1[3456789]\\d{9}$，否则 E5001「联系方式参数错误」；"
							+ "租户内重复（密文比对）E5402「店铺手机号已存在」；加密存储",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13912345678")
	public String contact_mobile;

	@Schema(description = "营业时间描述；可选，缺省空串（当前无格式校验；mergeString 双通道）", example = "08:30 - 21:30")
	public String hour;

	@Schema(
			description = "是否支持自提；可选 0/1（空串/非 0/1 报 E5001「是否支持自提参数错误」），缺省 0",
			allowableValues = {"0", "1"},
			example = "0")
	public String is_ziti;

	@Schema(
			description = "是否支持快递配送；可选 0/1（否则 E5001「是否支持快递参数错误」），缺省 1",
			allowableValues = {"0", "1"},
			example = "1")
	public String is_delivery;

	@Schema(
			description = "是否自动同步总部商品；可选 0/1（否则 E5001「是否自动同步总部商品参数错误」），缺省 0",
			allowableValues = {"0", "1"},
			example = "0")
	public String is_auto_sync_goods;

	@Schema(
			description =
					"是否开启同城配；可选 0/1（否则 E5001「是否开启同城配参数错误」），缺省 0；"
							+ "=1 时须租户已开启达达/闪送（未开启 E5300「该商户未开启达达同城配」）并远程建店，"
							+ "shop_code 被 originShopId 覆盖（覆盖后重复再报 E5402）",
			allowableValues = {"0", "1"},
			example = "0")
	public String is_dada;

	@Schema(
			description = "是否默认店铺；可选 0/1（否则 E5001「是否默认店铺参数错误」），缺省 0",
			allowableValues = {"0", "1"},
			example = "0")
	public String is_default;

	@Schema(description = "店铺 Logo 图片 URL；可选，缺省空串（当前无格式校验；mergeString 双通道）", example = "/x/logo.jpg")
	public String logo;
}