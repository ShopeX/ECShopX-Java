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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.point.domain.PointMemberPointCountSummary;
import cn.shopex.ecshopx.point.mapper.PointMemberLogMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PointMemberPointCountReadService {

	private final PointMemberLogMapper pointMemberLogMapper;

	public PointMemberPointCountReadService(PointMemberLogMapper pointMemberLogMapper) {
		this.pointMemberLogMapper = pointMemberLogMapper;
	}

	public Map<String, Object> getMemberPointTotal(long companyId) {
		PointMemberPointCountSummary row = pointMemberLogMapper.selectPointCountSummaryByCompanyId(companyId);
		long canUse = 0L;
		long total = 0L;
		long used = 0L;
		if (row != null) {
			if (row.getCanUse() != null) {
				canUse = row.getCanUse();
			}
			if (row.getTotal() != null) {
				total = row.getTotal();
			}
			if (row.getUsed() != null) {
				used = row.getUsed();
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("can_use", canUse);
		out.put("total", total);
		out.put("used", used);
		return out;
	}
}
