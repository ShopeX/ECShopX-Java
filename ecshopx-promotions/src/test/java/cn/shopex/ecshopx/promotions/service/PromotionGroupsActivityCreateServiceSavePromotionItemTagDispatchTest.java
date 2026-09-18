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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SavePromotionItemTagJobDispatchPublisher;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsRelGoodsWriteService.NormalizedGroupSkuItem;
import cn.shopex.ecshopx.promotions.service.event.PromotionGroupActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionGroupsActivityMultiLangWriteService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * In-transaction {@link SavePromotionItemTagJobDispatchPublisher} ordering vs rel goods write and committed event.
 */
class PromotionGroupsActivityCreateServiceSavePromotionItemTagDispatchTest {

	private static final long COMPANY_ID = 11L;
	private static final long GROUPS_ACTIVITY_ID = 901L;
	private static final long GOODS_ID = 55L;
	private static final long ITEM_ID = 100L;

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void createPromotionGroupsActivity_dispatchesSavePromotionItemTag_withSingleGroupPayloadBeforeRelGoodsAndEvent() {
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

		PromotionGroupsActivityMapper activityMapper = mock(PromotionGroupsActivityMapper.class);
		when(activityMapper.insert(isA(PromotionGroupsActivity.class)))
				.thenAnswer(
						invocation -> {
							PromotionGroupsActivity entity = invocation.getArgument(0);
							entity.setGroupsActivityId(GROUPS_ACTIVITY_ID);
							return 1;
						});
		when(
						activityMapper.selectCount(
								isA(
										com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
				.thenReturn(0L);

		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		when(jdbcTemplate.queryForObject(contains("COUNT(1)"), eq(Long.class), any(), any()))
				.thenReturn(0L);

		PromotionGroupsActivityGroupParamValidationService validationService =
				new PromotionGroupsActivityGroupParamValidationService(
						messageSource, catalogAccess, activityMapper, jdbcTemplate);

		PromotionGroupsActivityMultiLangWriteService multiLang =
				mock(PromotionGroupsActivityMultiLangWriteService.class);
		doNothing()
				.when(multiLang)
				.addForNewGroup(eq(GROUPS_ACTIVITY_ID), eq(COMPANY_ID), anyMap(), anyString());

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		SavePromotionItemTagJobDispatchPublisher tagPublisher = mock(SavePromotionItemTagJobDispatchPublisher.class);
		doNothing()
				.when(tagPublisher)
				.publishSavePromotionItemTag(
						anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyString(), anyList(), anyMap());
		MarketingActivityPostCommitJobsService postCommitJobs = mock(MarketingActivityPostCommitJobsService.class);

		PromotionGroupsRelGoodsWriteService relGoodsWriteService = mock(PromotionGroupsRelGoodsWriteService.class);
		NormalizedGroupSkuItem normalizedSku =
				new NormalizedGroupSkuItem(ITEM_ID, 999L, 20L, "title", "pic", "spec");
		when(relGoodsWriteService.normalizeAndValidateItems(anyMap(), eq(COMPANY_ID), any(Locale.class)))
				.thenReturn(List.of(normalizedSku));
		doNothing()
				.when(relGoodsWriteService)
				.replaceRelGoods(eq(GROUPS_ACTIVITY_ID), eq(COMPANY_ID), anyList(), anyInt());

		PromotionGroupsRelGoodsReadService relGoodsReadService = mock(PromotionGroupsRelGoodsReadService.class);
		when(relGoodsReadService.listByActivityId(eq(COMPANY_ID), eq(GROUPS_ACTIVITY_ID))).thenReturn(List.of());

		PromotionGroupsActivityCreateService svc =
				new PromotionGroupsActivityCreateService(
						messageSource,
						guard,
						catalogAccess,
						activityMapper,
						new PromotionGroupsActivityAdminRowAssembler(relGoodsReadService),
						multiLang,
						applicationEventPublisher,
						validationService,
						tagPublisher,
						postCommitJobs,
						relGoodsWriteService);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});

		Map<String, Object> params = baselineCreateParams();
		int begin = (int) params.get("_begin_epoch");
		int end = (int) params.get("_end_epoch");
		params.remove("_begin_epoch");
		params.remove("_end_epoch");

		TransactionTemplate tt = new TransactionTemplate(txMgr);
		tt.executeWithoutResult(
				st -> {
					Map<String, Object> row = svc.createPromotionGroupsActivity(params, "zh-CN");
					assertNotNull(row);
				});

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
		PromotionGroupActivityCommittedEvent ev = eventCaptor.getValue();
		assertNotNull(ev);
		verifyNoInteractions(postCommitJobs);
	}

	private static Map<String, Object> baselineCreateParams() {
		int begin = (int) (System.currentTimeMillis() / 1000L + 86_400);
		int end = begin + 86_400;
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("_begin_epoch", begin);
		params.put("_end_epoch", end);
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
