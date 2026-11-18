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

package cn.shopex.ecshopx.distribution.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListExportFilter;
import cn.shopex.ecshopx.common.dispatch.DistributorWhiteListExportFileJobDispatchPublisher;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** ECX-11158：列表筛选接受 admin 传入的 mobile 参数。 */
class DistributorWhiteListExportServiceFilterTest {

	private DistributorWhiteListExportService service;

	@BeforeEach
	void setUp() {
		service = new DistributorWhiteListExportService(
				mock(OperatorsQueryService.class),
				mock(DistributorListQueryService.class),
				mock(DistributorWhiteListExportFileJobDispatchPublisher.class));
	}

	@Test
	void buildExportFilterForWhiteListGet_mobileQueryParam_activatesMobileFilter() {
		DistributorWhiteListExportFilter filter =
				service.buildExportFilterForWhiteListGet(38L, "admin", null, Map.of("mobile", "13661830217"));

		assertThat(filter.mobileFilterActive()).isTrue();
		assertThat(filter.mobile()).isEqualTo("13661830217");
	}

	@Test
	void buildExportFilterForWhiteListGet_searchMobileTakesPrecedenceOverMobile() {
		DistributorWhiteListExportFilter filter = service.buildExportFilterForWhiteListGet(
				38L, "admin", null, Map.of("mobile", "111", "search_mobile", "222"));

		assertThat(filter.mobileFilterActive()).isTrue();
		assertThat(filter.mobile()).isEqualTo("222");
	}
}