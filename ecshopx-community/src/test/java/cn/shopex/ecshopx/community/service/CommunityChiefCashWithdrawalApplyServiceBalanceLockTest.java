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

package cn.shopex.ecshopx.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.domain.CommunityChief;
import cn.shopex.ecshopx.community.domain.CommunityChiefCashWithdrawal;
import cn.shopex.ecshopx.community.domain.CommunityChiefDistributor;
import cn.shopex.ecshopx.community.mapper.CommunityChiefCashWithdrawalMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefDistributorMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** ECX-10895：团长提现申请须先锁住团长，超额时不插入。 */
class CommunityChiefCashWithdrawalApplyServiceBalanceLockTest {

	private CommunityChiefService communityChiefService;
	private CommunityChiefDistributorMapper distributorMapper;
	private CommunityChiefMapper chiefMapper;
	private CommunityChiefCashWithdrawalMapper withdrawalMapper;
	private CommunityChiefCashWithdrawalQueryService queryService;
	private SensitiveFieldEncryptor encryptor;
	private CommunityChiefCashWithdrawalFrontQueryService frontQueryService;
	private CommunityChiefCashWithdrawalApplyService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, CommunityChiefDistributor.class);
		TableInfoHelper.initTableInfo(assistant, CommunityChief.class);
	}

	@BeforeEach
	void setUp() {
		communityChiefService = mock(CommunityChiefService.class);
		distributorMapper = mock(CommunityChiefDistributorMapper.class);
		chiefMapper = mock(CommunityChiefMapper.class);
		withdrawalMapper = mock(CommunityChiefCashWithdrawalMapper.class);
		queryService = mock(CommunityChiefCashWithdrawalQueryService.class);
		encryptor = mock(SensitiveFieldEncryptor.class);
		frontQueryService = mock(CommunityChiefCashWithdrawalFrontQueryService.class);
		service = new CommunityChiefCashWithdrawalApplyService(
				communityChiefService,
				distributorMapper,
				chiefMapper,
				withdrawalMapper,
				queryService,
				encryptor,
				frontQueryService);
	}

	@Test
	void applyForH5_locksChiefAndRejectsOverdrawWithoutInsert() throws Exception {
		when(communityChiefService.resolveChiefIdForCashWithdrawalApply(100L, Map.of())).thenReturn(7L);
		CommunityChiefDistributor distributor = new CommunityChiefDistributor();
		distributor.setDistributorId(3L);
		when(distributorMapper.selectOne(any())).thenReturn(distributor);
		when(chiefMapper.selectOne(any())).thenReturn(new CommunityChief());
		when(queryService.buildChiefRebateAggregateByChiefIds(100L, List.of(7L)))
				.thenReturn(Map.of(7L, Map.of("cash_withdrawal_rebate", 5_000L)));

		assertThatThrownBy(() -> service.applyForH5(100L, Map.of(), 10_000L, "wechat", "name"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("申请提现金额额度超出限制");

		ArgumentCaptor<LambdaQueryWrapper<CommunityChief>> chiefQuery = chiefQueryCaptor();
		verify(chiefMapper).selectOne(chiefQuery.capture());
		assertThat(chiefQuery.getValue().getSqlSegment()).contains("FOR UPDATE");
		verify(withdrawalMapper, never()).insert(any(CommunityChiefCashWithdrawal.class));

		Transactional transactional = CommunityChiefCashWithdrawalApplyService.class
				.getMethod("applyForH5", long.class, Map.class, long.class, String.class, String.class)
				.getAnnotation(Transactional.class);
		assertThat(transactional.isolation()).isEqualTo(Isolation.READ_COMMITTED);
	}

	@Test
	void applyForH5_insertsApplyWhenBalanceCoversTheRequest() {
		when(communityChiefService.resolveChiefIdForCashWithdrawalApply(100L, Map.of())).thenReturn(7L);
		CommunityChiefDistributor distributor = new CommunityChiefDistributor();
		distributor.setDistributorId(3L);
		when(distributorMapper.selectOne(any())).thenReturn(distributor);
		when(chiefMapper.selectOne(any())).thenReturn(new CommunityChief());
		when(queryService.buildChiefRebateAggregateByChiefIds(100L, List.of(7L)))
				.thenReturn(Map.of(7L, Map.of("cash_withdrawal_rebate", 20_000L)));
		when(encryptor.encrypt(any())).thenReturn("enc");
		when(withdrawalMapper.insert(any(CommunityChiefCashWithdrawal.class))).thenAnswer(invocation -> {
			CommunityChiefCashWithdrawal row = invocation.getArgument(0);
			row.setId(11L);
			return 1;
		});
		when(frontQueryService.toH5WithdrawalLine(any())).thenAnswer(invocation -> {
			CommunityChiefCashWithdrawal row = invocation.getArgument(0);
			return Map.of("id", row.getId(), "status", row.getStatus(), "money", row.getMoney());
		});

		Map<String, Object> result = service.applyForH5(100L, Map.of(), 10_000L, "wechat", "name");

		assertThat(result.get("status")).isEqualTo("apply");
		assertThat(result.get("money")).isEqualTo(10_000);
		ArgumentCaptor<CommunityChiefCashWithdrawal> inserted =
				ArgumentCaptor.forClass(CommunityChiefCashWithdrawal.class);
		verify(withdrawalMapper).insert(inserted.capture());
		assertThat(inserted.getValue().getStatus()).isEqualTo("apply");
		assertThat(inserted.getValue().getChiefId()).isEqualTo(7L);
		assertThat(inserted.getValue().getCompanyId()).isEqualTo(100L);
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<LambdaQueryWrapper<CommunityChief>> chiefQueryCaptor() {
		return ArgumentCaptor.forClass(LambdaQueryWrapper.class);
	}
}
