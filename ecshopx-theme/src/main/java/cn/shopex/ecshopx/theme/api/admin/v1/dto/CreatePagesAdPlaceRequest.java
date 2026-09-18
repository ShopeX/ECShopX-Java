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

import cn.shopex.ecshopx.theme.api.admin.v1.dto.support.LenientLongJsonDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePagesAdPlaceRequest {

	@NotBlank(message = "{theme.pages_ad_place.ad_type_not_blank}")
	@Pattern(regexp = "^(popup|carousel)$", message = "{theme.pages_ad_place.ad_type_pattern}")
	@JsonProperty("ad_type")
	private String adType;

	@NotBlank(message = "{theme.pages_ad_place.name_not_blank}")
	@JsonProperty("name")
	private String name;

	@NotNull(message = "{theme.pages_ad_place.start_time_not_null}")
	@JsonDeserialize(using = LenientLongJsonDeserializer.class)
	@JsonProperty("start_time")
	private Long startTime;

	@NotNull(message = "{theme.pages_ad_place.end_time_not_null}")
	@JsonDeserialize(using = LenientLongJsonDeserializer.class)
	@JsonProperty("end_time")
	private Long endTime;

	@NotBlank(message = "{theme.pages_ad_place.pages_not_blank}")
	@JsonProperty("pages")
	private String pages;

	@JsonProperty("rel_tags")
	private List<@Valid PagesAdPlaceRelTagItemRequest> relTags;

	@JsonProperty("regionauth_id")
	private Long regionauthId;

	@JsonProperty("distributor_id")
	private List<Long> distributorId;

	@JsonProperty("setting")
	private String setting;

	@JsonProperty("auto_play")
	private Integer autoPlay;

	@JsonProperty("play_interval")
	private Integer playInterval;

	@JsonProperty("auto_close")
	private Integer autoClose;

	@JsonProperty("close_delay")
	private Integer closeDelay;

	@JsonProperty("audit_remark")
	private String auditRemark;

	@JsonProperty("sort")
	private Integer sort;

	@JsonProperty("tracking_code")
	private String trackingCode;
}
