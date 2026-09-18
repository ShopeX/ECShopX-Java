package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.config.ImportDataJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.ItemAddEventDispatchPublisherImpl;
import cn.shopex.ecshopx.espier.dispatch.ImportDataJobHandler;
import cn.shopex.ecshopx.espier.service.UploadeFileRepository;
import cn.shopex.ecshopx.espier.service.UploadFileImportDataService;
import cn.shopex.ecshopx.espier.service.upload.AbstractEspierTableUploadHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadHeaderCatalog;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadRowContext;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import cn.shopex.ecshopx.goods.dispatch.ItemAddEventDispatchPublisher;
import cn.shopex.ecshopx.goods.dispatch.ItemCreateEventDispatchPublisher;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelPointAccessRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemProcessUpdateItemService;
import cn.shopex.ecshopx.goods.service.items.ItemSpecParamsResolver;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.goods.service.items.ItemsMultiLangWriteService;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.goods.service.items.ItemsRelCatsWriteService;
import cn.shopex.ecshopx.goods.service.items.NormalItemTypeHandler;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsAddService;
import cn.shopex.ecshopx.goods.service.items.ServicesItemTypeHandler;
import cn.shopex.ecshopx.espier.service.upload.IntroHtmlDataImageUploadService;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.merchant.service.MerchantQueryService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Probes async {@link EspierDispatchJobNames#IMPORT_DATA_JOB} dispatch, in-memory REDIS capture, and
 * {@link DispatchConsumerRuntime#consume} driving {@link ImportDataJobHandler} into a table-import row path that
 * performs a platform item create and schedules {@link ItemAddEventDispatchPublisher#schedulePublishAfterCommit}.
 */
class ImportDataJobHandlerItemAddPublishProbeTest {

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void dispatchImportDataJob_async_thenConsume_runsHandler_andItemAddPublisherScheduledAfterCommit() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_ADD,
				"listener:youshu.items_item_add",
				ListenerDispatchOptions.async("default", null),
				mock(DispatchListener.class));

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		ItemAddEventDispatchPublisherImpl itemPublisherBean = new ItemAddEventDispatchPublisherImpl(facade);
		ItemAddEventDispatchPublisher itemAddPublisherSpy = spy(itemPublisherBean);
		doAnswer(
				(InvocationOnMock inv) -> {
					inv.callRealMethod();
					assertEquals(
							1,
							captured.size(),
							"ITEM_ADD must not be enqueued before commit (expect afterCommit path, not immediate publish)");
					return null;
				})
				.when(itemAddPublisherSpy)
				.schedulePublishAfterCommit(anyLong(), anyLong());

		TransactionTemplate transactionTemplate = newAfterCommitMirroringTransactionTemplate();

		PlatformItemsAddService platformBase = newPlatformItemsAddService(itemAddPublisherSpy);
		@SuppressWarnings("unchecked")
		PlatformItemsAddService platformSpy = spy(platformBase);

		NormalGoodsRowCreatesPlatformItemHandler tableHandler =
				new NormalGoodsRowCreatesPlatformItemHandler(platformSpy, transactionTemplate);
		EspierUploadFileHandlerRegistry fileRegistry = new EspierUploadFileHandlerRegistry(List.of(tableHandler));
		fileRegistry.init();

		UploadeFileRepository uploadRepo = mock(UploadeFileRepository.class);
		LinkedHashMap<String, Object> uploadMeta = new LinkedHashMap<>();
		uploadMeta.put("left_job_num", 1);
		uploadMeta.put("handle_message", null);
		when(uploadRepo.getInfoById(4401L)).thenReturn(uploadMeta);
		when(uploadRepo.updateOneBy(any(), any())).thenReturn(new LinkedHashMap<>());

		FileStorageService fileStorage = mock(FileStorageService.class);
		org.springframework.transaction.PlatformTransactionManager txManager =
				mock(org.springframework.transaction.PlatformTransactionManager.class);
		when(txManager.getTransaction(any()))
				.thenReturn(mock(org.springframework.transaction.TransactionStatus.class));
		UploadFileImportDataService uploadImport =
				new UploadFileImportDataService(uploadRepo, new ObjectMapper(), fileRegistry, fileStorage, txManager);
		ImportDataJobHandler jobHandler = new ImportDataJobHandler(uploadImport);
		registry.registerJob(EspierDispatchJobNames.IMPORT_DATA_JOB, jobHandler);

		ImportDataJobDispatchPublisherImpl jobPublisher = new ImportDataJobDispatchPublisherImpl(facade);

		LinkedHashMap<String, Object> uploadInfo = new LinkedHashMap<>();
		uploadInfo.put("id", 4401L);
		uploadInfo.put("company_id", 8801L);
		uploadInfo.put("file_type", "normal_goods");
		uploadInfo.put("storage_path", "");

		LinkedHashMap<Integer, String> column = new LinkedHashMap<>();
		column.put(0, "item_name");
		column.put(1, "price");
		column.put(2, "item_bn");
		column.put(3, "goods_bn");
		column.put(4, "barcode");
		column.put(5, "weight");
		column.put(6, "store");

		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("item_name", "Async import probe article");
		row.put("price", "19.90");
		row.put("item_bn", "PROBE-BN-1990");
		row.put("goods_bn", "PROBE-G-1990");
		row.put("barcode", "BC-1990");
		row.put("weight", "1.2");
		row.put("store", 30);

		jobPublisher.dispatchImportDataChunk(false, uploadInfo, List.of(row), column, 1, List.of("A", "B"));

		assertEquals(1, captured.size());
		DispatchMessage jobMsg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, jobMsg.messageType());
		assertEquals("slow", jobMsg.queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(jobMsg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> addParamsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(platformSpy, times(1)).addItemsTransactional(addParamsCaptor.capture());
		Map<String, Object> passed = addParamsCaptor.getValue();
		assertEquals(8801L, ((Number) passed.get("company_id")).longValue());
		assertEquals("normal", passed.get("item_type").toString());

		ArgumentCaptor<Long> itemIdCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> companyIdCaptor = ArgumentCaptor.forClass(Long.class);
		verify(itemAddPublisherSpy, times(1)).schedulePublishAfterCommit(itemIdCaptor.capture(), companyIdCaptor.capture());
		assertEquals(55_001L, itemIdCaptor.getValue().longValue());
		assertEquals(8801L, companyIdCaptor.getValue().longValue());

		assertEquals(2, captured.size());
		DispatchMessage fanOut = captured.get(1);
		assertEquals(DispatchMessageType.EVENT, fanOut.messageType());
		assertEquals(GoodsDispatchEventNames.EVENT_ITEM_ADD, fanOut.messageName());
		assertEquals("listener:youshu.items_item_add", fanOut.listenerName());
		assertEquals("default", fanOut.queue());
		assertNull(fanOut.delay());
		assertEquals(55_001L, fanOut.payload().get("item_id"));
		assertEquals(8801L, fanOut.payload().get("company_id"));
	}

	private static PlatformItemsAddService newPlatformItemsAddService(ItemAddEventDispatchPublisher itemAddPublisher) {
		IntroHtmlDataImageUploadService introHtml = mock(IntroHtmlDataImageUploadService.class);
		when(introHtml.replaceDataImageUrlsInIntro(any(), anyLong())).thenAnswer(inv -> inv.getArgument(0));

		PointMemberRuleReadService pointRule = mock(PointMemberRuleReadService.class);
		when(pointRule.getPointRule(anyLong())).thenReturn(Map.of("access", "order"));

		MerchantQueryService merchantQuery = mock(MerchantQueryService.class);
		DistributorListQueryService distributorListQuery = mock(DistributorListQueryService.class);
		ItemsMedicineService itemsMedicine = mock(ItemsMedicineService.class);
		doNothing().when(itemsMedicine).assertMedicineSettingsAndEnrichParams(anyLong(), any(), any());

		ItemProcessUpdateItemService itemProcessUpdate = mock(ItemProcessUpdateItemService.class);
		ItemSpecParamsResolver specResolver = mock(ItemSpecParamsResolver.class);
		doAnswer(
				invocation -> {
					@SuppressWarnings("unchecked")
					Map<String, Object> work = invocation.getArgument(0);
					@SuppressWarnings("unchecked")
					Map<String, Object> skuParams = invocation.getArgument(1);
					work.put("approve_status", skuParams.get("approve_status").toString());
					work.put(
							"price",
							new BigDecimal(skuParams.get("price").toString())
									.multiply(BigDecimal.valueOf(100))
									.setScale(0, RoundingMode.HALF_UP)
									.intValue());
					work.put("item_bn", skuParams.get("item_bn").toString());
					work.put("goods_bn", skuParams.get("goods_bn").toString());
					work.put("barcode", skuParams.get("barcode").toString());
					work.put("weight", Double.parseDouble(skuParams.get("weight").toString()));
					work.put("cost_price", 0);
					work.put("market_price", 0);
					work.put("profit_fee", 0);
					work.put("item_unit", "个");
					work.put("is_default", Boolean.TRUE);
					work.put("point", 0);
					return null;
				})
				.when(specResolver)
				.resolve(any(), any(), any());

		NormalItemTypeHandler normalHandler = mock(NormalItemTypeHandler.class);
		ServicesItemTypeHandler servicesHandler = mock(ServicesItemTypeHandler.class);
		doNothing().when(normalHandler).preRelItemParams(any(), any(), any());
		when(normalHandler.createRelItem(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		ItemsRepository itemsRepository = mock(ItemsRepository.class);
		doAnswer(
				invocation -> {
					Items it = invocation.getArgument(0);
					it.setItemId(55_001L);
					it.setGoodsId(0L);
					it.setDefaultItemId(null);
					it.setItemCategory("77");
					it.setBrandId(3);
					return null;
				})
				.when(itemsRepository)
				.insert(any(Items.class));
		when(itemsRepository.getByItemIdAndCompany(anyLong(), anyLong())).thenReturn(null);
		doNothing().when(itemsRepository).updateByItemIds(anyLong(), any(), anyLong(), anyLong());
		doNothing().when(itemsRepository).clearDefaultFlagExcept(anyLong(), anyLong(), anyLong());
		doNothing().when(itemsRepository).setDefaultItem(anyLong(), anyLong(), anyBoolean());

		ItemsBarcodeRepository barcodeRepo = mock(ItemsBarcodeRepository.class);
		doNothing().when(barcodeRepo).saveBarcode(anyLong(), anyLong(), anyLong(), anyLong(), any());

		ItemStoreService itemStore = mock(ItemStoreService.class);
		doNothing().when(itemStore).saveItemStore(anyLong(), anyInt(), anyLong());

		ItemRelAttributesRepository relAttrRepo = mock(ItemRelAttributesRepository.class);
		doNothing().when(relAttrRepo).insert(any(ItemRelAttributes.class));

		ItemsRelPointAccessRepository pointAccessRepo = mock(ItemsRelPointAccessRepository.class);
		ItemCreatePromotionGuardService promoGuard = mock(ItemCreatePromotionGuardService.class);
		doNothing().when(promoGuard).checkItemPrice(anyLong(), any(), any());

		ItemsRelCatsWriteService relCats = mock(ItemsRelCatsWriteService.class);

		ItemCreateEventDispatchPublisher itemCreatePublisher = mock(ItemCreateEventDispatchPublisher.class);
		doNothing().when(itemCreatePublisher).publish(any(), any());

		ItemsMultiLangWriteService itemsMultiLangWriteService = mock(ItemsMultiLangWriteService.class);
		when(itemsMultiLangWriteService.resolveLang(any())).thenReturn("zh-CN");
		doNothing().when(itemsMultiLangWriteService).afterItemCreate(anyLong(), anyLong(), any(), any());
		doNothing().when(itemsMultiLangWriteService).afterItemUpdate(anyLong(), anyLong(), any(), any());

		return new PlatformItemsAddService(
				introHtml,
				pointRule,
				merchantQuery,
				distributorListQuery,
				itemsMedicine,
				itemProcessUpdate,
				specResolver,
				normalHandler,
				servicesHandler,
				itemsRepository,
				barcodeRepo,
				itemStore,
				relAttrRepo,
				pointAccessRepo,
				promoGuard,
				relCats,
				itemAddPublisher,
				itemCreatePublisher,
				itemsMultiLangWriteService,
				new ObjectMapper());
	}

	/**
	 * Same pattern as {@link cn.shopex.ecshopx.config.ItemAddEventDispatchPublisherImplAfterCommitTest}: mock
	 * {@link PlatformTransactionManager} initializes transaction synchronization and runs {@code afterCommit()}
	 * callbacks when {@code commit} is invoked, so {@code schedulePublishAfterCommit} exercises the deferred path.
	 */
	private static TransactionTemplate newAfterCommitMirroringTransactionTemplate() {
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
		return new TransactionTemplate(txMgr);
	}

	/**
	 * Test {@link cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler} that maps one import row to a
	 * minimal platform create payload (narrow slice for dispatch probe).
	 */
	private static final class NormalGoodsRowCreatesPlatformItemHandler extends AbstractEspierTableUploadHandler {

		private final PlatformItemsAddService platformItemsAddService;
		private final TransactionTemplate transactionTemplate;

		NormalGoodsRowCreatesPlatformItemHandler(
				PlatformItemsAddService platformItemsAddService, TransactionTemplate transactionTemplate) {
			super(EspierUploadHeaderCatalog.NORMAL_GOODS());
			this.platformItemsAddService = platformItemsAddService;
			this.transactionTemplate = transactionTemplate;
		}

		@Override
		public String supportedFileType() {
			return "normal_goods";
		}

		@Override
		protected void handleBusinessRow(EspierUploadRowContext ctx, Map<String, Object> row) {
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("company_id", ctx.companyId());
			params.put("operator_type", "admin");
			params.put("item_type", "normal");
			params.put("item_name", String.valueOf(row.getOrDefault("item_name", "probe")));
			params.put("item_main_cat_id", 77L);
			params.put("item_category", List.of(77L));
			params.put("brand_id", 3);
			params.put("recommend_items", List.of());
			params.put("isCreateRelData", Boolean.FALSE);
			params.put("nospec", "true");
			params.put("approve_status", "onsale");
			params.put("price", row.get("price"));
			params.put("item_bn", row.get("item_bn"));
			params.put("goods_bn", row.get("goods_bn"));
			params.put("barcode", row.get("barcode"));
			params.put("weight", row.get("weight"));
			params.put("store", row.get("store"));
			transactionTemplate.executeWithoutResult(st -> platformItemsAddService.addItemsTransactional(params));
		}
	}
}
