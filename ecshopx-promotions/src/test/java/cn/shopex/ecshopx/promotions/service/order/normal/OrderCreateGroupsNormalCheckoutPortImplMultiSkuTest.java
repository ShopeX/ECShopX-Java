package cn.shopex.ecshopx.promotions.service.order.normal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsRelGoodsReadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class OrderCreateGroupsNormalCheckoutPortImplMultiSkuTest {

	private static final long COMPANY_ID = 1L;
	private static final long ACT_ID = 42L;
	private static final long ITEM_A = 100L;
	private static final long MAIN_ACT_PRICE = 5000L;
	private static final long SKU_ACT_PRICE = 3900L;

	private PromotionGroupsActivityCheckCreateGroupOrderService checkService;
	private PromotionGroupsRelGoodsReadService relGoodsReadService;
	private StringRedisTemplate redisTemplate;
	private OrderCreateGroupsNormalCheckoutPortImpl checkoutPort;

	@BeforeEach
	void setUp() {
		checkService = mock(PromotionGroupsActivityCheckCreateGroupOrderService.class);
		relGoodsReadService = mock(PromotionGroupsRelGoodsReadService.class);
		redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(redisTemplate.opsForHash()).thenReturn(hashOps);
		checkoutPort =
				new OrderCreateGroupsNormalCheckoutPortImpl(
						checkService,
						redisTemplate,
						new ObjectMapper(),
						relGoodsReadService);
	}

	@Test
	@SuppressWarnings("unchecked")
	void applyAfterFormat_usesRelActivityPricePerSku() {
		PromotionGroupsActivity activity = new PromotionGroupsActivity();
		activity.setGroupsActivityId(ACT_ID);
		activity.setActPrice(MAIN_ACT_PRICE);
		activity.setActName("拼团");
		activity.setEndTime(9_999_999_999L);
		when(checkService.checkCreateGroupOrder(any(), any())).thenReturn(activity);

		PromotionGroupsRelGoods rel = new PromotionGroupsRelGoods();
		rel.setItemId(ITEM_A);
		rel.setActivityPrice(SKU_ACT_PRICE);
		when(relGoodsReadService.hasRelRows(COMPANY_ID, ACT_ID)).thenReturn(true);
		when(relGoodsReadService.findByActivityAndItem(COMPANY_ID, ACT_ID, ITEM_A))
				.thenReturn(Optional.of(rel));

		Map<String, Object> line = new LinkedHashMap<>();
		line.put("item_id", ITEM_A);
		line.put("num", 2);
		line.put("price", 8000L);

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.getParams().put("order_type", "normal_groups");
		p.getParams().put("company_id", COMPANY_ID);
		p.getParams().put("bargain_id", ACT_ID);
		p.getOrderData().put("items", List.of(line));

		checkoutPort.applyAfterFormat(p);

		Map<String, Object> updatedLine =
				((List<Map<String, Object>>) p.getOrderData().get("items")).get(0);
		assertEquals(String.valueOf(SKU_ACT_PRICE), String.valueOf(updatedLine.get("activity_price")));
		assertEquals(SKU_ACT_PRICE * 2, longVal(updatedLine.get("total_fee")));
		assertEquals(8000L * 2 - SKU_ACT_PRICE * 2, longVal(updatedLine.get("discount_fee")));
	}

	private static long longVal(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v));
	}
}
