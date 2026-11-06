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

package cn.shopex.ecshopx.aftersales.api.admin.v1;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.service.AftersalesAdminDetailService;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminListService;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminLogExportService;
import cn.shopex.ecshopx.aftersales.service.AftersalesApplyService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundConfirmService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRemindService;
import cn.shopex.ecshopx.aftersales.service.AftersalesReviewService;
import cn.shopex.ecshopx.aftersales.service.AftersalesSendbackService;
import cn.shopex.ecshopx.aftersales.service.AftersalesService;
import cn.shopex.ecshopx.aftersales.service.export.AftersalesFinancialExportService;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ECX-10936：管理端详情必须向服务层传递当前门店身份。 */
class AftersalesControllerDetailDistributorScopeTest {

	@Test
	void getAftersalesDetail_passesRequestToScopedService() {
		AftersalesAdminDetailService detailService = mock(AftersalesAdminDetailService.class);
		AftersalesController controller = new AftersalesController(
				mock(AftersalesApplyService.class),
				mock(AftersalesRefundConfirmService.class),
				mock(AftersalesReviewService.class),
				mock(AftersalesRemindService.class),
				mock(AftersalesSendbackService.class),
				mock(AftersalesService.class),
				mock(AftersalesAdminListService.class),
				detailService,
				mock(AftersalesFinancialExportService.class),
				mock(AftersalesAdminLogExportService.class));
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA))
				.thenReturn(Map.of("company_id", 100L, "distributor_id", 10L));
		when(detailService.getAftersalesDetail(100L, "555001", request)).thenReturn(Map.of());

		controller.getAftersalesDetail("555001", request);

		verify(detailService).getAftersalesDetail(same(100L), same("555001"), same(request));
	}
}
