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

package cn.shopex.ecshopx.aliyunsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10930：场景详情必须限定当前公司，不匹配时返回现有占位结果。 */
@ExtendWith(MockitoExtension.class)
class AliyunsmsSceneDetailServiceCompanyScopeTest {

	@Mock
	private SceneMapper sceneMapper;

	private AliyunsmsSceneDetailService service;

	@BeforeEach
	void setUp() {
		service = new AliyunsmsSceneDetailService(sceneMapper, new ObjectMapper());
	}

	private static Scene scene(long id, Long companyId, String name, String template) {
		Scene scene = new Scene();
		scene.setId(id);
		scene.setCompanyId(companyId);
		scene.setSceneName(name);
		scene.setSceneTitle(name);
		scene.setStatus("enabled");
		scene.setTemplateType("0");
		scene.setDefaultTemplate(template);
		scene.setVariables("{\"code\":\"numberCaptcha\"}");
		return scene;
	}

	@Test
	void foreignCompanyScene_returnsPlaceholderWithoutSceneFields() {
		when(sceneMapper.selectById(77L)).thenReturn(scene(77L, 99L, "验证码", "您的验证码${code}"));

		Map<String, Object> out = service.getDetail(77L, 11L);

		assertThat(out).containsOnlyKeys("default_template", "variables");
		assertThat(out.get("default_template")).isNull();
		assertThat(out.get("variables")).isNull();
	}

	@Test
	void missingScene_returnsSamePlaceholder() {
		when(sceneMapper.selectById(404L)).thenReturn(null);

		Map<String, Object> out = service.getDetail(404L, 11L);

		assertThat(out).containsOnlyKeys("default_template", "variables");
		assertThat(out.get("default_template")).isNull();
		assertThat(out.get("variables")).isNull();
	}

	@Test
	void sceneWithoutCompany_returnsSamePlaceholder() {
		Scene row = scene(77L, 99L, "验证码", "您的验证码${code}");
		row.setCompanyId(null);
		when(sceneMapper.selectById(77L)).thenReturn(row);

		Map<String, Object> out = service.getDetail(77L, 11L);

		assertThat(out).containsOnlyKeys("default_template", "variables");
	}

	@Test
	void ownCompanyScene_returnsDetailAndParsedVariables() {
		when(sceneMapper.selectById(77L)).thenReturn(scene(77L, 11L, "验证码", "您的验证码${code}"));

		Map<String, Object> out = service.getDetail(77L, 11L);

		assertThat(out.get("scene_name")).isEqualTo("验证码");
		assertThat(out.get("company_id")).isEqualTo(11L);
		assertThat(out.get("default_template")).isEqualTo("您的验证码${code}");
		assertThat(out.get("variables")).isEqualTo(Map.of("code", "numberCaptcha"));
	}
}
