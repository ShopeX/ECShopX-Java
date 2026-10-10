package cn.shopex.ecshopx.orders.service.orderexport.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class NormalMasterOrderExportCsvServiceDeliveryStatusTest {

	@Test
	void mapDeliveryStatus_partialShipment_isPartialLabel() throws Exception {
		assertEquals("部分发货", mapDeliveryStatus("PARTAIL"));
	}

	@Test
	void mapDeliveryStatus_doneAndPending_keepExistingLabels() throws Exception {
		assertEquals("已发货", mapDeliveryStatus("DONE"));
		assertEquals("未发货", mapDeliveryStatus("PENDING"));
		assertEquals("未发货", mapDeliveryStatus(""));
		assertEquals("未发货", mapDeliveryStatus(null));
	}

	private static String mapDeliveryStatus(String deliveryStatus) throws Exception {
		Method m =
				NormalMasterOrderExportCsvService.class.getDeclaredMethod(
						"mapDeliveryStatus", String.class);
		m.setAccessible(true);
		return (String) m.invoke(null, deliveryStatus);
	}
}
