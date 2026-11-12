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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SearchSynonymImportRowService {

	private static final int MAX_WORD_COLUMNS = 20;

	private final SearchSynonymService searchSynonymService;

	public SearchSynonymImportRowService(SearchSynonymService searchSynonymService) {
		this.searchSynonymService = searchSynonymService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void applyRow(long companyId, Map<String, Object> row) {
		String groupKey = stringValue(row.get("group_key"));
		if (!StringUtils.hasText(groupKey)) {
			throw new BadRequestException("group_key 必填");
		}
		groupKey = groupKey.trim();

		List<String> words = collectWords(row);
		if (words.isEmpty()) {
			throw new BadRequestException("words 不能为空");
		}

		searchSynonymService.upsertGroupWithoutEsReload(companyId, groupKey, words);
	}

	private List<String> collectWords(Map<String, Object> row) {
		LinkedHashSet<String> seen = new LinkedHashSet<>();
		for (int i = 1; i <= MAX_WORD_COLUMNS; i++) {
			Object raw = row.get("word_" + i);
			if (raw == null) {
				continue;
			}
			String word = stringValue(raw);
			if (!StringUtils.hasText(word)) {
				continue;
			}
			seen.add(word.trim());
		}
		return new ArrayList<>(seen);
	}

	private static String stringValue(Object value) {
		if (value == null) {
			return "";
		}
		return value.toString();
	}
}
