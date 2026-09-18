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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.api.admin.v1.dto.SetDistributionConfigRequest;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DistributionConfigValidationSupport {

	private DistributionConfigValidationSupport() {}

	/**
	 * Whether {@code value} counts as present for required-style validation rules (non-file, non-date):
	 * same predicate as {@link #hasEffectiveValidatorValue(Object)}.
	 */
	public static boolean validatorFieldPresent(Object value) {
		return hasEffectiveValidatorValue(value);
	}

	/**
	 * Whether {@code value} counts as present for distribution config validation: non-null, non-blank string,
	 * non-empty collection/map/array, or any other non-null scalar.
	 */
	public static boolean hasEffectiveValidatorValue(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof String s) {
			return !s.trim().isEmpty();
		}
		if (value instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (value instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (value instanceof Object[] a) {
			return a.length > 0;
		}
		return true;
	}

	/**
	 * {@code distributor.show} with {@code required_with:0,1}: dependency keys {@code "0"} / {@code "1"} are read
	 * only from {@code distributorMap} (the nested {@code distributor} payload), never from HTTP root.
	 */
	public static boolean distributorShowFailsRequiredWith01(Map<String, Object> distributorMap, Object distributorShow) {
		boolean anyDep = validatorFieldPresent(distributorMap.get("0"))
				|| validatorFieldPresent(distributorMap.get("1"));
		if (!anyDep) {
			return false;
		}
		return !validatorFieldPresent(distributorShow);
	}

	public static Map<String, Object> buildValidatorRoot(SetDistributionConfigRequest req) {
		Map<String, Object> root = new LinkedHashMap<>();
		SetDistributionConfigRequest.DistributorSection d = req.getDistributor();
		Map<String, Object> distMap = distributorSectionToMap(d);
		root.put("distributor", distMap);
		return root;
	}

	public static Map<String, Object> distributorSectionToMap(SetDistributionConfigRequest.DistributorSection d) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (d == null) {
			return m;
		}
		m.put("show", d.getShow());
		m.put("distributor", d.getDistributor());
		m.put("seller", d.getSeller());
		m.put("popularize_seller", d.getPopularize_seller());
		m.put("distributor_seller", d.getDistributor_seller());
		m.put("plan_limit_time", d.getPlan_limit_time());
		m.putAll(d.getDistributorKeys01());
		return m;
	}
}
