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

package cn.shopex.ecshopx.members.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MedicationPersonnel;
import cn.shopex.ecshopx.members.mapper.MedicationPersonnelMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicationPersonnelUpdateServiceScopeTest {

	@Mock
	private MedicationPersonnelMapper medicationPersonnelMapper;

	private MedicationPersonnelUpdateService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), MedicationPersonnel.class);
	}

	@BeforeEach
	void setUp() {
		service = new MedicationPersonnelUpdateService(medicationPersonnelMapper);
	}

	@Test
	void update_rejectsRecordNotOwnedByCaller() {
		MedicationPersonnel foreign = new MedicationPersonnel();
		foreign.setId(99L);
		foreign.setCompanyId(100L);
		foreign.setUserId(2L);
		when(medicationPersonnelMapper.selectOne(any(Wrapper.class))).thenReturn(foreign);

		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								service.update(
										100L,
										1L,
										99L,
										"张三",
										"110101199001011234",
										20,
										1,
										"13800000000",
										2));
		assertEquals("不存在该用药人", ex.getMessage());
		verify(medicationPersonnelMapper, never()).update(isNull(), any());
	}

	@Test
	void update_ownRecord_succeedsWithoutOwnershipTransfer() {
		MedicationPersonnel own = new MedicationPersonnel();
		own.setId(7L);
		own.setCompanyId(100L);
		own.setUserId(1L);
		when(medicationPersonnelMapper.selectOne(any(Wrapper.class))).thenReturn(own).thenReturn(null);
		when(medicationPersonnelMapper.update(isNull(), any())).thenReturn(1);

		Map<String, Object> result =
				service.update(100L, 1L, 7L, "李四", "110101199001011235", 30, 2, "13900000000", 3);
		assertEquals(Boolean.TRUE, result.get("success"));
		verify(medicationPersonnelMapper).update(isNull(), any());
	}
}
