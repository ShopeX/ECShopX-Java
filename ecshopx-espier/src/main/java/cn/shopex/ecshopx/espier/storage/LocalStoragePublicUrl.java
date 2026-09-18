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

package cn.shopex.ecshopx.espier.storage;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Builds publicly reachable URLs for local-disk objects under {@code /storage/**}.
 * <p>When {@link StorageProperties.Local#getUrl()} is set, that base is always used (plus the object
 * key), matching {@link LocalStorageDriver#url(String)}. Only if the configured URL is blank does
 * this fall back to a request-derived {@code /storage/...} absolute URL.
 */
public final class LocalStoragePublicUrl {

	private LocalStoragePublicUrl() {
	}

	public static String resolve(HttpServletRequest request, StorageProperties props, String objectKey) {
		String key = objectKey == null ? "" : objectKey.trim();
		if (key.startsWith("/")) {
			key = key.substring(1);
		}

		if (props != null && props.getLocal() != null) {
			String configured = props.getLocal().getUrl();
			if (configured != null && !configured.isBlank()) {
				String base = configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
				return base + "/" + key;
			}
		}

		String storagePath = "/storage/" + key;
		if (request == null) {
			return storagePath;
		}
		return ServletUriComponentsBuilder.fromRequest(request)
				.replaceQuery(null)
				.fragment(null)
				.replacePath(storagePath)
				.build()
				.toUri()
				.normalize()
				.toString();
	}
}
