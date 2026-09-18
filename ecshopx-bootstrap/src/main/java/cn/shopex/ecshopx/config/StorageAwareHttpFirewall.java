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

package cn.shopex.ecshopx.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Uses {@link DefaultHttpFirewall} for {@code /storage/**} so local-disk image URLs that contain
 * non-ASCII filenames (Chinese / legacy mojibake) are not rejected by
 * {@link StrictHttpFirewall}'s printable-ASCII {@code requestURI} check. All other paths keep
 * {@link StrictHttpFirewall} with the existing encoded-slash / semicolon relaxations.
 */
final class StorageAwareHttpFirewall implements HttpFirewall {

	private final StrictHttpFirewall strict = new StrictHttpFirewall();
	private final DefaultHttpFirewall storage = new DefaultHttpFirewall();

	StorageAwareHttpFirewall() {
		this.strict.setAllowUrlEncodedSlash(true);
		this.strict.setAllowUrlEncodedDoubleSlash(true);
		this.strict.setAllowSemicolon(true);
		this.storage.setAllowUrlEncodedSlash(true);
	}

	@Override
	public FirewalledRequest getFirewalledRequest(HttpServletRequest request) throws RequestRejectedException {
		String uri = request.getRequestURI();
		if (uri != null && isStoragePath(uri)) {
			return this.storage.getFirewalledRequest(request);
		}
		return this.strict.getFirewalledRequest(request);
	}

	@Override
	public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
		return this.strict.getFirewalledResponse(response);
	}

	static boolean isStoragePath(String requestUri) {
		return requestUri.startsWith("/storage/") || "/storage".equals(requestUri);
	}
}
