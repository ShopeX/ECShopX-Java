/**
 * Copyright 2019-2026 ShopeX
 */

package cn.shopex.ecshopx.goods.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.MemberPrice;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.MemberPriceMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchIndexMemberActivityRepositoryTest {

	@Mock private MemberPriceMapper memberPriceMapper;
	@Mock private SeckillRelGoodsMapper seckillRelGoodsMapper;
	@Mock private PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;
	@Mock private PromotionGroupsActivityMapper promotionGroupsActivityMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private SearchIndexMemberActivityRepository repository;

	@BeforeEach
	void setUp() {
		repository =
				new SearchIndexMemberActivityRepository(
						memberPriceMapper,
						seckillRelGoodsMapper,
						promotionGroupsRelGoodsMapper,
						promotionGroupsActivityMapper,
						objectMapper);
	}

	@Test
	void loadActivities_mapsSeckillTypeAndGroupWindow() throws Exception {
		SeckillRelGoods seckill = new SeckillRelGoods();
		seckill.setSeckillId(10L);
		seckill.setSeckillType("limited_time_sale");
		seckill.setItemId(42L);
		seckill.setActivityPrice(1200);
		seckill.setActivityStartTime(100);
		seckill.setActivityEndTime(200);
		seckill.setDisabled(false);
		when(seckillRelGoodsMapper.selectList(any())).thenReturn(List.of(seckill));

		PromotionGroupsRelGoods groupRel = new PromotionGroupsRelGoods();
		groupRel.setGroupsActivityId(77L);
		groupRel.setItemId(42L);
		groupRel.setActivityPrice(900L);
		when(promotionGroupsRelGoodsMapper.selectList(any())).thenReturn(List.of(groupRel));

		PromotionGroupsActivity groupAct = new PromotionGroupsActivity();
		groupAct.setGroupsActivityId(77L);
		groupAct.setBeginTime(300L);
		groupAct.setEndTime(400L);
		groupAct.setDisabled(false);
		when(promotionGroupsActivityMapper.selectList(any())).thenReturn(List.of(groupAct));

		List<SearchIndexSource.Activity> activities = repository.loadActivities(1L, List.of(42L));

		assertThat(activities).hasSize(2);
		assertThat(activities.get(0).type()).isEqualTo("limited_time_sale");
		assertThat(activities.get(1).type()).isEqualTo("group");
		assertThat(activities.get(1).startAt()).isEqualTo(300L);
	}

	@Test
	void loadMemberPrices_readsPromotionsMemberPriceRow() throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		ObjectNode grade = objectMapper.createObjectNode();
		grade.put("2", "1800");
		root.set("grade", grade);
		root.set("vipGrade", objectMapper.createObjectNode());
		String stored = MemberPriceColumnCodec.encodeForStorage(objectMapper, root);

		MemberPrice mp = new MemberPrice();
		mp.setItemId(99L);
		mp.setMprice(stored);
		when(memberPriceMapper.selectOne(any())).thenReturn(mp);

		SearchIndexMemberPriceMaterializer.Buckets buckets =
				repository.loadMemberPrices(1L, 99L, List.of(99L, 100L));

		assertThat(buckets.grade()).containsEntry(2L, 1800L);
	}
}
