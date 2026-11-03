/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.digios.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.digios.config.DigiOsResolvedConfig;
import cn.shopex.ecshopx.digios.service.DigiOsApiLogService;
import cn.shopex.ecshopx.digios.service.DigiOsConfigService;
import cn.shopex.ecshopx.digios.service.DigiOsTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DigiOsGatewayClientTest {

	@Mock
	private DigiOsConfigService configService;
	@Mock
	private DigiOsTokenService tokenService;
	@Mock
	private DigiOsApiLogService apiLogService;
	@Mock
	private RestTemplate restTemplate;

	private DigiOsGatewayClient client;

	@BeforeEach
	void setUp() {
		client = new DigiOsGatewayClient(
				configService, tokenService, apiLogService, new ObjectMapper(), restTemplate);
		DigiOsResolvedConfig cfg =
				new DigiOsResolvedConfig(
						true,
						"https://hobbits.test",
						"client",
						"secret",
						30,
						5,
						"/token",
						60,
						"CO",
						1L,
						Map.of());
		when(configService.resolve(1L)).thenReturn(cfg);
		when(configService.resolvePath("account.upsert", 1L)).thenReturn("/gateway/openapi/v1/account");
		when(tokenService.getAccessToken(1L, false))
				.thenReturn(new DigiOsTokenService.TokenInfo("tok", 3600, true));
	}

	@Test
	void put_http400_persistsStatusAndResponseBody() {
		byte[] bodyBytes =
				"{\"errorCode\":\"400\",\"errorMessage\":\"acc:账户名称格式不正确;\"}"
						.getBytes(StandardCharsets.UTF_8);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(String.class)))
				.thenThrow(
						HttpClientErrorException.create(
								HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, bodyBytes, StandardCharsets.UTF_8));

		DigiOsGatewayResult result = client.put(1L, "account.upsert", Map.of("acc", "bad"));

		assertFalse(result.ok());
		assertEquals(400, result.httpStatus());
		assertNotNull(result.body());
		assertEquals("400", result.body().get("errorCode"));
		assertEquals("400: acc:账户名称格式不正确;", result.error());

		ArgumentCaptor<Object> responseCaptor = ArgumentCaptor.forClass(Object.class);
		ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
		verify(apiLogService)
				.writeOutbound(
						eq(1L),
						eq("account.upsert"),
						eq(400),
						eq("fail"),
						anyInt(),
						any(),
						responseCaptor.capture(),
						errorCaptor.capture());
		@SuppressWarnings("unchecked")
		Map<String, Object> logged = (Map<String, Object>) responseCaptor.getValue();
		assertEquals("400", logged.get("errorCode"));
		assertEquals("400: acc:账户名称格式不正确;", errorCaptor.getValue());
	}

	@Test
	void put_http500_persistsStatusAndResponseBody() {
		byte[] bodyBytes =
				"{\"errorCode\":\"500\",\"errorMessage\":\"系统异常，请联系管理员。\"}"
						.getBytes(StandardCharsets.UTF_8);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(String.class)))
				.thenThrow(
						HttpServerErrorException.create(
								HttpStatus.INTERNAL_SERVER_ERROR,
								"Internal Server Error",
								HttpHeaders.EMPTY,
								bodyBytes,
								StandardCharsets.UTF_8));

		DigiOsGatewayResult result =
				client.put(1L, "account.upsert", Map.of("acc", "1255612", "nickNm", "小唐"));

		assertFalse(result.ok());
		assertEquals(500, result.httpStatus());
		assertNotNull(result.body());
		assertEquals("500", result.body().get("errorCode"));
		assertEquals("500: 系统异常，请联系管理员。", result.error());

		verify(apiLogService)
				.writeOutbound(
						eq(1L),
						eq("account.upsert"),
						eq(500),
						eq("fail"),
						anyInt(),
						any(),
						any(),
						eq("500: 系统异常，请联系管理员。"));
	}
}
