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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ECX-11390：展示渠道单选（string）。 */
class SearchHotwordChannelsTest {

	@Test
	void toResponseChannel_returnsSingleValue() {
		assertThat(SearchHotwordChannels.toResponseChannel("mobile")).isEqualTo("mobile");
		assertThat(SearchHotwordChannels.toResponseChannel("pc")).isEqualTo("pc");
		assertThat(SearchHotwordChannels.toResponseChannel("pc,mobile")).isEqualTo("pc");
		assertThat(SearchHotwordChannels.toResponseChannel("")).isNull();
		assertThat(SearchHotwordChannels.toResponseChannel(null)).isNull();
	}

	@Test
	void normalizeSaveChannel_acceptsStringOrSingleElementArray() {
		assertThat(SearchHotwordChannels.normalizeSaveChannel(" PC ")).isEqualTo("pc");
		assertThat(SearchHotwordChannels.normalizeSaveChannel(List.of("mobile"))).isEqualTo("mobile");
	}

	@Test
	void normalizeSaveChannel_rejectsMultiOrUnknown() {
		assertThatThrownBy(() -> SearchHotwordChannels.normalizeSaveChannel(List.of("mobile", "pc")))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("channel");
		assertThatThrownBy(() -> SearchHotwordChannels.normalizeSaveChannel("mobile,pc"))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> SearchHotwordChannels.normalizeSaveChannel("weapp"))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> SearchHotwordChannels.normalizeSaveChannel(null))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void queryChannel_defaultsToMobileAndRejectsUnknown() {
		assertThat(SearchHotwordChannels.normalizeQueryChannel(null)).isEqualTo("mobile");
		assertThat(SearchHotwordChannels.normalizeQueryChannel(" PC ")).isEqualTo("pc");
		assertThatThrownBy(() -> SearchHotwordChannels.normalizeQueryChannel("weapp"))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void optionalFilterChannel_blankIsNull() {
		assertThat(SearchHotwordChannels.optionalFilterChannel(null)).isNull();
		assertThat(SearchHotwordChannels.optionalFilterChannel("  ")).isNull();
		assertThat(SearchHotwordChannels.optionalFilterChannel("mobile")).isEqualTo("mobile");
		assertThatThrownBy(() -> SearchHotwordChannels.optionalFilterChannel("weapp"))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void matchesChannel_supportsLegacyCommaStored() {
		assertThat(SearchHotwordChannels.matchesChannel("mobile", "mobile")).isTrue();
		assertThat(SearchHotwordChannels.matchesChannel("mobile,pc", "pc")).isTrue();
		assertThat(SearchHotwordChannels.matchesChannel("mobile", "pc")).isFalse();
		assertThat(SearchHotwordChannels.matchesChannel(null, "mobile")).isFalse();
	}

	@Test
	void channelErrorsCarryFieldCodes() {
		try {
			SearchHotwordChannels.normalizeSaveChannel(null);
		} catch (BadRequestException e) {
			Map<String, List<String>> errors = e.getFieldErrors();
			assertThat(errors).isNotNull();
			assertThat(errors.get("channel")).containsExactly("validation.in");
		}
	}
}
