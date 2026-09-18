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

package cn.shopex.ecshopx.employeepurchase.dto.front;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateEmployeePurchaseOrderReceiverRequest {

	private Long orderId;

	@NotBlank(message = "请填写正确的收货人姓名")
	@Pattern(
			regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]+$",
			message = "请填写正确的收货人姓名")
	private String receiverName;

	@NotBlank(message = "请填写联系方式")
	private String receiverMobile;

	@NotBlank(message = "请填写正确的省份")
	@Pattern(
			regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]+$",
			message = "请填写正确的省份")
	private String receiverState;

	@NotBlank(message = "请填写正确的城市")
	@Pattern(
			regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]+$",
			message = "请填写正确的城市")
	private String receiverCity;

	@NotBlank(message = "请填写正确的地区")
	@Pattern(
			regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]+$",
			message = "请填写正确的地区")
	private String receiverDistrict;

	@NotBlank(message = "请填写正确的详细地址")
	private String receiverAddress;

	private String receiverZip;
}
