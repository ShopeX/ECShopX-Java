package cn.shopex.ecshopx.distribution.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorWxappShopListFilter;
import cn.shopex.ecshopx.distribution.repository.DistributorWxappShopListRepository;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidSettingService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorH5GetDistributorListServiceLatLngEnqueueTest {

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
	private DistributorIsValidSettingService distributorIsValidSettingService;

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

	@BeforeEach
	void setUp() {
		lenient()
				.when(distributorWxappShopListTagMetadataReadService.listFrontShowTagRows(anyLong()))
				.thenReturn(List.of());
		lenient()
				.when(distributorWxappShopListFilterBuildService.build(anyLong(), anyLong(), any(), any()))
				.thenReturn(new DistributorWxappShopListFilterBuildResult(new DistributorWxappShopListFilter(), 0, true));
		service =
				new DistributorH5GetDistributorListService(
						distributorWxappShopListTagMetadataReadService,
						memberAddressDefaultReadService,
						memberAddressLatLngNonNumericEnqueueService,
						distributorWxappShopListFilterBuildService,
						distributorWxappShopListSalesNetRankingService,
						merchantDisabledDistributorIdsQueryService,
						distributorIsValidSettingService,
						distributorWhiteListMapper,
						memberAccountService,
						distributorWxappShopListRepository,
						distributorMapper,
						distributorWxappShopListRowAssemblyService,
						distributorWxappShopListAppendBlocksService,
						distributorCategoryService);
	}

	private static DistributorWxappShopListQuery baselineQuery() {
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
				null);
	}

	@Test
	@DisplayName("getDistributorList when userId>0 and default address present calls enqueueIfNeeded once with same default map")
	void getDistributorList_whenUserIdPositiveAndDefaultAddress_present_callsEnqueueIfNeededOnceWithDefaultMap() {
		Map<String, Object> defaultAddrMap = new LinkedHashMap<>();
		defaultAddrMap.put("address_id", Long.valueOf(501L));
		defaultAddrMap.put("lat", "x");
		defaultAddrMap.put("lng", "y");
		when(memberAddressDefaultReadService.getDefaultAddress(COMPANY_ID, USER_ID)).thenReturn(defaultAddrMap);

		service.getDistributorList(COMPANY_ID, USER_ID, baselineQuery(), "zh-CN");

		verify(memberAddressLatLngNonNumericEnqueueService, times(1))
				.enqueueIfNeeded(eq(COMPANY_ID), eq(USER_ID), same(defaultAddrMap));
	}

	@Test
	@DisplayName("getDistributorList when userId<=0 never calls enqueueIfNeeded")
	void getDistributorList_whenUserIdNonPositive_neverCallsEnqueueIfNeeded() {
		service.getDistributorList(COMPANY_ID, 0L, baselineQuery(), "zh-CN");

		verify(memberAddressLatLngNonNumericEnqueueService, never()).enqueueIfNeeded(anyLong(), anyLong(), any());
	}
}
