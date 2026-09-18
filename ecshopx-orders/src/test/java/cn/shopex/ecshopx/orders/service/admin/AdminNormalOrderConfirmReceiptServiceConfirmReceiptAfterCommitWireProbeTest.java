package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptAfterCommitService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptTransactionService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminNormalOrderConfirmReceiptServiceConfirmReceiptAfterCommitWireProbeTest {

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private NormalOrdersServiceOrderDataAssembler assembler;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	private NormalOrderConfirmReceiptTransactionService transactionService;

	@Mock
	private NormalOrderConfirmReceiptAfterCommitService afterCommitService;

	@Captor
	private ArgumentCaptor<Integer> nowSecCaptor;

	@Captor
	private ArgumentCaptor<Long> autoCloseCaptor;

	private AdminNormalOrderConfirmReceiptService underTest;

	@BeforeEach
	void setUp() {
		underTest = new AdminNormalOrderConfirmReceiptService(
				orderAssociationsMapper,
				normalOrdersMapper,
				normalOrdersItemsMapper,
				assembler,
				orderValiditySettingRedisReadService,
				transactionService,
				afterCommitService);
	}

	@Test
	void confirmReceipt_afterSuccessfulTransaction_invokesAfterCommitRunWithAdminOperator() {
		long companyId = 100L;
		long operatorId = 999L;
		String orderIdRaw = "20001";
		long orderIdNum = 20001L;

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(companyId);
		assoc.setOrderId(orderIdNum);
		assoc.setOrderType("normal");
		when(orderAssociationsMapper.selectList(any())).thenReturn(List.of(assoc));

		NormalOrders preRow = new NormalOrders();
		preRow.setCompanyId(companyId);
		preRow.setOrderId(orderIdNum);
		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderIdNum);
		order.setUserId(10L);
		order.setBonusPoints(42);

		AtomicInteger normalOrdersSelectCalls = new AtomicInteger();
		when(normalOrdersMapper.selectList(any()))
				.thenAnswer(
						invocation -> {
							int n = normalOrdersSelectCalls.incrementAndGet();
							if (n == 1) {
								return List.of(preRow);
							}
							if (n == 2) {
								return List.of(order);
							}
							return List.of();
						});

		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("order_status", "WAIT_BUYER_CONFIRM");
		orderInfo.put("delivery_status", "DONE");
		orderInfo.put("cancel_status", "FAILS");
		when(assembler.toServiceOrderData(order)).thenReturn(orderInfo);

		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(0L);

		Map<String, Object> validity = new LinkedHashMap<>();
		validity.put("latest_aftersale_time", 7);
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId)).thenReturn(validity);

		NormalOrders fresh = new NormalOrders();
		fresh.setCompanyId(companyId);
		fresh.setOrderId(orderIdNum);
		when(transactionService.applyConfirmReceiptInTransaction(
						eq(companyId), eq(orderIdNum), eq(orderIdRaw), eq(order), anyLong(), anyInt()))
				.thenReturn(fresh);

		Map<String, Object> freshView = new LinkedHashMap<>();
		freshView.put("order_status", "DONE");
		when(assembler.toServiceOrderData(fresh)).thenReturn(freshView);

		underTest.confirmReceipt(companyId, operatorId, orderIdRaw);

		verify(afterCommitService, times(1))
				.run(
						eq(companyId),
						eq(orderIdNum),
						eq(fresh),
						nowSecCaptor.capture(),
						autoCloseCaptor.capture(),
						eq("admin"),
						eq(operatorId),
						eq(order.getBonusPoints()));

		long epoch = Instant.now().getEpochSecond();
		int nowSec = nowSecCaptor.getValue();
		assertThat(nowSec).isBetween((int) (epoch - 120), (int) (epoch + 120));

		long autoClose = autoCloseCaptor.getValue();
		int days = 7;
		assertThat(autoClose).isBetween(epoch + days * 86400L - 300, epoch + days * 86400L + 300);
	}
}
