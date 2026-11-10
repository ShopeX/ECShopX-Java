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

package cn.shopex.ecshopx.goods.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoodsSearchIndexRepositoryTest {

	@Test
	void toEsSourceIncludesSalesAndListedAt() {
		GoodsSearchDocument doc = new GoodsSearchDocument(
				"1_9_100",
				1L,
				9L,
				100L,
				"n",
				"",
				"",
				"",
				List.of(),
				"onsale",
				"approved",
				List.of(),
				1000L,
				1200L,
				88L,
				1_600_000_000L,
				new GoodsSearchDocument.MemberPrices(Map.of(), Map.of()),
				List.of(),
				List.of());
		Map<String, Object> src = GoodsSearchIndexRepository.toEsSource(doc);
		assertThat(src).containsEntry("sales", 88L).containsEntry("listed_at", 1_600_000_000L);
	}

	@Test
	void assertBulkSucceededPassesWhenNoErrors() {
		BulkResponse response = mock(BulkResponse.class);
		when(response.errors()).thenReturn(false);
		assertThatCode(() -> GoodsSearchIndexRepository.assertBulkSucceeded(response)).doesNotThrowAnyException();
	}

	@Test
	void assertBulkSucceededThrowsWhenBulkHasErrors() {
		BulkResponse response = mock(BulkResponse.class);
		when(response.errors()).thenReturn(true);
		assertThatThrownBy(() -> GoodsSearchIndexRepository.assertBulkSucceeded(response))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("bulk index failed");
	}
}
