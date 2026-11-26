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

package cn.shopex.ecshopx.goods.service.hotword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** ECX-11390 F-002/AC-005：热词图片格式、大小与文件头校验。 */
class SearchHotwordImageRulesTest {

	private static byte[] gif() {
		return "GIF89a".concat("rest").getBytes(StandardCharsets.ISO_8859_1);
	}

	private static byte[] png() {
		byte[] head = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
		byte[] body = new byte[16];
		System.arraycopy(head, 0, body, 0, head.length);
		return body;
	}

	private static byte[] jpeg() {
		byte[] head = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
		byte[] body = new byte[16];
		System.arraycopy(head, 0, body, 0, head.length);
		return body;
	}

	@Test
	void extension_whitelist() {
		assertThat(SearchHotwordImageRules.requireAllowedExtension("a.GIF")).isEqualTo("gif");
		assertThat(SearchHotwordImageRules.requireAllowedExtension("a.png")).isEqualTo("png");
		assertThat(SearchHotwordImageRules.requireAllowedExtension("a.jpeg")).isEqualTo("jpg");
		assertThatThrownBy(() -> SearchHotwordImageRules.requireAllowedExtension("a.webp"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("gif");
		assertThatThrownBy(() -> SearchHotwordImageRules.requireAllowedExtension("noext"))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void size_mustBeStrictlyLessThanFiveMegabytes() {
		SearchHotwordImageRules.assertSize(SearchHotwordImageRules.MAX_BYTES - 1L);

		assertThatThrownBy(() -> SearchHotwordImageRules.assertSize(SearchHotwordImageRules.MAX_BYTES))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("5MB");
		assertThatThrownBy(() -> SearchHotwordImageRules.assertSize(0L))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void magic_mustMatchExtension() {
		assertThat(SearchHotwordImageRules.detectType(gif())).isEqualTo("gif");
		assertThat(SearchHotwordImageRules.detectType(png())).isEqualTo("png");
		assertThat(SearchHotwordImageRules.detectType(jpeg())).isEqualTo("jpg");
		assertThat(SearchHotwordImageRules.detectType("plain text".getBytes(StandardCharsets.UTF_8)))
				.isNull();

		SearchHotwordImageRules.assertContentMatches(gif(), "gif");
		assertThatThrownBy(() -> SearchHotwordImageRules.assertContentMatches(png(), "gif"))
				.isInstanceOf(BadRequestException.class);
	}
}
