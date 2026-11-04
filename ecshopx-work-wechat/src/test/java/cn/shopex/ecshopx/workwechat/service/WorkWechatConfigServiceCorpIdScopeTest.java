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

package cn.shopex.ecshopx.workwechat.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.workwechat.config.WorkWechatModuleProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
class WorkWechatConfigServiceCorpIdScopeTest {

	@Mock StringRedisTemplate sharedStringRedisTemplate;
	@Mock ValueOperations<String, String> valueOps;
	@Mock WorkWechatVerifyDomainFileQueryService verifyDomainFileQueryService;
	@Mock WorkWechatModuleProperties moduleProperties;

	WorkWechatConfigService service;
	ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		when(sharedStringRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(verifyDomainFileQueryService.findLatestVerifyFileBaseName(org.mockito.ArgumentMatchers.anyLong()))
				.thenReturn(Optional.empty());
		when(moduleProperties.getDisWorkwechatH5BaseUri()).thenReturn("");
		service =
				new WorkWechatConfigService(
						sharedStringRedisTemplate, objectMapper, verifyDomainFileQueryService, moduleProperties);
	}

	@Test
	void save_foreignCorpidOccupied_rejectsWithoutWritingIndexes() {
		when(valueOps.get("workwechat:configcropid:corp-x"))
				.thenReturn("{\"company_id\":200,\"corpid\":\"corp-x\"}");

		assertThatThrownBy(() -> service.saveWorkWechatConfig(100L, body("corp-x", "app-1")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("corpid");

		verify(valueOps, never()).set(eq("workwechat:configcropid:corp-x"), anyString());
		verify(valueOps, never()).set(eq("workwechat:configappid:app-1"), anyString());
	}

	@Test
	void save_foreignAppidOccupied_rejects() {
		when(valueOps.get("workwechat:configcropid:corp-a")).thenReturn(null);
		when(valueOps.get(org.mockito.ArgumentMatchers.startsWith("workwechat:config:")))
				.thenReturn(null);
		when(valueOps.get("workwechat:configappid:app-taken")).thenReturn("200");

		assertThatThrownBy(() -> service.saveWorkWechatConfig(100L, body("corp-a", "app-taken")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("appid");

		verify(valueOps, never()).set(eq("workwechat:configappid:app-taken"), anyString());
	}

	@Test
	void save_ownCompanySameCorpid_overwritesIndexes() {
		when(valueOps.get("workwechat:configcropid:corp-a"))
				.thenReturn("{\"company_id\":100,\"corpid\":\"corp-a\"}");
		when(valueOps.get(org.mockito.ArgumentMatchers.startsWith("workwechat:config:")))
				.thenReturn("{\"company_id\":100,\"corpid\":\"corp-a\"}");
		when(valueOps.get("workwechat:configappid:app-1")).thenReturn("100");

		service.saveWorkWechatConfig(100L, body("corp-a", "app-1"));

		verify(valueOps).set(eq("workwechat:configcropid:corp-a"), anyString());
		verify(valueOps).set(eq("workwechat:configappid:app-1"), eq("100"));
	}

	@Test
	void save_companyChangesCorpid_deletesOldKeyOwnedBySelf() {
		when(valueOps.get("workwechat:configcropid:corp-new")).thenReturn(null);
		when(valueOps.get("workwechat:configappid:app-1")).thenReturn(null);
		when(valueOps.get(org.mockito.ArgumentMatchers.startsWith("workwechat:config:")))
				.thenReturn("{\"company_id\":100,\"corpid\":\"corp-old\"}");
		when(valueOps.get("workwechat:configcropid:corp-old"))
				.thenReturn("{\"company_id\":100,\"corpid\":\"corp-old\"}");

		service.saveWorkWechatConfig(100L, body("corp-new", "app-1"));

		verify(valueOps).set(eq("workwechat:configcropid:corp-new"), anyString());
		verify(sharedStringRedisTemplate).delete("workwechat:configcropid:corp-old");
	}

	private static Map<String, Object> body(String corpid, String appid) {
		Map<String, Object> app = new LinkedHashMap<>();
		app.put("appid", appid);
		app.put("agent_id", "1");
		app.put("secret", "s");
		app.put("token", "t");
		app.put("aes_key", "k");
		Map<String, Object> agents = new LinkedHashMap<>();
		agents.put("app", app);
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("corpid", corpid);
		root.put("agents", agents);
		return root;
	}
}
