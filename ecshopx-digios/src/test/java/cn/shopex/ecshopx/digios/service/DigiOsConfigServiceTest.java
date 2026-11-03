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

package cn.shopex.ecshopx.digios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.digios.config.DigiOsProperties;
import cn.shopex.ecshopx.digios.config.DigiOsResolvedConfig;
import cn.shopex.ecshopx.digios.domain.DigiOsCompanyConfig;
import cn.shopex.ecshopx.digios.mapper.DigiOsCompanyConfigMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DigiOsConfigServiceTest {

	private DigiOsCompanyConfigMapper configMapper;
	private DigiOsProperties properties;
	private DigiOsConfigService service;

	@BeforeEach
	void setUp() {
		configMapper = Mockito.mock(DigiOsCompanyConfigMapper.class);
		properties = new DigiOsProperties();
		properties.setEnabled(false);
		properties.setBaseUrl("https://example.test");
		properties.setClientId("env-client");
		properties.setAppSecret("env-secret");
		service = new DigiOsConfigService(configMapper, properties);
	}

	@Test
	void resolve_orEnabledFromCompanyRow() {
		DigiOsCompanyConfig row = new DigiOsCompanyConfig();
		row.setCompanyId(9L);
		row.setIsEnabled(1);
		row.setAppKey("row-client");
		row.setAppSecret("row-secret");
		row.setBaseUrl("https://company.test/");
		when(configMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		DigiOsResolvedConfig cfg = service.resolve(9L);
		assertTrue(cfg.enabled());
		assertEquals("https://company.test", cfg.baseUrl());
		assertEquals("row-client", cfg.clientId());
		assertEquals("row-secret", cfg.appSecret());
	}
}
