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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindWorkWechatMobileRequest {

	@NotBlank(message = "company_id必填")
	@JsonProperty("company_id")
	private String companyId;

	@NotBlank(message = "work_userid必填")
	@JsonProperty("work_userid")
	private String workUserid;

	@NotBlank(message = "check_token不能为空")
	@JsonProperty("check_token")
	private String checkToken;

	@NotBlank(message = "请输入手机号码")
	private String mobile;

	@NotBlank(message = "请输入短信验证码")
	private String vcode;

	public void setCompany_id(String companyId) {
		this.companyId = companyId;
	}

	public void setWork_userid(String workUserid) {
		this.workUserid = workUserid;
	}

	public void setCheck_token(String checkToken) {
		this.checkToken = checkToken;
	}
}
