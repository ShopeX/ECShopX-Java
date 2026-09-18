package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SavePromotionItemTagJobDispatchPublisher;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsRelGoodsWriteService.NormalizedGroupSkuItem;
import cn.shopex.ecshopx.promotions.service.event.PromotionGroupActivityCommittedEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ordering of {@link SavePromotionItemTagJobDispatchPublisher} vs rel goods write and committed event on update.
 */
class PromotionGroupsActivityUpdateServiceSavePromotionItemTagDispatchTest {

	private static final long COMPANY_ID = 11L;
	private static final long GROUPS_ACTIVITY_ID = 902L;
	private static final long GOODS_ID = 55L;
	private static final long ITEM_ID = 100L;

	@Test
	void updatePromotionGroupsActivity_dispatchesSavePromotionItemTag_withSingleGroupPayloadBeforeRelGoodsAndEvent() {
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
				.thenAnswer(invocation -> String.valueOf(invocation.getArgument(0)));

		MarketingActivityCatalogAccess catalogAccess = mock(MarketingActivityCatalogAccess.class);
		when(catalogAccess.anyGiftItem(anyLong(), any())).thenReturn(false);
		when(catalogAccess.listItemIdsByGoodsId(eq(COMPANY_ID), eq(GOODS_ID))).thenReturn(List.of(ITEM_ID));
		Map<String, Object> skuRow = new LinkedHashMap<>();
		skuRow.put("item_id", ITEM_ID);
		skuRow.put("company_id", COMPANY_ID);
		skuRow.put("price", 100_000L);
		skuRow.put("item_type", "services");
		when(catalogAccess.loadSkuItemsList(eq(COMPANY_ID), any())).thenReturn(Map.of("list", List.of(skuRow)));

		MarketingActivityCrossPromotionGuardService guard = mock(MarketingActivityCrossPromotionGuardService.class);
		doNothing().when(guard).checkActivityValidByGroup(anyMap());

		PromotionGroupsActivity existing = new PromotionGroupsActivity();
		existing.setGroupsActivityId(GROUPS_ACTIVITY_ID);
		existing.setCompanyId(COMPANY_ID);
		existing.setGoodsId(GOODS_ID);

		int begin = (int) (System.currentTimeMillis() / 1000L + 86_400);
		int end = begin + 86_400;
		existing.setBeginTime((long) begin);
		existing.setEndTime((long) end);

		PromotionGroupsActivityMapper activityMapper = mock(PromotionGroupsActivityMapper.class);
		when(activityMapper.selectOne(isA(LambdaQueryWrapper.class))).thenReturn(existing);
		when(
						activityMapper.selectCount(
								isA(LambdaQueryWrapper.class)))
				.thenReturn(0L);

		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		when(jdbcTemplate.queryForObject(contains("COUNT(1)"), eq(Long.class), any(), any()))
				.thenReturn(0L);

		PromotionGroupsActivityGroupParamValidationService validationService =
				new PromotionGroupsActivityGroupParamValidationService(
						messageSource, catalogAccess, activityMapper, jdbcTemplate);

		PromotionGroupsActivityUpdateWritesService updateWrites = mock(PromotionGroupsActivityUpdateWritesService.class);
		when(updateWrites.applyUpdateWrites(isA(PromotionGroupsActivity.class), anyMap(), anyString(), any()))
				.thenReturn(Map.of("groups_activity_id", GROUPS_ACTIVITY_ID));

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		SavePromotionItemTagJobDispatchPublisher tagPublisher = mock(SavePromotionItemTagJobDispatchPublisher.class);
		doNothing()
				.when(tagPublisher)
				.publishSavePromotionItemTag(
						anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyString(), anyList(), anyMap());

		MarketingActivityPostCommitJobsService postCommit = mock(MarketingActivityPostCommitJobsService.class);
		doNothing()
				.when(postCommit)
				.enqueueGroupSalespersonItemsShelves(anyLong(), anyLong());

		PromotionGroupsRelGoodsWriteService relGoodsWriteService = mock(PromotionGroupsRelGoodsWriteService.class);
		NormalizedGroupSkuItem normalizedSku =
				new NormalizedGroupSkuItem(ITEM_ID, 999L, 20L, "title", "pic", "spec");
		when(relGoodsWriteService.normalizeAndValidateItems(anyMap(), eq(COMPANY_ID), any(Locale.class)))
				.thenReturn(List.of(normalizedSku));
		doNothing()
				.when(relGoodsWriteService)
				.replaceRelGoods(eq(GROUPS_ACTIVITY_ID), eq(COMPANY_ID), anyList(), anyInt());

		PromotionGroupsActivityUpdateService svc =
				new PromotionGroupsActivityUpdateService(
						messageSource,
						activityMapper,
						validationService,
						guard,
						catalogAccess,
						updateWrites,
						applicationEventPublisher,
						postCommit,
						tagPublisher,
						relGoodsWriteService);

		Map<String, Object> params = baselineUpdateParams(begin, end);
		Map<String, Object> row =
				svc.updatePromotionGroupsActivity(params, String.valueOf(GROUPS_ACTIVITY_ID), "zh-CN");
		assertNotNull(row);

		InOrder order = inOrder(relGoodsWriteService, tagPublisher, applicationEventPublisher);
		order.verify(relGoodsWriteService)
				.replaceRelGoods(eq(GROUPS_ACTIVITY_ID), eq(COMPANY_ID), eq(List.of(normalizedSku)), anyInt());
		order.verify(tagPublisher)
				.publishSavePromotionItemTag(
						eq(COMPANY_ID),
						eq(GROUPS_ACTIVITY_ID),
						eq("single_group"),
						eq(begin),
						eq(end),
						eq("services"),
						eq(List.of(ITEM_ID)),
						eq(Map.of(ITEM_ID, new BigDecimal("9.99"))));
		ArgumentCaptor<PromotionGroupActivityCommittedEvent> eventCaptor =
				ArgumentCaptor.forClass(PromotionGroupActivityCommittedEvent.class);
		order.verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertNotNull(eventCaptor.getValue());
	}

	private static Map<String, Object> baselineUpdateParams(int begin, int end) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("act_name", "g1");
		params.put("limit_buy_num", 1);
		params.put("person_num", 2);
		params.put("goods_id", GOODS_ID);
		params.put("pics", "http://x/p.jpg");
		params.put("share_desc", "d");
		params.put("store", 20L);
		params.put("act_price", "9.99");
		params.put("limit_time", 1);
		params.put("date", List.of(begin, end));
		return params;
	}
}
