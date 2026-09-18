package cn.shopex.ecshopx.distribution.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DistributionEditEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributorUpdateEventDispatchPublisher;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelfDistributorDisplayUpdatePortImplDistributionEditDispatchProbeTest {

	@Mock
	private DistributorWriteRepository distributorWriteRepository;

	@Mock
	private DistributorUpdateService distributorUpdateService;

	@Mock
	private DistributionEditEventDispatchPublisher distributionEditEventDispatchPublisher;

	@Mock
	private DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher;

	private SelfDistributorDisplayUpdatePortImpl impl;

	@BeforeEach
	void setUp() {
		impl = new SelfDistributorDisplayUpdatePortImpl(
				distributorWriteRepository,
				distributorUpdateService,
				distributionEditEventDispatchPublisher,
				distributorUpdateEventDispatchPublisher);
	}

	@Test
	@DisplayName("entry-03-api-shops-setwxshopssetting")
	@SuppressWarnings("unchecked")
	void syncSelfDistributorBrandAndLogoIfPresent_whenSelfIdPresent_invokesPerformUpdateThenPublishesDistributionEditBeforeDistributorUpdate() {
		long companyId = 7L;
		long distributorId = 501L;
		when(distributorWriteRepository.findDistributorSelfId(companyId)).thenReturn(Optional.of(distributorId));

		Map<String, Object> apiRow = new LinkedHashMap<>();
		apiRow.put("company_id", companyId);
		apiRow.put("distributor_id", distributorId);
		apiRow.put("name", "BrandX");
		apiRow.put("logo", "https://logo");
		when(distributorUpdateService.performUpdateAndEvents(any(), eq(distributorId), eq("zh-CN")))
				.thenReturn(apiRow);

		impl.syncSelfDistributorBrandAndLogoIfPresent(companyId, "BrandX", "https://logo", "zh-CN");

		verify(distributorUpdateService)
				.performUpdateAndEvents(
						argThat(m ->
								companyId == toLong(m.get("company_id"))
										&& "BrandX".equals(m.get("name"))
										&& "https://logo".equals(m.get("logo"))),
						eq(distributorId),
						eq("zh-CN"));

		InOrder inOrder = inOrder(distributionEditEventDispatchPublisher, distributorUpdateEventDispatchPublisher);
		ArgumentCaptor<Map<String, Object>> editCap = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<Map<String, Object>> updateCap = ArgumentCaptor.forClass(Map.class);
		inOrder.verify(distributionEditEventDispatchPublisher).publish(editCap.capture());
		inOrder.verify(distributorUpdateEventDispatchPublisher).publish(updateCap.capture());

		Map<String, Object> editRow = editCap.getValue();
		Map<String, Object> updateRow = updateCap.getValue();
		assertEquals(editRow.keySet(), updateRow.keySet());
		assertEquals(companyId, toLong(editRow.get("company_id")));
		assertEquals(distributorId, toLong(editRow.get("distributor_id")));
		assertEquals("BrandX", editRow.get("name"));
		assertEquals("https://logo", editRow.get("logo"));
	}

	@Test
	void syncSelfDistributorBrandAndLogoIfPresent_whenSelfIdEmpty_skipsPerformUpdateAndPublishers() {
		when(distributorWriteRepository.findDistributorSelfId(1L)).thenReturn(Optional.empty());
		impl.syncSelfDistributorBrandAndLogoIfPresent(1L, "n", "l", "zh-CN");
		verifyNoInteractions(distributorUpdateService);
		verifyNoInteractions(distributionEditEventDispatchPublisher);
		verifyNoInteractions(distributorUpdateEventDispatchPublisher);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
