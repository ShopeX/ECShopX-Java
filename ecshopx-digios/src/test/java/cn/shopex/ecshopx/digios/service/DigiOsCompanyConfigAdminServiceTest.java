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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.digios.domain.DigiOsCompanyConfig;
import cn.shopex.ecshopx.digios.mapper.DigiOsCompanyConfigMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DigiOsCompanyConfigAdminServiceTest {

	private DigiOsCompanyConfigMapper configMapper;
	private DigiOsCompanyConfigAdminService service;

	@BeforeEach
	void setUp() {
		configMapper = Mockito.mock(DigiOsCompanyConfigMapper.class);
		service = new DigiOsCompanyConfigAdminService(configMapper);
	}

	@Test
	void get_returnsEmptyDefaultsWhenNoRow() {
		when(configMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> data = service.get(9L);

		assertEquals(0, data.get("is_enabled"));
		assertEquals("", data.get("base_url"));
		assertEquals("", data.get("client_id"));
		assertEquals("", data.get("app_secret"));
	}

	@Test
	void get_returnsPlaintextSecret() {
		DigiOsCompanyConfig row = row(9L, 1, "https://hobbits.ishopex.cn/", "cid", "sec");
		when(configMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		Map<String, Object> data = service.get(9L);

		assertEquals(1, data.get("is_enabled"));
		assertEquals("https://hobbits.ishopex.cn", data.get("base_url"));
		assertEquals("cid", data.get("client_id"));
		assertEquals("sec", data.get("app_secret"));
	}

	@Test
	void save_insertsWhenMissing_andNullsEmptyAppKey() {
		when(configMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		when(configMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("is_enabled", 0);
		body.put("base_url", " https://a.test/ ");
		body.put("client_id", "");
		body.put("app_secret", "keep-me");

		Map<String, Object> data = service.save(9L, body);

		ArgumentCaptor<DigiOsCompanyConfig> cap = ArgumentCaptor.forClass(DigiOsCompanyConfig.class);
		verify(configMapper).insert(cap.capture());
		DigiOsCompanyConfig inserted = cap.getValue();
		assertEquals(9L, inserted.getCompanyId());
		assertEquals(0, inserted.getIsEnabled());
		assertEquals("https://a.test", inserted.getBaseUrl());
		assertNull(inserted.getAppKey());
		assertEquals("keep-me", inserted.getAppSecret());
		assertEquals("", data.get("client_id"));
		assertEquals("keep-me", data.get("app_secret"));
	}

	@Test
	void save_omitsAppSecretKey_keepsExisting() {
		DigiOsCompanyConfig existing = row(9L, 0, "https://old.test", "old-cid", "old-secret");
		when(configMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
		when(configMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("is_enabled", 0);
		body.put("base_url", "https://new.test");
		body.put("client_id", "new-cid");

		service.save(9L, body);

		assertEquals("old-secret", existing.getAppSecret());
		assertEquals("https://new.test", existing.getBaseUrl());
		assertEquals("new-cid", existing.getAppKey());
		verify(configMapper).updateById(existing);
		verify(configMapper, never()).insert(any(DigiOsCompanyConfig.class));
	}

	@Test
	void save_enableRequiresFields_andAcceptsExistingSecret() {
		DigiOsCompanyConfig existing = row(9L, 0, null, null, "stored-secret");
		when(configMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
		when(configMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("is_enabled", 1);
		body.put("base_url", "https://hobbits.ishopex.cn");
		body.put("client_id", "cid");

		Map<String, Object> data = service.save(9L, body);

		assertEquals(1, data.get("is_enabled"));
		assertEquals("stored-secret", data.get("app_secret"));
	}

	@Test
	void save_enableFailsWithoutSecret() {
		when(configMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("is_enabled", 1);
		body.put("base_url", "https://hobbits.ishopex.cn");
		body.put("client_id", "cid");

		BadRequestException ex =
				assertThrows(BadRequestException.class, () -> service.save(9L, body));
		assertEquals("App Secret必填", ex.getMessage());
	}

	@Test
	void save_enableFailsWithoutBaseUrl() {
		when(configMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("is_enabled", 1);
		body.put("client_id", "cid");
		body.put("app_secret", "sec");

		BadRequestException ex =
				assertThrows(BadRequestException.class, () -> service.save(9L, body));
		assertEquals("进销存 API 地址必填", ex.getMessage());
	}

	@Test
	void save_rejectsDuplicateAppKey() {
		when(configMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		when(configMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("is_enabled", 0);
		body.put("client_id", "dup");

		assertThrows(BadRequestException.class, () -> service.save(9L, body));
	}

	private static DigiOsCompanyConfig row(
			long companyId, int enabled, String baseUrl, String appKey, String secret) {
		DigiOsCompanyConfig row = new DigiOsCompanyConfig();
		row.setId(1L);
		row.setCompanyId(companyId);
		row.setIsEnabled(enabled);
		row.setBaseUrl(baseUrl);
		row.setAppKey(appKey);
		row.setAppSecret(secret);
		row.setCreated(1);
		return row;
	}
}
