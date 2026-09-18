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

package cn.shopex.ecshopx.aliyunsms.dispatch;

import cn.shopex.ecshopx.aliyunsms.integration.AliyunsmsUpdateSmsSignClient;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsAddSmsSignImageSupport;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ModifySmsSignJobHandler implements DispatchHandler {

	private final AliyunsmsAddSmsSignImageSupport imageSupport;
	private final AliyunsmsUpdateSmsSignClient updateSmsSignClient;

	public ModifySmsSignJobHandler(
			AliyunsmsAddSmsSignImageSupport imageSupport, AliyunsmsUpdateSmsSignClient updateSmsSignClient) {
		this.imageSupport = imageSupport;
		this.updateSmsSignClient = updateSmsSignClient;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload.get("company_id"));
		String signName = stringify(payload.get("sign_name"));
		int signSource = extractInt(payload.get("sign_source"));
		String remark = stringify(payload.get("remark"));
		boolean thirdParty = extractBoolean(payload.get("third_party"));
		String qualificationId = stringify(payload.get("qualification_id"));

		Map<String, Object> imageInput = new LinkedHashMap<>();
		imageInput.put("sign_file", payload.get("sign_file"));
		imageInput.put("delegate_file", payload.get("delegate_file"));
		Map<String, Object> enriched = imageSupport.enrichModifyParams(companyId, imageInput);
		updateSmsSignClient.updateSmsSign(
				companyId, signName, signSource, remark, thirdParty, qualificationId, enriched);
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static int extractInt(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(raw).trim());
	}

	private static boolean extractBoolean(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(raw).trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}
}
