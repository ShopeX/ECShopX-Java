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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.form.domain.Transcripts;
import cn.shopex.ecshopx.form.mapper.TranscriptPropertiesMapper;
import cn.shopex.ecshopx.form.mapper.TranscriptsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
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

/** ECX-10912：成绩单更新必须限定当前租户，body 不得改写所有权。 */
@ExtendWith(MockitoExtension.class)
class TranscriptUpdateServiceCompanyScopeTest {

	@Mock
	private TranscriptsMapper transcriptsMapper;

	@Mock
	private TranscriptPropertiesMapper transcriptPropertiesMapper;

	private TranscriptUpdateService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Transcripts.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""),
				cn.shopex.ecshopx.form.domain.TranscriptProperties.class);
	}

	@BeforeEach
	void setUp() {
		service = new TranscriptUpdateService(transcriptsMapper, transcriptPropertiesMapper);
	}

	private static Map<String, Object> paramsWith(String transcriptName) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("transcript_name", transcriptName);
		params.put("evaluateItems", List.of());
		return params;
	}

	@Test
	void updateForeignCompanyTranscript_isRejectedWithoutUpdate() {
		Transcripts foreign = new Transcripts();
		foreign.setTranscriptId(77L);
		foreign.setCompanyId(99L);
		when(transcriptsMapper.selectById(77L)).thenReturn(foreign);

		assertThatThrownBy(() -> service.update(11L, 77L, paramsWith("新名字")))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该成绩单");

		verify(transcriptsMapper, never()).updateById(any(Transcripts.class));
	}

	@Test
	void updateOwnCompanyTranscript_succeeds() {
		Transcripts own = new Transcripts();
		own.setTranscriptId(77L);
		own.setCompanyId(11L);
		own.setTranscriptName("旧名字");
		when(transcriptsMapper.selectById(77L)).thenReturn(own);
		when(transcriptPropertiesMapper.selectList(any())).thenReturn(List.of());

		Map<String, Object> out = service.update(11L, 77L, paramsWith("新名字"));

		assertThat(out.get("transcript_name")).isEqualTo("新名字");
		verify(transcriptsMapper).updateById(any(Transcripts.class));
	}

	@Test
	void bodyCompanyId_cannotOverwriteOwnership() {
		Transcripts own = new Transcripts();
		own.setTranscriptId(77L);
		own.setCompanyId(11L);
		own.setTranscriptName("旧名字");
		when(transcriptsMapper.selectById(77L)).thenReturn(own);
		when(transcriptPropertiesMapper.selectList(any())).thenReturn(List.of());

		Map<String, Object> params = paramsWith("新名字");
		params.put("company_id", 99L);

		service.update(11L, 77L, params);

		ArgumentCaptor<Transcripts> captor = ArgumentCaptor.forClass(Transcripts.class);
		verify(transcriptsMapper).updateById(captor.capture());
		assertThat(captor.getValue().getCompanyId()).isEqualTo(11L);
	}
}
