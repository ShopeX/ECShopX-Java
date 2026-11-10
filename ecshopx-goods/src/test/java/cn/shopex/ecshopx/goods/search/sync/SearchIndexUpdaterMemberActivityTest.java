/**
 * Copyright 2019-2026 ShopeX
 */

package cn.shopex.ecshopx.goods.search.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocument;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexNames;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexRepository;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSource;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSourceLoader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchIndexUpdaterMemberActivityTest {

	private static final long COMPANY_ID = 11L;
	private static final long DISTRIBUTOR_ID = 22L;
	private static final long DEFAULT_ITEM_ID = 33L;

	@Mock private SearchIndexSourceLoader sourceLoader;
	@Mock private GoodsSearchIndexRepository indexRepository;

	private SearchEsProperties properties;
	private SearchIndexUpdater updater;

	@BeforeEach
	void setUp() {
		properties = new SearchEsProperties();
		properties.setEnabled(true);
		updater = new SearchIndexUpdater(properties, sourceLoader, indexRepository);
	}

	@Test
	void upsertMaterializesMemberPricesAndActivityPriorities() throws Exception {
		SearchIndexSource src =
				SearchIndexSource.builder()
						.companyId(COMPANY_ID)
						.distributorId(DISTRIBUTOR_ID)
						.defaultItemId(DEFAULT_ITEM_ID)
						.hqPriceFen(2000L)
						.memberPriceGrade(Map.of(3L, 1500L))
						.memberPriceVipGrade(Map.of(8L, 1400L))
						.skus(List.of(new SearchIndexSource.Sku(33L, 5L, "onsale")))
						.activities(
								List.of(
										new SearchIndexSource.Activity(
												"seckill", 9001L, 999L, 1_700_000_000L, 1_800_000_000L, null),
										new SearchIndexSource.Activity(
												"group", 7001L, 888L, 1_700_000_100L, 1_800_000_100L, null)))
						.build();
		when(sourceLoader.fetchOne(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID)).thenReturn(Optional.of(src));

		updater.upsert(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID);

		ArgumentCaptor<List<GoodsSearchDocument>> docsCaptor = ArgumentCaptor.forClass(List.class);
		verify(indexRepository)
				.bulkIndex(eq(GoodsSearchIndexNames.alias(properties)), docsCaptor.capture());
		GoodsSearchDocument doc = docsCaptor.getValue().get(0);
		assertThat(doc.memberPrices().grade()).containsEntry(3L, 1500L);
		assertThat(doc.memberPrices().vipGrade()).containsEntry(8L, 1400L);
		assertThat(doc.activities()).extracting(GoodsSearchDocument.Activity::type)
				.containsExactly("seckill", "group");
		assertThat(doc.activities()).extracting(GoodsSearchDocument.Activity::priority).containsExactly(1, 2);
	}
}
