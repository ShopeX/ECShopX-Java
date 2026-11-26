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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ECX-11390 F-002/AC-001 ~ AC-005：热词保存入参归一化与校验。 */
class SearchHotwordInputNormalizerTest {

	private static Map<String, Object> validText() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("content", " 双十一 ");
		input.put("start_time", "2027-01-01 00:00:00");
		input.put("end_time", "2027-01-10 23:59:59");
		input.put("channel", "mobile");
		return input;
	}

	@Test
	void textType_appliesDefaults() {
		SearchHotwordInputNormalizer.Normalized normalized =
				SearchHotwordInputNormalizer.normalize(validText());

		assertThat(normalized.content()).isEqualTo("双十一");
		assertThat(normalized.type()).isEqualTo("text");
		assertThat(normalized.textColor()).isEqualTo("#000000");
		assertThat(normalized.backgroundColor()).isEqualTo("#FFFFFF");
		assertThat(normalized.sort()).isZero();
		assertThat(normalized.channel()).isEqualTo("mobile");
		assertThat(normalized.imageUrl()).isNull();
		assertThat(normalized.startTime()).isNotNull();
		assertThat(normalized.endTime()).isNotNull();
	}

	@Test
	void camelCaseKeysAreAccepted() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("content", "热词");
		input.put("startTime", 1_800_000_000);
		input.put("endTime", 1_800_086_400);
		input.put("channel", "pc");
		input.put("background_color", "#abc");

		SearchHotwordInputNormalizer.Normalized normalized =
				SearchHotwordInputNormalizer.normalize(input);

		assertThat(normalized.startTime()).isEqualTo(1_800_000_000);
		assertThat(normalized.backgroundColor()).isEqualTo("#abc");
		assertThat(normalized.channel()).isEqualTo("pc");
	}

	@Test
	void missingRequiredFields_areAllReported() {
		Map<String, Object> input = new LinkedHashMap<>();

		try {
			SearchHotwordInputNormalizer.normalize(input);
		} catch (BadRequestException e) {
			Map<String, List<String>> errors = e.getFieldErrors();
			assertThat(errors).containsKeys("content", "start_time", "end_time", "channel");
		}
	}

	@Test
	void endTimeBeforeStartTime_isRejected() {
		Map<String, Object> input = validText();
		input.put("end_time", "2026-12-31 23:59:59");

		assertThatThrownBy(() -> SearchHotwordInputNormalizer.normalize(input))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void invalidColor_isRejected() {
		Map<String, Object> input = validText();
		input.put("text_color", "red");

		try {
			SearchHotwordInputNormalizer.normalize(input);
		} catch (BadRequestException e) {
			assertThat(e.getFieldErrors()).containsKey("text_color");
		}
	}

	@Test
	void imageType_requiresImageUrlAndKeepsTextStyles() {
		Map<String, Object> input = validText();
		input.put("type", "image");
		input.put("text_color", "#123456");

		try {
			SearchHotwordInputNormalizer.normalize(input);
		} catch (BadRequestException e) {
			assertThat(e.getFieldErrors()).containsKey("image_url");
		}

		input.put("image_url", "https://cdn.example.com/a.gif");
		SearchHotwordInputNormalizer.Normalized normalized =
				SearchHotwordInputNormalizer.normalize(input);

		assertThat(normalized.type()).isEqualTo("image");
		assertThat(normalized.imageUrl()).isEqualTo("https://cdn.example.com/a.gif");
		assertThat(normalized.textColor()).isEqualTo("#123456");
	}

	@Test
	void textType_ignoresImageUrl() {
		Map<String, Object> input = validText();
		input.put("image_url", "https://cdn.example.com/a.gif");

		assertThat(SearchHotwordInputNormalizer.normalize(input).imageUrl()).isNull();
	}

	@Test
	void jumpPage_acceptsObjectOrJsonString() {
		Map<String, Object> input = validText();
		input.put(
				"jump_page",
				Map.of("linkPage", "link", "id", "registActivity", "title", "报名活动"));
		SearchHotwordInputNormalizer.Normalized normalized =
				SearchHotwordInputNormalizer.normalize(input);
		assertThat(normalized.jumpPage())
				.isEqualTo("{\"linkPage\":\"link\",\"id\":\"registActivity\",\"title\":\"报名活动\"}");

		Map<String, Object> asString = validText();
		asString.put(
				"jump_page", "{\"linkPage\":\"goods\",\"id\":\"1\",\"title\":\"测试商品\"}");
		assertThat(SearchHotwordInputNormalizer.normalize(asString).jumpPage())
				.contains("\"linkPage\":\"goods\"")
				.contains("\"id\":\"1\"");
	}

	@Test
	void jumpPage_rejectsLegacyLinkFieldsAndInvalidJson() {
		Map<String, Object> legacy = validText();
		legacy.put("link_page", "/pages/goods/list");
		assertThatThrownBy(() -> SearchHotwordInputNormalizer.normalize(legacy))
				.isInstanceOf(BadRequestException.class);

		Map<String, Object> invalidJson = validText();
		invalidJson.put("jump_page", "not-json");
		assertThatThrownBy(() -> SearchHotwordInputNormalizer.normalize(invalidJson))
				.isInstanceOf(BadRequestException.class);

		Map<String, Object> missingId = validText();
		missingId.put("jump_page", Map.of("linkPage", "goods"));
		assertThatThrownBy(() -> SearchHotwordInputNormalizer.normalize(missingId))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void optionalId_parsesOrRejects() {
		assertThat(SearchHotwordInputNormalizer.optionalId(Map.of())).isNull();
		assertThat(SearchHotwordInputNormalizer.optionalId(Map.of("id", "12"))).isEqualTo(12L);
		assertThatThrownBy(() -> SearchHotwordInputNormalizer.optionalId(Map.of("id", "abc")))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> SearchHotwordInputNormalizer.optionalId(Map.of("id", "0")))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void mergeRequestParams_flattensFormArraysAndKeepsBodyValues() {
		Map<String, String[]> params = new LinkedHashMap<>();
		params.put("content", new String[] {"表单热词"});
		params.put("channel", new String[] {"mobile"});
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("sort", 5);

		Map<String, Object> merged = SearchHotwordInputNormalizer.mergeRequestParams(params, body);

		assertThat(merged.get("content")).isEqualTo("表单热词");
		assertThat(merged.get("channel")).isEqualTo("mobile");
		assertThat(merged.get("sort")).isEqualTo(5);
	}
}
