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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.selfservice.domain.FormSetting;
import cn.shopex.ecshopx.selfservice.mapper.FormSettingMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormSettingOutsideMultiLangReadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10938：表单设置详情只返回当前公司的配置。 */
@ExtendWith(MockitoExtension.class)
class FormSettingDataInfoServiceCompanyScopeTest {

	@Mock
	private FormSettingMapper formSettingMapper;

	@Mock
	private FormSettingOutsideMultiLangReadService multiLangReadService;

	private FormSettingDataInfoService service;

	@BeforeEach
	void setUp() {
		service =
				new FormSettingDataInfoService(
						formSettingMapper, multiLangReadService, new FormSettingApiRowAssembler(new ObjectMapper()));
	}

	private static FormSetting setting(long id, Long companyId) {
		FormSetting entity = new FormSetting();
		entity.setId(id);
		entity.setCompanyId(companyId);
		entity.setFieldTitle("到店时间");
		entity.setFieldName("arrive_at");
		return entity;
	}

	@Test
	void foreignCompany_returnsEmptyListWithoutMultiLang() {
		when(formSettingMapper.selectById(77L)).thenReturn(setting(77L, 99L));

		Object out = service.getDataInfo("77", 11L, "zh-CN");

		assertThat(out).isEqualTo(java.util.List.of());
		verify(multiLangReadService, never()).applyFieldTitleOverrides(anyLong(), anyList(), anyString());
	}

	@Test
	void missingCompany_returnsEmptyListWithoutMultiLang() {
		when(formSettingMapper.selectById(77L)).thenReturn(setting(77L, null));

		Object out = service.getDataInfo("77", 11L, "zh-CN");

		assertThat(out).isEqualTo(java.util.List.of());
		verify(multiLangReadService, never()).applyFieldTitleOverrides(anyLong(), anyList(), anyString());
	}

	@Test
	void ownCompany_returnsFieldNameAndAppliesMultiLang() {
		when(formSettingMapper.selectById(77L)).thenReturn(setting(77L, 11L));

		Object out = service.getDataInfo("77", 11L, "zh-CN");

		assertThat(out).isInstanceOf(Map.class);
		assertThat(((Map<?, ?>) out).get("field_name")).isEqualTo("arrive_at");
		assertThat(((Map<?, ?>) out).get("company_id")).isEqualTo(11L);
		verify(multiLangReadService).applyFieldTitleOverrides(eq(11L), anyList(), eq("zh-CN"));
	}
}
