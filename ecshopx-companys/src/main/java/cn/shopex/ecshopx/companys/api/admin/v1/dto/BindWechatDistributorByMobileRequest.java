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

package cn.shopex.ecshopx.companys.api.admin.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BindWechatDistributorByMobileRequest {

	@NotBlank(message = "company_id必填")
	@JsonProperty("company_id")
	private String companyId;

	@NotBlank(message = "app_id不能为空")
	@JsonProperty("app_id")
	private String appId;

	@NotBlank(message = "app_type不能为空")
	@JsonProperty("app_type")
	private String appType;

	@NotBlank(message = "openid不能为空")
	private String openid;

	@NotBlank(message = "unionid不能为空")
	private String unionid;

	@NotBlank(message = "请输入手机号码")
	private String mobile;

	@NotBlank(message = "请输入短信验证码")
	private String vcode;

	@NotBlank(message = "check_token不能为空")
	@JsonProperty("check_token")
	private String checkToken;

	/** 表单字段 {@code company_id} */
	public void setCompany_id(String companyId) {
		this.companyId = companyId;
	}

	/** 表单字段 {@code app_id} */
	public void setApp_id(String appId) {
		this.appId = appId;
	}

	/** 表单字段 {@code app_type} */
	public void setApp_type(String appType) {
		this.appType = appType;
	}

	/** 表单字段 {@code check_token} */
	public void setCheck_token(String checkToken) {
		this.checkToken = checkToken;
	}
}
