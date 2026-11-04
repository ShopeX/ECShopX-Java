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

package cn.shopex.ecshopx.ali.service.minisetting;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.ali.domain.AliMiniAppSetting;
import cn.shopex.ecshopx.ali.mapper.AliMiniAppSettingMapper;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AliMiniAppSettingSaveServiceScopeTest {

	@Mock
	private AliMiniAppSettingMapper mapper;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private AliMiniAppSettingSaveService service;

	@BeforeEach
	void setUp() {
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOperations);
		service = new AliMiniAppSettingSaveService(mapper, new ObjectMapper(), companysRedisTemplate);
	}

	@Test
	void save_rejectsNonEmptySettingIdWhenTenantHasNoRow() {
		when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> params = baseParams();
		params.put("setting_id", 99L);

		assertThrows(ForbiddenException.class, () -> service.save(100L, params));
		verify(mapper, never()).selectById(any());
		verify(mapper, never()).updateById(any(AliMiniAppSetting.class));
		verify(mapper, never()).update(isNull(), any());
		verify(mapper, never()).insert(any(AliMiniAppSetting.class));
	}

	@Test
	void save_rejectsUpdateWhenExistingRowBelongsToOtherCompany() {
		AliMiniAppSetting own = new AliMiniAppSetting();
		own.setSettingId(7L);
		own.setCompanyId(100L);
		when(mapper.selectOne(any(Wrapper.class))).thenReturn(own);
		when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);

		AliMiniAppSetting foreign = new AliMiniAppSetting();
		foreign.setSettingId(7L);
		foreign.setCompanyId(200L);
		when(mapper.selectById(7L)).thenReturn(foreign);

		Map<String, Object> params = baseParams();
		params.put("setting_id", 7L);

		assertThrows(ForbiddenException.class, () -> service.save(100L, params));
		verify(mapper, never()).updateById(any(AliMiniAppSetting.class));
		verify(mapper, never()).update(isNull(), any());
	}

	private static Map<String, Object> baseParams() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("authorizer_appid", "app-new-100");
		params.put("merchant_private_key", "pk");
		params.put("api_sign_method", "key");
		return params;
	}
}
