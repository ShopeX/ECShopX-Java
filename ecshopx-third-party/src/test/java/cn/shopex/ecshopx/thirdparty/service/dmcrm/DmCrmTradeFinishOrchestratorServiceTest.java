package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.distribution.DmCrmTradeFinishGuideDistributorReadPort;
import cn.shopex.ecshopx.common.port.goods.GoodsItemsListsByItemIdsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.point.PointMemberDmPointMemberInfoReadPort;
import cn.shopex.ecshopx.common.port.salesperson.ShoppingGuideSalespersonRowReadPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DmCrmTradeFinishOrchestratorServiceTest {

	@Mock private DmCrmSettingReadPort dmCrmSettingReadPort;
	@Mock private OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	@Mock private OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	@Mock private GoodsItemsListsByItemIdsReadPort goodsItemsListsByItemIdsReadPort;
	@Mock private ShoppingGuideSalespersonRowReadPort shoppingGuideSalespersonRowReadPort;
	@Mock private DmCrmTradeFinishGuideDistributorReadPort dmCrmTradeFinishGuideDistributorReadPort;
	@Mock private PointMemberDmPointMemberInfoReadPort pointMemberDmPointMemberInfoReadPort;
	@Mock private DmCrmManualPointChangePort dmCrmManualPointChangePort;
	@Mock private DmCrmTradeFinishOrderSyncPort dmCrmTradeFinishOrderSyncPort;

	private DmCrmTradeFinishOrchestratorService service;

	@BeforeEach
	void setUp() {
		service =
				new DmCrmTradeFinishOrchestratorService(
						dmCrmSettingReadPort,
						orderNormalOrderHeaderReadPort,
						orderNormalOrderItemsReadPort,
						goodsItemsListsByItemIdsReadPort,
						shoppingGuideSalespersonRowReadPort,
						dmCrmTradeFinishGuideDistributorReadPort,
						pointMemberDmPointMemberInfoReadPort,
						dmCrmManualPointChangePort,
						dmCrmTradeFinishOrderSyncPort);
	}

	@Test
	void handleTradeFinish_whenSettingDisabled_doesNotCallPorts() {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(10L)).thenReturn(false);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("order_id", 99L);

		service.handleTradeFinish(payload);

		verify(dmCrmManualPointChangePort, never()).changePoint(any());
		verify(dmCrmTradeFinishOrderSyncPort, never()).syncOrderForTradeFinish(anyLong(), any(), any());
	}

	@Test
	void handleTradeFinish_whenEnabled_invokesPointAndSyncOrderPerGuards() {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(10L)).thenReturn(true);
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("company_id", 10L);
		header.put("order_id", 99L);
		header.put("dm_point_preid", "pre-1");
		header.put("point_fee", 0);
		header.put("point_use", 0);
		header.put("user_id", 5L);
		header.put("total_fee", "5000");
		header.put("item_fee", "4000");
		header.put("freight_fee", 500);
		header.put("order_class", "normal");
		header.put("salesman_id", 0L);
		header.put("sale_salesman_distributor_id", 0L);
		header.put("receiver_name", "n");
		header.put("receiver_mobile", "m");
		header.put("receiver_zip", "");
		header.put("receiver_address", "a");
		header.put("create_time", 1700000000);
		header.put("remark", "");
		header.put("discount_info", "");
		when(orderNormalOrderHeaderReadPort.getHeader(10L, 99L)).thenReturn(Optional.of(header));

		Map<String, Object> line = new LinkedHashMap<>();
		line.put("item_id", 7001L);
		line.put("num", 2);
		line.put("total_fee", 1000);
		line.put("item_fee", 1000);
		line.put("price", 500);
		line.put("pic", "");
		when(orderNormalOrderItemsReadPort.listItems(10L, 99L)).thenReturn(List.of(line));

		Map<String, Object> goodsRow = new LinkedHashMap<>();
		goodsRow.put("item_id", 7001L);
		goodsRow.put("item_name", "Sku");
		goodsRow.put("goods_bn", "gb");
		goodsRow.put("item_bn", "ib");
		goodsRow.put("is_gift", 0);
		when(goodsItemsListsByItemIdsReadPort.listRowsByItemIds(eq(10L), any()))
				.thenReturn(List.of(goodsRow));

		Map<String, Object> member = new LinkedHashMap<>();
		member.put("mobile", "13800000000");
		member.put("dm_card_no", "card");
		when(pointMemberDmPointMemberInfoReadPort.getMemberInfo(5L, 10L)).thenReturn(member);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("order_id", 99L);

		service.handleTradeFinish(payload);

		verify(dmCrmManualPointChangePort).changePoint(any());
		verify(dmCrmTradeFinishOrderSyncPort).syncOrderForTradeFinish(eq(10L), eq("99"), any());
	}
}
