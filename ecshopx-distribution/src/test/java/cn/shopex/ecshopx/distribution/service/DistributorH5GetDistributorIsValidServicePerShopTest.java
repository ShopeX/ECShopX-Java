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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.integration.MapGeocodeClient;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidAppendFieldsService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidCoreQueryService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidNearShopService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidNostoresBranchService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidWhiteListBranchService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.dto.DistributorIsValidQuery;
import cn.shopex.ecshopx.members.service.address.MemberAddressDefaultReadService;
import cn.shopex.ecshopx.members.service.address.MemberAddressLatLngNonNumericEnqueueService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-11159：isValid / 附近店按店隔离，不再依赖公司级 status。 */
@ExtendWith(MockitoExtension.class)
class DistributorH5GetDistributorIsValidServicePerShopTest {

	private static final long COMPANY_ID = 38L;
	private static final long USER_ID = 100L;

	@Mock
	private DistributorIsValidWhiteListBranchService distributorIsValidWhiteListBranchService;

	@Mock
	private MemberAddressDefaultReadService memberAddressDefaultReadService;

	@Mock
	private MemberAddressLatLngNonNumericEnqueueService memberAddressLatLngNonNumericEnqueueService;

	@Mock
	private MapGeocodeClient mapGeocodeClient;

	@Mock
	private DistributorIsValidCoreQueryService distributorIsValidCoreQueryService;

	@Mock
	private DistributorIsValidNostoresBranchService distributorIsValidNostoresBranchService;

	@Mock
	private DistributorIsValidNearShopService distributorIsValidNearShopService;

	@Mock
	private DistributorIsValidAppendFieldsService distributorIsValidAppendFieldsService;

	@Mock
	private DistributionStoreEntryRuleRedisService distributionStoreEntryRuleRedisService;

	@Mock
	private DistributorWhiteListCheckUserValidService distributorWhiteListCheckUserValidService;

	private DistributorH5GetDistributorIsValidService service;

	@BeforeEach
	void setUp() {
		lenient()
				.when(distributionStoreEntryRuleRedisService.getInRule(anyLong()))
				.thenReturn(Map.of("radio_type", Integer.valueOf(2)));
		service =
				new DistributorH5GetDistributorIsValidService(
						distributorIsValidWhiteListBranchService,
						memberAddressDefaultReadService,
						memberAddressLatLngNonNumericEnqueueService,
						mapGeocodeClient,
						distributorIsValidCoreQueryService,
						distributorIsValidNostoresBranchService,
						distributorIsValidNearShopService,
						distributorIsValidAppendFieldsService,
						distributionStoreEntryRuleRedisService,
						distributorWhiteListCheckUserValidService);
	}

	@Test
	void showTypeSelf_entersWhitelistBranch_withoutCompanyStatus() {
		Map<String, Object> wl = new LinkedHashMap<>();
		wl.put("distributor_id", Long.valueOf(501L));
		wl.put("is_valid", "true");
		when(distributorIsValidWhiteListBranchService.getWhiteListDistributor(
						eq(USER_ID), eq(COMPANY_ID), any(), any(), any()))
				.thenReturn(wl);

		DistributorIsValidQuery query =
				DistributorIsValidQuery.of(
						"121.0", "31.0", null, "self", null, null, null, Integer.valueOf(0), null, null, null, null, null, null);

		Map<String, Object> result =
				service.getDistributorIsValid(COMPANY_ID, USER_ID, COMPANY_ID, USER_ID, query);

		assertThat(result.get("distributor_id")).isEqualTo(501L);
		verify(distributorIsValidWhiteListBranchService)
				.getWhiteListDistributor(eq(USER_ID), eq(COMPANY_ID), any(), any(), any());
		verify(distributorIsValidNearShopService, never())
				.getNearShopData(any(), anyDouble(), anyDouble(), any());
	}

	@Test
	void nearShop_usesPublicOrWhitelistMix_withoutCompanyStatus() {
		Map<String, Object> near = new LinkedHashMap<>();
		near.put("distributor_id", Long.valueOf(701L));
		near.put("is_valid", "true");
		when(distributorIsValidNearShopService.getNearShopData(any(), anyDouble(), anyDouble(), any()))
				.thenReturn(near);
		when(distributorWhiteListCheckUserValidService.listWhitelistedDistributorIds(COMPANY_ID, USER_ID))
				.thenReturn(List.of(501L));
		lenient()
				.when(distributorIsValidAppendFieldsService.applyTailFields(anyLong(), any(), any()))
				.thenAnswer(inv -> inv.getArgument(1));

		DistributorIsValidQuery query =
				DistributorIsValidQuery.of(
						"121.0", "31.0", null, null, null, null, null, Integer.valueOf(0), null, null, null, null, null, null);

		service.getDistributorIsValid(COMPANY_ID, USER_ID, COMPANY_ID, USER_ID, query);

		ArgumentCaptor<Map<String, Object>> filterCap = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Long>> wlCap = ArgumentCaptor.forClass(List.class);
		verify(distributorIsValidNearShopService)
				.getNearShopData(filterCap.capture(), eq(31.0d), eq(121.0d), wlCap.capture());
		assertThat(filterCap.getValue().get("open_divided")).isNull();
		assertThat(wlCap.getValue()).containsExactly(501L);
	}

	@Test
	void specificDistributor_whenOpenDivided_returnsShopTruthWithoutWhitelistGate() {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("distributor_id", Long.valueOf(501L));
		info.put("is_valid", "true");
		info.put("open_divided", Long.valueOf(1L));
		info.put("mobile", "13800000000");
		info.put("address", "addr");
		info.put("name", "shop-501");
		when(distributorIsValidCoreQueryService.getInfo(any())).thenReturn(info);

		DistributorIsValidQuery query =
				DistributorIsValidQuery.of(
						null, null, "501", null, null, null, null, Integer.valueOf(0), null, null, null, null, null, null);

		Map<String, Object> result =
				service.getDistributorIsValid(COMPANY_ID, USER_ID, COMPANY_ID, USER_ID, query);

		assertThat(result.get("distributor_id")).isEqualTo(501L);
		assertThat(String.valueOf(result.get("is_valid"))).isEqualTo("true");
		assertThat(result.get("open_divided")).isEqualTo(1L);
		verify(distributorWhiteListCheckUserValidService, never())
				.checkUserValidInDistributor(anyLong(), anyLong(), anyLong());
	}
}
