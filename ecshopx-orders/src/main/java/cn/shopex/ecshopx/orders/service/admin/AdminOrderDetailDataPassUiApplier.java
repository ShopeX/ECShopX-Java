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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.util.DataMasking;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdminOrderDetailDataPassUiApplier {

	public void applyBlockedAppInfoAndOrderMask(Map<String, Object> orderInfo) {
		Object appInfoObj = orderInfo.get("app_info");
		if (appInfoObj instanceof Map<?, ?> appInfoRaw) {
			@SuppressWarnings("unchecked")
			Map<String, Object> appInfo = (Map<String, Object>) appInfoRaw;
			filterContactButton(appInfo);
		}
		applyOrderContactMask(orderInfo);
	}

	public void maskOperatorDescIfPresent(Map<String, Object> orderInfo) {
		Object raw = orderInfo.get("operator_desc");
		if (raw == null) {
			return;
		}
		String s = String.valueOf(raw);
		String sep = " : ";
		int idx = s.indexOf(sep);
		if (idx < 0) {
			sep = ":";
			idx = s.indexOf(sep);
		}
		if (idx > 0 && idx < s.length() - 1) {
			String mobilePart = s.substring(0, idx).trim();
			String namePart = s.substring(idx + sep.length()).trim();
			orderInfo.put(
					"operator_desc",
					DataMasking.maskMobile(mobilePart) + " : " + DataMasking.maskTruename(namePart));
		} else {
			orderInfo.put("operator_desc", DataMasking.maskTruename(s));
		}
	}

	private static void filterContactButton(Map<String, Object> appInfo) {
		Object buttonsObj = appInfo.get("buttons");
		if (!(buttonsObj instanceof List<?> buttons)) {
			return;
		}
		List<Map<String, Object>> kept = new ArrayList<>();
		for (Object o : buttons) {
			if (!(o instanceof Map<?, ?> bm)) {
				continue;
			}
			Object type = bm.get("type");
			if ("contact".equals(String.valueOf(type))) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> copy = new LinkedHashMap<String, Object>((Map<String, Object>) bm);
			kept.add(copy);
		}
		appInfo.put("buttons", kept);
	}

	private static void applyOrderContactMask(Map<String, Object> orderInfo) {
		Object mob = orderInfo.get("mobile");
		orderInfo.put("mobile", DataMasking.maskMobile(mob == null ? "" : String.valueOf(mob)));
		Object rn = orderInfo.get("receiver_name");
		orderInfo.put("receiver_name", DataMasking.maskTruename(rn == null ? "" : String.valueOf(rn)));
		Object rm = orderInfo.get("receiver_mobile");
		orderInfo.put("receiver_mobile", DataMasking.maskMobile(rm == null ? "" : String.valueOf(rm)));
		orderInfo.put("receiver_address", DataMasking.maskAddress(String.valueOf(orderInfo.get("receiver_address"))));
	}
}
