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

package cn.shopex.ecshopx.goods.search.synonym;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
final class SearchSynonymWordsCodec {

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

	List<String> decode(String wordsJson) {
		if (!StringUtils.hasText(wordsJson)) {
			return List.of();
		}
		try {
			List<String> raw = JSON.readValue(wordsJson.trim(), STRING_LIST);
			if (raw == null || raw.isEmpty()) {
				return List.of();
			}
			return List.copyOf(raw);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("invalid synonym words json", e);
		}
	}

	String encode(List<String> words) {
		List<String> normalized = normalize(words);
		try {
			return JSON.writeValueAsString(normalized);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("serialize synonym words", e);
		}
	}

	static List<String> normalize(List<String> words) {
		if (words == null || words.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<String> seen = new LinkedHashSet<>();
		for (String w : words) {
			if (w == null) {
				continue;
			}
			String t = w.trim();
			if (StringUtils.hasText(t)) {
				seen.add(t);
			}
		}
		return new ArrayList<>(seen);
	}
}
