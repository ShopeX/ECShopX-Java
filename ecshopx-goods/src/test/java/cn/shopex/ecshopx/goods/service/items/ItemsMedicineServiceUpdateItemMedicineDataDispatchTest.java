package cn.shopex.ecshopx.goods.service.items;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.goods.dispatch.MedicineItemsSubmitAuditDispatchPublisher;
import cn.shopex.ecshopx.goods.domain.ItemsMedicine;
import cn.shopex.ecshopx.goods.repository.ItemsMedicineRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsMedicineServiceUpdateItemMedicineDataDispatchTest {

	private static final long COMPANY_ID = 81001L;
	private static final long ITEM_ID = 91001L;

	private ItemsMedicineRepository itemsMedicineRepository;
	private ItemsRepository itemsRepository;
	private ItemsListMultiLangApplier itemsListMultiLangApplier;
	private MedicineItemsSubmitAuditDispatchPublisher medicineItemsSubmitAuditDispatchPublisher;
	private LangueProperties langueProperties;

	private ItemsMedicineService sut;

	@BeforeEach
	void setUp() {
		itemsMedicineRepository = mock(ItemsMedicineRepository.class);
		itemsRepository = mock(ItemsRepository.class);
		itemsListMultiLangApplier = mock(ItemsListMultiLangApplier.class);
		medicineItemsSubmitAuditDispatchPublisher = mock(MedicineItemsSubmitAuditDispatchPublisher.class);
		langueProperties = mock(LangueProperties.class);
		sut = new ItemsMedicineService(
				itemsMedicineRepository,
				itemsRepository,
				itemsListMultiLangApplier,
				medicineItemsSubmitAuditDispatchPublisher,
				langueProperties);
	}

	@Test
	void updateItemMedicineData_whenAuditStatusPositive_dispatchesPublishMedicineItemSubmitAudit() {
		Map<String, Object> medicineData = new LinkedHashMap<>();
		medicineData.put("is_prescription", 1);

		Map<String, Object> skuParams = new LinkedHashMap<>();
		skuParams.put("spec_name", "500mg");
		skuParams.put("max_num", 10);
		skuParams.put("barcode", "6900001");
		skuParams.put("item_name", "from-sku-name");

		Map<String, Object> itemsResult = new LinkedHashMap<>();
		itemsResult.put("item_id", ITEM_ID);
		itemsResult.put("company_id", COMPANY_ID);
		itemsResult.put("price", 1999);

		ItemsMedicine reloaded = new ItemsMedicine();
		reloaded.setItemId(ITEM_ID);
		reloaded.setCompanyId(COMPANY_ID);
		reloaded.setAuditStatus(1);

		when(itemsMedicineRepository.mapByItemIds(eq(COMPANY_ID), any())).thenReturn(Map.of(ITEM_ID, reloaded));

		sut.updateItemMedicineData(skuParams, medicineData, itemsResult);

		verify(medicineItemsSubmitAuditDispatchPublisher, times(1))
				.publishMedicineItemSubmitAudit(
						argThat(
								p ->
										p.companyId() == COMPANY_ID
												&& p.itemId() == ITEM_ID
												&& p.itemName().equals("from-sku-name")
												&& p.barcode().equals("6900001")
												&& p.price() == 1999
												&& p.medicineRow() == reloaded));
	}

	@Test
	void updateItemMedicineData_whenAuditStatusZeroOrNull_doesNotDispatch() {
		Map<String, Object> medicineData = Map.of("is_prescription", 1);
		Map<String, Object> skuParams = new LinkedHashMap<>();
		skuParams.put("spec_name", "500mg");
		skuParams.put("max_num", 10);
		Map<String, Object> itemsResult = new LinkedHashMap<>();
		itemsResult.put("item_id", ITEM_ID);
		itemsResult.put("company_id", COMPANY_ID);
		itemsResult.put("price", 100);

		ItemsMedicine zero = new ItemsMedicine();
		zero.setItemId(ITEM_ID);
		zero.setCompanyId(COMPANY_ID);
		zero.setAuditStatus(0);
		when(itemsMedicineRepository.mapByItemIds(eq(COMPANY_ID), any())).thenReturn(Map.of(ITEM_ID, zero));

		sut.updateItemMedicineData(skuParams, medicineData, itemsResult);
		verifyNoInteractions(medicineItemsSubmitAuditDispatchPublisher);

		ItemsMedicine nilAudit = new ItemsMedicine();
		nilAudit.setItemId(ITEM_ID);
		nilAudit.setCompanyId(COMPANY_ID);
		nilAudit.setAuditStatus(null);
		when(itemsMedicineRepository.mapByItemIds(eq(COMPANY_ID), any())).thenReturn(Map.of(ITEM_ID, nilAudit));

		sut.updateItemMedicineData(skuParams, medicineData, itemsResult);
		verify(medicineItemsSubmitAuditDispatchPublisher, never()).publishMedicineItemSubmitAudit(any());
	}
}
