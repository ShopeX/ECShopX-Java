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

package cn.shopex.ecshopx.distribution.api.admin.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class CreatePickupLocationRequest {

	@NotBlank
	@Size(max = 255)
	private String name;

	@NotBlank
	private String province;

	@NotBlank
	private String city;

	@NotBlank
	private String area;

	@NotBlank
	@Size(max = 255)
	private String address;

	@JsonProperty("area_code")
	private String areaCode;

	@NotBlank
	@JsonProperty("contract_phone")
	private String contractPhone;

	@NotEmpty
	private List<
					@NotNull @Size(min = 2, max = 2) @Valid
					List<@NotBlank @Pattern(regexp = "^\\d{2}:\\d{2}$") String>>
			hours;

	@NotEmpty
	private List<JsonNode> workdays;

	@NotBlank
	@JsonProperty("wait_pickup_days")
	private String waitPickupDays;

	@NotBlank
	@JsonProperty("latest_pickup_time")
	private String latestPickupTime;

	@JsonProperty("distributor_id")
	private Long distributorId;
}
