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

import cn.shopex.ecshopx.theme.domain.PagesAdPlace;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PagesAdPlaceColumnNamesDataMapper {

	public Map<String, Object> toColumnNamesData(PagesAdPlace entity) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", entity.getId());
		m.put("company_id", entity.getCompanyId());
		m.put("regionauth_id", entity.getRegionauthId() == null ? 0L : entity.getRegionauthId());
		m.put("use_bound", entity.getUseBound());
		m.put("ad_type", entity.getAdType());
		m.put("name", entity.getName());
		m.put("pages", entity.getPages());
		m.put("start_time", entity.getStartTime());
		m.put("end_time", entity.getEndTime());
		m.put("created", entity.getCreated());
		m.put("updated", entity.getUpdated());
		m.put("setting", entity.getSetting());
		m.put("auto_play", entity.getAutoPlay());
		m.put("play_interval", entity.getPlayInterval());
		m.put("auto_close", entity.getAutoClose());
		m.put("close_delay", entity.getCloseDelay());
		m.put("source_id", entity.getSourceId());
		m.put("audit_status", entity.getAuditStatus());
		m.put("audit_remark", entity.getAuditRemark());
		m.put("sort", entity.getSort());
		m.put("tracking_code", entity.getTrackingCode());
		return m;
	}
}
