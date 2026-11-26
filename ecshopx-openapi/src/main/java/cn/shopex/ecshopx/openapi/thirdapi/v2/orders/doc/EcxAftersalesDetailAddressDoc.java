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
 * 仅 OpenAPI 文档；售后回寄地址（aftersales.aftersales_address JSON 解析，解析失败/空/"0" 时为 {}）。
 * 固定键来自写入方 {@code AftersalesReviewService}（4 键）与
 * {@code AftersalesApplyShopApplyByNumHandleService}（另加 aftersales_name/aftersales_hours）。
 */
@Schema(
		name = "EcxAftersalesDetailAddress",
		description =
				"售后回寄地址对象；无地址时为 {}。线上审核路径写 4 键；店务按数量申请路径另含"
						+ " aftersales_name/aftersales_hours（共 6 键）。")
public class EcxAftersalesDetailAddressDoc {

	@Schema(description = "售后地址 ID（整型）", example = "1")
	public Long aftersales_address_id;

	@Schema(description = "收件联系人（地址表 contact）", example = "王五")
	public String aftersales_contact;

	@Schema(description = "收件电话（地址表 mobile）", example = "13912345678")
	public String aftersales_mobile;

	@Schema(description = "完整地址：省+市+区+详细地址拼接", example = "北京市北京市海淀区中关村大街1号")
	public String aftersales_address;

	@Schema(description = "退货门店名称（仅店务按数量申请路径写入；其余路径无此键）", example = "新门店名")
	public String aftersales_name;

	@Schema(description = "门店营业时间（仅店务按数量申请路径写入，可为空串；其余路径无此键）", example = "")
	public String aftersales_hours;
}