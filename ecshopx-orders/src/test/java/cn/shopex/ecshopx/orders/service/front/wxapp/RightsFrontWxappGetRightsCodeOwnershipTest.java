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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.wxapp.MemberBarcodeGenerateService;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** ECX-10924：核销码生成必须校验权益归属本人本租户。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RightsFrontWxappGetRightsCodeOwnershipTest {

	private RightsMapper rightsMapper;
	private StringRedisTemplate wechatStringRedisTemplate;
	private MemberBarcodeGenerateService memberBarcodeGenerateService;
	private RightsFrontWxappGetRightsCodeService service;

	@BeforeEach
	void setUp() {
		rightsMapper = mock(RightsMapper.class);
		wechatStringRedisTemplate = mock(StringRedisTemplate.class);
		memberBarcodeGenerateService = mock(MemberBarcodeGenerateService.class);
		service =
				new RightsFrontWxappGetRightsCodeService(
						wechatStringRedisTemplate, memberBarcodeGenerateService, rightsMapper);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOps = mock(ValueOperations.class);
		when(wechatStringRedisTemplate.opsForValue()).thenReturn(valueOps);
	}

	private static Map<String, Object> auth(long companyId, long userId) {
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", companyId);
		auth.put("user_id", userId);
		return auth;
	}

	private static Rights rights(long companyId, long userId) {
		Rights r = new Rights();
		r.setRightsId(12L);
		r.setCompanyId(companyId);
		r.setUserId(userId);
		return r;
	}

	@Test
	void otherUsersRights_isRejectedWithoutSideEffects() {
		when(rightsMapper.selectById(12L)).thenReturn(rights(100L, 999L));

		assertThatThrownBy(() -> service.getRightsCode("12", auth(100L, 7L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该权益");

		verify(wechatStringRedisTemplate, never()).opsForValue();
		verify(memberBarcodeGenerateService, never()).generateBarcodeAndQrcodeDataUrlsForPlainContent(anyString());
	}

	@Test
	void foreignCompanyRights_isRejectedWithoutSideEffects() {
		when(rightsMapper.selectById(12L)).thenReturn(rights(99L, 7L));

		assertThatThrownBy(() -> service.getRightsCode("12", auth(100L, 7L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该权益");
	}

	@Test
	void missingRights_isRejectedWithoutSideEffects() {
		when(rightsMapper.selectById(404L)).thenReturn(null);

		assertThatThrownBy(() -> service.getRightsCode("404", auth(100L, 7L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该权益");
	}

	@Test
	void invalidRightsId_throwsBadRequest() {
		assertThatThrownBy(() -> service.getRightsCode("abc", auth(100L, 7L)))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("获取核销码出错");

		assertThatThrownBy(() -> service.getRightsCode("0", auth(100L, 7L)))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("获取核销码出错");
	}

	@Test
	void ownRights_generatesCode() {
		when(rightsMapper.selectById(12L)).thenReturn(rights(100L, 7L));
		when(memberBarcodeGenerateService.generateBarcodeAndQrcodeDataUrlsForPlainContent(anyString()))
				.thenReturn(new LinkedHashMap<>(Map.of(
						"barcode_url", "data:image/jpg;base64:AAA",
						"qrcode_url", "data:image/jpg;base64:BBB")));

		Map<String, Object> out = service.getRightsCode("12", auth(100L, 7L));

		assertThat(out.get("code")).isInstanceOf(String.class);
		assertThat(((String) out.get("code"))).hasSize(16);
		assertThat(out.get("barcode_url")).isEqualTo("data:image/jpg;base64:AAA");
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOps =
				(ValueOperations<String, String>) wechatStringRedisTemplate.opsForValue();
		verify(valueOps)
				.set(anyString(), org.mockito.ArgumentMatchers.eq("12"), any(Duration.class));
	}
}
