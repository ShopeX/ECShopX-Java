package cn.shopex.ecshopx.espier.service.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EspierUploadFileQueuedPayloadTest {

	@Test
	void fromPersistedRow_readsOperatorTypeForQueuedReplay() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", 42L);
		result.put("company_id", 9L);
		result.put("operator_id", 3L);
		result.put("supplier_id", 0L);
		result.put("distributor_id", 0L);
		result.put("merchant_id", 0L);
		result.put("file_type", "normal_orders_cancel");
		result.put("operator_type", "merchant");

		EspierUploadFileQueuedPayload p = EspierUploadFileQueuedPayload.fromPersistedRow(result, "storage/a.xlsx", "normal_orders_cancel");

		assertEquals("merchant", p.getOperatorType());
		assertEquals(42L, p.getId());
	}

	@Test
	void fromPersistedRow_missingOperatorType_defaultsToEmptyString() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", 1L);
		result.put("company_id", 1L);
		result.put("operator_id", 1L);
		result.put("supplier_id", 0L);
		result.put("distributor_id", 0L);
		result.put("merchant_id", 0L);
		result.put("file_type", "member_info");

		EspierUploadFileQueuedPayload p = EspierUploadFileQueuedPayload.fromPersistedRow(result, "p", "member_info");

		assertEquals("", p.getOperatorType());
	}
}
