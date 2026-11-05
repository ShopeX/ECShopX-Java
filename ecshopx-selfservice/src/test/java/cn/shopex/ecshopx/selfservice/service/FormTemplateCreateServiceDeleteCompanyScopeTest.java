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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormTemplateOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;

/** ECX-10901：废弃表单模板必须带 company_id，跨公司 ID 不得更新。 */
class FormTemplateCreateServiceDeleteCompanyScopeTest {

	private FormTemplateMapper formTemplateMapper;
	private FormTemplateCreateService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), FormTemplate.class);
	}

	@BeforeEach
	void setUp() {
		formTemplateMapper = mock(FormTemplateMapper.class);
		service =
				new FormTemplateCreateService(
						formTemplateMapper,
						mock(FormTemplateOutsideMultiLangWriteService.class),
						new ObjectMapper(),
						mock(MessageSource.class),
						mock(FormTemplateApiRowAssembler.class));
	}

	@Test
	void deleteData_scopesDiscardByCompany() {
		when(formTemplateMapper.update(nullable(FormTemplate.class), any())).thenReturn(1);

		Object result = service.deleteData("7", 100L);

		assertThat(result).isEqualTo(Map.of("status", true));
		assertDiscardScopedToCompany(100L);
	}

	@Test
	void deleteData_throwsWhenCompanyScopeMisses() {
		when(formTemplateMapper.update(nullable(FormTemplate.class), any())).thenReturn(0);

		assertThatThrownBy(() -> service.deleteData("7", 100L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("数据不存在");

		assertDiscardScopedToCompany(100L);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void assertDiscardScopedToCompany(long companyId) {
		ArgumentCaptor<LambdaUpdateWrapper<FormTemplate>> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(formTemplateMapper).update(isNull(), cap.capture());
		LambdaUpdateWrapper<FormTemplate> wrapper = cap.getValue();
		assertThat(wrapper.getSqlSegment()).contains("company_id");
		assertThat(wrapper.getParamNameValuePairs().values()).contains(companyId);
	}
}
