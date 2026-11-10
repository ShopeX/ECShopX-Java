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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchIndexStoreItemKeyOrderTest {

	@Test
	void compoundCursorPagesAllKeysWithSameDefaultItemId() {
		List<SearchIndexStoreItemKey> all = List.of(
				new SearchIndexStoreItemKey(1, 100),
				new SearchIndexStoreItemKey(2, 100),
				new SearchIndexStoreItemKey(0, 101));
		all = new ArrayList<>(all);
		all.sort(Comparator.comparingLong(SearchIndexStoreItemKey::defaultItemId)
				.thenComparingLong(SearchIndexStoreItemKey::distributorId));

		long afterItemId = 0L;
		long afterDistId = 0L;
		List<SearchIndexStoreItemKey> seen = new ArrayList<>();
		while (true) {
			long cursorItem = afterItemId;
			long cursorDist = afterDistId;
			List<SearchIndexStoreItemKey> page = all.stream()
					.filter(k -> SearchIndexStoreItemKeyOrder.isAfter(
							k.defaultItemId(), k.distributorId(), cursorItem, cursorDist))
					.limit(1)
					.toList();
			if (page.isEmpty()) {
				break;
			}
			SearchIndexStoreItemKey last = page.get(page.size() - 1);
			seen.add(last);
			afterItemId = last.defaultItemId();
			afterDistId = last.distributorId();
		}

		assertThat(seen).containsExactlyElementsOf(all);
	}

	@Test
	void isAfterMatchesLexicographicOrder() {
		assertThat(SearchIndexStoreItemKeyOrder.isAfter(100, 1, 0, 0)).isTrue();
		assertThat(SearchIndexStoreItemKeyOrder.isAfter(100, 1, 100, 0)).isTrue();
		assertThat(SearchIndexStoreItemKeyOrder.isAfter(100, 1, 100, 1)).isFalse();
		assertThat(SearchIndexStoreItemKeyOrder.isAfter(99, 99, 100, 0)).isFalse();
	}
}
