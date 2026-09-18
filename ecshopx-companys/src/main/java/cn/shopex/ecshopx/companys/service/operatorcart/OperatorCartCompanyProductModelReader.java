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

package cn.shopex.ecshopx.companys.service.operatorcart;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 读取企业经营模式（platform / standard 等），供运营购物车与卡券店铺范围 SQL 使用。
 */
@Component
public class OperatorCartCompanyProductModelReader {

	private static final Map<Integer, String> MENU_TYPE_TO_STR = new HashMap<>();

	static {
		MENU_TYPE_TO_STR.put(1, "all");
		MENU_TYPE_TO_STR.put(2, "b2c");
		MENU_TYPE_TO_STR.put(3, "platform");
		MENU_TYPE_TO_STR.put(4, "standard");
		MENU_TYPE_TO_STR.put(5, "in_purchase");
	}

	private final CompanysMapper companysMapper;

	@Value("${common.product-model:platform}")
	private String defaultProductModel;

	public OperatorCartCompanyProductModelReader(CompanysMapper companysMapper) {
		this.companysMapper = companysMapper;
	}

	public String getProductModel(long companyId) {
		Companys c = companysMapper.selectById(companyId);
		if (c == null || c.getMenuType() == null || c.getMenuType() == 0) {
			return defaultProductModel != null ? defaultProductModel : "platform";
		}
		return MENU_TYPE_TO_STR.getOrDefault(c.getMenuType(), defaultProductModel != null ? defaultProductModel : "platform");
	}
}
