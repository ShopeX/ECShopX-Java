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

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorInfoAccessGate {

	private final ObjectMapper objectMapper;

	public DistributorInfoAccessGate(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void apply(HttpServletRequest request, Map<String, Object> user, Map<String, Object> merged) {
		Object opTypeObj = user.get("operator_type");
		String operatorType = opTypeObj != null ? opTypeObj.toString().trim() : "";
		Set<Long> allowed = parseAllowedDistributorIds(user.get("distributor_ids"));

		if ("distributor".equals(operatorType)) {
			if (allowed.isEmpty()) {
				throw new ResourceException("权限信息有误");
			}
			applySelectedDistributorForDistributor(user, merged, allowed);
			return;
		}

		if ("staff".equals(operatorType) && !allowed.isEmpty()) {
			long queryDid = parseMergedDistributorId(merged.get("distributor_id"));
			if (queryDid > 0 && !allowed.contains(queryDid)) {
				throw new ForbiddenException("您没有权限管理此店铺");
			}
		}
	}

	private void applySelectedDistributorForDistributor(
			Map<String, Object> user, Map<String, Object> merged, Set<Long> allowed) {
		long selected = toLong(user.get("distributor_id"));
		if (selected <= 0L) {
			return;
		}
		merged.put("distributor_id", (int) selected);
		if (!allowed.contains(selected)) {
			throw new ForbiddenException("您没有权限管理此店铺");
		}
	}

	private Set<Long> parseAllowedDistributorIds(Object raw) {
		Set<Long> out = new HashSet<>();
		if (raw == null) {
			return out;
		}
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				addAllowedId(o, out);
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				JsonNode arr = objectMapper.readTree(s);
				if (!arr.isArray()) {
					return out;
				}
				for (JsonNode n : arr) {
					if (n.isObject()) {
						JsonNode idNode = n.get("distributor_id");
						if (idNode != null && idNode.isNumber()) {
							long v = idNode.longValue();
							if (v > 0) {
								out.add(v);
							}
						} else if (idNode != null && idNode.isTextual()) {
							addLongString(idNode.asText(), out);
						}
					} else if (n.isNumber()) {
						long v = n.longValue();
						if (v > 0) {
							out.add(v);
						}
					}
				}
			} catch (Exception e) {
				return out;
			}
		}
		return out;
	}

	private static void addAllowedId(Object o, Set<Long> out) {
		if (o instanceof Map<?, ?> m) {
			Object did = m.get("distributor_id");
			addLongObject(did, out);
		} else if (o instanceof Number n) {
			long v = n.longValue();
			if (v > 0) {
				out.add(v);
			}
		}
	}

	private static void addLongObject(Object did, Set<Long> out) {
		if (did == null) {
			return;
		}
		if (did instanceof Number n) {
			long v = n.longValue();
			if (v > 0) {
				out.add(v);
			}
			return;
		}
		addLongString(did.toString(), out);
	}

	private static void addLongString(String s, Set<Long> out) {
		if (!StringUtils.hasText(s)) {
			return;
		}
		try {
			long v = Long.parseLong(s.trim());
			if (v > 0) {
				out.add(v);
			}
		} catch (NumberFormatException ignored) {
		}
	}

	private static long parseMergedDistributorId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		return Long.parseLong(o.toString());
	}
}
