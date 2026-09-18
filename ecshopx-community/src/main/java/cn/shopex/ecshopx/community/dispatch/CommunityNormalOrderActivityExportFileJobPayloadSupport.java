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

package cn.shopex.ecshopx.community.dispatch;

import cn.shopex.ecshopx.community.dto.CommunityActivityAdminListQuery;
import cn.shopex.ecshopx.community.service.export.CommunityOrderActivityExportContext;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CommunityNormalOrderActivityExportFileJobPayloadSupport {

	private CommunityNormalOrderActivityExportFileJobPayloadSupport() {}

	public static LinkedHashMap<String, Object> toFilterMap(CommunityActivityAdminListQuery query) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("distributor_operator", query.isDistributorOperator());
		m.put("distributor_id_filter", query.getDistributorIdFilter());
		m.put("created_at_gte", query.getCreatedAtGte());
		m.put("created_at_lte", query.getCreatedAtLte());
		m.put("filter_created_at_lte_with_null", query.isFilterCreatedAtLteWithNull());
		m.put("activity_status", query.getActivityStatus());
		m.put("success", query.isSuccess());
		m.put("activity_name_contains", query.getActivityNameContains());
		return m;
	}

	public static CommunityOrderActivityExportContext toContext(Map<String, Object> payload) {
		long companyId = requiredLong(payload, "company_id");
		long operatorId = requiredLong(payload, "operator_id");
		boolean datapassAllowed = requiredBoolean(payload, "datapass_allowed");
		Long optionalActivityId = optionalLong(payload.get("optional_activity_id"));
		CommunityActivityAdminListQuery query = parseQuery(payload.get("filter"));
		return new CommunityOrderActivityExportContext(
				companyId, operatorId, datapassAllowed, query, optionalActivityId);
	}

	private static CommunityActivityAdminListQuery parseQuery(Object rawFilter) {
		CommunityActivityAdminListQuery q = new CommunityActivityAdminListQuery();
		if (!(rawFilter instanceof Map<?, ?> fm)) {
			return q;
		}
		q.setDistributorOperator(booleanValue(fm.get("distributor_operator")));
		q.setDistributorIdFilter(integerOrNull(fm.get("distributor_id_filter")));
		q.setCreatedAtGte(longOrNull(fm.get("created_at_gte")));
		q.setCreatedAtLte(longOrNull(fm.get("created_at_lte")));
		q.setFilterCreatedAtLteWithNull(booleanValue(fm.get("filter_created_at_lte_with_null")));
		Object status = fm.get("activity_status");
		q.setActivityStatus(status == null ? null : String.valueOf(status));
		q.setSuccess(booleanValue(fm.get("success")));
		Object name = fm.get("activity_name_contains");
		q.setActivityNameContains(name == null ? null : String.valueOf(name));
		return q;
	}

	private static long requiredLong(Map<String, Object> payload, String key) {
		Object v = payload.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static boolean requiredBoolean(Map<String, Object> payload, String key) {
		Object v = payload.get(key);
		if (v instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(String.valueOf(v));
	}

	private static Long optionalLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return null;
		}
		return Long.parseLong(s);
	}

	private static Integer integerOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}

	private static Long longOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static boolean booleanValue(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v == null) {
			return false;
		}
		return Boolean.parseBoolean(String.valueOf(v));
	}
}
