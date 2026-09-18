package cn.shopex.ecshopx.orders.service.offline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OmeTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.orders.dispatch.UpdateGroupsActivityOrderDispatchListener;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.domain.SubOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.SubOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.common.orders.port.NormalGroupsTradeFinishOnPayPort;
import cn.shopex.ecshopx.common.orders.port.NormalGroupsTradeFinishRefundBranchPort;
import cn.shopex.ecshopx.orders.service.group.GroupPromotionOrderPayedService;
import cn.shopex.ecshopx.orders.service.group.NormalGroupsTradeFinishRefundOrchestrator;
import cn.shopex.ecshopx.orders.service.groups.UpdateGroupsActivityOrderBusService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Probe: offline approve path publishes {@code EVENT_TRADE_FINISH} rows that fan-in to
 * {@link UpdateGroupsActivityOrderDispatchListener}; under {@code trade_source_type=normal_groups} and
 * {@code trade_state=SUCCESS}, the chain reaches {@link GroupPromotionOrderPayedService} and
 * {@link ThirdPartyTradeUpdateDispatchPublisher#publish}. Aligns payload keys with
 * {@link OfflinePaymentDoCheckServiceTradeFinishPublishTest#doCheck_whenConfirmSuccess_normalGroups_offlinePay_positivePayFee_publishesGroupsGateRow}.
 */
@ExtendWith(MockitoExtension.class)
class OfflinePaymentDoCheckNormalGroupsThirdPartyTradeUpdatePublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), ServiceOrders.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), SubOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Rights.class);
	}

	@Mock
	private ServiceOrdersMapper serviceOrdersMapper;
	@Mock
	private NormalOrdersMapper normalOrdersMapper;
	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;
	@Mock
	private SubOrdersMapper subOrdersMapper;
	@Mock
	private RightsMapper rightsMapper;
	@Mock
	private TradeMapper tradeMapper;
	@Mock
	private OmeTradeUpdateDispatchPublisher omeTradeUpdateDispatchPublisher;
	@Mock
	private ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;
	@Mock
	private NormalGroupsTradeFinishRefundBranchPort normalGroupsTradeFinishRefundBranchPort;

	private UpdateGroupsActivityOrderDispatchListener listener;

	@BeforeEach
	void setUp() {
		NormalGroupsTradeFinishRefundOrchestrator refundOrchestrator =
				new NormalGroupsTradeFinishRefundOrchestrator(
						normalGroupsTradeFinishRefundBranchPort,
						tradeMapper,
						normalOrdersMapper,
						orderAssociationsMapper,
						thirdPartyTradeUpdateDispatchPublisher);
		GroupPromotionOrderPayedService groupPromotionOrderPayedService =
				new GroupPromotionOrderPayedService(
						serviceOrdersMapper,
						normalOrdersMapper,
						orderAssociationsMapper,
						subOrdersMapper,
						rightsMapper,
						tradeMapper,
						new ObjectMapper(),
						omeTradeUpdateDispatchPublisher,
						thirdPartyTradeUpdateDispatchPublisher);
		NormalGroupsTradeFinishOnPayPort onPayPort = org.mockito.Mockito.mock(NormalGroupsTradeFinishOnPayPort.class);
		org.mockito.Mockito.doAnswer(
						inv -> {
							groupPromotionOrderPayedService.markNormalGroupOrderPayedAndPublishErpSync(
									inv.getArgument(0),
									inv.getArgument(1),
									inv.getArgument(2));
							return null;
						})
				.when(onPayPort)
				.onTradePaySuccess(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
		UpdateGroupsActivityOrderBusService busService =
				new UpdateGroupsActivityOrderBusService(onPayPort, refundOrchestrator);
		listener = new UpdateGroupsActivityOrderDispatchListener(busService);
	}

	@Test
	void tradeFinishRow_offlineNormalGroups_success_invokesThirdPartyTradeUpdatePublishOnce() {
		long companyId = 77L;
		long orderIdNum = 55L;
		long userId = 9001L;
		String tradeId = "trade-confirm-groups-1";

		Trade primary = new Trade();
		primary.setTradeId(tradeId);
		when(tradeMapper.selectOne(any())).thenReturn(primary);

		Map<String, Object> tradeFinishRow = new LinkedHashMap<>();
		tradeFinishRow.put("company_id", String.valueOf(companyId));
		tradeFinishRow.put("order_id", String.valueOf(orderIdNum));
		tradeFinishRow.put("user_id", String.valueOf(userId));
		tradeFinishRow.put("trade_source_type", "normal_groups");
		tradeFinishRow.put("trade_state", "SUCCESS");
		tradeFinishRow.put("pay_type", "offline_pay");
		tradeFinishRow.put("pay_fee", 10_000);
		tradeFinishRow.put("time_start", "1704067200");
		tradeFinishRow.put("mobile", "13800138000");

		listener.onEvent(tradeFinishRow);

		verify(thirdPartyTradeUpdateDispatchPublisher, times(1))
				.publish(
						argThat(
								map ->
										map != null
												&& Long.valueOf(companyId).equals(map.get("company_id"))
												&& String.valueOf(orderIdNum).equals(map.get("order_id"))
												&& Long.valueOf(userId).equals(map.get("user_id"))
												&& "normal_groups".equals(map.get("order_class"))
												&& tradeId.equals(map.get("trade_id"))));
		verify(omeTradeUpdateDispatchPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.size() == 4
												&& Long.valueOf(companyId).equals(m.get("company_id"))
												&& String.valueOf(orderIdNum).equals(m.get("order_id"))
												&& "normal_groups".equals(m.get("order_class"))
												&& Long.valueOf(userId).equals(m.get("user_id"))));
	}
}
