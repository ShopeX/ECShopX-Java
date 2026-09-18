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

package cn.shopex.ecshopx.theme.support;

import cn.shopex.ecshopx.theme.domain.PagesSideBar;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PagesSideBarColumnNamesDataMapper {

	public Map<String, Object> toColumnNamesData(PagesSideBar entity) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", entity.getId());
		map.put("company_id", entity.getCompanyId());
		map.put("regionauth_id", entity.getRegionauthId() == null ? 0L : entity.getRegionauthId());
		map.put("name", entity.getName());
		map.put("pages", entity.getPages());
		map.put("disabled", entity.getDisabled() == null ? Boolean.FALSE : entity.getDisabled());
		map.put("setting", entity.getSetting());
		return map;
	}
}
