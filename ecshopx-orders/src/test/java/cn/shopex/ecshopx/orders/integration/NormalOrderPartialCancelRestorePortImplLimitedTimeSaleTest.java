package cn.shopex.ecshopx.orders.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.excard.ExcardInventoryPort;
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
class NormalOrderPartialCancelRestorePortImplLimitedTimeSaleTest {

	@Mock NormalOrdersMapper normalOrdersMapper;
	@Mock NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock ExcardInventoryPort excardInventoryPort;
	@Mock PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort;
	@Mock PartialCancelPromotionRestorePort partialCancelPromotionRestorePort;

	private NormalOrderPartialCancelRestorePortImpl port;

	@BeforeEach
	void setUp() {
		port =
				new NormalOrderPartialCancelRestorePortImpl(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						excardInventoryPort,
						pointsmallPartialCancelItemStorePort,
						partialCancelPromotionRestorePort);
	}

	@Test
	void partialCancelRestore_limitedTimeSaleQuotaUsesActivityPriceTimesQuantity() {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(100L);
		order.setUserId(10L);
		order.setOrderClass("normal");
		order.setReceiptType("logistics");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems item = new NormalOrdersItems();
		item.setCompanyId(1L);
		item.setUserId(10L);
		item.setItemId(24385L);
		item.setNum(2);
		item.setCancelItemNum(1);
		item.setPrice(800);
		item.setDiscountFee(1400);
		item.setDistributorId(0L);
		item.setIsTotalStore(true);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));

		port.partialCancelRestore(1L, 100L, true);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<PartialCancelOrderItemRow>> captor = ArgumentCaptor.forClass(List.class);
		verify(partialCancelPromotionRestorePort).restoreLimitedTimeSaleUserBuys(eq(1L), eq(100L), captor.capture());
		PartialCancelOrderItemRow row = captor.getValue().get(0);
		Assertions.assertEquals(24385L, row.itemId());
		Assertions.assertEquals(1, row.cancelItemNum());
		Assertions.assertEquals(100, row.priceFen());
	}
}
