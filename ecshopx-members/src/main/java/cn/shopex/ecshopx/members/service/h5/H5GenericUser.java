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

package cn.shopex.ecshopx.members.service.h5;

import java.util.LinkedHashMap;
import java.util.Map;

public class H5GenericUser {

	private final Map<String, Object> attributes;

	public H5GenericUser(Map<String, Object> attributes) {
		this.attributes = new LinkedHashMap<>(attributes);
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public long getUserId() {
		Object v = attributes.get("user_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	public String getJwtSubject() {
		Object id = attributes.get("id");
		if (id == null) {
			return "";
		}
		return String.valueOf(id);
	}

	public Map<String, Object> getJwtCustomClaims() {
		return new LinkedHashMap<>(attributes);
	}
}
