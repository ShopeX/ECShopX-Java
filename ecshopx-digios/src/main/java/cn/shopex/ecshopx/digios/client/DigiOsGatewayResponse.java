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

/** 对齐 PHP {@code GatewayResponse}。 */
public final class DigiOsGatewayResponse {

	private DigiOsGatewayResponse() {}

	public static boolean isSuccess(Integer httpStatus, Map<String, Object> body) {
		if (httpStatus == null || httpStatus < 200 || httpStatus >= 300) {
			return false;
		}
		return resolveError(body) == null;
	}

	public static String resolveError(Map<String, Object> body) {
		if (body == null) {
			return null;
		}
		String errorCode = trim(body.get("errorCode"));
		String errorMessage = trim(body.get("errorMessage"));
		if (!StringUtils.hasText(errorCode) && !StringUtils.hasText(errorMessage)) {
			return null;
		}
		if (StringUtils.hasText(errorCode) && StringUtils.hasText(errorMessage)) {
			return errorCode + ": " + errorMessage;
		}
		return StringUtils.hasText(errorCode) ? errorCode : errorMessage;
	}

	public static boolean isTokenUnauthorized(Integer httpStatus, Map<String, Object> body) {
		if (httpStatus != null && (httpStatus == 401 || httpStatus == 403)) {
			return true;
		}
		if (body == null) {
			return false;
		}
		String errorCode = String.valueOf(body.getOrDefault("errorCode", ""));
		String errorMessage = String.valueOf(body.getOrDefault("errorMessage", ""));
		return "MS-GATEWAY-0003".equals(errorCode)
				|| errorMessage.contains("Token")
				|| errorMessage.contains("Client ID");
	}

	private static String trim(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
