package cn.shopex.ecshopx.theme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemsPriceOverlayService;
import cn.shopex.ecshopx.goods.service.ItemsGroupGetGroupItemsService;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsAdminListService;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateWidgetItemsQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class PagesTemplateWidgetItemsServicePointsmallTest {

	@Mock private ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	@Mock private SeckillActivityMapper seckillActivityMapper;
	@Mock private SeckillRelGoodsMapper seckillRelGoodsMapper;
	@Mock private ItemsGroupGetGroupItemsService itemsGroupGetGroupItemsService;
	@Mock private WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	@Mock private GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	@Mock private WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	@Mock private PointsmallItemsMapper pointsmallItemsMapper;
	@Mock private PointsmallItemsAdminListService pointsmallItemsAdminListService;
	@Mock private ActivitiesMapper activitiesMapper;
	@Mock private EmployeePurchaseActivityItemsPriceOverlayService overlayService;

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
						overlayService);
	}

	@Test
	void pointsmallItemsUsesDataValueArrayIds() {
		PointsmallItems ent = new PointsmallItems();
		ent.setItemId(403L);
		ent.setDefaultItemId(403L);
		ent.setCompanyId(38L);
		IPage<PointsmallItems> page = new Page<>(1, 10);
		page.setRecords(List.of(ent));
		when(pointsmallItemsMapper.selectPage(any(), any())).thenReturn(page);
		when(pointsmallItemsMapper.selectObjs(any())).thenReturn(List.of(5));
		when(pointsmallItemsAdminListService.toListRowMap(ent)).thenReturn(new LinkedHashMap<>(Map.of("item_id", 403L)));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pagestemplate/widget/items");
		request.setParameter("data_type", "pointsmall_items");
		request.addParameter("data_value[]", "403");
		PagesTemplateWidgetItemsQuery q = PagesTemplateWidgetItemsQuery.fromHttpServletRequest(request);

		Map<String, Object> result = service.getWidgetItems(38L, "zh-CN", q);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
		assertFalse(data.isEmpty());
		assertEquals(403L, data.get(0).get("item_id"));
		verify(pointsmallItemsMapper).selectPage(any(), any());
	}
}
