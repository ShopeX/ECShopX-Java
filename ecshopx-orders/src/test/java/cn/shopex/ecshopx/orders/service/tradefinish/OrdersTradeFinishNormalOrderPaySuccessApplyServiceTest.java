package cn.shopex.ecshopx.orders.service.tradefinish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdersTradeFinishNormalOrderPaySuccessApplyServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(assistant, OrderAssociations.class);
		TableInfoHelper.initTableInfo(assistant, SupplierOrder.class);
	}

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Test
	void markPayedIfNeeded_employeePurchaseTradeSource_updatesNotpayToPayed() {
		NormalOrders existing = new NormalOrders();
		existing.setOrderStatus("NOTPAY");
		existing.setPayStatus("NOTPAY");
		when(normalOrdersMapper.selectOne(any())).thenReturn(existing);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		when(supplierOrderMapper.update(any(), any())).thenReturn(1);

		OrdersTradeFinishNormalOrderPaySuccessApplyService service =
				new OrdersTradeFinishNormalOrderPaySuccessApplyService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderAssociationsMapper,
						supplierOrderMapper,
						orderValiditySettingRedisReadService);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("trade_state", "SUCCESS");
		payload.put("company_id", 1L);
		payload.put("order_id", 5331426000090002L);
		payload.put("trade_source_type", "normal_employee_purchase");
		payload.put("pay_type", "deposit");

		Optional<Map<String, Object>> out = service.markPayedIfNeeded(payload);

		assertTrue(out.isPresent());
		assertEquals(1L, out.get().get("company_id"));
		assertEquals(5331426000090002L, out.get().get("order_id"));
		verify(normalOrdersMapper).update(any(), any());
		verify(orderAssociationsMapper).update(any(), any());
		verify(supplierOrderMapper).update(any(), any());
	}

	@Test
	void markPayedIfNeeded_unknownTradeSource_skipsUpdate() {
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		OrdersTradeFinishNormalOrderPaySuccessApplyService service =
				new OrdersTradeFinishNormalOrderPaySuccessApplyService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderAssociationsMapper,
						supplierOrderMapper,
						orderValiditySettingRedisReadService);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("trade_state", "SUCCESS");
		payload.put("company_id", 1L);
		payload.put("order_id", 100L);
		payload.put("trade_source_type", "membercard");

		assertTrue(service.markPayedIfNeeded(payload).isEmpty());
		verify(normalOrdersMapper, never()).update(any(), any());
	}
}
