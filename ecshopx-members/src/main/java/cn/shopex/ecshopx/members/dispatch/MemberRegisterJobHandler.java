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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.members.service.h5.auth.ShuyunLoginBridgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class MemberRegisterJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(MemberRegisterJobHandler.class);

	private final ShuyunLoginBridgeService shuyunLoginBridgeService;

	public MemberRegisterJobHandler(ShuyunLoginBridgeService shuyunLoginBridgeService) {
		this.shuyunLoginBridgeService = shuyunLoginBridgeService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Map<String, Object> inputData = new LinkedHashMap<>();
		inputData.put("appid", trimToString(payload.get("appid")));
		inputData.put("source_id", numberOrZero(payload.get("source_id")));
		inputData.put("monitor_id", numberOrZero(payload.get("monitor_id")));
		inputData.put("inviter_id", numberOrZero(payload.get("inviter_id")));
		inputData.put("source_from", stringOrDefault(payload.get("source_from"), "default"));
		Object shuyunApp = payload.get("shuyunappid");
		inputData.put("shuyunappid", shuyunApp == null ? "" : String.valueOf(shuyunApp).trim());

		Map<String, Object> bridgeParams = new LinkedHashMap<>();
		bridgeParams.put("company_id", payload.get("company_id"));
		bridgeParams.put("open_id", Objects.toString(payload.get("open_id"), ""));
		bridgeParams.put("unionid", Objects.toString(payload.get("unionid"), ""));

		Optional<Map<String, Object>> out =
				shuyunLoginBridgeService.shuyunMemberSilent(inputData, bridgeParams);
		if (out.isEmpty()) {
			Object companyObj = bridgeParams.get("company_id");
			Object union = bridgeParams.get("unionid");
			String unionStr = union == null ? "" : String.valueOf(union);
			String masked = maskUnionid(unionStr);
			log.warn("shuyunMemberSilent empty company_id={} unionid={}", companyObj, masked);
		}
	}

	private static String trimToString(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static int numberOrZero(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringOrDefault(Object o, String def) {
		String s = o == null ? "" : String.valueOf(o).trim();
		return StringUtils.hasText(s) ? s : def;
	}

	private static String maskUnionid(String u) {
		if (u == null || u.length() <= 4) {
			return "****";
		}
		return u.substring(0, 2) + "****" + u.substring(u.length() - 2);
	}
}
