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

package cn.shopex.ecshopx.digios.client;

import java.util.Map;
import org.springframework.util.StringUtils;

public final class DigiOsAccountResponse {

	private DigiOsAccountResponse() {}

	public record CreateResult(boolean ok, String usrId, String acc, String error) {}

	public static CreateResult resolveCreateResult(DigiOsGatewayResult result, String expectedAcc) {
		if (!result.ok()) {
			String error = trim(result.error());
			if (!StringUtils.hasText(error)) {
				error = DigiOsGatewayResponse.resolveError(result.body());
				if (!StringUtils.hasText(error)) {
					error = "digios account create failed";
				}
			}
			return new CreateResult(false, null, null, error);
		}
		Map<String, Object> body = result.body();
		if (body == null) {
			return new CreateResult(false, null, null, "invalid response body");
		}
		String gatewayError = DigiOsGatewayResponse.resolveError(body);
		if (gatewayError != null) {
			return new CreateResult(false, null, null, gatewayError);
		}
		Object dataRaw = body.get("data");
		if (!(dataRaw instanceof Map<?, ?> dataMap)) {
			return new CreateResult(false, null, null, "missing data in response");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) dataMap;
		String usrId = trim(data.get("usrId"));
		String acc = trim(data.get("acc"));
		if (!StringUtils.hasText(usrId)) {
			return new CreateResult(false, null, StringUtils.hasText(acc) ? acc : null, "missing usrId in response");
		}
		if (StringUtils.hasText(expectedAcc) && StringUtils.hasText(acc) && !expectedAcc.equals(acc)) {
			return new CreateResult(false, usrId, acc, "response acc mismatch");
		}
		return new CreateResult(true, usrId, StringUtils.hasText(acc) ? acc : expectedAcc, null);
	}

	private static String trim(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
