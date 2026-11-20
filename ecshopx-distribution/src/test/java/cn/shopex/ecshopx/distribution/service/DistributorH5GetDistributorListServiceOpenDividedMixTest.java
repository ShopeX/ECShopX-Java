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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorWhiteList;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorWxappShopListFilter;
import cn.shopex.ecshopx.distribution.repository.DistributorWxappShopListRepository;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListAppendBlocksService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListFilterBuildService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListRowAssemblyService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListSalesNetRankingService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListTagMetadataReadService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.dto.DistributorWxappShopListFilterBuildResult;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.dto.DistributorWxappShopListQuery;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.address.MemberAddressDefaultReadService;
import cn.shopex.ecshopx.members.service.address.MemberAddressLatLngNonNumericEnqueueService;
import cn.shopex.ecshopx.merchant.service.MerchantDisabledDistributorIdsQueryService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-11159：默认店铺列表按店混排，不依赖公司级 status。 */
@ExtendWith(MockitoExtension.class)
class DistributorH5GetDistributorListServiceOpenDividedMixTest {

	private static final long COMPANY_ID = 11L;
	private static final long USER_ID = 99L;

	@Mock
	private DistributorWxappShopListTagMetadataReadService distributorWxappShopListTagMetadataReadService;

	@Mock
	private MemberAddressDefaultReadService memberAddressDefaultReadService;

	@Mock
	private MemberAddressLatLngNonNumericEnqueueService memberAddressLatLngNonNumericEnqueueService;

	@Mock
	private DistributorWxappShopListFilterBuildService distributorWxappShopListFilterBuildService;

	@Mock
	private DistributorWxappShopListSalesNetRankingService distributorWxappShopListSalesNetRankingService;

	@Mock
	private MerchantDisabledDistributorIdsQueryService merchantDisabledDistributorIdsQueryService;

	@Mock
	private DistributorWhiteListMapper distributorWhiteListMapper;

	@Mock
	private MemberAccountService memberAccountService;

	@Mock
	private DistributorWxappShopListRepository distributorWxappShopListRepository;

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private DistributorWxappShopListRowAssemblyService distributorWxappShopListRowAssemblyService;

	@Mock
	private DistributorWxappShopListAppendBlocksService distributorWxappShopListAppendBlocksService;

	@Mock
	private DistributorCategoryService distributorCategoryService;

	private DistributorH5GetDistributorListService service;

	@BeforeAll
	static void initTables() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Distributor.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), DistributorWhiteList.class);
	}

	@BeforeEach
	void setUp() {
		lenient()
				.when(distributorWxappShopListTagMetadataReadService.listFrontShowTagRows(anyLong()))
				.thenReturn(List.of());
		lenient()
				.when(distributorWxappShopListFilterBuildService.build(anyLong(), anyLong(), any(), any()))
				.thenReturn(new DistributorWxappShopListFilterBuildResult(new DistributorWxappShopListFilter(), 0, false));
		lenient()
				.when(merchantDisabledDistributorIdsQueryService.listDistributorIdsLinkedToDisabledMerchants(anyLong()))
				.thenReturn(List.of());
		lenient()
				.when(distributorMapper.selectList(any()))
				.thenReturn(List.of());
		lenient()
				.when(distributorWxappShopListSalesNetRankingService.buildNetSalesMapForCompany(anyLong(), anyLong()))
				.thenReturn(Map.of());
		lenient().when(distributorWxappShopListRepository.countByFilter(any())).thenReturn(0L);
		lenient().when(distributorWxappShopListRepository.selectPageByFilter(any())).thenReturn(List.of());
		lenient()
				.when(
						distributorWxappShopListRowAssemblyService.toResponseRows(
								anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any(), any()))
				.thenReturn(List.of());
		service =
				new DistributorH5GetDistributorListService(
						distributorWxappShopListTagMetadataReadService,
						memberAddressDefaultReadService,
						memberAddressLatLngNonNumericEnqueueService,
						distributorWxappShopListFilterBuildService,
						distributorWxappShopListSalesNetRankingService,
						merchantDisabledDistributorIdsQueryService,
						distributorWhiteListMapper,
						memberAccountService,
						distributorWxappShopListRepository,
						distributorMapper,
						distributorWxappShopListRowAssemblyService,
						distributorWxappShopListAppendBlocksService,
						distributorCategoryService);
	}

	private static DistributorWxappShopListQuery query(String showType) {
		return new DistributorWxappShopListQuery(
				1,
				10,
				null,
				null,
				null,
				null,
				null,
				null,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				null,
				0,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				0,
				showType);
	}

	@Test
	void defaultList_appliesOpenDividedMixWithWhitelistIds_withoutCompanyStatus() {
		DistributorWhiteList wl = new DistributorWhiteList();
		wl.setId(1L);
		wl.setDistributorId(501L);
		wl.setMobile("13800000000");
		wl.setCompanyId(COMPANY_ID);
		when(distributorWhiteListMapper.selectList(any())).thenReturn(List.of(wl));
		when(memberAccountService.findMobileStored(COMPANY_ID, USER_ID)).thenReturn("13800000000");

		service.getDistributorList(COMPANY_ID, USER_ID, query(null), "zh-CN");

		ArgumentCaptor<DistributorWxappShopListFilter> cap =
				ArgumentCaptor.forClass(DistributorWxappShopListFilter.class);
		verify(distributorWxappShopListRepository).countByFilter(cap.capture());
		DistributorWxappShopListFilter filter = cap.getValue();
		assertThat(filter.getOpenDivided()).isNull();
		assertThat(filter.isOpenDividedPublicOrWhitelist()).isTrue();
		assertThat(filter.getOpenDividedWhitelistDistributorIds()).containsExactly(501L);
	}

	@Test
	void showTypeSelf_listsOnlyWhitelistedOpenDividedShops() {
		DistributorWhiteList wl = new DistributorWhiteList();
		wl.setId(2L);
		wl.setDistributorId(502L);
		wl.setMobile("13800000000");
		wl.setCompanyId(COMPANY_ID);
		when(distributorWhiteListMapper.selectList(any())).thenReturn(List.of(wl));
		when(memberAccountService.findMobileStored(COMPANY_ID, USER_ID)).thenReturn("13800000000");

		service.getDistributorList(COMPANY_ID, USER_ID, query("self"), "zh-CN");

		ArgumentCaptor<DistributorWxappShopListFilter> cap =
				ArgumentCaptor.forClass(DistributorWxappShopListFilter.class);
		verify(distributorWxappShopListRepository).countByFilter(cap.capture());
		DistributorWxappShopListFilter filter = cap.getValue();
		assertThat(filter.isOpenDividedPublicOrWhitelist()).isFalse();
		assertThat(filter.getOpenDivided()).isEqualTo(1);
		assertThat(filter.getDistributorIdInList()).containsExactly(502L);
	}
}
