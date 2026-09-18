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

package cn.shopex.ecshopx.selfservice.dto.admin.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class FormTemplateCreateRequest {

	@JsonProperty("id")
	private String id;

	@JsonProperty("tem_name")
	private String temName;

	@JsonProperty("tem_type")
	private String temType;

	private JsonNode content;

	private String status;

	@JsonProperty("key_index")
	private JsonNode keyIndex;

	@JsonProperty("form_style")
	private String formStyle;

	@JsonProperty("header_link_title")
	private String headerLinkTitle;

	@JsonProperty("header_title")
	private String headerTitle;

	@JsonProperty("bottom_title")
	private String bottomTitle;

	@JsonProperty("header_bg_pic")
	private String headerBgPic;

	@JsonProperty("header_height")
	private String headerHeight;
}
