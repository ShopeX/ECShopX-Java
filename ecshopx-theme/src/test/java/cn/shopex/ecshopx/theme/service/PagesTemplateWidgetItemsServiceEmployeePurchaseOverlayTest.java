package cn.shopex.ecshopx.theme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemsPriceOverlayService;
import cn.shopex.ecshopx.goods.service.ItemsGroupGetGroupItemsService;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsAdminListService;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateWidgetItemsQuery;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class PagesTemplateWidgetItemsServiceEmployeePurchaseOverlayTest {

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

	private PagesTemplateWidgetItemsService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), Activities.class);
	}

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
	void overlayRunsAfterPromotionEnrichment() {
		List<Map<String, Object>> list = new ArrayList<>();
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", 100L);
		list.add(row);
		when(wxappGoodsItemsListQueryOrchestrator.queryItemListData(eq(141L), any(), anyList()))
				.thenReturn(Map.of("list", list));

		service.getWidgetItems(141L, "zh-CN", h5Query("166"), 1185L);

		InOrder order = inOrder(goodsItemsListPromotionEnrichmentService, overlayService);
		order.verify(goodsItemsListPromotionEnrichmentService).enrich(list);
		order.verify(overlayService).overlayActivityPrice(list, 141L, 166L);
	}

	@Test
	void noOverlayWhenEActivityIdMissing() {
		List<Map<String, Object>> list = new ArrayList<>();
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", 100L);
		list.add(row);
		when(wxappGoodsItemsListQueryOrchestrator.queryItemListData(eq(141L), any(), anyList()))
				.thenReturn(Map.of("list", list));

		service.getWidgetItems(141L, "zh-CN", h5Query(null), 1185L);

		verify(overlayService, never()).overlayActivityPrice(any(), anyLong(), anyLong());
	}

	@Test
	void eActivityIdInjectsActivityDistributorAndIsCanSaleOnH5() {
		Activities activity = new Activities();
		activity.setId(166L);
		activity.setDistributorId(88);
		when(activitiesMapper.selectOne(any())).thenReturn(activity);
		when(wxappGoodsItemsListQueryOrchestrator.queryItemListData(eq(141L), any(), anyList()))
				.thenReturn(Map.of("list", List.of()));

		service.getWidgetItems(141L, "zh-CN", h5Query("166"), 1185L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LinkedHashMap<String, Object>> captor = ArgumentCaptor.forClass(LinkedHashMap.class);
		verify(wxappGoodsItemsListQueryOrchestrator).queryItemListData(eq(141L), captor.capture(), anyList());
		LinkedHashMap<String, Object> params = captor.getValue();
		assertEquals(88L, params.get("distributor_id"));
		assertEquals(Boolean.TRUE, params.get("is_can_sale"));
		assertTrue(Boolean.TRUE.equals(params.get("is_can_sale")));
	}

	private static PagesTemplateWidgetItemsQuery h5Query(String eActivityId) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("data_type", "items");
		request.setParameter("data_value[0]", "7670");
		request.setParameter("num", "4");
		if (eActivityId != null) {
			request.setParameter("e_activity_id", eActivityId);
		}
		return PagesTemplateWidgetItemsQuery.fromH5FrontHttpServletRequest(request);
	}
}
