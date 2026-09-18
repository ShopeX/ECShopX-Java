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

package cn.shopex.ecshopx.supplier.dto.admin.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplierRegisterRequest {

	@NotBlank(message = "供应商名称必填")
	@JsonProperty("supplier_name")
	private String supplierName;

	@NotBlank(message = "联系人必填")
	@JsonProperty("contact")
	private String contact;

	@NotBlank(message = "请输入手机号")
	@Size(min = 11, max = 11, message = "请输入手机号")
	@Pattern(regexp = "^\\d{11}$", message = "请输入手机号")
	@JsonProperty("mobile")
	private String mobile;

	@NotBlank(message = "营业执照有误(图片地址过长)")
	@Size(max = 512, message = "营业执照有误(图片地址过长)")
	@JsonProperty("business_license")
	private String businessLicense;

	@NotBlank(message = "请上传企微二维码")
	@Size(max = 512, message = "请上传企微二维码")
	@JsonProperty("wechat_qrcode")
	private String wechatQrcode;

	@NotBlank(message = "请输入客服电话")
	@Size(max = 50, message = "请输入客服电话")
	@JsonProperty("service_tel")
	private String serviceTel;

	@NotBlank(message = "请输入银行名称")
	@Size(max = 100, message = "请输入银行名称")
	@JsonProperty("bank_name")
	private String bankName;

	@NotBlank(message = "请输入银行账号")
	@Size(max = 100, message = "请输入银行账号")
	@JsonProperty("bank_account")
	private String bankAccount;
}
