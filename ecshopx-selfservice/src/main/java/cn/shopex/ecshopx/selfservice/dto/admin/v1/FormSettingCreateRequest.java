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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/** Ignores JSON properties that have no matching bean field (forward-compatible with extended request bodies). */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FormSettingCreateRequest {

	@JsonProperty("id")
	private String id;

	@JsonProperty("field_title")
	private String fieldTitle;

	@JsonProperty("field_name")
	private String fieldName;

	@JsonProperty("form_element")
	private String formElement;

	private List<FormSettingOptionItem> options;

	@JsonProperty("image_url")
	private String imageUrl;

	@JsonProperty("pic_name")
	private String picName;
}
