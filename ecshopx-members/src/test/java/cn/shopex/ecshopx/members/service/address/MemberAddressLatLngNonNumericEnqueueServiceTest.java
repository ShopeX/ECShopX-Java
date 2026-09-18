package cn.shopex.ecshopx.members.service.address;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.members.dispatch.UpdateAddressLatAndLngJobDispatchPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberAddressLatLngNonNumericEnqueueServiceTest {

	@Mock
	private UpdateAddressLatAndLngJobDispatchPublisher updateAddressLatAndLngJobDispatchPublisher;

	@InjectMocks
	private MemberAddressLatLngNonNumericEnqueueService service;

	@Test
	void enqueueIfNeeded_whenLatLngNonNumeric_dispatchesJob() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("address_id", 9L);
		row.put("lat", "x");
		row.put("lng", "1");
		row.put("city", "");
		row.put("adrdetail", "");

		service.enqueueIfNeeded(100L, 200L, row);

		verify(updateAddressLatAndLngJobDispatchPublisher).enqueueUpdateAddressLatAndLng(eq(100L), eq(200L), eq(9L));
	}

	@Test
	void enqueueIfNeeded_whenLatLngNumeric_doesNotDispatch() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("address_id", 9L);
		row.put("lat", "31.2");
		row.put("lng", "121.5");

		service.enqueueIfNeeded(100L, 200L, row);

		verify(updateAddressLatAndLngJobDispatchPublisher, never())
				.enqueueUpdateAddressLatAndLng(anyLong(), anyLong(), anyLong());
	}
}
