package cn.shopex.ecshopx.thirdparty.service.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.thirdparty.domain.MapConfig;
import cn.shopex.ecshopx.thirdparty.mapper.MapConfigMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class CompanyMapGeocodeServiceReverseTest {

	@Mock
	private MapConfigMapper mapConfigMapper;

	@Mock
	private MessageSource messageSource;

	@Test
	@DisplayName("tencent reverse uses company key and returns result.address")
	@SuppressWarnings({"rawtypes", "unchecked"})
	void getPositionByLatAndLngRaw_tencentSuccess_usesCompanyKeyAndReturnsResult() {
		RestClient restClient = mock(RestClient.class);
		RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
		ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
		when(restClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(uriCaptor.capture())).thenReturn(uriSpec);
		when(uriSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.body(String.class))
				.thenReturn(
						"{\"status\":0,\"result\":{\"address\":\"上海市闵行区吴中路\",\"address_component\":{\"province\":\"上海市\",\"city\":\"上海市\",\"district\":\"闵行区\"}}}");
		when(mapConfigMapper.selectList(any())).thenReturn(List.of(tencentConfig()));
		CompanyMapGeocodeService service = new CompanyMapGeocodeService(mapConfigMapper, restClient, messageSource);

		Object data = service.getPositionByLatAndLngRaw(141L, "31.162939", "121.404398");

		assertInstanceOf(Map.class, data);
		Map<?, ?> result = (Map<?, ?>) data;
		assertEquals("上海市闵行区吴中路", result.get("address"));
		assertInstanceOf(Map.class, result.get("address_component"));
		String url = uriCaptor.getValue();
		assertTrue(url.contains("key=TESTKEY"), url);
		String expectedSig =
				md5LowerHex(
						"/ws/geocoder/v1?get_poi=0&key=TESTKEY&location=31.162939,121.404398&poi_options=address_format=shortTESTSECRET");
		assertTrue(url.contains("sig=" + expectedSig), url);
	}

	@Test
	@DisplayName("tencent reverse status!=0 returns empty list")
	@SuppressWarnings({"rawtypes", "unchecked"})
	void getPositionByLatAndLngRaw_tencentNonZeroStatus_returnsEmptyList() {
		RestClient restClient = mock(RestClient.class);
		RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
		when(restClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(anyString())).thenReturn(uriSpec);
		when(uriSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.body(String.class)).thenReturn("{\"status\":311,\"message\":\"key格式错误\"}");
		when(mapConfigMapper.selectList(any())).thenReturn(List.of(tencentConfig()));
		CompanyMapGeocodeService service = new CompanyMapGeocodeService(mapConfigMapper, restClient, messageSource);

		Object data = service.getPositionByLatAndLngRaw(141L, "31.162939", "121.404398");

		assertEquals(Collections.emptyList(), data);
	}

	@Test
	@DisplayName("non-numeric lat/lng does not call tencent")
	void getPositionByLatAndLngRaw_nonNumeric_returnsEmptyListWithoutHttp() {
		RestClient restClient = mock(RestClient.class);
		CompanyMapGeocodeService service = new CompanyMapGeocodeService(mapConfigMapper, restClient, messageSource);

		Object data = service.getPositionByLatAndLngRaw(141L, "abc", "121.404398");

		assertEquals(Collections.emptyList(), data);
		verify(restClient, never()).get();
	}

	@Test
	@DisplayName("amap reverse maps formatted_address and addressComponent")
	@SuppressWarnings({"rawtypes", "unchecked"})
	void getPositionByLatAndLngRaw_amapSuccess_mapsFrontendShape() {
		RestClient restClient = mock(RestClient.class);
		RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
		when(restClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(anyString())).thenReturn(uriSpec);
		when(uriSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.body(String.class))
				.thenReturn(
						"{\"status\":\"1\",\"regeocode\":{\"formatted_address\":\"上海市闵行区吴中路\",\"addressComponent\":{\"country\":\"中国\",\"province\":\"上海市\",\"city\":\"上海市\",\"district\":\"闵行区\"}}}");
		when(mapConfigMapper.selectList(any())).thenReturn(List.of(amapConfig()));
		CompanyMapGeocodeService service = new CompanyMapGeocodeService(mapConfigMapper, restClient, messageSource);

		Object data = service.getPositionByLatAndLngRaw(141L, "31.162939", "121.404398");

		assertInstanceOf(Map.class, data);
		Map<?, ?> result = (Map<?, ?>) data;
		assertEquals("上海市闵行区吴中路", result.get("address"));
		Map<?, ?> component = (Map<?, ?>) result.get("address_component");
		assertEquals("上海市", component.get("province"));
		assertEquals("上海市", component.get("city"));
		assertEquals("闵行区", component.get("district"));
	}

	private static MapConfig tencentConfig() {
		MapConfig cfg = new MapConfig();
		cfg.setCompanyId(141L);
		cfg.setType("tencent");
		cfg.setAppKey("TESTKEY");
		cfg.setAppSecret("TESTSECRET");
		cfg.setIsDefault(true);
		return cfg;
	}

	private static MapConfig amapConfig() {
		MapConfig cfg = new MapConfig();
		cfg.setCompanyId(141L);
		cfg.setType("amap");
		cfg.setAppKey("AMAPKEY");
		cfg.setAppSecret("");
		cfg.setIsDefault(true);
		return cfg;
	}

	private static String md5LowerHex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
