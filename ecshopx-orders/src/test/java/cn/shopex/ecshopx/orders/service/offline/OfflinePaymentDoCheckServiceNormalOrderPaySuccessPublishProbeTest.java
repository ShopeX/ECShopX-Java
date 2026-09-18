package cn.shopex.ecshopx.orders.service.offline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.NormalOrderPaySuccessDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.espier.domain.OfflineBankAccount;
import cn.shopex.ecshopx.espier.mapper.OfflineBankAccountMapper;
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.tradefinish.OrdersTradeFinishNormalOrderPaySuccessApplyService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Probe: admin offline {@code do_check} approval drives {@link OrdersTradeFinishDispatchPublisher#publish} (same
 * contract as production offline bean targeting the CSV-292 trade-finish message plane) with a mocked synchronous
 * fan-out into {@link OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener}, asserting
 * {@link NormalOrderPaySuccessDispatchPublisher#publish} once for eligible trades. Reject path must not publish pay
 * success. Async Youshu fan-out stays covered by bootstrap/youshu module tests.
 *
 * <p>In production, {@link NormalOrderPaySuccessDispatchPublisher#publish} is wired to Bus parent message
 * {@link OrdersDispatchEventNames#EVENT_NORMAL_ORDER_PAY_SUCCESS}; this mock records the payload only.
 */
@ExtendWith(MockitoExtension.class)
class OfflinePaymentDoCheckServiceNormalOrderPaySuccessPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, OfflinePayment.class);
		TableInfoHelper.initTableInfo(assistant, OfflineBankAccount.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, Trade.class);
		TableInfoHelper.initTableInfo(assistant, OrderAssociations.class);
		TableInfoHelper.initTableInfo(assistant, cn.shopex.ecshopx.supplier.domain.SupplierOrder.class);
	}

	@Mock(name = "companysRedisTemplate")
	private StringRedisTemplate companysRedisTemplate;

	@Mock(name = "sharedStringRedisTemplate")
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock
	private OfflinePaymentMapper offlinePaymentMapper;

	@Mock
	private OfflineBankAccountMapper offlineBankAccountMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Mock
	private OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private ValueOperations<String, String> companysValueOps;

	@Mock
	private ValueOperations<String, String> sharedValueOps;

	@Mock
	private HashOperations<String, Object, Object> sharedHashOps;

	private NormalOrderPaySuccessDispatchPublisher normalOrderPaySuccessDispatchPublisher;

	private OfflinePaymentDoCheckService service;

	@BeforeEach
	void setUp() {
		lenient()
				.doAnswer(
						invocation -> {
							@SuppressWarnings("unchecked")
							Consumer<TransactionStatus> action = invocation.getArgument(0);
							action.accept(mock(TransactionStatus.class));
							return null;
						})
				.when(transactionTemplate)
				.executeWithoutResult(any());

		lenient().when(companysRedisTemplate.opsForValue()).thenReturn(companysValueOps);
		lenient().when(sharedStringRedisTemplate.opsForValue()).thenReturn(sharedValueOps);
		lenient().when(sharedStringRedisTemplate.opsForHash()).thenReturn(sharedHashOps);
		lenient().doNothing().when(companysValueOps).set(anyString(), anyString(), any(Duration.class));
		lenient().when(sharedValueOps.increment(anyString())).thenReturn(1L);
		lenient().when(sharedHashOps.get(anyString(), any())).thenReturn(null);
		lenient().doNothing().when(sharedHashOps).put(anyString(), any(), any());
		lenient().when(sharedStringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
		lenient().doNothing().when(orderProcessLogPublishPort).publish(any());

		normalOrderPaySuccessDispatchPublisher = mock(NormalOrderPaySuccessDispatchPublisher.class);
		OrdersTradeFinishNormalOrderPaySuccessApplyService applyService =
				new OrdersTradeFinishNormalOrderPaySuccessApplyService(
						normalOrdersMapper,
						mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class),
						orderAssociationsMapper,
						mock(cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper.class),
						mock(cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService.class));
		OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener bridge =
				new OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener(
						applyService, normalOrderPaySuccessDispatchPublisher);

		lenient()
				.doAnswer(
						inv -> {
							@SuppressWarnings("unchecked")
							Map<String, Object> row = inv.getArgument(0);
							bridge.onEvent(row);
							return null;
						})
				.when(ordersTradeFinishDispatchPublisher)
				.publish(any());

		NormalOrders existingPaySuccess = new NormalOrders();
		existingPaySuccess.setOrderStatus("NOTPAY");
		existingPaySuccess.setOrderClass("normal");
		existingPaySuccess.setReceiptType("logistics");
		lenient().when(normalOrdersMapper.selectOne(any())).thenReturn(existingPaySuccess);
		lenient().when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		lenient().when(orderAssociationsMapper.update(any(), any())).thenReturn(1);

		service = new OfflinePaymentDoCheckService(
				companysRedisTemplate,
				sharedStringRedisTemplate,
				offlinePaymentMapper,
				offlineBankAccountMapper,
				normalOrdersMapper,
				tradeMapper,
				orderProcessLogPublishPort,
				ordersTradeFinishDispatchPublisher,
				objectMapper,
				transactionTemplate);
	}

	@Test
	void approve_offlinePay_normal_thenNormalOrderPaySuccessPublishOnce() {
		long paymentId = 20L;
		long companyId = 88L;
		long orderIdNum = 42L;
		String tradeId = "trade-probe-normal-1";

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(7L);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);

		Trade notPay = new Trade();
		notPay.setTradeId(tradeId);
		notPay.setCompanyId(String.valueOf(companyId));
		notPay.setOrderId(String.valueOf(orderIdNum));
		notPay.setDistributorId("0");
		notPay.setTradeState("NOTPAY");
		notPay.setPayType("offline_pay");
		notPay.setTradeSourceType("normal");
		notPay.setPayFee(10_000);
		notPay.setUserId("9001");
		notPay.setShopId("501");
		notPay.setTimeStart("1704067200");
		notPay.setMobile("13900139888");
		when(tradeMapper.selectOne(any())).thenReturn(notPay);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Trade success = new Trade();
		success.setTradeId(tradeId);
		success.setCompanyId(String.valueOf(companyId));
		success.setOrderId(String.valueOf(orderIdNum));
		success.setTradeState("SUCCESS");
		success.setPayType("offline_pay");
		success.setTradeSourceType("normal");
		success.setPayFee(10_000);
		success.setUserId("9001");
		success.setShopId("501");
		success.setDistributorId("0");
		success.setTimeStart("1704067200");
		success.setMobile("13900139888");
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", 7L);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		// Payload is what the bridge forwards; parent message name is OrdersDispatchEventNames.EVENT_NORMAL_ORDER_PAY_SUCCESS.
		verify(normalOrderPaySuccessDispatchPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& Objects.equals(companyId, longFrom(m.get("company_id")))
												&& Objects.equals(orderIdNum, longFrom(m.get("order_id")))
												&& Objects.equals(
														"offline_pay", Objects.toString(m.get("pay_type"), null))
												&& Objects.equals(
														"normal", Objects.toString(m.get("trade_source_type"), null))));
	}

	/**
	 * {@code trade_source_type = normal_groups}: same approval and NOTPAY→SUCCESS transition as
	 * {@link #approve_offlinePay_normal_thenNormalOrderPaySuccessPublishOnce()}, then trade-finish publish and bridge.
	 */
	@Test
	void approve_offlinePay_normalGroups_thenNormalOrderPaySuccessPublishOnce() {
		long paymentId = 21L;
		long companyId = 77L;
		long orderIdNum = 55L;
		String tradeId = "trade-probe-groups-1";

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(71L);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);

		Trade notPay = new Trade();
		notPay.setTradeId(tradeId);
		notPay.setCompanyId(String.valueOf(companyId));
		notPay.setOrderId(String.valueOf(orderIdNum));
		notPay.setDistributorId("0");
		notPay.setTradeState("NOTPAY");
		notPay.setPayType("offline_pay");
		notPay.setTradeSourceType("normal_groups");
		notPay.setPayFee(10_000);
		notPay.setUserId("9001");
		notPay.setShopId("501");
		notPay.setTimeStart("1704067200");
		notPay.setMobile("13800138000");
		when(tradeMapper.selectOne(any())).thenReturn(notPay);
		when(tradeMapper.update(any(), any())).thenReturn(1);

		Trade success = new Trade();
		success.setTradeId(tradeId);
		success.setCompanyId(String.valueOf(companyId));
		success.setOrderId(String.valueOf(orderIdNum));
		success.setTradeState("SUCCESS");
		success.setPayType("offline_pay");
		success.setTradeSourceType("normal_groups");
		success.setPayFee(10_000);
		success.setUserId("9001");
		success.setShopId("501");
		success.setDistributorId("0");
		success.setTimeStart("1704067200");
		success.setMobile("13800138000");
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", 71L);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		// Same bridge and parent message name as normal mall variant; payload carries trade_source_type normal_groups.
		verify(normalOrderPaySuccessDispatchPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& Objects.equals(companyId, longFrom(m.get("company_id")))
												&& Objects.equals(orderIdNum, longFrom(m.get("order_id")))
												&& Objects.equals(
														"offline_pay", Objects.toString(m.get("pay_type"), null))
												&& Objects.equals(
														"normal_groups",
														Objects.toString(m.get("trade_source_type"), null))));
	}

	@Test
	@DisplayName("Reject path must not publish normal-order pay success")
	void refuse_offlinePay_thenNoNormalOrderPaySuccessPublish() {
		long paymentId = 22L;
		long companyId = 99L;
		long orderIdNum = 43L;

		when(companysValueOps.get("offline_pay_check:" + paymentId)).thenReturn("");

		OfflinePayment pending = new OfflinePayment();
		pending.setId(paymentId);
		pending.setCompanyId(companyId);
		pending.setOrderId(orderIdNum);
		pending.setCheckStatus(0);
		when(offlinePaymentMapper.selectById(paymentId)).thenReturn(pending);

		OfflineBankAccount acc = new OfflineBankAccount();
		acc.setId(8L);
		acc.setCompanyId(companyId);
		when(offlineBankAccountMapper.selectOne(any())).thenReturn(acc);

		when(offlinePaymentMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 2);
		body.put("bank_account_id", 8L);
		body.put("pay_fee", "50.00");
		body.put("remark", "bad voucher");

		service.doCheck(companyId, 2L, "admin", "op", body);

		verifyNoInteractions(normalOrderPaySuccessDispatchPublisher);
	}

	private static Long longFrom(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
