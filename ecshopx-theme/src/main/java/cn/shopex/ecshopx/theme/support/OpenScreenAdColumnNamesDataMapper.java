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

import cn.shopex.ecshopx.theme.domain.OpenScreenAd;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenScreenAdColumnNamesDataMapper {

	public Map<String, Object> toColumnNamesData(OpenScreenAd entity) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", entity.getId());
		m.put("company_id", entity.getCompanyId());
		m.put("ad_material", entity.getAdMaterial());
		m.put("is_enable", entity.getIsEnable());
		m.put("show_time", entity.getShowTime());
		m.put("position", entity.getPosition());
		m.put("is_jump", entity.getIsJump());
		m.put("material_type", entity.getMaterialType());
		m.put("waiting_time", entity.getWaitingTime());
		m.put("ad_url", entity.getAdUrl());
		m.put("app", entity.getApp());
		m.put("start_time", entity.getStartTime());
		m.put("end_time", entity.getEndTime());
		m.put("created", entity.getCreated());
		m.put("updated", entity.getUpdated());
		return m;
	}
}
