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

package cn.shopex.ecshopx.salesperson.integration;

import cn.shopex.ecshopx.members.service.h5.bind.ShoppingGuideForH5BindLookup;
import cn.shopex.ecshopx.salesperson.service.SalespersonGetInfoService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ShoppingGuideForH5BindLookupImpl implements ShoppingGuideForH5BindLookup {

	private final SalespersonGetInfoService salespersonGetInfoService;

	public ShoppingGuideForH5BindLookupImpl(SalespersonGetInfoService salespersonGetInfoService) {
		this.salespersonGetInfoService = salespersonGetInfoService;
	}

	@Override
	public Map<String, Object> getShoppingGuideDetailForH5Bind(long companyId, String workUserid) {
		return salespersonGetInfoService.getShoppingGuideDetailForH5Bind(companyId, workUserid);
	}
}
