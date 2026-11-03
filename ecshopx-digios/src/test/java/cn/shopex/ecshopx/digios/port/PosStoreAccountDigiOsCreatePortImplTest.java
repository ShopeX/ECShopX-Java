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

package cn.shopex.ecshopx.digios.port;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.digios.client.DigiOsGatewayClient;
import cn.shopex.ecshopx.digios.client.DigiOsGatewayResult;
import cn.shopex.ecshopx.digios.config.DigiOsResolvedConfig;
import cn.shopex.ecshopx.digios.service.DigiOsConfigService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosStoreAccountDigiOsCreatePortImplTest {

	@Mock
	private DigiOsConfigService configService;
	@Mock
	private DigiOsGatewayClient gatewayClient;

	private PosStoreAccountDigiOsCreatePortImpl port;

	@BeforeEach
	void setUp() {
		port = new PosStoreAccountDigiOsCreatePortImpl(configService, gatewayClient);
	}

	@Test
	void disabled_skipsGateway() {
		when(configService.resolve(1L)).thenReturn(disabledCfg());
		assertDoesNotThrow(() -> port.ensureSyncedOnCreate(1L, "E01", "张三", "", ""));
		verify(gatewayClient, never()).put(anyLong(), any(), any());
	}

	@Test
	void enabled_blankMobile_throwsBeforeGateway() {
		when(configService.resolve(1L)).thenReturn(enabledCfg());
		BadRequestException ex =
				assertThrows(
						BadRequestException.class,
						() -> port.ensureSyncedOnCreate(1L, "E01", "张三", " ", ""));
		assertEquals("手机号必填", ex.getMessage());
		verify(gatewayClient, never()).put(anyLong(), any(), any());
	}

	@Test
	void enabled_gateway400_throwsWithDigiOsMessage() {
		when(configService.resolve(1L)).thenReturn(enabledCfg());
		when(gatewayClient.put(eq(1L), eq("account.upsert"), any()))
				.thenReturn(
						new DigiOsGatewayResult(
								false,
								400,
								Map.of("errorCode", "400", "errorMessage", "acc:账户名称格式不正确;"),
								null,
								"400: acc:账户名称格式不正确;"));
		BadRequestException ex =
				assertThrows(
						BadRequestException.class,
						() -> port.ensureSyncedOnCreate(1L, "bad", "小唐", "13661830217", ""));
		assertEquals("DigiOS同步失败：acc:账户名称格式不正确;", ex.getMessage());
	}

	@Test
	void enabled_gateway500_throwsWithDigiOsMessage() {
		when(configService.resolve(1L)).thenReturn(enabledCfg());
		when(gatewayClient.put(eq(1L), eq("account.upsert"), any()))
				.thenReturn(
						new DigiOsGatewayResult(
								false,
								500,
								Map.of("errorCode", "500", "errorMessage", "系统异常，请联系管理员。"),
								null,
								"500: 系统异常，请联系管理员。"));
		BadRequestException ex =
				assertThrows(
						BadRequestException.class,
						() -> port.ensureSyncedOnCreate(1L, "1255612", "小唐", "13661830217", ""));
		assertEquals("DigiOS同步失败：系统异常，请联系管理员。", ex.getMessage());
	}

	@Test
	void enabled_success_ok() {
		when(configService.resolve(1L)).thenReturn(enabledCfg());
		Map<String, Object> data = Map.of("usrId", "9", "acc", "E01");
		when(gatewayClient.put(eq(1L), eq("account.upsert"), any()))
				.thenReturn(new DigiOsGatewayResult(true, 200, Map.of("data", data), null, null));
		assertDoesNotThrow(
				() -> port.ensureSyncedOnCreate(1L, "E01", "张三", "13900001111", "a@b.c"));
		verify(gatewayClient).put(eq(1L), eq("account.upsert"), any());
	}

	@Test
	void frontendMessage_stripsHttpStylePrefix() {
		assertEquals("acc:账户名称格式不正确;", PosStoreAccountDigiOsCreatePortImpl.frontendMessage("400: acc:账户名称格式不正确;"));
		assertEquals("系统异常，请联系管理员。", PosStoreAccountDigiOsCreatePortImpl.frontendMessage("500: 系统异常，请联系管理员。"));
		assertEquals("未知错误", PosStoreAccountDigiOsCreatePortImpl.frontendMessage(""));
	}

	private static DigiOsResolvedConfig enabledCfg() {
		return new DigiOsResolvedConfig(
				true, "https://h.test", "c", "s", 30, 5, "/token", 60, "CO", 1L, Map.of());
	}

	private static DigiOsResolvedConfig disabledCfg() {
		return new DigiOsResolvedConfig(
				false, "https://h.test", "c", "s", 30, 5, "/token", 60, "CO", 1L, Map.of());
	}
}
