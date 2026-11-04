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

package cn.shopex.ecshopx.adapay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsCommandService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DealerDelSubServiceScopeTest {

	@Mock OperatorsQueryService operatorsQueryService;
	@Mock OperatorsCommandService operatorsCommandService;

	DealerDelSubService service;

	@BeforeEach
	void setUp() {
		service = new DealerDelSubService(operatorsQueryService, operatorsCommandService);
	}

	@Test
	void delDealerSub_crossCompany_notFound_doesNotDelete() {
		when(operatorsQueryService.getInfo(any())).thenReturn(Map.of());

		assertThatThrownBy(() -> service.delDealerSub(100L, "42"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("未找到删除账号");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> filterCaptor = ArgumentCaptor.forClass(Map.class);
		verify(operatorsQueryService).getInfo(filterCaptor.capture());
		assertThat(filterCaptor.getValue().get("company_id")).isEqualTo(100L);
		assertThat(filterCaptor.getValue().get("operator_id")).isEqualTo("42");
		verify(operatorsCommandService, never()).deleteByOperatorIdOnly(anyLong());
		verify(operatorsCommandService, never()).deleteByOperatorIdAndCompanyId(anyLong(), anyLong());
	}

	@Test
	void delDealerSub_ownCompanySub_deletesWithCompanyScope() {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("operator_id", 42L);
		info.put("is_dealer_main", 0);
		info.put("company_id", 100L);
		when(operatorsQueryService.getInfo(any())).thenReturn(info);

		service.delDealerSub(100L, "42");

		verify(operatorsCommandService).deleteByOperatorIdAndCompanyId(eq(100L), eq(42L));
		verify(operatorsCommandService, never()).deleteByOperatorIdOnly(anyLong());
	}

	@Test
	void delDealerSub_mainAccount_rejects() {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("operator_id", 42L);
		info.put("is_dealer_main", 1);
		when(operatorsQueryService.getInfo(any())).thenReturn(info);

		assertThatThrownBy(() -> service.delDealerSub(100L, "42"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("主账号不可删除");

		verify(operatorsCommandService, never()).deleteByOperatorIdOnly(anyLong());
		verify(operatorsCommandService, never()).deleteByOperatorIdAndCompanyId(anyLong(), anyLong());
	}
}
