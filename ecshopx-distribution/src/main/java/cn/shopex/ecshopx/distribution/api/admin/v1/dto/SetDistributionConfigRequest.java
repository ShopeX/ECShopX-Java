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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetDistributionConfigRequest {

	private final Map<String, Object> rootKeys01 = new LinkedHashMap<>();

	private DistributorSection distributor;

	/**
	 * Top-level keys {@code "0"} and {@code "1"} captured for forward compatibility; they do not affect
	 * {@code distributor.show} validation (that rule reads only nested keys under {@code distributor}).
	 */
	public Map<String, Object> getRootKeys01() {
		return rootKeys01;
	}

	@JsonAnySetter
	public void captureRootNumericKeys(String name, Object value) {
		if ("0".equals(name) || "1".equals(name)) {
			rootKeys01.put(name, value);
		}
	}

	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DistributorSection {

		private final Map<String, Object> distributorKeys01 = new LinkedHashMap<>();

		private Object show;
		private Object distributor;
		private Object seller;
		private Object popularize_seller;
		private Object distributor_seller;
		private Object plan_limit_time;

		@JsonAnySetter
		public void captureDistributorNumericKeys(String name, Object value) {
			if ("0".equals(name) || "1".equals(name)) {
				distributorKeys01.put(name, value);
			}
		}
	}
}
