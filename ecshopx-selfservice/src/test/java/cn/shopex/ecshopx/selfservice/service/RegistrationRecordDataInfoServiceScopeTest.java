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

package cn.shopex.ecshopx.selfservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.export.RegistrationRecordCsvExportService;
import cn.shopex.ecshopx.selfservice.service.multilang.FormTemplateOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationRecordOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationRecordDataInfoServiceScopeTest {

	@Mock
	private RegistrationRecordMapper registrationRecordMapper;

	@Mock
	private RegistrationActivityMapper registrationActivityMapper;

	@Mock
	private RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;

	@Mock
	private RegistrationRecordOutsideMultiLangReadService registrationRecordOutsideMultiLangReadService;

	@Mock
	private RegistrationRecordContentDatapassService registrationRecordContentDatapassService;

	@Mock
	private AddressMapper addressMapper;

	@Mock
	private SensitiveFieldEncryptor sensitiveFieldEncryptor;

	@Mock
	private MessageSource messageSource;

	@Mock
	private FormTemplateMapper formTemplateMapper;

	@Mock
	private RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler;

	@Mock
	private RegistrationRecordCsvExportService registrationRecordCsvExportService;

	@Mock
	private FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService;

	private RegistrationRecordDataInfoService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), RegistrationRecord.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), RegistrationActivity.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new RegistrationRecordDataInfoService(
						registrationRecordMapper,
						registrationActivityMapper,
						registrationActivityOutsideMultiLangReadService,
						registrationRecordOutsideMultiLangReadService,
						registrationRecordContentDatapassService,
						addressMapper,
						sensitiveFieldEncryptor,
						messageSource,
						new ObjectMapper(),
						formTemplateMapper,
						registrationRecordRowMapAssembler,
						registrationRecordCsvExportService,
						formTemplateOutsideMultiLangReadService);
	}

	@Test
	void getDataInfo_rejectsForeignTenantRecordWithSoftMissing() {
		RegistrationRecord foreign = new RegistrationRecord();
		foreign.setRecordId(9L);
		foreign.setCompanyId(200L);
		foreign.setMobile("cipher-mobile");
		foreign.setContent("[]");
		when(registrationRecordMapper.selectById(9L)).thenReturn(foreign);
		when(registrationRecordMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> body =
				service.getDataInfo(100L, 9L, "zh-CN", Locale.SIMPLIFIED_CHINESE, false);

		assertEquals(1, body.size());
		assertNull(body.get("content"));
		assertFalse(body.containsKey("mobile"));
		verify(sensitiveFieldEncryptor, never()).decrypt(anyString());
		verify(registrationRecordContentDatapassService, never()).applyMaskIfNeeded(any(), anyBoolean());
	}

	@Test
	void getDataInfo_ignoresActivityFromOtherCompany() {
		RegistrationRecord own = new RegistrationRecord();
		own.setRecordId(7L);
		own.setCompanyId(100L);
		own.setActivityId(55L);
		own.setStatus("pending");
		own.setContent("[]");
		own.setMobile(null);
		own.setFormMobile(null);
		when(registrationRecordMapper.selectOne(any(Wrapper.class))).thenReturn(own);

		RegistrationActivity foreignActivity = new RegistrationActivity();
		foreignActivity.setActivityId(55L);
		foreignActivity.setCompanyId(200L);
		foreignActivity.setActivityName("secret-activity");
		when(registrationActivityMapper.selectById(55L)).thenReturn(foreignActivity);
		when(registrationRecordContentDatapassService.applyMaskIfNeeded(any(), anyBoolean()))
				.thenReturn(null);
		when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("ok");

		Map<String, Object> body =
				service.getDataInfo(100L, 7L, "zh-CN", Locale.SIMPLIFIED_CHINESE, false);

		assertEquals("", body.get("activity_name"));
		assertEquals(0, body.get("start_time"));
		Object activityInfo = body.get("activity_info");
		assertEquals(true, activityInfo instanceof Map && ((Map<?, ?>) activityInfo).isEmpty());
		verify(registrationActivityOutsideMultiLangReadService, never())
				.applyDetailOutsideLangOverrides(anyLong(), anyLong(), any(), anyString());
	}
}
