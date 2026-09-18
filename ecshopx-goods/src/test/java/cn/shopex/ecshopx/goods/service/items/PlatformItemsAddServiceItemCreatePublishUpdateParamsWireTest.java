package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.dispatch.ItemAddEventDispatchPublisher;
import cn.shopex.ecshopx.goods.dispatch.ItemCreateEventDispatchPublisher;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelPointAccessRepository;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.espier.service.upload.IntroHtmlDataImageUploadService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class PlatformItemsAddServiceItemCreatePublishUpdateParamsWireTest {

	@Mock
	private IntroHtmlDataImageUploadService introHtmlDataImageUploadService;

	@Mock
	private PointMemberRuleReadService pointMemberRuleReadService;

	@Mock
	private MerchantQueryService merchantQueryService;

	@Mock
	private DistributorListQueryService distributorListQueryService;

	@Mock
	private ItemsMedicineService itemsMedicineService;

	@Mock
	private ItemProcessUpdateItemService itemProcessUpdateItemService;

	@Mock
	private ItemSpecParamsResolver itemSpecParamsResolver;

	@Mock
	private NormalItemTypeHandler normalItemTypeHandler;

	@Mock
	private ServicesItemTypeHandler servicesItemTypeHandler;

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private ItemsBarcodeRepository itemsBarcodeRepository;

	@Mock
	private ItemStoreService itemStoreService;

	@Mock
	private ItemRelAttributesRepository itemRelAttributesRepository;

	@Mock
	private ItemsRelPointAccessRepository itemsRelPointAccessRepository;

	@Mock
	private ItemCreatePromotionGuardService itemCreatePromotionGuardService;

	@Mock
	private ItemsRelCatsWriteService itemsRelCatsWriteService;

	@Mock
	private ItemAddEventDispatchPublisher itemAddEventDispatchPublisher;

	@Mock
	private ItemCreateEventDispatchPublisher itemCreateEventDispatchPublisher;

	@Mock
	private ItemsMultiLangWriteService itemsMultiLangWriteService;

	private PlatformItemsAddService sut;

	@BeforeEach
	void setUp() {
		sut = new PlatformItemsAddService(
				introHtmlDataImageUploadService,
				pointMemberRuleReadService,
				merchantQueryService,
				distributorListQueryService,
				itemsMedicineService,
				itemProcessUpdateItemService,
				itemSpecParamsResolver,
				normalItemTypeHandler,
				servicesItemTypeHandler,
				itemsRepository,
				itemsBarcodeRepository,
				itemStoreService,
				itemRelAttributesRepository,
				itemsRelPointAccessRepository,
				itemCreatePromotionGuardService,
				itemsRelCatsWriteService,
				itemAddEventDispatchPublisher,
				itemCreateEventDispatchPublisher,
				itemsMultiLangWriteService,
				new ObjectMapper());

		when(itemsMultiLangWriteService.resolveLang(any())).thenReturn("zh-CN");
		lenient().doNothing().when(itemsMultiLangWriteService).afterItemCreate(anyLong(), anyLong(), any(), any());
		lenient().doNothing().when(itemsMultiLangWriteService).afterItemUpdate(anyLong(), anyLong(), any(), any());
		doNothing().when(itemsMedicineService).assertMedicineSettingsAndEnrichParams(anyLong(), any(), any());
		when(pointMemberRuleReadService.getPointRule(anyLong())).thenReturn(Map.of("access", "order"));
		doAnswer(invocation -> {
			Map<String, Object> work = invocation.getArgument(0);
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
				.when(itemSpecParamsResolver)
				.resolve(any(), any(), any());
		doNothing().when(normalItemTypeHandler).preRelItemParams(any(), any(), any());
		when(normalItemTypeHandler.createRelItem(any(), any(), any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().doNothing().when(itemsRepository).updateByItemId(anyLong(), anyLong(), any());
		doNothing().when(itemsBarcodeRepository).saveBarcode(anyLong(), anyLong(), anyLong(), anyLong(), any());
		doNothing().when(itemStoreService).saveItemStore(anyLong(), anyInt(), anyLong());
		doNothing().when(itemsRepository).updateByItemIds(anyLong(), any(), anyLong(), anyLong());
		doNothing().when(itemsRepository).clearDefaultFlagExcept(anyLong(), anyLong(), anyLong());
		doNothing().when(itemsRepository).setDefaultItem(anyLong(), anyLong(), anyBoolean());
		doNothing().when(itemCreatePromotionGuardService).checkItemPrice(anyLong(), any(), any());
		doNothing().when(itemAddEventDispatchPublisher).schedulePublishAfterCommit(anyLong(), anyLong());
	}

	@Test
	void addItemsTransactional_withUpdateStyleParams_invokesItemCreatePublisherOnce() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 7001L);
		params.put("operator_type", "admin");
		params.put("item_type", "normal");
		params.put("goods_id", 9_999L);
		params.put("item_id", 55L);
		params.put("nospec", "false");
		params.put("item_name", "Wire test goods");
		params.put("item_main_cat_id", 77L);
		params.put("brand_id", 3);
		params.put("recommend_items", List.of());
		params.put("isCreateRelData", Boolean.FALSE);

		List<Map<String, Object>> specItems = new ArrayList<>();
		Map<String, Object> rowA = new LinkedHashMap<>();
		rowA.put("item_id", 200L);
		rowA.put("price", "10");
		rowA.put("approve_status", "onsale");
		rowA.put("item_bn", "SKU200");
		rowA.put("goods_bn", "G001");
		rowA.put("barcode", "B200");
		rowA.put("weight", "1");
		rowA.put("store", 10);
		specItems.add(rowA);
		Map<String, Object> rowB = new LinkedHashMap<>();
		rowB.put("item_id", 201L);
		rowB.put("price", "12");
		rowB.put("approve_status", "onsale");
		rowB.put("item_bn", "SKU201");
		rowB.put("goods_bn", "G001");
		rowB.put("barcode", "B201");
		rowB.put("weight", "1");
		rowB.put("store", 10);
		specItems.add(rowB);
		params.put("spec_items", specItems);

		sut.addItemsTransactional(params);

		verify(itemCreateEventDispatchPublisher)
				.publish(
						argThat(entities -> {
							assertEquals(7001L, entities.get("company_id"));
							assertEquals(201L, ((Number) entities.get("item_id")).longValue());
							assertEquals(77L, ((Number) entities.get("item_main_cat_id")).longValue());
							return true;
						}),
						argThat(ids -> ids.size() == 2 && ids.contains(200L) && ids.contains(201L)));

		verify(itemAddEventDispatchPublisher).schedulePublishAfterCommit(200L, 7001L);
		verify(itemAddEventDispatchPublisher).schedulePublishAfterCommit(201L, 7001L);
	}

	@Test
	void addItemsTransactional_withSingleSku_nospecDefault_schedulesItemAddPublishOnce() {
		final long assignedItemId = 301L;
		final long companyId = 7001L;
		doAnswer(invocation -> {
			Items entity = invocation.getArgument(0);
			entity.setItemId(assignedItemId);
			entity.setGoodsId(assignedItemId);
			return null;
		})
				.when(itemsRepository)
				.insert(any(Items.class));

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("operator_type", "admin");
		params.put("item_type", "normal");
		params.put("item_name", "Wire single-sku goods");
		params.put("item_main_cat_id", 77L);
		params.put("brand_id", 3);
		params.put("price", "10");
		params.put("approve_status", "onsale");
		params.put("item_bn", "SKU301");
		params.put("goods_bn", "G301");
		params.put("barcode", "B301");
		params.put("weight", "1");
		params.put("store", 10);
		params.put("recommend_items", List.of());
		params.put("isCreateRelData", Boolean.FALSE);

		sut.addItemsTransactional(params);

		verify(itemAddEventDispatchPublisher, times(1)).schedulePublishAfterCommit(assignedItemId, companyId);

		verify(itemCreateEventDispatchPublisher, times(1))
				.publish(
						argThat(entities -> {
							assertEquals(companyId, entities.get("company_id"));
							assertEquals(assignedItemId, ((Number) entities.get("item_id")).longValue());
							assertEquals(77L, ((Number) entities.get("item_main_cat_id")).longValue());
							return true;
						}),
						argThat(ids -> ids.size() == 1 && ids.contains(assignedItemId)));
	}

	@Test
	void addItemsTransactional_withUpdateStyleParams_singleSku_nospec_schedulesItemAddPublishOnce() {
		final long existingItemId = 55L;
		final long companyId = 7001L;

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("operator_type", "admin");
		params.put("item_type", "normal");
		params.put("goods_id", 9_999L);
		params.put("item_id", existingItemId);
		params.put("nospec", Boolean.TRUE);
		params.put("item_name", "Wire update-style single sku");
		params.put("item_main_cat_id", 77L);
		params.put("brand_id", 3);
		params.put("price", "10");
		params.put("approve_status", "onsale");
		params.put("item_bn", "SKU302");
		params.put("goods_bn", "G302");
		params.put("barcode", "B302");
		params.put("weight", "1");
		params.put("store", 10);
		params.put("recommend_items", List.of());
		params.put("isCreateRelData", Boolean.FALSE);

		sut.addItemsTransactional(params);

		verify(itemAddEventDispatchPublisher, times(1)).schedulePublishAfterCommit(existingItemId, companyId);
		verify(itemsBarcodeRepository).saveBarcode(eq(companyId), eq(0L), eq(existingItemId), anyLong(), eq("B302"));

		verify(itemCreateEventDispatchPublisher, times(1))
				.publish(
						argThat(entities -> {
							assertEquals(companyId, entities.get("company_id"));
							assertEquals(existingItemId, ((Number) entities.get("item_id")).longValue());
							assertEquals(77L, ((Number) entities.get("item_main_cat_id")).longValue());
							return true;
						}),
						argThat(ids -> ids.size() == 1 && ids.contains(existingItemId)));
	}

	@Test
	void addItemsTransactional_update_singleSku_keepsCommonParamsAuditStatus() {
		final long itemId = 6784L;
		final long companyId = 38L;
		Items existing = new Items();
		existing.setItemId(itemId);
		existing.setGoodsId(itemId);
		existing.setAuditStatus("approved");
		when(itemsRepository.getByItemIdAndCompany(itemId, companyId)).thenReturn(existing);

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("operator_type", "admin");
		params.put("item_type", "normal");
		params.put("item_id", itemId);
		params.put("item_name", "Audit status wire test");
		params.put("sort", 1);
		params.put("pics", List.of("https://example.com/pic.jpg"));
		params.put("templates_id", 346);
		params.put("brand_id", 960);
		params.put("item_category", List.of(6530L));
		params.put("item_main_cat_id", 6529L);
		params.put("price", "521");
		params.put("approve_status", "onsale");
		params.put("item_bn", "KC26011700000031");
		params.put("goods_bn", "PC26011700000031");
		params.put("barcode", "");
		params.put("weight", "0");
		params.put("store", 700);
		params.put("audit_status", "processing");
		params.put("recommend_items", List.of());

		sut.addItemsTransactional(params);

		ArgumentCaptor<Items> captor = ArgumentCaptor.forClass(Items.class);
		verify(itemsRepository).updateByItemId(eq(itemId), eq(companyId), captor.capture());
		assertEquals("approved", captor.getValue().getAuditStatus());
	}
}
