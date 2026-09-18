package cn.shopex.ecshopx.orders.service.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

class Kuaidi100TrackQueryServiceTest {

	private static final String POLL_URL = "https://poll.kuaidi100.com/poll/query.do";

	@Test
	void postsCustomerAsAppSecretAndSignsWithAppKey() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		ObjectMapper objectMapper = new ObjectMapper();
		Kuaidi100TrackQueryService service = new Kuaidi100TrackQueryService(objectMapper);
		Field restTemplateField = Kuaidi100TrackQueryService.class.getDeclaredField("restTemplate");
		restTemplateField.setAccessible(true);
		restTemplateField.set(service, restTemplate);

		String appKey = "demo-kuaidi100-key";
		String appSecret = "demo-kuaidi100-customer";
		ObjectNode settings = objectMapper.createObjectNode();
		settings.put("app_key", appKey);
		settings.put("app_secret", appSecret);

		ObjectNode expectedParam = objectMapper.createObjectNode();
		expectedParam.put("com", "shentong");
		expectedParam.put("num", "773438156027936");
		expectedParam.put("phone", "15601636091");
		expectedParam.put("from", "");
		expectedParam.put("to", "");
		String expectedParamJson = objectMapper.writeValueAsString(expectedParam);
		String expectedSign = md5UpperHex(expectedParamJson + appKey + appSecret);

		LinkedMultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
		expectedForm.add("customer", appSecret);
		expectedForm.add("param", expectedParamJson);
		expectedForm.add("sign", expectedSign);

		server
				.expect(MockRestRequestMatchers.requestTo(POLL_URL))
				.andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
				.andExpect(MockRestRequestMatchers.content().formData(expectedForm))
				.andRespond(
						MockRestResponseCreators.withSuccess(
								"{\"message\":\"ok\",\"status\":\"200\",\"data\":[{\"ftime\":\"2026-08-27 12:19:44\",\"context\":\"派送中\"}]}",
								MediaType.APPLICATION_JSON));

		List<LinkedHashMap<String, String>> traces =
				service.queryTraces(1L, "shentong", "773438156027936", "15601636091", settings);

		server.verify();
		assertThat(traces).hasSize(1);
		assertThat(traces.get(0).get("AcceptTime")).isEqualTo("2026-08-27 12:19:44");
		assertThat(traces.get(0).get("AcceptStation")).isEqualTo("派送中");
	}

	private static String md5UpperHex(String input) throws Exception {
		byte[] digest = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder(32);
		for (byte b : digest) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}
}
