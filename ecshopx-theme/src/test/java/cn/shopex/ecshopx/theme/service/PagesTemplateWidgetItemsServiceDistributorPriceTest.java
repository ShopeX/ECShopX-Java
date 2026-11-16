package cn.shopex.ecshopx.theme.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemsPriceOverlayService;
import cn.shopex.ecshopx.goods.service.ItemsGroupGetGroupItemsService;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.goods.service.wxapp.WxappStandardDistributorSkuListReplaceService;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsAdminListService;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateWidgetItemsQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class PagesTemplateWidgetItemsServiceDistributorPriceTest {

	@Mock ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	@Mock SeckillActivityMapper seckillActivityMapper;
	@Mock SeckillRelGoodsMapper seckillRelGoodsMapper;
	@Mock ItemsGroupGetGroupItemsService itemsGroupGetGroupItemsService;
	@Mock WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	@Mock GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	@Mock WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	@Mock PointsmallItemsMapper pointsmallItemsMapper;
	@Mock PointsmallItemsAdminListService pointsmallItemsAdminListService;
	@Mock ActivitiesMapper activitiesMapper;
	@Mock EmployeePurchaseActivityItemsPriceOverlayService overlayService;
	@Mock WxappStandardDistributorSkuListReplaceService wxappStandardDistributorSkuListReplaceService;

	private PagesTemplateWidgetItemsService service;

	@BeforeEach
	void setUp() {
		service =
				new PagesTemplateWidgetItemsService(
						new ObjectMapper(),
						itemsCategoryItemIdResolver,
						seckillActivityMapper,
						seckillRelGoodsMapper,
						itemsGroupGetGroupItemsService,
						wxappGoodsItemsListQueryOrchestrator,
						goodsItemsListPromotionEnrichmentService,
						wxappGoodsItemsListMemberPriceApplyService,
						pointsmallItemsMapper,
						pointsmallItemsAdminListService,
						activitiesMapper,
						overlayService,
						wxappStandardDistributorSkuListReplaceService);
	}

	@Test
	void h5ItemsWidget_appliesStandardDistributorSkuReplaceAfterListQuery() {
		List<Map<String, Object>> list = new ArrayList<>();
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", 8254L);
		row.put("price", 10000);
		list.add(row);
		when(wxappGoodsItemsListQueryOrchestrator.queryItemListData(eq(38L), any(), anyList()))
				.thenReturn(Map.of("list", list));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("data_type", "items");
		request.setParameter("data_value[0]", "8254");
		request.setParameter("num", "4");
		request.setParameter("distributor_id", "3370");
		PagesTemplateWidgetItemsQuery q = PagesTemplateWidgetItemsQuery.fromH5FrontHttpServletRequest(request);

		service.getWidgetItems(38L, "zh-CN", q);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(wxappStandardDistributorSkuListReplaceService)
				.applyIfNeeded(eq(38L), paramsCaptor.capture(), eq(list));
		Map<String, Object> capturedParams = paramsCaptor.getValue();
		org.junit.jupiter.api.Assertions.assertEquals(3370L, capturedParams.get("distributor_id"));
		org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, capturedParams.get("is_can_sale"));
	}
}