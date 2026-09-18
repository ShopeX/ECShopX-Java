package cn.shopex.ecshopx.orders.service.espier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
class NormalOrdersUploadImportRowServicePlatformIgnoresRowSupplierIdTest {

	@Mock NormalOrdersMapper normalOrdersMapper;
	@Mock SupplierOrderMapper supplierOrderMapper;
	@Mock NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver;
	@Mock NormalOrderEspierBatchDeliveryService batchDeliveryService;

	@InjectMocks NormalOrdersUploadImportRowService service;

	@Test
	void acceptRow_whenPlatformOperator_ignoresExcelSupplierIdAndShipsAsSelf() {
		long companyId = 1L;
		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(9003L);
		order.setOrderStatus("PAYED");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(deliveryCorpResolver.resolveDeliveryCorpCode(eq(companyId), eq("顺丰"), eq(0L)))
				.thenReturn("SF");

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("order_id", "9003");
		row.put("delivery_code", "SF123");
		row.put("delivery_corp_name", "顺丰");
		row.put("supplier_id", "18");

		// JWT supplierId = 0 → 平台
		service.acceptRow(companyId, 1L, 0L, 0L, 0L, row, "admin");

		verify(batchDeliveryService).deliverBatchForUpload(companyId, 9003L, 0L, "SF", "SF123");
		verify(supplierOrderMapper, never()).selectOne(any());
	}
}
