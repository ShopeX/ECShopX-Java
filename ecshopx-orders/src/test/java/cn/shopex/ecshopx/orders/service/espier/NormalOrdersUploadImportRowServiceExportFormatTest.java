package cn.shopex.ecshopx.orders.service.espier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrdersUploadImportRowServiceExportFormatTest {

	@Mock NormalOrdersMapper normalOrdersMapper;
	@Mock SupplierOrderMapper supplierOrderMapper;
	@Mock NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver;
	@Mock NormalOrderEspierBatchDeliveryService batchDeliveryService;

	@InjectMocks NormalOrdersUploadImportRowService service;

	@Test
	void acceptRow_whenOrderIdHasExportQuotesAndTab_parsesAndShips() {
		stubOrderAndCorp(9003L);
		Map<String, Object> row = baseRow("\"9003\t\"", " ");
		service.acceptRow(1L, 1L, 0L, 0L, 0L, row, "admin");
		verify(batchDeliveryService).deliverBatchForUpload(1L, 9003L, 0L, "SF", "SF123");
	}

	@Test
	void acceptRow_whenOrderIdHasLeadingBom_parsesAndShips() {
		stubOrderAndCorp(9003L);
		Map<String, Object> row = baseRow("\uFEFF9003", "");
		service.acceptRow(1L, 1L, 0L, 0L, 0L, row, "admin");
		verify(batchDeliveryService).deliverBatchForUpload(1L, 9003L, 0L, "SF", "SF123");
	}

	@Test
	void acceptRow_whenExcelSupplierIdIsBlank_treatsAsZeroAndShipsSelf() {
		stubOrderAndCorp(9003L);
		Map<String, Object> row = baseRow("9003", "  ");
		service.acceptRow(1L, 1L, 0L, 0L, 0L, row, "admin");
		verify(batchDeliveryService).deliverBatchForUpload(1L, 9003L, 0L, "SF", "SF123");
	}

	private void stubOrderAndCorp(long orderId) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(orderId);
		order.setOrderStatus("PAYED");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(deliveryCorpResolver.resolveDeliveryCorpCode(eq(1L), eq("顺丰"), eq(0L))).thenReturn("SF");
	}

	private static Map<String, Object> baseRow(String orderId, String supplierId) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("order_id", orderId);
		row.put("delivery_code", "SF123");
		row.put("delivery_corp_name", "顺丰");
		row.put("supplier_id", supplierId);
		return row;
	}
}
