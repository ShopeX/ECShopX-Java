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
 * 仅 OpenAPI 文档；list 行。{@code OpenapiThirdApiV1WxappShopListService#toOpenapiRow} 的 10 键
 * （camelCase 为 PHP 旧契约键名）。
 */
@Schema(name = "EcxWxappShopListItem")
public class EcxWxappShopListItemDoc {

	@Schema(description = "店铺 ID（distribution_distributor.distributor_id，整型）", example = "1001")
	public Long wxShopId;

	@Schema(description = "店铺名称（默认语言分表非空覆盖；null 回退空串）", example = "新门店名")
	public String storeName;

	@Schema(description = "店铺号（shop_code；null 回退空串）", example = "S001")
	public String shopBn;

	@Schema(description = "店铺 logo（默认语言分表非空覆盖；可为 null）", nullable = true, example = "/x/logo.jpg")
	public String logo;

	@Schema(description = "联系电话（mobile 解密明文；null 回退空串）", example = "13912345678")
	public String contractPhone;

	@Schema(description = "营业时间描述（可为 null）", nullable = true, example = "08:30 - 21:30")
	public String hour;

	@Schema(description = "经度字符串（腾讯地图坐标，原样返回；可为 null）", nullable = true, example = "116.310003")
	public String lng;

	@Schema(description = "纬度字符串（腾讯地图坐标，原样返回；可为 null）", nullable = true, example = "39.992806")
	public String lat;

	@Schema(
			description = "完整地址：province+city+area+address 直接拼接（各段 null 按空串；默认语言分表非空覆盖）",
			example = "北京市北京市海淀区中关村大街1号")
	public String address;

	@Schema(
			description = "是否停用：is_valid=\"true\" → \"0\"（启用），否则 \"1\"；本接口不过滤停用店铺",
			allowableValues = {"0", "1"},
			example = "0")
	public String is_deleted;
}