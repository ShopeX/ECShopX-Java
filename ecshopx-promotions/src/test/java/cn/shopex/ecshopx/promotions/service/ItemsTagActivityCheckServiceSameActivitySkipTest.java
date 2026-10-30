package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.dto.ItemActivityCheckRow;
import cn.shopex.ecshopx.promotions.dto.LimitItemTagCheckRow;
import cn.shopex.ecshopx.promotions.dto.LimitPromotionNameRow;
import cn.shopex.ecshopx.promotions.dto.MarketingActivityItemTagCheckRow;
import cn.shopex.ecshopx.promotions.dto.MarketingActivityLabelRow;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ECX-10579：打标签冲突校验应跳过同一 marketing_id / limit_id 的多行自撞。
 */
@ExtendWith(MockitoExtension.class)
class ItemsTagActivityCheckServiceSameActivitySkipTest {

	@Mock
	private LimitItemPromotionsMapper limitItemPromotionsMapper;

	@Mock
	private MarketingActivityItemsMapper marketingActivityItemsMapper;

	@Mock
	private LimitPromotionsMapper limitPromotionsMapper;

	@Mock
	private MarketingActivityMapper marketingActivityMapper;

	@InjectMocks
	private ItemsTagActivityCheckService service;

	@Test
	void checkActivity_sameMarketingIdTwoNormalRows_passesWithoutConflict() {
		stubEmptyLimits();
		int now = (int) (System.currentTimeMillis() / 1000L);
		MarketingActivityItemTagCheckRow a = marketingRow(57L, 1L, "normal", now - 10, now + 1000, "full_discount");
		MarketingActivityItemTagCheckRow b = marketingRow(57L, 2L, "normal", now - 10, now + 1000, "full_discount");
		when(marketingActivityItemsMapper.selectActiveRowsForTagActivityCheck(
						eq(1L), anyInt(), anyList(), anyList(), anyList(), anyList()))
				.thenReturn(List.of(a, b));

		AtomicReference<String> err = new AtomicReference<>();
		boolean ok = service.checkActivity(itemRows(List.of(1L, 2L)), List.of(19L), 1L, err);

		assertThat(ok).isTrue();
		assertThat(err.get()).isNull();
		verify(marketingActivityMapper, never()).selectMarketingLabelsByMarketingIds(anyList());
	}

	@Test
	void checkActivity_twoDifferentMarketingIdsOverlapping_fails() {
		stubEmptyLimits();
		int now = (int) (System.currentTimeMillis() / 1000L);
		MarketingActivityItemTagCheckRow a = marketingRow(57L, 1L, "normal", now - 10, now + 1000, "full_discount");
		MarketingActivityItemTagCheckRow b = marketingRow(58L, 19L, "tag", now - 10, now + 1000, "full_discount");
		when(marketingActivityItemsMapper.selectActiveRowsForTagActivityCheck(
						eq(1L), anyInt(), anyList(), anyList(), anyList(), anyList()))
				.thenReturn(List.of(a, b));
		MarketingActivityLabelRow la = new MarketingActivityLabelRow();
		la.setMarketingId(57L);
		la.setMarketingName("活动A");
		la.setPromotionTag("满折");
		MarketingActivityLabelRow lb = new MarketingActivityLabelRow();
		lb.setMarketingId(58L);
		lb.setMarketingName("活动B");
		lb.setPromotionTag("满折");
		when(marketingActivityMapper.selectMarketingLabelsByMarketingIds(anyList())).thenReturn(List.of(la, lb));

		AtomicReference<String> err = new AtomicReference<>();
		boolean ok = service.checkActivity(itemRows(List.of(1L)), List.of(19L), 1L, err);

		assertThat(ok).isFalse();
		assertThat(err.get()).contains("冲突");
	}

	@Test
	void checkActivity_fullDiscountAndFullMinusDifferentIds_fails() {
		stubEmptyLimits();
		int now = (int) (System.currentTimeMillis() / 1000L);
		MarketingActivityItemTagCheckRow a = marketingRow(10L, 1L, "normal", now - 10, now + 1000, "full_discount");
		MarketingActivityItemTagCheckRow b = marketingRow(11L, 1L, "normal", now - 10, now + 1000, "full_minus");
		when(marketingActivityItemsMapper.selectActiveRowsForTagActivityCheck(
						eq(1L), anyInt(), anyList(), anyList(), anyList(), anyList()))
				.thenReturn(List.of(a, b));
		MarketingActivityLabelRow la = new MarketingActivityLabelRow();
		la.setMarketingId(10L);
		la.setMarketingName("满折活动");
		la.setPromotionTag("满折");
		MarketingActivityLabelRow lb = new MarketingActivityLabelRow();
		lb.setMarketingId(11L);
		lb.setMarketingName("满减活动");
		lb.setPromotionTag("满减");
		when(marketingActivityMapper.selectMarketingLabelsByMarketingIds(anyList())).thenReturn(List.of(la, lb));

		AtomicReference<String> err = new AtomicReference<>();
		boolean ok = service.checkActivity(itemRows(List.of(1L)), List.of(99L), 1L, err);

		assertThat(ok).isFalse();
		assertThat(err.get()).contains("冲突");
	}

	@Test
	void checkActivity_sameLimitIdTwoRows_passesWithoutConflict() {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LimitItemTagCheckRow a = limitRow(7L, 1L, "normal", now - 10, now + 1000);
		LimitItemTagCheckRow b = limitRow(7L, 2L, "normal", now - 10, now + 1000);
		when(limitItemPromotionsMapper.selectActiveRowsForTagActivityCheck(
						eq(1L), anyInt(), anyList(), anyList(), anyList(), anyList()))
				.thenReturn(List.of(a, b));
		when(marketingActivityItemsMapper.selectActiveRowsForTagActivityCheck(
						eq(1L), anyInt(), anyList(), anyList(), anyList(), anyList()))
				.thenReturn(List.of());

		AtomicReference<String> err = new AtomicReference<>();
		boolean ok = service.checkActivity(itemRows(List.of(1L, 2L)), List.of(19L), 1L, err);

		assertThat(ok).isTrue();
		verify(limitPromotionsMapper, never()).selectLimitNamesByLimitIds(anyList());
	}

	@Test
	void checkActivity_twoDifferentLimitIdsOverlapping_fails() {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LimitItemTagCheckRow a = limitRow(7L, 1L, "normal", now - 10, now + 1000);
		LimitItemTagCheckRow b = limitRow(8L, 19L, "tag", now - 10, now + 1000);
		when(limitItemPromotionsMapper.selectActiveRowsForTagActivityCheck(
						eq(1L), anyInt(), anyList(), anyList(), anyList(), anyList()))
				.thenReturn(List.of(a, b));
		LimitPromotionNameRow n1 = new LimitPromotionNameRow();
		n1.setLimitName("限购A");
		LimitPromotionNameRow n2 = new LimitPromotionNameRow();
		n2.setLimitName("限购B");
		when(limitPromotionsMapper.selectLimitNamesByLimitIds(anyList())).thenReturn(List.of(n1, n2));

		AtomicReference<String> err = new AtomicReference<>();
		boolean ok = service.checkActivity(itemRows(List.of(1L)), List.of(19L), 1L, err);

		assertThat(ok).isFalse();
		assertThat(err.get()).contains("限购");
	}

	private void stubEmptyLimits() {
		when(limitItemPromotionsMapper.selectActiveRowsForTagActivityCheck(
						eq(1L), anyInt(), anyList(), anyList(), anyList(), anyList()))
				.thenReturn(List.of());
	}

	private static List<ItemActivityCheckRow> itemRows(List<Long> itemIds) {
		List<ItemActivityCheckRow> rows = new java.util.ArrayList<>();
		for (Long id : itemIds) {
			ItemActivityCheckRow r = new ItemActivityCheckRow();
			r.setItemId(id);
			r.setBrandId(3L);
			r.setMainCatId(3L);
			r.setAllRequestItemIds(itemIds);
			rows.add(r);
		}
		return rows;
	}

	private static MarketingActivityItemTagCheckRow marketingRow(
			long marketingId, long itemId, String itemType, int start, int end, String marketingType) {
		MarketingActivityItemTagCheckRow row = new MarketingActivityItemTagCheckRow();
		row.setMarketingId(marketingId);
		row.setItemId(itemId);
		row.setItemType(itemType);
		row.setStartTime(start);
		row.setEndTime(end);
		row.setMarketingType(marketingType);
		return row;
	}

	private static LimitItemTagCheckRow limitRow(long limitId, long itemId, String itemType, int start, int end) {
		LimitItemTagCheckRow row = new LimitItemTagCheckRow();
		row.setLimitId(limitId);
		row.setItemId(itemId);
		row.setItemType(itemType);
		row.setStartTime(start);
		row.setEndTime(end);
		return row;
	}
}
