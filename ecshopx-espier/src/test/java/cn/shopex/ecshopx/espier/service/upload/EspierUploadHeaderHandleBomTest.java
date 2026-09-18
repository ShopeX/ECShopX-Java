package cn.shopex.ecshopx.espier.service.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EspierUploadHeaderHandleBomTest {

	@Test
	void normalizeHeaderCell_stripsLeadingUtf8Bom() {
		assertEquals("订单号", EspierUploadHeaderHandle.normalizeHeaderCell("\uFEFF订单号"));
		assertEquals("订单号", EspierUploadHeaderHandle.normalizeHeaderCell("\u00EF\u00BB\u00BF订单号"));
		assertEquals("订单号", EspierUploadHeaderHandle.normalizeHeaderCell("\uFEFF 订单号"));
	}

	@Test
	void buildColumnMap_whenExportCsvBomOnFirstHeader_mapsOrderId() {
		List<String> headers = List.of("\uFEFF订单号", "快递单号", "快递公司");
		Map<Integer, String> column =
				assertDoesNotThrow(
						() -> EspierUploadHeaderHandle.buildColumnMap(headers, EspierUploadHeaderCatalog.NORMAL_ORDERS()));
		assertEquals("order_id", column.get(0));
		assertEquals("delivery_code", column.get(1));
		assertEquals("delivery_corp_name", column.get(2));
	}
}
