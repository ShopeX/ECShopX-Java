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

package cn.shopex.ecshopx.distribution.support;

import cn.shopex.ecshopx.distribution.domain.Slider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class SliderColumnNamesDataMapper {

	private SliderColumnNamesDataMapper() {}

	public static Map<String, Object> toColumnNamesData(Slider row, ObjectMapper objectMapper) {
		return toColumnNamesData(row, objectMapper, false, false);
	}

	/**
	 * When {@code styleParamsAsStoredJsonString} / {@code imageListAsStoredJsonString} are true, echoes the DB
	 * column text (normalized by {@code toJsonColumn}) as a Java {@link String} in the map
	 * for clients that submitted JSON columns as string values. Otherwise values are parsed to Map/List.
	 */
	public static Map<String, Object> toColumnNamesData(
			Slider row,
			ObjectMapper objectMapper,
			boolean styleParamsAsStoredJsonString,
			boolean imageListAsStoredJsonString) {
		Map<String, Object> map = new LinkedHashMap<>();
		long slideId = row.getSlideId() == null ? 0L : row.getSlideId();
		map.put("slide_id", slideId);
		map.put("title", row.getTitle());
		map.put("sub_title", row.getSubTitle());
		map.put("company_id", row.getCompanyId());
		map.put(
				"style_params",
				styleParamsAsStoredJsonString
						? stringOrEmpty(row.getStyleParams(), "{}")
						: readJsonObjectOrEmpty(row.getStyleParams(), objectMapper, true));
		map.put(
				"image_list",
				imageListAsStoredJsonString
						? stringOrEmpty(row.getImageList(), "[]")
						: readJsonObjectOrEmpty(row.getImageList(), objectMapper, false));
		map.put("desc_status", Boolean.TRUE.equals(row.getDescStatus()));
		map.put("distributor_id", row.getDistributorId());
		return map;
	}

	private static String stringOrEmpty(String json, String whenBlank) {
		return StringUtils.hasText(json) ? json : whenBlank;
	}

	private static Object readJsonObjectOrEmpty(String json, ObjectMapper objectMapper, boolean preferMap) {
		if (!StringUtils.hasText(json)) {
			return preferMap ? Collections.emptyMap() : Collections.emptyList();
		}
		try {
			return objectMapper.readValue(json, Object.class);
		} catch (JsonProcessingException ex) {
			return preferMap ? Collections.emptyMap() : Collections.emptyList();
		}
	}
}
