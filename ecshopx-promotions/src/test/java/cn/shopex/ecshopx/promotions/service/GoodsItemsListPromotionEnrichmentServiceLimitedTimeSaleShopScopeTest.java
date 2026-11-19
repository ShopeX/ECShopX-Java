package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.PromotionsItemsTag;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.service.systemtag.MemberCrowdMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PHP Align: ItemsService list promotion tags — limited_time_sale 按替换后的店铺 distributor_id
 * 与活动 source_id 互斥过滤。
 */
@ExtendWith(MockitoExtension.class)
class GoodsItemsListPromotionEnrichmentServiceLimitedTimeSaleShopScopeTest {

	@Mock private PromotionsItemsTagMapper promotionsItemsTagMapper;
	@Mock private MarketingActivityMapper marketingActivityMapper;
	@Mock private SeckillActivityMapper seckillActivityMapper;
	@Mock private MemberAccountService memberAccountService;
	@Mock private MemberCrowdMatcher memberCrowdMatcher;

	private GoodsItemsListPromotionEnrichmentService service;

	@BeforeEach
	void setUp() {
		service =
				new GoodsItemsListPromotionEnrichmentService(
						promotionsItemsTagMapper,
						marketingActivityMapper,
						seckillActivityMapper,
						memberAccountService,
						new ObjectMapper(),
						memberCrowdMatcher);
	}

	@Test
	void passesLimitedTimeSaleShopScope_requiresSourceIdMatchAndOptionalCsv() {
		SeckillActivity act = new SeckillActivity();
		act.setSourceId(129L);
		act.setDistributorId(",129,");
		assertTrue(GoodsItemsListPromotionEnrichmentService.passesLimitedTimeSaleShopScope(act, 129L));
		assertFalse(GoodsItemsListPromotionEnrichmentService.passesLimitedTimeSaleShopScope(act, 289L));
		assertFalse(GoodsItemsListPromotionEnrichmentService.passesLimitedTimeSaleShopScope(null, 129L));
	}

	@Test
	void enrich_dropsLimitedTimeSaleTagWhenShopContextDoesNotMatchSourceId() {
		PromotionsItemsTag tag = new PromotionsItemsTag();
		tag.setPromotionId(205L);
		tag.setTagType("limited_time_sale");
		tag.setGoodsId(6745L);
		tag.setItemId(6745L);
		tag.setActivityPrice(100L);
		tag.setIsAllItems(0L);
		tag.setStartTime(1);
		tag.setEndTime(Integer.MAX_VALUE);
		tag.setCompanyId(38L);
		when(promotionsItemsTagMapper.selectList(any())).thenReturn(List.of(tag));
		when(marketingActivityMapper.selectBatchIds(any())).thenReturn(List.of());

		SeckillActivity seckill = new SeckillActivity();
		seckill.setSeckillId(205L);
		seckill.setSourceId(129L);
		seckill.setDistributorId(",129,");
		seckill.setSeckillType("limited_time_sale");
		when(seckillActivityMapper.selectBatchIds(any())).thenReturn(List.of(seckill));

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 38L);
		row.put("goods_id", 6745L);
		row.put("item_id", 6745L);
		row.put("distributor_id", 289L);
		row.put("price", 1200000L);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row);

		service.enrich(rows, 0L);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> acts = (List<Map<String, Object>>) row.get("promotion_activity");
		assertTrue(acts == null || acts.isEmpty());
		assertEquals(null, row.get("activity_price"));
		assertEquals(1200000L, ((Number) row.get("price")).longValue());
	}

	@Test
	void enrich_keepsLimitedTimeSaleTagWhenShopContextMatchesSourceId() {
		PromotionsItemsTag tag = new PromotionsItemsTag();
		tag.setPromotionId(205L);
		tag.setTagType("limited_time_sale");
		tag.setGoodsId(6745L);
		tag.setItemId(6745L);
		tag.setActivityPrice(100L);
		tag.setIsAllItems(0L);
		tag.setStartTime(1);
		tag.setEndTime(Integer.MAX_VALUE);
		tag.setCompanyId(38L);
		when(promotionsItemsTagMapper.selectList(any())).thenReturn(List.of(tag));
		when(marketingActivityMapper.selectBatchIds(any())).thenReturn(List.of());

		SeckillActivity seckill = new SeckillActivity();
		seckill.setSeckillId(205L);
		seckill.setSourceId(129L);
		seckill.setDistributorId(",129,");
		seckill.setSeckillType("limited_time_sale");
		when(seckillActivityMapper.selectBatchIds(any())).thenReturn(List.of(seckill));
		when(memberCrowdMatcher.matchesLimitedTimeSale(any(Long.class), any(Long.class), any(), any(), any()))
				.thenReturn(true);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 38L);
		row.put("goods_id", 6745L);
		row.put("item_id", 6745L);
		row.put("distributor_id", 129L);
		row.put("price", 1200000L);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row);

		service.enrich(rows, 0L);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> acts = (List<Map<String, Object>>) row.get("promotion_activity");
		assertEquals(1, acts.size());
		assertEquals(100L, ((Number) row.get("activity_price")).longValue());
		// PHP 对齐：命中限时特惠只写 activity_price，不覆盖原售价 price
		assertEquals(1200000L, ((Number) row.get("price")).longValue());
	}
}
