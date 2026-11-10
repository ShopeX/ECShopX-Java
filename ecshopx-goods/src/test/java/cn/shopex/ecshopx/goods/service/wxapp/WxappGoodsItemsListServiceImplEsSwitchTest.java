/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.goods.service.wxapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.distributor.DistributorCouponListAppendService;
import cn.shopex.ecshopx.goods.repository.ItemCrossBorderTaxQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import cn.shopex.ecshopx.goods.search.GoodsSearchFacade;
import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import cn.shopex.ecshopx.goods.search.price.SearchMemberTier;
import cn.shopex.ecshopx.goods.search.price.SearchMemberTierResolver;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.cart.salesperson.SalespersonCartDistributorSkuReplaceService;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import cn.shopex.ecshopx.goods.service.pointsmall.PointsmallFrontBrandListService;
import cn.shopex.ecshopx.goods.service.popularize.CompanyPopularizeConfigReadService;
import cn.shopex.ecshopx.members.mapper.MemberItemsFavMapper;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.popularize.service.PromoterOnsaleGoodsIdsQueryService;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import cn.shopex.ecshopx.salesperson.service.WxappItemsListSalesmanGateResult;
import cn.shopex.ecshopx.tdkset.service.TdkGivenSaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappGoodsItemsListServiceImplEsSwitchTest {

	@Mock private WxappGoodsItemsKeywordSpecialCharChecker wxappGoodsItemsKeywordSpecialCharChecker;
	@Mock private WxappGoodsItemsListParamBuilder wxappGoodsItemsListParamBuilder;
	@Mock private PromoterOnsaleGoodsIdsQueryService promoterOnsaleGoodsIdsQueryService;
	@Mock private WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	@Mock private CompanyPopularizeConfigReadService companyPopularizeConfigReadService;
	@Mock private WxappGoodsItemsListPromoterPriceReplaceService wxappGoodsItemsListPromoterPriceReplaceService;
	@Mock private WxappPromoterPointConvertService wxappPromoterPointConvertService;
	@Mock private WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	@Mock private GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	@Mock private MemberItemsFavMapper memberItemsFavMapper;
	@Mock private CartMapper cartMapper;
	@Mock private ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository;
	@Mock private ItemsRepository itemsRepository;
	@Mock private CompanyDefaultCurrencyService companyDefaultCurrencyService;
	@Mock private TdkGivenSaveService tdkGivenSaveService;
	@Mock private PointsmallFrontBrandListService pointsmallFrontBrandListService;
	@Mock private ItemsRelTagsRepository itemsRelTagsRepository;
	@Mock private ItemsTagsRepository itemsTagsRepository;
	@Mock private DistributorCouponListAppendService distributorCouponListAppendService;
	@Mock private DistributorListQueryService distributorListQueryService;
	@Mock private ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	@Mock private SalespersonCartDistributorSkuReplaceService salespersonCartDistributorSkuReplaceService;
	@Mock private LangueProperties langueProperties;
	@Mock private ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;
	@Mock private SearchEsProperties searchEsProperties;
	@Mock private GoodsSearchFacade goodsSearchFacade;
	@Mock private SearchMemberTierResolver searchMemberTierResolver;
	@Mock private HttpServletRequest request;

	private WxappGoodsItemsListServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new WxappGoodsItemsListServiceImpl(
				wxappGoodsItemsKeywordSpecialCharChecker,
				wxappGoodsItemsListParamBuilder,
				promoterOnsaleGoodsIdsQueryService,
				wxappGoodsItemsListQueryOrchestrator,
				companyPopularizeConfigReadService,
				wxappGoodsItemsListPromoterPriceReplaceService,
				wxappPromoterPointConvertService,
				wxappGoodsItemsListMemberPriceApplyService,
				goodsItemsListPromotionEnrichmentService,
				memberItemsFavMapper,
				cartMapper,
				itemCrossBorderTaxQueryRepository,
				itemsRepository,
				companyDefaultCurrencyService,
				tdkGivenSaveService,
				pointsmallFrontBrandListService,
				itemsRelTagsRepository,
				itemsTagsRepository,
				distributorCouponListAppendService,
				distributorListQueryService,
				itemsCategoryDistributorIdResolver,
				salespersonCartDistributorSkuReplaceService,
				langueProperties,
				itemLogisticsStoreEnricher,
				searchEsProperties,
				goodsSearchFacade,
				searchMemberTierResolver);
	}

	private void stubOpenSalesmanGate() {
		WxappItemsListSalesmanGateResult gate = WxappItemsListSalesmanGateResult.proceed(Map.of());
		when(wxappGoodsItemsListQueryOrchestrator.resolveSalesmanGateForWxappItemList(anyLong(), any())).thenReturn(gate);
	}

	@Test
	void whenDisabled_delegateToDbPath() {
		when(searchEsProperties.isEnabled()).thenReturn(false);
		when(request.getParameter("page")).thenReturn("1");
		when(request.getParameter("pageSize")).thenReturn("10");
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("is_default", Boolean.TRUE);
		when(wxappGoodsItemsListParamBuilder.build(request, 1L, 0L, false)).thenReturn(params);
		when(wxappGoodsItemsListQueryOrchestrator.effectiveBusinessParamsForWxappItemList(anyLong(), any()))
				.thenReturn(new LinkedHashMap<>(params));
		stubOpenSalesmanGate();
		when(wxappGoodsItemsListQueryOrchestrator.queryItemListData(anyLong(), any(), any()))
				.thenReturn(Map.of("total_count", 0L, "list", List.of()));
		when(companyDefaultCurrencyService.getCur(anyLong())).thenReturn(new cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate());

		service.execute(1L, 0L, request);

		verify(goodsSearchFacade, never()).queryListWithHydration(anyLong(), any(), any(), any());
		verify(wxappGoodsItemsListParamBuilder).build(request, 1L, 0L, false);
		verify(wxappGoodsItemsListQueryOrchestrator).queryItemListData(eq(1L), any(), any());
	}

	@Test
	void whenEnabled_usesFacadeAndSkipsKeywordLikePreresolve() {
		when(searchEsProperties.isEnabled()).thenReturn(true);
		when(request.getParameter("page")).thenReturn("1");
		when(request.getParameter("pageSize")).thenReturn("10");
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("is_default", Boolean.TRUE);
		params.put("keywords", "test");
		when(wxappGoodsItemsListParamBuilder.build(request, 1L, 0L, true)).thenReturn(params);
		LinkedHashMap<String, Object> effective = new LinkedHashMap<>(params);
		when(wxappGoodsItemsListQueryOrchestrator.effectiveBusinessParamsForWxappItemList(anyLong(), any()))
				.thenReturn(effective);
		stubOpenSalesmanGate();
		when(searchMemberTierResolver.resolve(1L, 0L)).thenReturn(new SearchMemberTier("normal", 1L, 100, "默认"));
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", 99L);
		row.put("price", 100);
		when(goodsSearchFacade.queryListWithHydration(anyLong(), any(), any(), any()))
				.thenReturn(Map.of("total_count", 1L, "list", List.of(row)));
		when(companyDefaultCurrencyService.getCur(anyLong())).thenReturn(new cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate());
		when(companyPopularizeConfigReadService.readWxappPromoterConfigSlice(anyLong())).thenReturn(Map.of());

		service.execute(1L, 0L, request);

		verify(wxappGoodsItemsListParamBuilder).build(request, 1L, 0L, true);
		verify(goodsSearchFacade).queryListWithHydration(eq(1L), any(), any(), any());
		verify(wxappGoodsItemsListQueryOrchestrator, never()).queryItemListData(anyLong(), any(), any());
		verify(wxappGoodsItemsListMemberPriceApplyService, never()).applyForRows(anyLong(), anyLong(), any(), any());
	}

	@Test
	void whenSalesmanGateEmpty_doesNotCallEs() {
		when(searchEsProperties.isEnabled()).thenReturn(true);
		when(request.getParameter("page")).thenReturn("1");
		when(request.getParameter("pageSize")).thenReturn("10");
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("is_default", Boolean.TRUE);
		when(wxappGoodsItemsListParamBuilder.build(request, 1L, 0L, true)).thenReturn(params);
		when(wxappGoodsItemsListQueryOrchestrator.effectiveBusinessParamsForWxappItemList(anyLong(), any()))
				.thenReturn(new LinkedHashMap<>(params));
		WxappItemsListSalesmanGateResult emptyGate = WxappItemsListSalesmanGateResult.empty(1);
		when(wxappGoodsItemsListQueryOrchestrator.resolveSalesmanGateForWxappItemList(anyLong(), any())).thenReturn(emptyGate);
		when(wxappGoodsItemsListQueryOrchestrator.emptyWxappItemListForSalesmanGate(emptyGate))
				.thenReturn(Map.of("total_count", 0L, "list", List.of(), "nosalestore", 1));
		when(companyDefaultCurrencyService.getCur(anyLong())).thenReturn(new cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate());

		service.execute(1L, 0L, request);

		verify(goodsSearchFacade, never()).queryListWithHydration(anyLong(), any(), any(), any());
		verify(wxappGoodsItemsListQueryOrchestrator, never()).queryItemListData(anyLong(), any(), any());
		verify(wxappGoodsItemsListQueryOrchestrator).emptyWxappItemListForSalesmanGate(emptyGate);
	}
}
