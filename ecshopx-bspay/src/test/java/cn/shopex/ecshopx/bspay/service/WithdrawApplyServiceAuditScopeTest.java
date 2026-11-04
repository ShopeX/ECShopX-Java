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

package cn.shopex.ecshopx.bspay.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.bspay.domain.WithdrawApply;
import cn.shopex.ecshopx.bspay.mapper.DivFeeMapper;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import cn.shopex.ecshopx.bspay.mapper.WithdrawApplyMapper;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WithdrawApplyServiceAuditScopeTest {

	@Mock
	private WithdrawApplyMapper withdrawApplyMapper;

	@Mock
	private DivFeeMapper divFeeMapper;

	@Mock
	private EntryApplyMapper entryApplyMapper;

	@Mock
	private UserEntMapper userEntMapper;

	@Mock
	private UserIndvMapper userIndvMapper;

	@Mock
	private BsPayPaymentSettingService bsPayPaymentSettingService;

	@Mock
	private WithdrawApplyExportQueryService withdrawApplyExportQueryService;

	@Mock
	private WithdrawApplyAdminFilterBuilder withdrawApplyAdminFilterBuilder;

	@Mock
	private DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;

	@Mock
	private DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	@Mock
	private MerchantMapper merchantMapper;

	@Mock
	private UserCardMapper userCardMapper;

	@Mock
	private WithdrawHuifuWithdrawTransactionalService withdrawHuifuWithdrawTransactionalService;

	@Mock
	private PlatformTransactionManager platformTransactionManager;

	private WithdrawApplyService service;

	@BeforeEach
	void setUp() {
		service =
				new WithdrawApplyService(
						withdrawApplyMapper,
						divFeeMapper,
						entryApplyMapper,
						userEntMapper,
						userIndvMapper,
						bsPayPaymentSettingService,
						withdrawApplyExportQueryService,
						withdrawApplyAdminFilterBuilder,
						distributionDistributorSelfReadMapper,
						distributorRepositoryGetInfoSimpleService,
						merchantMapper,
						userCardMapper,
						withdrawHuifuWithdrawTransactionalService,
						platformTransactionManager);
	}

	@Test
	void auditWithdraw_rejectsOtherTenantApplyId() {
		WithdrawApply apply = new WithdrawApply();
		apply.setId(9L);
		apply.setCompanyId(200L);
		apply.setStatus(0);
		apply.setAmount(100);
		when(withdrawApplyMapper.selectById(9L)).thenReturn(apply);

		assertThrows(
				ForbiddenException.class,
				() -> service.auditWithdraw(9L, 100L, "approve", "13800000000", 1L, "", "admin"));
		verify(withdrawApplyMapper, never()).update(any(), any());
	}

	@Test
	void executeHuifuWithdraw_rejectsOtherTenantApplyId() {
		WithdrawApply apply = new WithdrawApply();
		apply.setId(9L);
		apply.setCompanyId(200L);
		apply.setStatus(1);
		when(withdrawApplyMapper.selectById(9L)).thenReturn(apply);

		assertThrows(ForbiddenException.class, () -> service.executeHuifuWithdraw(9L, 100L));
		verify(withdrawHuifuWithdrawTransactionalService, never())
				.runWithdrawEncashmentInTransaction(any(Long.class), any(), any());
	}
}
