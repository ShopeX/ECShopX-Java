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

package cn.shopex.ecshopx.members.service.address;

import cn.shopex.ecshopx.members.dispatch.UpdateAddressLatAndLngJobDispatchPublisher;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberAddressLatLngNonNumericEnqueueService {

	private final UpdateAddressLatAndLngJobDispatchPublisher updateAddressLatAndLngJobDispatchPublisher;

	public MemberAddressLatLngNonNumericEnqueueService(
			UpdateAddressLatAndLngJobDispatchPublisher updateAddressLatAndLngJobDispatchPublisher) {
		this.updateAddressLatAndLngJobDispatchPublisher = updateAddressLatAndLngJobDispatchPublisher;
	}

	public void enqueueIfNeeded(long companyId, long userId, Map<String, Object> defaultAddressRowOrNull) {
		if (userId <= 0L || defaultAddressRowOrNull == null) {
			return;
		}
		Object aid = defaultAddressRowOrNull.get("address_id");
		if (!(aid instanceof Number n)) {
			return;
		}
		String lat = stringVal(defaultAddressRowOrNull.get("lat"));
		String lng = stringVal(defaultAddressRowOrNull.get("lng"));
		if (isNumericCoordinate(lat) && isNumericCoordinate(lng)) {
			return;
		}
		updateAddressLatAndLngJobDispatchPublisher.enqueueUpdateAddressLatAndLng(companyId, userId, n.longValue());
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static boolean isNumericCoordinate(String raw) {
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		try {
			double v = Double.parseDouble(raw.trim());
			return Double.isFinite(v);
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
