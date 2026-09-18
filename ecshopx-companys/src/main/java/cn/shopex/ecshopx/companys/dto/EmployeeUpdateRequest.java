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

package cn.shopex.ecshopx.companys.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeUpdateRequest {

	private String username;

	@JsonProperty("login_name")
	private String loginName;

	private String mobile;

	@JsonProperty("head_portrait")
	private String headPortrait;

	private String password;

	@JsonProperty("role_id")
	private List<String> roleId;

	@JsonProperty("distributor_ids")
	private List<DistributorIdRef> distributorIds;

	@JsonProperty("shop_ids")
	private List<Object> shopIds;

	@JsonProperty("operator_type")
	private String operatorType;

	@JsonProperty("regionauth_id")
	private Long regionauthId;

	@JsonProperty("staff_type")
	private String staffType;

	@JsonProperty("staff_no")
	private String staffNo;

	@JsonProperty("staff_attribute")
	private String staffAttribute;

	@JsonProperty("payment_method")
	private String paymentMethod;

	@JsonProperty("payment_fee")
	private Number paymentFee;
}
