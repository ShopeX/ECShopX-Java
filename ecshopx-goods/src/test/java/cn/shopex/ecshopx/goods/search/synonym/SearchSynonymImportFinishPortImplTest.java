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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchSynonymImportFinishPortImplTest {

	@Mock
	private SearchSynonymService searchSynonymService;

	@Mock
	private SearchEsProperties searchEsProperties;

	private SearchSynonymImportFinishPortImpl port;

	@BeforeEach
	void setUp() {
		port = new SearchSynonymImportFinishPortImpl(searchSynonymService, searchEsProperties);
	}

	@Test
	void onImportFinished_successLinePositive_reloads() {
		when(searchEsProperties.isEnabled()).thenReturn(true);
		port.onImportFinished(10L, 3, 1);
		verify(searchSynonymService).reloadToEs(10L);
	}

	@Test
	void onImportFinished_zeroSuccess_skipsReload() {
		port.onImportFinished(10L, 0, 5);
		verify(searchSynonymService, never()).reloadToEs(anyLong());
	}

	@Test
	void onImportFinished_esDisabled_skipsReload() {
		when(searchEsProperties.isEnabled()).thenReturn(false);
		assertThat(port.onImportFinished(10L, 3, 0)).isNull();
		verify(searchSynonymService, never()).reloadToEs(anyLong());
	}

	@Test
	void onImportFinished_reloadFails_returnsHintAndDoesNotThrow() {
		when(searchEsProperties.isEnabled()).thenReturn(true);
		doThrow(new SearchSynonymEsReloadException("es down", new RuntimeException("cause")))
				.when(searchSynonymService)
				.reloadToEs(10L);
		assertThat(port.onImportFinished(10L, 3, 0))
				.isEqualTo(SearchSynonymImportFinishPortImpl.ES_RELOAD_WARNING_HINT);
	}
}
