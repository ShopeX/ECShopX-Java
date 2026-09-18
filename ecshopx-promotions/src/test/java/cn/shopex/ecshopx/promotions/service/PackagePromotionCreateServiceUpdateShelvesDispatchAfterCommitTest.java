package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PackageItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackageMainItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.mapper.PackageItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackageMainItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class PackagePromotionCreateServiceUpdateShelvesDispatchAfterCommitTest {

	private static final long COMPANY_ID = 1L;
	private static final long PACKAGE_ID = 50L;
	private static final long MAIN_ITEM_ID = 100L;
	private static final long CHILD_ITEM_ID = 200L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PackagePromotions.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PackageMainItemPromotions.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PackageItemPromotions.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void update_whenTransactionCommits_invokesSalespersonItemsShelvesJobPublishOnceAfterCommit() {
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("err");

		MarketingActivityCrossPromotionGuardService guard = mock(MarketingActivityCrossPromotionGuardService.class);
		doNothing().when(guard).checkActivityValidByPackage(any(), anyLong());

		MarketingActivityCatalogAccess catalog = mock(MarketingActivityCatalogAccess.class);
		Map<String, Object> mainSkuPack = mainSkuPack();
		Map<String, Object> childSkuPack = childSkuPack();
		when(catalog.loadSkuItemsList(eq(COMPANY_ID), any()))
				.thenAnswer(
						invocation -> {
							@SuppressWarnings("unchecked")
							List<Long> ids = (List<Long>) invocation.getArgument(1);
							if (ids.size() == 1 && ids.get(0).equals(MAIN_ITEM_ID)) {
								return mainSkuPack;
							}
							if (ids.size() == 1 && ids.get(0).equals(CHILD_ITEM_ID)) {
								return childSkuPack;
							}
							return Map.of("total_count", 0, "list", List.of());
						});
		List<Map<String, Object>> typeRows = new ArrayList<>();
		Map<String, Object> t1 = new LinkedHashMap<>();
		t1.put("type", "retail");
		Map<String, Object> t2 = new LinkedHashMap<>();
		t2.put("type", "retail");
		typeRows.add(t1);
		typeRows.add(t2);
		when(catalog.loadItemIdAndTypeRows(eq(COMPANY_ID), any())).thenReturn(typeRows);

		PackagePromotionsMapper packagePromotionsMapper = mock(PackagePromotionsMapper.class);
		when(packagePromotionsMapper.update(any(), any())).thenReturn(1);
		when(packagePromotionsMapper.updateById(any(PackagePromotions.class))).thenReturn(1);
		PackagePromotions persisted = new PackagePromotions();
		persisted.setPackageId(PACKAGE_ID);
		persisted.setCompanyId(COMPANY_ID);
		when(packagePromotionsMapper.selectById(PACKAGE_ID)).thenReturn(persisted);

		PackageMainItemPromotionsMapper mainRelMapper = mock(PackageMainItemPromotionsMapper.class);
		when(mainRelMapper.delete(any())).thenReturn(0);
		when(mainRelMapper.insert(isA(PackageMainItemPromotions.class))).thenReturn(1);

		PackageItemPromotionsMapper itemMapper = mock(PackageItemPromotionsMapper.class);
		when(itemMapper.delete(any())).thenReturn(0);
		when(itemMapper.insert(isA(PackageItemPromotions.class))).thenReturn(1);

		SalespersonItemsShelvesJobDispatchPublisher shelvesPublisher =
				mock(SalespersonItemsShelvesJobDispatchPublisher.class);

		PackagePromotionCreateService svc =
				new PackagePromotionCreateService(
						messageSource,
						guard,
						catalog,
						packagePromotionsMapper,
						mainRelMapper,
						itemMapper,
						shelvesPublisher,
						new ObjectMapper());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any(TransactionStatus.class));

		TransactionTemplate tt = new TransactionTemplate(txMgr);
		Map<String, Object> params = baselineUpdateParams();

		tt.executeWithoutResult(
				st -> {
					Map<String, Object> row =
							svc.update(Long.toString(PACKAGE_ID), params, "zh-CN");
					assertNotNull(row);
					verifyNoInteractions(shelvesPublisher);
				});

		verify(shelvesPublisher).publish(eq(COMPANY_ID), eq(PACKAGE_ID), eq("package"));
		verifyNoMoreInteractions(shelvesPublisher);
	}

	private static Map<String, Object> baselineUpdateParams() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("package_name", "pkg-u1");

		Map<String, Object> mainItem = new LinkedHashMap<>();
		mainItem.put("item_id", MAIN_ITEM_ID);
		mainItem.put("item_price", "10.00");
		params.put("main_item", mainItem);

		List<Map<String, Object>> mainItems = new ArrayList<>();
		Map<String, Object> mi = new LinkedHashMap<>();
		mi.put("item_id", MAIN_ITEM_ID);
		mi.put("item_price", "10.00");
		mainItems.add(mi);
		params.put("main_items", mainItems);

		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> it = new LinkedHashMap<>();
		it.put("item_id", CHILD_ITEM_ID);
		it.put("new_price", "5.00");
		items.add(it);
		params.put("items", items);

		params.put("valid_grade", List.of(1));
		params.put("used_platform", 1);
		params.put("start_time", 1_000);
		params.put("end_time", 2_000);
		return params;
	}

	private static Map<String, Object> mainSkuPack() {
		Map<String, Object> pack = new LinkedHashMap<>();
		pack.put("total_count", 1);
		List<Map<String, Object>> list = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", MAIN_ITEM_ID);
		row.put("goods_id", 10L);
		list.add(row);
		pack.put("list", list);
		return pack;
	}

	private static Map<String, Object> childSkuPack() {
		Map<String, Object> pack = new LinkedHashMap<>();
		List<Map<String, Object>> list = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", CHILD_ITEM_ID);
		row.put("goods_id", 20L);
		row.put("item_name", "child-sku");
		row.put("pics", List.of());
		row.put("price", 1000);
		row.put("special_type", "");
		list.add(row);
		pack.put("list", list);
		return pack;
	}
}
