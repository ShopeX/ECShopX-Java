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

package cn.shopex.ecshopx.form.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.form.domain.TranscriptProperties;
import cn.shopex.ecshopx.form.domain.Transcripts;
import cn.shopex.ecshopx.form.mapper.TranscriptPropertiesMapper;
import cn.shopex.ecshopx.form.mapper.TranscriptsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10875：成绩单属性更新必须限定本租户本成绩单。 */
@ExtendWith(MockitoExtension.class)
class TranscriptUpdateServicePropScopeTest {

	@Mock
	private TranscriptsMapper transcriptsMapper;

	@Mock
	private TranscriptPropertiesMapper transcriptPropertiesMapper;

	private TranscriptUpdateService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Transcripts.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), TranscriptProperties.class);
	}

	@BeforeEach
	void setUp() {
		service = new TranscriptUpdateService(transcriptsMapper, transcriptPropertiesMapper);
	}

	@Test
	void update_rejectsForeignPropIdWithoutUpdate() {
		Transcripts entity = baseTranscript(10L, 100L);
		// oldProps 非空以走更新分支；请求携带的 prop_id 在本租户本单下查不到
		TranscriptProperties oldProp = baseProp(7L, 10L, 100L, "语文");

		when(transcriptsMapper.selectById(10L)).thenReturn(entity);
		when(transcriptPropertiesMapper.selectList(any(Wrapper.class))).thenReturn(List.of(oldProp));
		when(transcriptPropertiesMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> params = new LinkedHashMap<>();
		// 与 oldProp 同 id，避免先删后改；scoped selectOne 返回 null 模拟外租户/越权
		params.put("evaluateItems", List.of(Map.of("prop_id", 7L, "prop_name", "被抢")));

		assertThatThrownBy(() -> service.update(100L, 10L, params))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("7");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<TranscriptProperties>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(transcriptPropertiesMapper).selectOne(captor.capture());
		assertThat(captor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(transcriptPropertiesMapper, never()).updateById(any(TranscriptProperties.class));
	}

	@Test
	void update_updatesOwnCompanyTranscriptProp() {
		Transcripts entity = baseTranscript(10L, 100L);
		TranscriptProperties oldProp = baseProp(7L, 10L, 100L, "语文");
		TranscriptProperties scoped = baseProp(7L, 10L, 100L, "语文");

		when(transcriptsMapper.selectById(10L)).thenReturn(entity);
		when(transcriptPropertiesMapper.selectList(any(Wrapper.class))).thenReturn(List.of(oldProp));
		when(transcriptPropertiesMapper.selectOne(any(Wrapper.class))).thenReturn(scoped);
		when(transcriptPropertiesMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
		when(transcriptPropertiesMapper.updateById(any(TranscriptProperties.class))).thenReturn(1);

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("prop_id", 7L);
		item.put("prop_name", "数学");
		item.put("prop_unit", "分");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("evaluateItems", List.of(item));

		Map<String, Object> out = service.update(100L, 10L, params);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) out.get("evaluate_items");
		assertThat(items).hasSize(1);
		assertThat(items.get(0).get("prop_name")).isEqualTo("数学");
		assertThat(items.get(0).get("prop_id")).isEqualTo(7L);
		verify(transcriptPropertiesMapper).updateById(scoped);
	}

	private static Transcripts baseTranscript(long transcriptId, long companyId) {
		Transcripts t = new Transcripts();
		t.setTranscriptId(transcriptId);
		t.setCompanyId(companyId);
		t.setTranscriptName("期末");
		t.setTemplateName("tpl");
		t.setTranscriptStatus("on");
		t.setCreated(1);
		t.setUpdated(1);
		return t;
	}

	private static TranscriptProperties baseProp(
			long propId, long transcriptId, long companyId, String name) {
		TranscriptProperties p = new TranscriptProperties();
		p.setPropId(propId);
		p.setTranscriptId(transcriptId);
		p.setCompanyId(companyId);
		p.setPropName(name);
		p.setPropUnit("");
		return p;
	}
}
