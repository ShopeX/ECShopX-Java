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

import io.undertow.UndertowOptions;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * Undertow 2.3+ enforces a separate multipart entity cap (defaults near 2MB) independent of
 * {@code spring.servlet.multipart.*}. Raise it so Spring’s multipart limits govern uploads.
 */
@Component
public class UndertowMultipartMaxEntityCustomizer implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

	private static final long BYTES_64MB = 64L * 1024L * 1024L;

	@Override
	public void customize(UndertowServletWebServerFactory factory) {
		factory.addBuilderCustomizers(builder -> builder
				.setServerOption(UndertowOptions.MULTIPART_MAX_ENTITY_SIZE, BYTES_64MB)
				// Allow %2F in request paths so encoded slashes reach the servlet mapping layer.
				.setServerOption(UndertowOptions.ALLOW_ENCODED_SLASH, true));
	}
}
