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

package cn.shopex.ecshopx.kujiale.api.front.v1.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KujialeDesginWorkListRequest {

	@JsonProperty("page")
	private Integer page;

	@JsonProperty("pageSize")
	private Integer pageSize;

	@JsonProperty("keywords")
	private String keywords;

	/**
	 * JSON array of tag objects; empty JSON object {@code {}} is treated like absent (no filter). Other
	 * non-array values (e.g. string) are rejected in service with ResourceException.
	 */
	@JsonProperty("tags_params")
	private Object tagsParams;

	/** Scalar or JSON array; passed through for city filter semantics. */
	@JsonProperty("city_id")
	private Object cityId;

	@JsonProperty("sort")
	private String sort;
}
