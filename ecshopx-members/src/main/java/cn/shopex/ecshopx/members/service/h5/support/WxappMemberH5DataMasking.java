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

package cn.shopex.ecshopx.members.service.h5.support;

import cn.shopex.ecshopx.common.util.DataMasking;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WxappMemberH5DataMasking {

	private static final int DATAPASS_BLOCK_ON = 1;

	private WxappMemberH5DataMasking() {
	}

	public static void applyMemberInfoMobileBirthdayAddressSex(
			Map<String, Object> memberInfo, boolean oemShuyunFlag) {
		Object mobile = memberInfo.get("mobile");
		if (mobile != null) {
			memberInfo.put(
					"mobile",
					DataMasking.maskMobileIfBlocked(String.valueOf(mobile), DATAPASS_BLOCK_ON));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> requestFields =
				memberInfo.get("requestFields") instanceof Map<?, ?> rf
						? (Map<String, Object>) rf
						: null;
		if (requestFields != null) {
			if (requestFields.containsKey("birthday")) {
				requestFields.put(
						"birthday",
						DataMasking.maskBirthday(String.valueOf(requestFields.get("birthday"))));
			}
			if (requestFields.containsKey("address")) {
				requestFields.put(
						"address",
						DataMasking.maskDetailedAddress(String.valueOf(requestFields.get("address"))));
			}
			if (requestFields.containsKey("sex")) {
				Object rawSex = requestFields.get("sex");
				if ((rawSex instanceof Number n && n.longValue() == 0L)
						|| Objects.toString(rawSex, "").trim().equals("0")) {
					requestFields.put("sex", "-");
				} else {
					String t = Objects.toString(rawSex, "");
					if (t.isBlank()) {
						requestFields.put("sex", t);
					} else {
						requestFields.put("sex", DataMasking.maskSex(t));
					}
				}
			}
		}
		if (!oemShuyunFlag && requestFields != null) {
			@SuppressWarnings("unchecked")
			List<Object> mobileFieldKeys =
					memberInfo.get("datapassRequestFields") instanceof Map<?, ?> dpm
							&& ((Map<?, ?>) dpm).get("mobile") instanceof List<?> lst
									? (List<Object>) lst
									: null;
			if (mobileFieldKeys != null) {
				for (Object keyObj : mobileFieldKeys) {
					if (keyObj == null) {
						continue;
					}
					String key = String.valueOf(keyObj);
					if (!requestFields.containsKey(key)) {
						continue;
					}
					requestFields.put(
							key,
							DataMasking.maskMobileIfBlocked(
									String.valueOf(requestFields.get(key)), DATAPASS_BLOCK_ON));
				}
			}
		}
	}
}
