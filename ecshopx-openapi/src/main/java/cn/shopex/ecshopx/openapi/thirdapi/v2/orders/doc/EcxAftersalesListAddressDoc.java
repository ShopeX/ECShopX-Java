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
 * 仅 OpenAPI 文档；列表行 aftersales_address 的两种来源：
 * 主单 JSON 原样解析（键见 {@link EcxAftersalesDetailAddressDoc}），或主单地址空/\"0\" 且门店有默认售后地址时
 * 改用默认地址 5 键（{@code DistributorDefaultAftersalesAddressReadPortImpl#findDefaultAddress}）。
 */
@Schema(
		name = "EcxAftersalesListAddress",
		description =
				"售后回寄地址对象；无地址且门店无默认地址时为 {}。"
						+ " 主单已有地址时键为 aftersales_address_id/aftersales_contact/aftersales_mobile/aftersales_address"
						+ "（店务按数量申请另有 aftersales_name/aftersales_hours）；"
						+ " 回退门店默认地址时键为下列 5 键（含 is_default，aftersales_address 为地址表 address 单字段）。")
public class EcxAftersalesListAddressDoc {

	@Schema(description = "售后地址 ID（整型）", example = "1")
	public Long aftersales_address_id;

	@Schema(description = "地址（默认地址分支取地址表 address 单字段，不拼接省市区）", example = "中关村大街1号")
	public String aftersales_address;

	@Schema(description = "收件联系人（trim 后，空值空串）", example = "王五")
	public String aftersales_contact;

	@Schema(description = "收件电话（trim 后，空值空串）", example = "13912345678")
	public String aftersales_mobile;

	@Schema(description = "是否默认地址：1 是 / 0 否（仅默认地址分支返回）", example = "1")
	public Integer is_default;
}