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

public final class DigiOsUpsertResponse {

	private DigiOsUpsertResponse() {}

	public record NumericIdResult(boolean ok, Long id, String cd, String error) {}

	public record MutationResult(boolean ok, String error) {}

	public static NumericIdResult resolveWithNumericId(DigiOsGatewayResult result, String expectedCd) {
		BaseResolved base = resolveBase(result, "digios upsert failed");
		if (!base.ok) {
			return new NumericIdResult(false, null, null, base.error);
		}
		Map<String, Object> data = base.data;
		long id = toLong(data.get("id"));
		String cd = trim(data.get("cd"));
		if (id <= 0) {
			return new NumericIdResult(false, null, cd.isEmpty() ? null : cd, "missing id in response");
		}
		if (StringUtils.hasText(expectedCd) && StringUtils.hasText(cd) && !expectedCd.equals(cd)) {
			return new NumericIdResult(false, id, cd, "response cd mismatch");
		}
		String resolvedCd = StringUtils.hasText(cd) ? cd : (StringUtils.hasText(expectedCd) ? expectedCd : null);
		return new NumericIdResult(true, id, resolvedCd, null);
	}

	public static MutationResult resolveMutation(DigiOsGatewayResult result) {
		if (!result.ok()) {
			String error = trim(result.error());
			if (!StringUtils.hasText(error)) {
				error = DigiOsGatewayResponse.resolveError(result.body());
				if (!StringUtils.hasText(error)) {
					error = "digios mutation failed";
				}
			}
			return new MutationResult(false, error);
		}
		String gatewayError = DigiOsGatewayResponse.resolveError(result.body());
		if (gatewayError != null) {
			return new MutationResult(false, gatewayError);
		}
		return new MutationResult(true, null);
	}

	private record BaseResolved(boolean ok, Map<String, Object> data, String error) {}

	private static BaseResolved resolveBase(DigiOsGatewayResult result, String defaultError) {
		if (!result.ok()) {
			String error = trim(result.error());
			if (!StringUtils.hasText(error)) {
				error = DigiOsGatewayResponse.resolveError(result.body());
				if (!StringUtils.hasText(error)) {
					error = defaultError;
				}
			}
			return new BaseResolved(false, null, error);
		}
		Map<String, Object> body = result.body();
		if (body == null) {
			return new BaseResolved(false, null, "empty response body");
		}
		Object dataRaw = body.get("data");
		if (!(dataRaw instanceof Map<?, ?> dataMap)) {
			return new BaseResolved(false, null, "missing data in response");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) dataMap;
		return new BaseResolved(true, data, null);
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static String trim(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
