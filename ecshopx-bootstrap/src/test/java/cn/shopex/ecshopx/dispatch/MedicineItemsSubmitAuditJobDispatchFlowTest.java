package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.GoodsBundleDispatchJobNames;
import cn.shopex.ecshopx.config.MedicineItemsSubmitAuditDispatchPublisherImpl;
import cn.shopex.ecshopx.goods.dispatch.MedicineItemsSubmitAuditJobHandler;
import cn.shopex.ecshopx.goods.domain.ItemsMedicine;
import cn.shopex.ecshopx.goods.repository.ItemsMedicineRepository;
import cn.shopex.ecshopx.goods.service.items.MedicineSubmitAuditPayload;
import cn.shopex.ecshopx.thirdparty.service.kuaizhen580.MedicineSyncHttpClient;
import cn.shopex.ecshopx.thirdparty.service.kuaizhen580.MedicineSyncMedicineItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MedicineItemsSubmitAuditJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesMedicineSyncHttpClient() {
		MedicineSyncHttpClient httpClient = mock(MedicineSyncHttpClient.class);
		ItemsMedicineRepository repo = mock(ItemsMedicineRepository.class);
		MedicineItemsSubmitAuditJobHandler handler = new MedicineItemsSubmitAuditJobHandler(httpClient, repo);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.MEDICINE_ITEMS_SUBMIT_AUDIT, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		LinkedHashMap<String, Object> medicineData = new LinkedHashMap<>();
		medicineData.put("medicine_type", 0);
		medicineData.put("common_name", "acetaminophen");
		medicineData.put("dosage", "tab");
		medicineData.put("spec", "500mg");
		medicineData.put("packing_spec", "10");
		medicineData.put("manufacturer", "Pharma");
		medicineData.put("approval_number", "H123");
		medicineData.put("unit", "box");
		medicineData.put("is_prescription", 1);
		medicineData.put("special_common_name", "alias");
		medicineData.put("special_spec", "alt");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 99L);
		payload.put("item_id", 1001L);
		payload.put("item_name", "ShelfA");
		payload.put("barcode", "690123456");
		payload.put("price", 2500);
		payload.put("medicine_data", medicineData);

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.MEDICINE_ITEMS_SUBMIT_AUDIT,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(GoodsBundleDispatchJobNames.MEDICINE_ITEMS_SUBMIT_AUDIT, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(99L, asLong(got.get("company_id")));
		assertEquals(1001L, asLong(got.get("item_id")));
		assertEquals("ShelfA", got.get("item_name"));
		assertEquals("690123456", got.get("barcode"));
		assertEquals(2500, ((Number) got.get("price")).intValue());
		@SuppressWarnings("unchecked")
		Map<String, Object> gotMed = (Map<String, Object>) got.get("medicine_data");
		assertEquals(0, ((Number) gotMed.get("medicine_type")).intValue());
		assertEquals("acetaminophen", gotMed.get("common_name"));
		assertEquals("tab", gotMed.get("dosage"));
		assertEquals("500mg", gotMed.get("spec"));
		assertEquals("10", gotMed.get("packing_spec"));
		assertEquals("Pharma", gotMed.get("manufacturer"));
		assertEquals("H123", gotMed.get("approval_number"));
		assertEquals("box", gotMed.get("unit"));
		assertEquals(1, ((Number) gotMed.get("is_prescription")).intValue());
		assertEquals("alias", gotMed.get("special_common_name"));
		assertEquals("alt", gotMed.get("special_spec"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MedicineSyncMedicineItem>> captor = ArgumentCaptor.forClass(List.class);
		verify(httpClient, times(1)).medicineSync(eq(99L), captor.capture());
		List<MedicineSyncMedicineItem> list = captor.getValue();
		assertEquals(1, list.size());
		MedicineSyncMedicineItem it = list.get(0);
		assertEquals(Integer.valueOf(0), it.categoryId());
		assertEquals("acetaminophen", it.commonName());
		assertEquals("ShelfA", it.name());
		assertEquals("tab", it.dosage());
		assertEquals("500mg", it.spec());
		assertEquals("10", it.packingSpec());
		assertEquals("Pharma", it.manufacturer());
		assertEquals("H123", it.approvalNumber());
		assertEquals("box", it.unit());
		assertEquals(Long.valueOf(1001L), it.medicineId());
		assertEquals("690123456", it.barCode());
		assertEquals(Integer.valueOf(0), it.isPrescription());
		assertEquals("2500", it.price());
		assertEquals("", it.stock());
		assertEquals("alias", it.specialCommonName());
		assertEquals("alt", it.specialSpec());
	}

	@Test
	void dispatchJob_publishPayloadMatchesMedicineItemsSubmitAuditEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		MedicineItemsSubmitAuditDispatchPublisherImpl publisher =
				new MedicineItemsSubmitAuditDispatchPublisherImpl(dispatchFacade);

		ItemsMedicine m = new ItemsMedicine();
		m.setMedicineType(3);
		m.setCommonName("commonX");
		m.setDosage("capsule");
		m.setSpec("100iu");
		m.setPackingSpec("blister");
		m.setManufacturer("Maker");
		m.setApprovalNumber("ZH999");
		m.setUnit("pack");
		m.setIsPrescription(0);
		m.setSpecialCommonName("specCn");
		m.setSpecialSpec("specSp");

		MedicineSubmitAuditPayload payload = new MedicineSubmitAuditPayload(55L, 404L, "ItemTitle", "BAR009", 77, m);
		publisher.publishMedicineItemSubmitAudit(payload);

		verify(dispatchFacade)
				.dispatchJob(
						eq(GoodsBundleDispatchJobNames.MEDICINE_ITEMS_SUBMIT_AUDIT),
						argThat(
								map -> {
									if (55L != asLong(map.get("company_id"))) {
										return false;
									}
									if (404L != asLong(map.get("item_id"))) {
										return false;
									}
									if (!"ItemTitle".equals(map.get("item_name"))
											|| !"BAR009".equals(map.get("barcode"))) {
										return false;
									}
									if (!(map.get("price") instanceof Number n) || n.intValue() != 77) {
										return false;
									}
									Object md = map.get("medicine_data");
									if (!(md instanceof Map<?, ?> med)) {
										return false;
									}
									if (3 != asInt(med.get("medicine_type"))) {
										return false;
									}
									if (!"commonX".equals(med.get("common_name"))
											|| !"capsule".equals(med.get("dosage"))
											|| !"100iu".equals(med.get("spec"))
											|| !"blister".equals(med.get("packing_spec"))
											|| !"Maker".equals(med.get("manufacturer"))
											|| !"ZH999".equals(med.get("approval_number"))
											|| !"pack".equals(med.get("unit"))) {
										return false;
									}
									if (0 != asInt(med.get("is_prescription"))) {
										return false;
									}
									if (!"specCn".equals(med.get("special_common_name"))
											|| !"specSp".equals(med.get("special_spec"))) {
										return false;
									}
									return true;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static int asInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}
}
