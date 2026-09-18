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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Monitors;
import cn.shopex.ecshopx.datacube.mapper.MonitorsMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MonitorsDetailService {

	private final MonitorsMapper monitorsMapper;

	public MonitorsDetailService(MonitorsMapper monitorsMapper) {
		this.monitorsMapper = monitorsMapper;
	}

	public Map<String, Object> getDetailMapByMonitorId(long monitorId) {
		Monitors row = monitorsMapper.selectById(monitorId);
		if (row == null) {
			throw new ResourceException("monitor_id=" + monitorId + "的跟踪链接不存在");
		}
		Map<String, Object> m = new LinkedHashMap<>();
		Long mid = row.getMonitorId();
		m.put("monitor_id", mid != null && mid <= Integer.MAX_VALUE ? mid.intValue() : mid);
		Long cid = row.getCompanyId();
		m.put("company_id", cid != null && cid <= Integer.MAX_VALUE ? cid.intValue() : cid);
		m.put("wxappid", row.getWxappid() != null ? row.getWxappid() : "");
		m.put("nick_name", row.getNickName() != null ? row.getNickName() : "");
		m.put("monitor_path", row.getMonitorPath() != null ? row.getMonitorPath() : "");
		m.put("page_name", row.getPageName() != null ? row.getPageName() : "");
		m.put("monitor_path_params", row.getMonitorPathParams());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("regionauth_id", row.getRegionauthId());
		return m;
	}
}
