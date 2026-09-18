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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Parses discount from vip grade privileges JSON. */
public final class VipGradeGradeDiscountParser {

	private VipGradeGradeDiscountParser() {
	}

	public static int parseDiscountFromPrivilegesJson(String privilegesJson, ObjectMapper objectMapper) {
		if (privilegesJson == null || privilegesJson.isBlank()) {
			return 0;
		}
		try {
			JsonNode n = objectMapper.readTree(privilegesJson);
			JsonNode disc = n.get("discount");
			if (disc != null && disc.isNumber()) {
				return disc.asInt();
			}
			if (disc != null && disc.isTextual()) {
				try {
					return Integer.parseInt(disc.asText().trim());
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		} catch (JsonProcessingException e) {
			return 0;
		}
		return 0;
	}
}
