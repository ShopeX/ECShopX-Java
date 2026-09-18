package cn.shopex.ecshopx.promotions.service.bargain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BargainHelpPayTradeFinishActivityService")
class BargainHelpPayTradeFinishActivityServiceTest {

	@Mock private NormalOrdersMapper normalOrdersMapper;
	@Mock private UserBargainsMapper userBargainsMapper;
	@Mock private BargainPromotionsMapper bargainPromotionsMapper;

	@InjectMocks private BargainHelpPayTradeFinishActivityService service;

	@Test
	void onTradeFinishTradeRow_noOpWhenTradeSourceTypeIsNotBargain() {
		Map<String, Object> row = baseRow();
		row.put("trade_source_type", "normal");

		service.onTradeFinishTradeRow(row);

		verify(normalOrdersMapper, never()).selectOne(any());
		verify(userBargainsMapper, never()).update(any(), any());
		verify(bargainPromotionsMapper, never()).update(any(), any());
	}

	@Test
	void onTradeFinishTradeRow_noUserBargainUpdateWhenOrderClassIsNotBargain() {
		Map<String, Object> row = baseRow();

		NormalOrders order = new NormalOrders();
		order.setOrderId(77001L);
		order.setCompanyId(91L);
		order.setOrderClass("normal");
		order.setActId(500L);
		order.setUserId(9001L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		service.onTradeFinishTradeRow(row);

		verify(normalOrdersMapper, times(1)).selectOne(any());
		verify(userBargainsMapper, never()).update(any(), any());
		verify(bargainPromotionsMapper, never()).update(any(), any());
	}

	@Test
	void onTradeFinishTradeRow_updatesUserBargainAndPromotionWhenGatesPass() {
		Map<String, Object> row = baseRow();

		NormalOrders order = new NormalOrders();
		order.setOrderId(77001L);
		order.setCompanyId(91L);
		order.setOrderClass("bargain");
		order.setActId(500L);
		order.setUserId(9001L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		service.onTradeFinishTradeRow(row);

		verify(normalOrdersMapper, times(1)).selectOne(any());
		verify(userBargainsMapper, times(1)).update(any(), any());
		verify(bargainPromotionsMapper, times(1)).update(any(), any());
	}

	private static Map<String, Object> baseRow() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 91L);
		row.put("order_id", 77001L);
		row.put("trade_id", "T-099");
		row.put("trade_source_type", "bargain");
		return row;
	}
}
