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

package cn.shopex.ecshopx.datacube.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.datacube.mapper.MonitorsMapper;
import cn.shopex.ecshopx.datacube.mapper.SourcesMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

/** ECX-10920：追踪上报的 monitor/source 须为本租户已注册实体，防任意 ID 刷 Redis key。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrackViewNumServiceMonitorScopeTest {

	@Mock
	private MemberAccountService memberAccountService;

	@Mock
	private StringRedisTemplate datacubeStringRedisTemplate;

	@Mock
	private UvBloomFilterService uvBloomFilterService;

	@Mock
	private MonitorsMapper monitorsMapper;

	@Mock
	private SourcesMapper sourcesMapper;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	private TrackViewNumService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, cn.shopex.ecshopx.datacube.domain.Monitors.class);
		TableInfoHelper.initTableInfo(assistant, cn.shopex.ecshopx.datacube.domain.Sources.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new TrackViewNumService(
						memberAccountService,
						datacubeStringRedisTemplate,
						uvBloomFilterService,
						monitorsMapper,
						sourcesMapper);
		when(datacubeStringRedisTemplate.opsForHash()).thenReturn(hashOperations);
	}

	@Test
	void unregisteredMonitor_isSilentlyIgnored() {
		when(monitorsMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

		service.addViewNum(11L, "77", "5", "open-1");

		verifyNoInteractions(hashOperations);
		verifyNoInteractions(uvBloomFilterService);
	}

	@Test
	void unregisteredSource_isSilentlyIgnored() {
		when(monitorsMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		when(sourcesMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

		service.addViewNum(11L, "77", "5", "open-1");

		verifyNoInteractions(hashOperations);
		verifyNoInteractions(uvBloomFilterService);
	}

	@Test
	void nonNumericMonitorId_isSilentlyIgnored() {
		service.addViewNum(11L, "abc-77", "5", "open-1");

		verifyNoInteractions(hashOperations);
		verifyNoInteractions(uvBloomFilterService);
		verify(monitorsMapper, never()).selectCount(any(Wrapper.class));
	}

	@Test
	void registeredMonitorAndSource_countsNormally() {
		when(monitorsMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		when(sourcesMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		when(uvBloomFilterService.checkAndAdd("77", "5", "open-1")).thenReturn(true);
		when(memberAccountService.getWechatSimpleUser(any())).thenReturn(java.util.Map.of());

		service.addViewNum(11L, "77", "5", "open-1");

		// PV + UV（bloom 判新）各一次 increment
		verify(hashOperations, org.mockito.Mockito.times(2)).increment(anyString(), anyString(), eq(1L));
		verify(uvBloomFilterService).checkAndAdd("77", "5", "open-1");
	}
}
