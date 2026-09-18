package cn.shopex.ecshopx.goods.service.order.normal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.goods.service.items.ItemInventoryOrchestratorService;
import cn.shopex.ecshopx.promotions.service.LimitPersonBuyService;
import cn.shopex.ecshopx.promotions.service.SeckillUserBuysStoreService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCreateInventoryPortImplLimitedBuyTest {

	@Mock
	private ItemInventoryOrchestratorService itemInventoryOrchestratorService;

	@Mock
	private PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort;

	@Mock
	private LimitPersonBuyService limitPersonBuyService;

	@Mock
	private SeckillUserBuysStoreService seckillUserBuysStoreService;

	@InjectMocks
	private OrderCreateInventoryPortImpl port;

	@Test
	void minusStores_recordsLimitedBuyPurchasesFromItemsPromotion() {
		when(itemInventoryOrchestratorService.minusItemStore(any(), anyInt())).thenReturn(true);

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 7670L);
		item.put("num", 2);
		item.put("distributor_id", 0L);
		item.put("is_total_store", true);

		Map<String, Object> promo = new LinkedHashMap<>();
		promo.put("activity_type", "limited_buy");
		promo.put("activity_id", 88L);
		promo.put("item_id", 7670L);
		promo.put("user_id", 1172L);
		promo.put("company_id", 141L);
		promo.put("shop_id", 0L);
		promo.put("activity_rule", Map.of("limit", 5, "day", 0));

		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 141L);
		od.put("user_id", 1172L);
		od.put("distributor_id", 0L);
		od.put("receipt_type", "logistics");
		od.put("order_class", "normal");
		od.put("items", List.of(item));
		od.put("items_promotion", List.of(promo));

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.setOrderData(od);

		port.minusStores(p);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(limitPersonBuyService).createLimitPerson(captor.capture());
		Map<String, Object> params = captor.getValue();
		org.assertj.core.api.Assertions.assertThat(params.get("limit_id")).isEqualTo(88L);
		org.assertj.core.api.Assertions.assertThat(params.get("item_id")).isEqualTo(7670L);
		org.assertj.core.api.Assertions.assertThat(params.get("number")).isEqualTo(2L);
		org.assertj.core.api.Assertions.assertThat(params.get("user_id")).isEqualTo(1172L);
		org.assertj.core.api.Assertions.assertThat(params.get("company_id")).isEqualTo(141L);
	}

	@Test
	void minusStores_skipsCreateLimitPersonWhenNoLimitedBuyPromotion() {
		when(itemInventoryOrchestratorService.minusItemStore(any(), anyInt())).thenReturn(true);

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 7670L);
		item.put("num", 1);
		item.put("distributor_id", 0L);

		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 141L);
		od.put("user_id", 1172L);
		od.put("receipt_type", "logistics");
		od.put("order_class", "normal");
		od.put("items", List.of(item));
		od.put("items_promotion", List.of());

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.setOrderData(od);

		port.minusStores(p);

		verify(limitPersonBuyService, never()).createLimitPerson(any());
		verify(seckillUserBuysStoreService, never()).setUserBuysStore(anyLong(), anyLong(), anyLong(), anyLong(), anyInt(), anyLong());
	}

	@Test
	void minusStores_recordsLimitedTimeSalePurchasesFromItemsPromotion() {
		when(itemInventoryOrchestratorService.minusItemStore(any(), anyInt())).thenReturn(true);

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 7670L);
		item.put("num", 1);
		item.put("price", 990);
		item.put("distributor_id", 0L);
		item.put("is_total_store", true);

		Map<String, Object> promo = new LinkedHashMap<>();
		promo.put("activity_type", "limited_time_sale");
		promo.put("activity_id", 55L);
		promo.put("item_id", 7670L);
		promo.put("user_id", 1172L);
		promo.put("company_id", 141L);

		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 141L);
		od.put("user_id", 1172L);
		od.put("distributor_id", 0L);
		od.put("receipt_type", "logistics");
		od.put("order_class", "normal");
		od.put("items", List.of(item));
		od.put("items_promotion", List.of(promo));

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.setOrderData(od);

		port.minusStores(p);

		verify(seckillUserBuysStoreService).setUserBuysStore(55L, 141L, 1172L, 7670L, 1, 990L);
		verify(limitPersonBuyService, never()).createLimitPerson(any());
	}

	@Test
	void minusStores_recordsLimitedTimeSalePurchasesUsingActivityPriceTimesQuantity() {
		when(itemInventoryOrchestratorService.minusItemStore(any(), anyInt())).thenReturn(true);

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 24385L);
		item.put("num", 2);
		item.put("price", 800);
		item.put("activity_price", 100);
		item.put("distributor_id", 0L);
		item.put("is_total_store", true);

		Map<String, Object> promo = new LinkedHashMap<>();
		promo.put("activity_type", "limited_time_sale");
		promo.put("activity_id", 6L);
		promo.put("item_id", 24385L);
		promo.put("user_id", 45100L);
		promo.put("company_id", 1L);

		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 1L);
		od.put("user_id", 45100L);
		od.put("distributor_id", 0L);
		od.put("receipt_type", "logistics");
		od.put("order_class", "normal");
		od.put("items", List.of(item));
		od.put("items_promotion", List.of(promo));

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.setOrderData(od);

		port.minusStores(p);

		verify(seckillUserBuysStoreService).setUserBuysStore(6L, 1L, 45100L, 24385L, 2, 200L);
	}
}
