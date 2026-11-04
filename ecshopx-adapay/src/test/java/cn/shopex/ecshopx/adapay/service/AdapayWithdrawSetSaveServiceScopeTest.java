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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.adapay.domain.AdapayWithdrawSet;
import cn.shopex.ecshopx.adapay.mapper.AdapayWithdrawSetMapper;
import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdapayWithdrawSetSaveServiceScopeTest {

	@Mock
	private AdapayWithdrawSetMapper adapayWithdrawSetMapper;

	@Mock
	private DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	@Mock
	private AdapayOperationLogRecordPort adapayOperationLogRecordPort;

	private AdapayWithdrawSetSaveService service;

	@BeforeEach
	void setUp() {
		service =
				new AdapayWithdrawSetSaveService(
						adapayWithdrawSetMapper,
						distributorRepositoryGetInfoSimpleService,
						adapayOperationLogRecordPort);
	}

	@Test
	void save_rejectsCrossShopDistributorId_beforeWrite() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);
		jwt.put("operator_id", 7L);

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("distributor_id", 200L);
		input.put("isAuto", false);

		assertThrows(ForbiddenException.class, () -> service.save(1L, jwt, input));
		verify(adapayWithdrawSetMapper, never()).insert(isA(AdapayWithdrawSet.class));
		verify(adapayWithdrawSetMapper, never()).updateById(isA(AdapayWithdrawSet.class));
		verify(distributorRepositoryGetInfoSimpleService, never())
				.getDistributorInfoForCompanyShop(anyLong(), anyLong());
	}

	@Test
	void save_rejectsShopNotInCompany() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "admin");
		jwt.put("distributor_id", 0L);
		jwt.put("operator_id", 7L);

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("distributor_id", 55L);
		input.put("isAuto", false);

		when(distributorRepositoryGetInfoSimpleService.getDistributorInfoForCompanyShop(eq(1L), eq(55L)))
				.thenReturn(Collections.emptyMap());

		assertThrows(ForbiddenException.class, () -> service.save(1L, jwt, input));
		verify(adapayWithdrawSetMapper, never()).insert(isA(AdapayWithdrawSet.class));
		verify(adapayWithdrawSetMapper, never()).updateById(isA(AdapayWithdrawSet.class));
	}
}
