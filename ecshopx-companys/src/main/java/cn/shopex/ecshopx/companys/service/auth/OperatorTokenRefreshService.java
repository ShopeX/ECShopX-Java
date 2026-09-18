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

package cn.shopex.ecshopx.companys.service.auth;

import cn.shopex.ecshopx.common.auth.OperatorJwtIssuerPort;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class OperatorTokenRefreshService {

	private final OperatorJwtIssuerPort operatorJwtIssuerPort;

	public OperatorTokenRefreshService(OperatorJwtIssuerPort operatorJwtIssuerPort) {
		this.operatorJwtIssuerPort = operatorJwtIssuerPort;
	}

	public String tokenRefresh(HttpServletRequest request) {
		String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		String compactJwt = auth.substring(7).trim();
		if (compactJwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		return operatorJwtIssuerPort.refreshAccessToken(compactJwt);
	}
}
