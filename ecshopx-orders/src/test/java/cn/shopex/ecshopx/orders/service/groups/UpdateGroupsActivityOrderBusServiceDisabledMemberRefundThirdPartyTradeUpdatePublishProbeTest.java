package cn.shopex.ecshopx.orders.service.groups;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OmeTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.orders.port.NormalGroupsTradeFinishOnPayPort;
import cn.shopex.ecshopx.common.orders.port.NormalGroupsTradeFinishRefundBranchPort;
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
import cn.shopex.ecshopx.orders.service.group.GroupPromotionOrderPayedService;
import cn.shopex.ecshopx.orders.service.group.NormalGroupsTradeFinishRefundOrchestrator;
import cn.shopex.ecshopx.orders.service.group.NormalGroupsTradeFinishRefundOrchestrator.RefundBranchOutcome;
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

@ExtendWith(MockitoExtension.class)
public class UpdateGroupsActivityOrderBusServiceDisabledMemberRefundThirdPartyTradeUpdatePublishProbeTest {

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
	private NormalGroupsTradeFinishRefundBranchPort normalGroupsTradeFinishRefundBranchPort;
	@Mock
	private TradeMapper tradeMapper;
	@Mock
	private NormalOrdersMapper normalOrdersMapper;
	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;
	@Mock
	private ThirdPartyTradeUpdateDispatchPublisher thirdPartyForRefundPath;
	@Mock
	private ServiceOrdersMapper serviceOrdersMapper;
	@Mock
	private SubOrdersMapper subOrdersMapper;
	@Mock
	private RightsMapper rightsMapper;
	@Mock
	private OmeTradeUpdateDispatchPublisher omeTradeUpdateDispatchPublisher;
	@Mock
	private ThirdPartyTradeUpdateDispatchPublisher thirdPartyForPayedPath;
	@Mock
	private NormalGroupsTradeFinishRefundOrchestrator normalGroupsTradeFinishRefundOrchestrator;

	@BeforeEach
	void setUp() {}

	@Test
	void tradeFinish_whenRefundBranchMatched_invokesPublishOnceWithCancelAssociationMap() {
		long companyId = 77L;
		long orderIdNum = 55L;
		long userId = 9001L;

		when(normalGroupsTradeFinishRefundBranchPort.shouldRefundDisabledMemberInFormedTeam(
						companyId, orderIdNum, userId))
				.thenReturn(true);
		Trade successTrade = new Trade();
		successTrade.setTradeId("trade-refund-probe-1");
		when(tradeMapper.selectOne(any())).thenReturn(successTrade);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		OrderAssociations canceled = new OrderAssociations();
		canceled.setCompanyId(companyId);
		canceled.setOrderId(orderIdNum);
		canceled.setUserId(userId);
		canceled.setOrderClass("groups");
		canceled.setOrderStatus("CANCEL");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(canceled);

		NormalGroupsTradeFinishRefundOrchestrator orchestrator =
				new NormalGroupsTradeFinishRefundOrchestrator(
						normalGroupsTradeFinishRefundBranchPort,
						tradeMapper,
						normalOrdersMapper,
						orderAssociationsMapper,
						thirdPartyForRefundPath);
		NormalGroupsTradeFinishOnPayPort mockOnPay = mock(NormalGroupsTradeFinishOnPayPort.class);
		UpdateGroupsActivityOrderBusService busService =
				new UpdateGroupsActivityOrderBusService(mockOnPay, orchestrator);

		Map<String, Object> tradeFinishRow = new LinkedHashMap<>();
		tradeFinishRow.put("company_id", String.valueOf(companyId));
		tradeFinishRow.put("order_id", String.valueOf(orderIdNum));
		tradeFinishRow.put("user_id", String.valueOf(userId));
		tradeFinishRow.put("trade_source_type", "normal_groups");
		tradeFinishRow.put("trade_state", "SUCCESS");

		busService.handleTradeFinishRow(tradeFinishRow);

		verify(thirdPartyForRefundPath, times(1))
				.publish(
						argThat(
								map ->
										map != null
												&& "CANCEL".equals(map.get("order_status"))
												&& "groups".equals(map.get("order_class"))
												&& Long.valueOf(companyId).equals(map.get("company_id"))
												&& String.valueOf(orderIdNum).equals(map.get("order_id"))
												&& Long.valueOf(userId).equals(map.get("user_id"))));
		verify(mockOnPay, never()).onTradePaySuccess(anyLong(), anyLong(), anyLong());
	}

	@Test
	void tradeFinish_whenPayedBranchMatched_invokesPayedPathAndNotRefundPublish() {
		long companyId = 77L;
		long orderIdNum = 55L;
		long userId = 9001L;
		String tradeId = "trade-payed-probe-1";

		when(normalGroupsTradeFinishRefundOrchestrator.tryRefundDisabledMemberInFormedTeamAndPublishTradeUpdate(
						companyId, userId, orderIdNum))
				.thenReturn(RefundBranchOutcome.NOT_APPLICABLE_CONTINUE_PAYED);

		Trade primary = new Trade();
		primary.setTradeId(tradeId);
		when(tradeMapper.selectOne(any())).thenReturn(primary);

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
						thirdPartyForPayedPath);
		NormalGroupsTradeFinishOnPayPort onPayPort = mock(NormalGroupsTradeFinishOnPayPort.class);
		doAnswer(
						inv -> {
							groupPromotionOrderPayedService.markNormalGroupOrderPayedAndPublishErpSync(
									inv.getArgument(0),
									inv.getArgument(1),
									inv.getArgument(2));
							return null;
						})
				.when(onPayPort)
				.onTradePaySuccess(anyLong(), anyLong(), anyLong());
		UpdateGroupsActivityOrderBusService busService =
				new UpdateGroupsActivityOrderBusService(
						onPayPort, normalGroupsTradeFinishRefundOrchestrator);

		Map<String, Object> tradeFinishRow = new LinkedHashMap<>();
		tradeFinishRow.put("company_id", String.valueOf(companyId));
		tradeFinishRow.put("order_id", String.valueOf(orderIdNum));
		tradeFinishRow.put("user_id", String.valueOf(userId));
		tradeFinishRow.put("trade_source_type", "normal_groups");
		tradeFinishRow.put("trade_state", "SUCCESS");

		busService.handleTradeFinishRow(tradeFinishRow);

		verify(normalGroupsTradeFinishRefundOrchestrator, times(1))
				.tryRefundDisabledMemberInFormedTeamAndPublishTradeUpdate(companyId, userId, orderIdNum);
		verify(thirdPartyForPayedPath, times(1))
				.publish(
						argThat(
								map ->
										map != null
												&& "normal_groups".equals(map.get("order_class"))
												&& tradeId.equals(map.get("trade_id"))
												&& !"CANCEL".equals(map.get("order_status"))));
	}
}
