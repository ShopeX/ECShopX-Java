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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.bspay.domain.EntryApply;
import cn.shopex.ecshopx.bspay.domain.UserEnt;
import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.RegionsThirdMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorInfoResolveService;
import cn.shopex.ecshopx.distribution.service.LocalDeliveryDistributorInfoService;
import cn.shopex.ecshopx.orders.mapper.CompanyRelDadaMapper;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BsPaySubUserSubApproveInfoServiceScopeTest {

	@Mock
	private EntryApplyMapper entryApplyMapper;

	@Mock
	private UserEntMapper userEntMapper;

	@Mock
	private UserIndvMapper userIndvMapper;

	@Mock
	private UserCardMapper userCardMapper;

	@Mock
	private RegionsThirdMapper regionsThirdMapper;

	@Mock
	private OperatorsMapper operatorsMapper;

	@Mock
	private OperatorsQueryService operatorsQueryService;

	@Mock
	private DistributorInfoResolveService distributorInfoResolveService;

	@Mock
	private LocalDeliveryDistributorInfoService localDeliveryDistributorInfoService;

	@Mock
	private CompanyRelDadaMapper companyRelDadaMapper;

	private BsPaySubUserSubApproveInfoService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), EntryApply.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), UserEnt.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), UserIndv.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new BsPaySubUserSubApproveInfoService(
						entryApplyMapper,
						userEntMapper,
						userIndvMapper,
						userCardMapper,
						regionsThirdMapper,
						operatorsMapper,
						operatorsQueryService,
						distributorInfoResolveService,
						localDeliveryDistributorInfoService,
						companyRelDadaMapper,
						"");
	}

	@Test
	void subApproveInfo_rejectsForeignTenantApplyBeforeKycLoad() {
		when(entryApplyMapper.selectById(anyLong())).thenAnswer(inv -> {
			EntryApply foreign = new EntryApply();
			foreign.setId(9L);
			foreign.setCompanyId(200L);
			foreign.setUserType("ent");
			foreign.setUserId("77");
			return foreign;
		});
		when(entryApplyMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertThrows(
				ResourceException.class, () -> service.subApproveInfo(100L, "9", "zh-CN"));
		verify(userEntMapper, never()).selectById(anyLong());
		verify(userEntMapper, never()).selectOne(any(Wrapper.class));
		verify(userIndvMapper, never()).selectById(anyLong());
		verify(userIndvMapper, never()).selectOne(any(Wrapper.class));
	}
}
