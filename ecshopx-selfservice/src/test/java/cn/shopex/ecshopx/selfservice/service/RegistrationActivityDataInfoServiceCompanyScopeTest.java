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

package cn.shopex.ecshopx.selfservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeMultiLangReadService;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityRelShopMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Collections;
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

/** ECX-10916：报名活动详情必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationActivityDataInfoServiceCompanyScopeTest {

	@Mock
	private RegistrationActivityMapper registrationActivityMapper;

	@Mock
	private RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;

	@Mock
	private RegistrationActivityRelShopMapper registrationActivityRelShopMapper;

	@Mock
	private AddressMapper addressMapper;

	@Mock
	private EnterprisesMapper enterprisesMapper;

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private DistributorSelfMetaService distributorSelfMetaService;

	@Mock
	private MemberCardGradeMapper memberCardGradeMapper;

	@Mock
	private VipGradeMapper vipGradeMapper;

	@Mock
	private MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService;

	private RegistrationActivityDataInfoService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), RegistrationActivity.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new RegistrationActivityDataInfoService(
						registrationActivityMapper,
						registrationActivityOutsideMultiLangReadService,
						registrationActivityRelShopMapper,
						addressMapper,
						enterprisesMapper,
						distributorMapper,
						distributorSelfMetaService,
						memberCardGradeMapper,
						vipGradeMapper,
						memberCardGradeMultiLangReadService);
	}

	private static RegistrationActivity activity(long companyId) {
		RegistrationActivity a = new RegistrationActivity();
		a.setActivityId(77L);
		a.setCompanyId(companyId);
		a.setActivityName("门店周年庆报名");
		return a;
	}

	@Test
	void foreignCompanyActivity_returnsEmptyWithoutSideEffects() {
		when(registrationActivityMapper.selectById(77L)).thenReturn(activity(99L));

		Object out = service.getDataInfo("77", 11L, "zh-CN");

		assertThat(out).isEqualTo(Collections.emptyList());
		verify(registrationActivityOutsideMultiLangReadService, never())
				.applyDetailOutsideLangOverrides(anyLong(), anyLong(), any(), anyString());
		verify(registrationActivityRelShopMapper, never()).selectList(any());
	}

	@Test
	void ownCompanyActivity_returnsDetail() {
		when(registrationActivityMapper.selectById(77L)).thenReturn(activity(11L));
		when(registrationActivityRelShopMapper.selectList(any()))
				.thenReturn(Collections.emptyList());

		Object out = service.getDataInfo("77", 11L, "zh-CN");

		assertThat(out).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> row = (Map<String, Object>) out;
		assertThat(row.get("activity_name")).isEqualTo("门店周年庆报名");
	}

	@Test
	void missingActivity_returnsEmpty() {
		when(registrationActivityMapper.selectById(404L)).thenReturn(null);

		Object out = service.getDataInfo("404", 11L, "zh-CN");

		assertThat(out).isEqualTo(Collections.emptyList());
	}
}
