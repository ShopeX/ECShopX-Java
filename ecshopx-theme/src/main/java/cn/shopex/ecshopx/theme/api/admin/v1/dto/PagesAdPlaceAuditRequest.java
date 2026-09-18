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

package cn.shopex.ecshopx.theme.api.admin.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagesAdPlaceAuditRequest {

	@NotBlank(message = "{theme.pages_ad_place.audit_status_required}")
	@Pattern(regexp = "^(approved|rejected)$", message = "{theme.pages_ad_place.audit_status_pattern}")
	@JsonProperty("audit_status")
	private String auditStatus;

	@JsonProperty("audit_remark")
	private String auditRemark;

	@AssertTrue(message = "{theme.pages_ad_place.audit_remark_required_when_rejected}")
	public boolean isAuditRemarkValidWhenRejected() {
		if (!"rejected".equals(auditStatus)) {
			return true;
		}
		return StringUtils.hasText(auditRemark);
	}
}
