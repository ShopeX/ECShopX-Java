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

import cn.shopex.ecshopx.goods.domain.GoodsSearchSynonym;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SearchSynonymService {

	private final SearchSynonymRepository repository;
	private final SearchSynonymEsReloader reloader;
	private final SearchSynonymWordsCodec wordsCodec;

	public SearchSynonymService(
			SearchSynonymRepository repository, SearchSynonymEsReloader reloader, SearchSynonymWordsCodec wordsCodec) {
		this.repository = repository;
		this.reloader = reloader;
		this.wordsCodec = wordsCodec;
	}

	public List<SynonymGroupView> list(long companyId) {
		return repository.listByCompanyId(companyId).stream().map(this::toView).toList();
	}

	public SynonymGroupView replaceGroup(long companyId, String groupKey, List<String> words) {
		GoodsSearchSynonym row = repository.replaceGroup(companyId, groupKey, words);
		reloadToEs(companyId);
		return toView(row);
	}

	public void deleteGroup(long companyId, long id) {
		repository.deleteByCompanyAndId(companyId, id);
		reloadToEs(companyId);
	}

	public void reloadToEs(long companyId) {
		List<GoodsSearchSynonym> rows = repository.listByCompanyId(companyId);
		Map<String, List<String>> groups = new LinkedHashMap<>();
		for (GoodsSearchSynonym row : rows) {
			if (row.getGroupKey() == null) {
				continue;
			}
			groups.put(row.getGroupKey(), wordsCodec.decode(row.getWords()));
		}
		try {
			reloader.reload(companyId, groups);
		} catch (IOException e) {
			throw new SearchSynonymEsReloadException("同义词 ES 热更新失败", e);
		}
	}

	private SynonymGroupView toView(GoodsSearchSynonym row) {
		return new SynonymGroupView(
				row.getId(), row.getGroupKey(), List.copyOf(wordsCodec.decode(row.getWords())));
	}

	public record SynonymGroupView(long id, String groupKey, List<String> words) {}
}
