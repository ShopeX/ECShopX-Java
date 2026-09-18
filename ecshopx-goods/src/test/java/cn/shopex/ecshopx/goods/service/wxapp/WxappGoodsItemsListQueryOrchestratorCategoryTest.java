package cn.shopex.ecshopx.goods.service.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListFacadeService;
import cn.shopex.ecshopx.merchant.service.MerchantDisabledDistributorIdsQueryService;
import cn.shopex.ecshopx.salesperson.service.WxappItemsListSalesmanDistributorResolveService;
import cn.shopex.ecshopx.salesperson.service.WxappItemsListSalesmanGateResult;
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
class WxappGoodsItemsListQueryOrchestratorCategoryTest {

	@Mock
	private MerchantDisabledDistributorIdsQueryService merchantDisabledDistributorIdsQueryService;
	@Mock
	private WxappItemsListSalesmanDistributorResolveService wxappItemsListSalesmanDistributorResolveService;
	@Mock
	private GoodsItemsListFacadeService goodsItemsListFacadeService;
	@Mock
	private ItemsRelTagsRepository itemsRelTagsRepository;
	@Mock
	private ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	@Mock
	private DistributorListQueryService distributorListQueryService;

	private WxappGoodsItemsListQueryOrchestrator sut;

	@BeforeEach
	void setUp() {
		sut = new WxappGoodsItemsListQueryOrchestrator(merchantDisabledDistributorIdsQueryService,
				wxappItemsListSalesmanDistributorResolveService, goodsItemsListFacadeService, itemsRelTagsRepository,
				itemsCategoryDistributorIdResolver, distributorListQueryService);
		when(merchantDisabledDistributorIdsQueryService.listDistributorIdsLinkedToDisabledMerchants(anyLong()))
				.thenReturn(List.of());
		when(wxappItemsListSalesmanDistributorResolveService.resolve(anyLong(), anyLong(), any()))
				.thenReturn(WxappItemsListSalesmanGateResult.proceed(Map.of()));
		when(goodsItemsListFacadeService.wxappQueryDefaultItemList(anyLong(), anyMap(), anyInt(), anyInt(), anyString(),
				anyString())).thenReturn(Map.of("list", List.of(), "total_count", 0L));
	}

	@Test
	void queryItemListData_categoryIdWithoutResolvedHits_forcesNoMatchItemIdFilter() {
		LinkedHashMap<String, Object> params = baseParams();
		params.put("category_id", "909");

		sut.queryItemListData(1L, params, List.of());

		assertEquals(List.of(-1L), capturedItemIdOrDefaultIds());
	}

	@Test
	void queryItemListData_emptyCategoryResolvedHits_forcesNoMatchItemIdFilter() {
		LinkedHashMap<String, Object> params = baseParams();
		params.put("category_id", "909");
		params.put("category_resolved_item_ids", List.of());

		sut.queryItemListData(1L, params, List.of());

		assertEquals(List.of(-1L), capturedItemIdOrDefaultIds());
	}

	private static LinkedHashMap<String, Object> baseParams() {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("user_id", 0L);
		params.put("approve_status", List.of("onsale", "only_show"));
		params.put("audit_status", "approved");
		params.put("is_default", Boolean.TRUE);
		params.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE, 1);
		params.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE_SIZE, 10);
		params.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_ACCEPT_LANGUAGE, "zh-CN");
		return params;
	}

	private List<?> capturedItemIdOrDefaultIds() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> repoCaptor = ArgumentCaptor.forClass(Map.class);
		verify(goodsItemsListFacadeService).wxappQueryDefaultItemList(eq(1L), repoCaptor.capture(), eq(1), eq(10),
				anyString(), eq("zh-CN"));
		return (List<?>) repoCaptor.getValue().get(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS);
	}
}
