package cn.shopex.ecshopx.orders.service.orderexport.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.shopex.ecshopx.orders.service.supplier.OrdersShopexErpSettingRedisReader;
import cn.shopex.ecshopx.orders.service.supplier.SupplierOrderListRowMaps;
import cn.shopex.ecshopx.orders.service.supplier.SupplierOrderStatusMessageService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SupplierOrderExportCsvServiceStatusTest {

	@Test
	void paidCancelPendingMatchesListRefundProcessing() {
		assertMatchesList("PAYED", "PENDING", "NOTZITI", "WAIT_PROCESS", false, "退款处理中");
	}

	@Test
	void paidPickupPendingMatchesListWaitPickup() {
		assertMatchesList("PAYED", "PENDING", "PENDING", "NO_APPLY_CANCEL", false, "待自提");
	}

	@Test
	void paidWithoutDeliveryMatchesListReviewingWhenErpOpen() {
		assertMatchesList("PAYED", "PENDING", "NOTZITI", "NO_APPLY_CANCEL", true, "审核中");
	}

	@Test
	void paidWithoutDeliveryMatchesListWaitDeliveryWhenErpClosed() {
		assertMatchesList("PAYED", "PENDING", "NOTZITI", "NO_APPLY_CANCEL", false, "待发货");
	}

	@Test
	void cancelAfterPickupDoneMatchesListClosed() {
		assertMatchesList("CANCEL", "PENDING", "DONE", "SUCCESS", false, "已关闭");
	}

	@Test
	void cancelRefundProcessMatchesList() {
		assertMatchesList("CANCEL", "PENDING", "NOTZITI", "REFUND_PROCESS", false, "退款处理中");
	}

	@Test
	void cancelRefundFailMatchesListWaitRefund() {
		assertMatchesList("CANCEL", "PENDING", "NOTZITI", "FAILS", false, "等待退款");
	}

	@Test
	void unknownStatusMatchesListExceptionWithoutRawCode() {
		assertMatchesList("FOO", "PENDING", "NOTZITI", "NO_APPLY_CANCEL", false, "订单异常");
	}

	private static void assertMatchesList(
			String orderStatus,
			String deliveryStatus,
			String zitiStatus,
			String cancelStatus,
			boolean erpOpen,
			String expected) {
		SupplierOrder order = new SupplierOrder();
		order.setCompanyId(1L);
		order.setOrderStatus(orderStatus);
		order.setDeliveryStatus(deliveryStatus);
		order.setZitiStatus(zitiStatus);
		order.setCancelStatus(cancelStatus);
		order.setReceiptType("logistics");

		SupplierOrderStatusMessageService listStatus =
				new SupplierOrderStatusMessageService(new FixedErpReader(erpOpen));
		Map<String, Object> row = SupplierOrderListRowMaps.toRow(order);
		row.putIfAbsent("prescription_status", 0);
		row.putIfAbsent("diagnosis_data", null);

		String exported = SupplierOrderExportCsvService.resolveOrderStatusMsg(order, listStatus);
		assertEquals(listStatus.getOrderStatusMsgForSupplier(row), exported);
		assertEquals(expected, exported);
	}

	private static final class FixedErpReader extends OrdersShopexErpSettingRedisReader {
		private final boolean open;

		private FixedErpReader(boolean open) {
			super(null, new ObjectMapper());
			this.open = open;
		}

		@Override
		public boolean isErpOpen(long companyId) {
			return open;
		}
	}
}
