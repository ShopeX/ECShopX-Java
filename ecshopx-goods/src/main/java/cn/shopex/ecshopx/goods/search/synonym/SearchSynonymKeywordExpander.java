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

import cn.shopex.ecshopx.goods.search.synonym.SearchSynonymService.SynonymGroupView;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 查询前按公司同义词组扩展 keywords，供 {@code multi_match} 召回（不依赖索引侧 synonym filter）。
 */
@Component
public class SearchSynonymKeywordExpander {

	private final SearchSynonymService synonymService;

	public SearchSynonymKeywordExpander(SearchSynonymService synonymService) {
		this.synonymService = synonymService;
	}

	public String expand(long companyId, String keywords) {
		if (!StringUtils.hasText(keywords)) {
			return keywords;
		}
		String raw = keywords;
		String trimmed = keywords.trim();
		List<SynonymGroupView> groups = synonymService.list(companyId);
		if (groups.isEmpty()) {
			return keywords;
		}

		LinkedHashSet<String> terms = new LinkedHashSet<>();
		terms.add(trimmed);
		boolean expanded = false;
		for (SynonymGroupView group : groups) {
			if (groupMatched(trimmed, group.words())) {
				expanded = true;
				for (String word : group.words()) {
					if (word == null) {
						continue;
					}
					String w = word.trim();
					if (StringUtils.hasText(w) && !keywordEqualsOrToken(keywords, w)) {
						terms.add(w);
					}
				}
			}
		}
		if (!expanded) {
			return raw;
		}
		return String.join(" ", terms);
	}

	private static boolean groupMatched(String keywords, List<String> words) {
		for (String word : words) {
			if (word == null) {
				continue;
			}
			String w = word.trim();
			if (StringUtils.hasText(w) && keywordEqualsOrToken(keywords, w)) {
				return true;
			}
		}
		return false;
	}

	private static boolean keywordEqualsOrToken(String keywords, String word) {
		if (keywords.equals(word)) {
			return true;
		}
		for (String token : keywords.split("\\s+")) {
			if (token.equals(word)) {
				return true;
			}
		}
		return false;
	}
}
