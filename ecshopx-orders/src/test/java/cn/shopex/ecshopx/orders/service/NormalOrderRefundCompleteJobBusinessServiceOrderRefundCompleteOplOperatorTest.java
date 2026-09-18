package cn.shopex.ecshopx.orders.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.service.AftersalesDetailAggregateService;
import cn.shopex.ecshopx.common.port.order.OrderCancelUserDiscountRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptAfterCommitService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptTransactionService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrderRefundCompleteJobBusinessServiceOrderRefundCompleteOplOperatorTest {

	private static final long COMPANY_ID = 10L;
	private static final long ORDER_ID = 9001L;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private AftersalesDetailAggregateService aftersalesDetailAggregateService;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	private NormalOrderConfirmReceiptTransactionService normalOrderConfirmReceiptTransactionService;

	@Mock
	private NormalOrderConfirmReceiptAfterCommitService normalOrderConfirmReceiptAfterCommitService;

	@Mock
	private OrderCancelUserDiscountRestorePort orderCancelUserDiscountRestorePort;

	@Mock
	private DmCrmSettingReadPort dmCrmSettingReadPort;

	private NormalOrderRefundCompleteJobBusinessService underTest;

	@BeforeEach
	void setUp() {
		underTest =
				new NormalOrderRefundCompleteJobBusinessService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						aftersalesDetailAggregateService,
						orderValiditySettingRedisReadService,
						normalOrderConfirmReceiptTransactionService,
						normalOrderConfirmReceiptAfterCommitService,
						orderCancelUserDiscountRestorePort,
						dmCrmSettingReadPort,
						new ObjectMapper());
	}

	@Test
	void execute_whenAutoConfirmReceipt_succeeds_invokesAfterCommitWithUserOperatorAndBuyerUserId() {
		NormalOrders order = baseWaitBuyerConfirmOrder();
		order.setUserId(1L);
		Integer bonusPoints = 3;
		order.setBonusPoints(bonusPoints);

		NormalOrdersItems line = lineFullyDelivered(20L, 5, 5, 0);
		stubMappersAndValidity(order, List.of(line));
		when(aftersalesDetailAggregateService.getAppliedNum(COMPANY_ID, String.valueOf(ORDER_ID), 20L))
				.thenReturn(5);

		NormalOrders fresh = new NormalOrders();
		fresh.setOrderId(ORDER_ID);
		fresh.setCompanyId(COMPANY_ID);
		fresh.setUserId(88L);
		when(normalOrderConfirmReceiptTransactionService.applyConfirmReceiptInTransaction(
						eq(COMPANY_ID),
						eq(ORDER_ID),
						eq(String.valueOf(ORDER_ID)),
						eq(order),
						anyLong(),
						anyInt()))
				.thenReturn(fresh);
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(true);

		underTest.execute(COMPANY_ID, ORDER_ID);

		verify(normalOrderConfirmReceiptAfterCommitService, times(1))
				.run(
						eq(COMPANY_ID),
						eq(ORDER_ID),
						same(fresh),
						anyInt(),
						anyLong(),
						eq("user"),
						eq(88L),
						eq(bonusPoints));
	}

	@Test
	void execute_whenBuyerUserIdMissing_invokesAfterCommitWithUserOperatorAndZeroOperatorId() {
		assertAfterCommitUsesZeroOperatorIdWhenFreshUserId(null);
		clearInvocations(normalOrderConfirmReceiptAfterCommitService);
		assertAfterCommitUsesZeroOperatorIdWhenFreshUserId(0L);
		clearInvocations(normalOrderConfirmReceiptAfterCommitService);
		assertAfterCommitUsesZeroOperatorIdWhenFreshUserId(-3L);
	}

	private void assertAfterCommitUsesZeroOperatorIdWhenFreshUserId(Long freshUserId) {
		NormalOrders order = baseWaitBuyerConfirmOrder();
		order.setUserId(1L);

		NormalOrdersItems line = lineFullyDelivered(99L, 2, 2, 0);
		stubMappersAndValidity(order, List.of(line));
		when(aftersalesDetailAggregateService.getAppliedNum(COMPANY_ID, String.valueOf(ORDER_ID), 99L))
				.thenReturn(2);

		NormalOrders fresh = new NormalOrders();
		fresh.setOrderId(ORDER_ID);
		fresh.setCompanyId(COMPANY_ID);
		fresh.setUserId(freshUserId);
		when(normalOrderConfirmReceiptTransactionService.applyConfirmReceiptInTransaction(
						anyLong(), anyLong(), anyString(), any(), anyLong(), anyInt()))
				.thenReturn(fresh);
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(true);

		underTest.execute(COMPANY_ID, ORDER_ID);

		verify(normalOrderConfirmReceiptAfterCommitService, times(1))
				.run(
						eq(COMPANY_ID),
						eq(ORDER_ID),
						same(fresh),
						anyInt(),
						anyLong(),
						eq("user"),
						eq(0L),
						any());
	}

	private void stubMappersAndValidity(NormalOrders order, List<NormalOrdersItems> lines) {
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(lines);
		when(orderValiditySettingRedisReadService.readPlatformSetting(COMPANY_ID))
				.thenReturn(Map.of("latest_aftersale_time", 7));
	}

	private static NormalOrders baseWaitBuyerConfirmOrder() {
		NormalOrders order = new NormalOrders();
		order.setOrderId(ORDER_ID);
		order.setCompanyId(COMPANY_ID);
		order.setOrderStatus("WAIT_BUYER_CONFIRM");
		order.setCancelStatus("FAILS");
		order.setDeliveryStatus("DONE");
		order.setReceiptType("logistics");
		order.setZitiStatus("NOTZITI");
		return order;
	}

	private static NormalOrdersItems lineFullyDelivered(long id, int num, int deliveryNum, int cancelNum) {
		NormalOrdersItems line = new NormalOrdersItems();
		line.setId(id);
		line.setOrderId(ORDER_ID);
		line.setCompanyId(COMPANY_ID);
		line.setNum(num);
		line.setDeliveryItemNum(deliveryNum);
		line.setCancelItemNum(cancelNum);
		line.setAutoCloseAftersalesTime(0);
		return line;
	}
}
