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
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenScreenAdSaveRequest {

	@JsonProperty("ad_material")
	private String adMaterial;

	@JsonProperty("is_enable")
	private String isEnable;

	@JsonProperty("show_time")
	private String showTime;

	@JsonProperty("position")
	private String position;

	@JsonProperty("is_jump")
	private String isJump;

	@JsonProperty("material_type")
	private String materialType;

	@JsonProperty("waiting_time")
	private String waitingTime;

	@JsonProperty("ad_url")
	private JsonNode adUrl;

	@JsonProperty("app")
	private String app;

	@JsonProperty("start_time")
	private String startTime;

	@JsonProperty("end_time")
	private String endTime;
}
