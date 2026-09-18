package cn.shopex.ecshopx.orders.service.offline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.espier.domain.OfflineBankAccount;
import cn.shopex.ecshopx.espier.mapper.OfflineBankAccountMapper;
import cn.shopex.ecshopx.hfpay.dispatch.HfpayTradeRecordTradeFinishPaySuccessDispatchListener;
import cn.shopex.ecshopx.hfpay.service.HfpayTradeRecordService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
 * Probe: admin offline {@code do_check} approval publishes a snake_case trade row; the publisher mock fans into
 * {@link HfpayTradeRecordTradeFinishPaySuccessDispatchListener} so {@link HfpayTradeRecordService#paySuccess} receives the
 * same {@code order_id} string as the reloaded trade row (production wires the offline-specific trade-finish bean).
 */
@ExtendWith(MockitoExtension.class)
class OfflinePaymentDoCheckServiceHfpayTradeRecordCsv292TradeFinishPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, OfflinePayment.class);
		TableInfoHelper.initTableInfo(assistant, OfflineBankAccount.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, Trade.class);
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
	private TradeMapper tradeMapper;

	@Mock
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Mock
	private OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private ValueOperations<String, String> companysValueOps;

	@Mock
	private ValueOperations<String, String> sharedValueOps;

	@Mock
	private HashOperations<String, Object, Object> sharedHashOps;

	private final HfpayTradeRecordService hfpayTradeRecordService = mock(HfpayTradeRecordService.class);

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

		HfpayTradeRecordTradeFinishPaySuccessDispatchListener hfpayListener =
				new HfpayTradeRecordTradeFinishPaySuccessDispatchListener(hfpayTradeRecordService);

		lenient()
				.doAnswer(
						inv -> {
							hfpayListener.onEvent(inv.getArgument(0));
							return null;
						})
				.when(ordersTradeFinishDispatchPublisher)
				.publish(any());

		service = new OfflinePaymentDoCheckService(
				companysRedisTemplate,
				sharedStringRedisTemplate,
				offlinePaymentMapper,
				offlineBankAccountMapper,
				normalOrdersMapper,
				tradeMapper,
				orderProcessLogPublishPort,
				ordersTradeFinishDispatchPublisher,
				new ObjectMapper(),
				transactionTemplate);
	}

	@Test
	void executeConfirm_afterTradeReload_invokesHfpayPaySuccessOnce_withOrderIdFromSnakeTradeRow() {
		long paymentId = 30L;
		long companyId = 88L;
		long orderIdNum = 42L;
		String tradeId = "trade-hfpay-probe-1";

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
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

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
		when(tradeMapper.selectById(tradeId)).thenReturn(success);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", paymentId);
		body.put("order_id", String.valueOf(orderIdNum));
		body.put("check_status", 1);
		body.put("bank_account_id", 7L);
		body.put("pay_fee", "100.00");

		service.doCheck(companyId, 1L, "admin", "op", body);

		verify(hfpayTradeRecordService, times(1)).paySuccess(eq(String.valueOf(orderIdNum)));
	}
}
