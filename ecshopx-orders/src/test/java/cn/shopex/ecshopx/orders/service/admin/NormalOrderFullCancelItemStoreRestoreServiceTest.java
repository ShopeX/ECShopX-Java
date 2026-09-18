package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.port.order.EmployeePurchaseOrderCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderCancelItemStoreRestorePort;
import cn.shopex.ecshopx.common.port.order.PartialCancelOrderItemRow;
import cn.shopex.ecshopx.common.port.order.PartialCancelPromotionRestorePort;
import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrderFullCancelItemStoreRestoreServiceTest {

	@Mock NormalOrdersMapper normalOrdersMapper;
	@Mock NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock OrderCancelItemStoreRestorePort orderCancelItemStoreRestorePort;
	@Mock PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort;
	@Mock PartialCancelPromotionRestorePort partialCancelPromotionRestorePort;
	@Mock EmployeePurchaseOrderCancelRestorePort employeePurchaseOrderCancelRestorePort;

	private NormalOrderFullCancelItemStoreRestoreService service;

	@BeforeEach
	void setUp() {
		service =
				new NormalOrderFullCancelItemStoreRestoreService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderCancelItemStoreRestorePort,
						pointsmallPartialCancelItemStorePort,
						partialCancelPromotionRestorePort,
						employeePurchaseOrderCancelRestorePort);
	}

	@Test
	void restoreOnCancelSuccess_normalOrder_restoresStoreAndReduceLimitPerson() {
		NormalOrders order = order(1L, 100L, "normal");
		NormalOrdersItems item = item(1L, 100L, 10L, 200L, 3);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));

		service.restoreOnCancelSuccess(1L, 100L, 0L);

		verify(orderCancelItemStoreRestorePort).restoreStore(any(ItemInventoryLineContext.class), eq(3));
		verify(partialCancelPromotionRestorePort).reduceLimitPerson(1L, 10L, 200L, 3);
	}

	@Test
	void restoreOnCancelSuccess_seckill_skipsStoreButStillReduceLimitPerson() {
		NormalOrders order = order(1L, 100L, "seckill");
		NormalOrdersItems item = item(1L, 100L, 10L, 200L, 2);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));

		service.restoreOnCancelSuccess(1L, 100L, 0L);

		verify(orderCancelItemStoreRestorePort, never()).restoreStore(any(), anyInt());
		verify(pointsmallPartialCancelItemStorePort, never())
				.minusItemStore(anyLong(), anyLong(), anyInt(), anyBoolean());
		verify(partialCancelPromotionRestorePort).reduceLimitPerson(1L, 10L, 200L, 2);
	}

	@Test
	void restoreOnCancelSuccess_pointsmall_restoresPointStoreAndReduceLimitPerson() {
		NormalOrders order = order(1L, 100L, "pointsmall");
		NormalOrdersItems item = item(1L, 100L, 10L, 200L, 1);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));

		service.restoreOnCancelSuccess(1L, 100L, 0L);

		verify(pointsmallPartialCancelItemStorePort).minusItemStore(1L, 200L, -1, true);
		verify(orderCancelItemStoreRestorePort, never()).restoreStore(any(), anyInt());
		verify(partialCancelPromotionRestorePort).reduceLimitPerson(1L, 10L, 200L, 1);
	}

	@Test
	void restoreOnCancelSuccess_employeePurchaseShareStore_restoresGenericAndAggregate() {
		NormalOrders order = order(1L, 100L, "employee_purchase");
		NormalOrdersItems item = item(1L, 100L, 10L, 200L, 2);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(employeePurchaseOrderCancelRestorePort.isShareStore(1L, 100L)).thenReturn(true);

		service.restoreOnCancelSuccess(1L, 100L, 0L);

		verify(orderCancelItemStoreRestorePort).restoreStore(any(ItemInventoryLineContext.class), eq(2));
		verify(employeePurchaseOrderCancelRestorePort).restoreOnOrderCancel(100L, 1L);
	}

	@Test
	void restoreOnCancelSuccess_employeePurchaseNonShare_skipsGenericRestoresAggregate() {
		NormalOrders order = order(1L, 100L, "employee_purchase");
		NormalOrdersItems item = item(1L, 100L, 10L, 200L, 2);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(employeePurchaseOrderCancelRestorePort.isShareStore(1L, 100L)).thenReturn(false);

		service.restoreOnCancelSuccess(1L, 100L, 0L);

		verify(orderCancelItemStoreRestorePort, never()).restoreStore(any(), anyInt());
		verify(employeePurchaseOrderCancelRestorePort).restoreOnOrderCancel(100L, 1L);
	}

	@Test
	void restoreOnCancelSuccess_limitedTimeSaleQuotaUsesActivityPriceTimesQuantity() {
		NormalOrders order = order(1L, 100L, "normal");
		NormalOrdersItems item = item(1L, 100L, 10L, 24385L, 2);
		item.setPrice(800);
		item.setDiscountFee(1400);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));

		service.restoreOnCancelSuccess(1L, 100L, 0L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<PartialCancelOrderItemRow>> captor = ArgumentCaptor.forClass(List.class);
		verify(partialCancelPromotionRestorePort).restoreLimitedTimeSaleUserBuys(eq(1L), eq(100L), captor.capture());
		PartialCancelOrderItemRow row = captor.getValue().get(0);
		Assertions.assertEquals(24385L, row.itemId());
		Assertions.assertEquals(2, row.cancelItemNum());
		Assertions.assertEquals(100, row.priceFen());
	}

	private static NormalOrders order(long companyId, long orderId, String orderClass) {
		NormalOrders o = new NormalOrders();
		o.setCompanyId(companyId);
		o.setOrderId(orderId);
		o.setOrderClass(orderClass);
		o.setReceiptType("express");
		return o;
	}

	private static NormalOrdersItems item(
			long companyId, long orderId, long userId, long itemId, int num) {
		NormalOrdersItems i = new NormalOrdersItems();
		i.setCompanyId(companyId);
		i.setOrderId(orderId);
		i.setUserId(userId);
		i.setItemId(itemId);
		i.setNum(num);
		i.setDistributorId(0L);
		i.setIsTotalStore(true);
		return i;
	}
}
