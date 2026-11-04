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

package cn.shopex.ecshopx.wsugc.service.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class UgcWxContentCheckBaseUriGuardTest {

	@Test
	void normalizeAndRequire_allowsOfficialWeixinHttps() {
		assertThat(UgcWxContentCheckBaseUriGuard.normalizeAndRequire("https://api.weixin.qq.com/"))
				.isEqualTo("https://api.weixin.qq.com");
		assertThat(UgcWxContentCheckBaseUriGuard.normalizeAndRequire("https://api.weixin.qq.com"))
				.isEqualTo("https://api.weixin.qq.com");
	}

	@Test
	void normalizeAndRequire_rejectsHttpPrivateAndForeignHosts() {
		assertThatThrownBy(() -> UgcWxContentCheckBaseUriGuard.normalizeAndRequire("http://api.weixin.qq.com"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("contentCheck_url");
		assertThatThrownBy(() -> UgcWxContentCheckBaseUriGuard.normalizeAndRequire("https://127.0.0.1"))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> UgcWxContentCheckBaseUriGuard.normalizeAndRequire("https://evil.example.com"))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> UgcWxContentCheckBaseUriGuard.normalizeAndRequire("file:///etc/passwd"))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(
						() -> UgcWxContentCheckBaseUriGuard.normalizeAndRequire(
								"https://api.weixin.qq.com/wxa"))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void requireOptional_allowsBlank_andValidatesWhenPresent() {
		UgcWxContentCheckBaseUriGuard.requireOptional("");
		UgcWxContentCheckBaseUriGuard.requireOptional(null);
		assertThatThrownBy(
						() -> UgcWxContentCheckBaseUriGuard.requireOptional("https://evil.example.com"))
				.isInstanceOf(BadRequestException.class);
	}
}
