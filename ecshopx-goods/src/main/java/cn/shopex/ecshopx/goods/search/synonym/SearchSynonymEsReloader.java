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

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexNames;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.synonyms.PutSynonymRequest;
import co.elastic.clients.elasticsearch.synonyms.SynonymRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 将公司同义词组推送至 ES 8 Synonyms Set API，并尝试 reload search analyzers。 */
@Service
public class SearchSynonymEsReloader {

	private final ElasticsearchClient client;
	private final SearchEsProperties properties;

	public SearchSynonymEsReloader(ElasticsearchClient client, SearchEsProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	public void reload(long companyId, Map<String, List<String>> groups) throws IOException {
		String setId = SearchSynonymSetNames.synonymSetId(companyId);
		List<SynonymRule> rules = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
			String groupKey = entry.getKey();
			List<String> words = entry.getValue();
			if (!StringUtils.hasText(groupKey) || words == null || words.isEmpty()) {
				continue;
			}
			String synonymLine = String.join(", ", words);
			rules.add(SynonymRule.of(r -> r.id(groupKey.trim()).synonyms(synonymLine)));
		}

		client.synonyms()
				.putSynonym(PutSynonymRequest.of(p -> p.id(setId).synonymsSet(rules)));

		reloadAliasSearchAnalyzers();
	}

	private void reloadAliasSearchAnalyzers() throws IOException {
		String aliasName = GoodsSearchIndexNames.alias(properties);
		if (!client.indices().existsAlias(e -> e.name(aliasName)).value()) {
			return;
		}
		var bound = client.indices().getAlias(g -> g.name(aliasName)).result();
		for (String physicalIndex : bound.keySet()) {
			client.indices().reloadSearchAnalyzers(r -> r.index(physicalIndex));
		}
	}
}
