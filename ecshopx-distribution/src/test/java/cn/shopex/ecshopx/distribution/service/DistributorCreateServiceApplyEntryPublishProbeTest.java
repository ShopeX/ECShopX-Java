package cn.shopex.ecshopx.distribution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DistributorCreateEventDispatchPublisher;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorCreateServiceApplyEntryPublishProbeTest {

	private static final long COMPANY_ID = 42L;

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private DistributorWriteRepository distributorWriteRepository;

	@Mock
	private DistributorMultiLangWriteService distributorMultiLangWriteService;

	@Mock
	private DistributorCreateEventDispatchPublisher publisher;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private DistributorCreateService service;

	private final Distributor[] insertedHolder = new Distributor[1];

	@BeforeEach
	void setUp() {
		service = new DistributorCreateService(
				distributorMapper,
				distributorWriteRepository,
				distributorMultiLangWriteService,
				publisher,
				objectMapper);
	}

	@Test
	@SuppressWarnings("unchecked")
	void performInsertAndEvents_thenPublishReceivesApiRowWithCompanyAndDistributorId() {
		when(distributorWriteRepository.existsOtherWithMobile(eq(COMPANY_ID), anyString(), eq(null))).thenReturn(false);
		when(distributorMapper.insert(any(Distributor.class))).thenAnswer(inv -> {
			Distributor d = inv.getArgument(0);
			insertedHolder[0] = d;
			d.setDistributorId(999L);
			return 1;
		});
		doNothing().when(distributorMultiLangWriteService).applyAfterInsert(anyLong(), anyLong(), any(), anyString());

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", COMPANY_ID);
		merged.put("mobile", "13800138000");
		merged.put("source_from", 2);
		merged.put("name", "Probe Shop");
		Map<String, Object> user = Map.of();

		Map<String, Object> returned = service.performInsertAndEvents(merged, user, "zh-CN");

		ArgumentCaptor<Map<String, Object>> rowCaptor = ArgumentCaptor.forClass(Map.class);
		verify(publisher).publish(rowCaptor.capture());

		Map<String, Object> published = rowCaptor.getValue();
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("distributor_id")).isEqualTo(999L);
		assertThat(published.get("source_from")).isEqualTo(2);
		assertThat(returned).isSameAs(published);

		Distributor entity = insertedHolder[0];
		assertThat(entity).isNotNull();
		Map<String, Object> expectedRow = DistributorRowMaps.toApiRow(entity, objectMapper);
		assertThat(published).isEqualTo(expectedRow);
	}

	@Test
	@SuppressWarnings("unchecked")
	void performInsertAndEvents_distributorShopCreateAliasMergedShape_thenPublisherReceivesRowWithHourSourceFromAndIsDistributorFlag() {
		when(distributorWriteRepository.existsOtherWithMobile(eq(COMPANY_ID), anyString(), eq(null))).thenReturn(false);
		when(distributorMapper.insert(any(Distributor.class))).thenAnswer(inv -> {
			Distributor d = inv.getArgument(0);
			insertedHolder[0] = d;
			d.setDistributorId(999L);
			return 1;
		});
		doNothing().when(distributorMultiLangWriteService).applyAfterInsert(anyLong(), anyLong(), any(), anyString());

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", COMPANY_ID);
		merged.put("mobile", "13800138000");
		merged.put("name", "Shop Alias");
		merged.put("source_from", 1);
		merged.put("hour", "09:00 - 18:00");
		merged.put("is_distributor", Boolean.FALSE);
		Map<String, Object> user = Map.of();

		Map<String, Object> returned = service.performInsertAndEvents(merged, user, "zh-CN");

		ArgumentCaptor<Map<String, Object>> rowCaptor = ArgumentCaptor.forClass(Map.class);
		verify(publisher).publish(rowCaptor.capture());

		Map<String, Object> published = rowCaptor.getValue();
		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("distributor_id")).isEqualTo(999L);
		assertThat(published.get("source_from")).isEqualTo(1);
		assertThat(published.get("hour")).isEqualTo("09:00 - 18:00");
		assertThat(published.get("is_distributor")).isEqualTo(false);
		assertThat(returned).isSameAs(published);

		Distributor entity = insertedHolder[0];
		assertThat(entity).isNotNull();
		Map<String, Object> expectedRow = DistributorRowMaps.toApiRow(entity, objectMapper);
		assertThat(published).isEqualTo(expectedRow);
	}
}
