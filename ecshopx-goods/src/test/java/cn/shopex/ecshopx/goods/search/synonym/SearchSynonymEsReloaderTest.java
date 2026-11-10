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

import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchSynonymEsReloaderTest {

	@Mock
	private ElasticsearchClient client;

	@Test
	void reloadWhenDisabledDoesNotCallElasticsearch() throws Exception {
		SearchEsProperties properties = new SearchEsProperties();
		properties.setEnabled(false);
		SearchSynonymEsReloader reloader = new SearchSynonymEsReloader(client, properties);

		reloader.reload(10L, Map.of("brand", List.of("手机", "移动电话")));

		verifyNoInteractions(client);
	}
}
