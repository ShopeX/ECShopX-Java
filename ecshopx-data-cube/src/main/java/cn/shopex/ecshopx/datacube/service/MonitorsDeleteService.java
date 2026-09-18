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
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitorsDeleteService {

	private final MonitorsMapper monitorsMapper;

	public MonitorsDeleteService(MonitorsMapper monitorsMapper) {
		this.monitorsMapper = monitorsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteMonitor(long companyId, long monitorId) {
		if (monitorId <= 0) {
			throw new ResourceException("跟踪链接id不能为空.");
		}
		Monitors row = monitorsMapper.selectById(monitorId);
		if (row == null) {
			throw new ResourceException("monitor_id=" + monitorId + "的跟踪链接不存在");
		}
		if (!Objects.equals(companyId, row.getCompanyId())) {
			throw new ResourceException("删除跟踪链接信息有误.");
		}
		int n = monitorsMapper.deleteById(monitorId);
		if (n == 0) {
			throw new ResourceException("monitor_id=" + monitorId + "的跟踪链接不存在");
		}
	}
}
