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

package cn.shopex.ecshopx.distribution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorWhiteList;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-11159：按店 open_divided 生效，不再依赖公司级 OpenDistributorDivided.status。 */
@ExtendWith(MockitoExtension.class)
class DistributorWhiteListCheckUserValidServicePerShopTest {

	private static final long COMPANY_ID = 11L;
	private static final long USER_ID = 99L;
	private static final long DISTRIBUTOR_ID = 501L;

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private DistributorWhiteListMapper distributorWhiteListMapper;

	@Mock
	private MemberAccountService memberAccountService;

	private DistributorWhiteListCheckUserValidService service;

	@BeforeAll
	static void initTables() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Distributor.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), DistributorWhiteList.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new DistributorWhiteListCheckUserValidService(
						distributorMapper, distributorWhiteListMapper, memberAccountService);
	}

	@Test
	void checkUserValidCommon_whenShopOpenDividedAndUserNotWhitelisted_deniesEvenIfCompanyStatusOff() {
		Distributor shop = new Distributor();
		shop.setDistributorId(DISTRIBUTOR_ID);
		shop.setOpenDivided(1L);
		when(distributorMapper.selectOne(any())).thenReturn(shop);
		when(memberAccountService.findMobileStored(COMPANY_ID, USER_ID)).thenReturn("13800000000");
		when(distributorWhiteListMapper.selectOne(any())).thenReturn(null);

		boolean ok = service.checkUserValidCommon(DISTRIBUTOR_ID, USER_ID, COMPANY_ID);

		assertThat(ok).isFalse();
	}

	@Test
	void checkUserValidCommon_whenShopOpenDividedAndUserWhitelisted_allowsWithoutCompanyStatus() {
		Distributor shop = new Distributor();
		shop.setDistributorId(DISTRIBUTOR_ID);
		shop.setOpenDivided(1L);
		when(distributorMapper.selectOne(any())).thenReturn(shop);
		when(memberAccountService.findMobileStored(COMPANY_ID, USER_ID)).thenReturn("13800000000");
		DistributorWhiteList wl = new DistributorWhiteList();
		wl.setDistributorId(DISTRIBUTOR_ID);
		wl.setMobile("13800000000");
		when(distributorWhiteListMapper.selectOne(any())).thenReturn(wl);

		boolean ok = service.checkUserValidCommon(DISTRIBUTOR_ID, USER_ID, COMPANY_ID);

		assertThat(ok).isTrue();
	}

	@Test
	void checkUserValidCommon_whenShopNotOpenDivided_allowsWithoutWhitelistLookup() {
		Distributor shop = new Distributor();
		shop.setDistributorId(DISTRIBUTOR_ID);
		shop.setOpenDivided(0L);
		when(distributorMapper.selectOne(any())).thenReturn(shop);

		boolean ok = service.checkUserValidCommon(DISTRIBUTOR_ID, USER_ID, COMPANY_ID);

		assertThat(ok).isTrue();
		verify(distributorWhiteListMapper, never()).selectOne(any());
	}
}
