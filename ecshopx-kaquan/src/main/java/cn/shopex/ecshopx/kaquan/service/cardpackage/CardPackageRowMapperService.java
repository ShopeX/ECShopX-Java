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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CardPackageRowMapperService {

	public Map<String, Object> toSnakeCaseMap(CardPackage e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("package_id", e.getPackageId());
		m.put("company_id", e.getCompanyId());
		m.put("title", e.getTitle());
		m.put("package_describe", e.getPackageDescribe());
		m.put("limit_count", e.getLimitCount());
		m.put("get_num", e.getGetNum());
		m.put("row_status", e.getRowStatus());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}
}
