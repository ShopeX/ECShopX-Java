package cn.shopex.ecshopx.distribution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorH5GetAreaByJwdServiceTest {

	@Mock
	private CompanyMapGeocodePort companyMapGeocodePort;

	@Test
	@DisplayName("passes companyId plus lat/lng to company map reverse geocode")
	void getAreaByJwd_delegatesToCompanyMapPort() {
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("address", "上海市闵行区吴中路");
		when(companyMapGeocodePort.getPositionByLatAndLngRaw(141L, "31.162939", "121.404398"))
				.thenReturn(expected);
		DistributorH5GetAreaByJwdService service = new DistributorH5GetAreaByJwdService(companyMapGeocodePort);

		Object actual = service.getAreaByJwd(141L, "31.162939", "121.404398");

		assertSame(expected, actual);
		verify(companyMapGeocodePort).getPositionByLatAndLngRaw(eq(141L), eq("31.162939"), eq("121.404398"));
	}

	@Test
	@DisplayName("returns empty list when reverse geocode throws")
	void getAreaByJwd_returnsEmptyListWhenPortThrows() {
		when(companyMapGeocodePort.getPositionByLatAndLngRaw(141L, "31.16", "121.40"))
				.thenThrow(new RuntimeException("tencent down"));
		DistributorH5GetAreaByJwdService service = new DistributorH5GetAreaByJwdService(companyMapGeocodePort);

		Object actual = service.getAreaByJwd(141L, "31.16", "121.40");

		assertEquals(List.of(), actual);
		assertEquals(Collections.emptyList(), actual);
	}
}
