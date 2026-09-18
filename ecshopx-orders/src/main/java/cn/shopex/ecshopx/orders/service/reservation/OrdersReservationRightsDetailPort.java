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

package cn.shopex.ecshopx.orders.service.reservation;

import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.reservation.port.ReservationRightsDetailPort;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrdersReservationRightsDetailPort implements ReservationRightsDetailPort {

	private final RightsMapper rightsMapper;

	public OrdersReservationRightsDetailPort(RightsMapper rightsMapper) {
		this.rightsMapper = rightsMapper;
	}

	@Override
	public Map<String, Object> getRightsDetail(long rightsId, long companyId) {
		Rights r = rightsMapper.selectOne(
				Wrappers.<Rights>lambdaQuery().eq(Rights::getRightsId, rightsId).eq(Rights::getCompanyId, companyId));
		if (r == null) {
			return Map.of();
		}
		long now = System.currentTimeMillis() / 1000L;
		boolean isValid = r.getEndTime() == null || r.getEndTime() >= now;
		Integer isNotLimitNum = r.getIsNotLimitNum();
		long totalNum = r.getTotalNum() != null ? r.getTotalNum() : 0L;
		long totalConsumNum = r.getTotalConsumNum() != null ? r.getTotalConsumNum() : 0L;
		if (isValid && isNotLimitNum != null && isNotLimitNum == 2) {
			if (totalNum > totalConsumNum) {
				isValid = true;
			} else {
				isValid = false;
			}
		}
		long totalSurplusNum;
		if (isNotLimitNum != null && isNotLimitNum == 1) {
			totalSurplusNum = -1L;
		} else {
			totalSurplusNum = totalNum - totalConsumNum;
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("rights_id", r.getRightsId());
		m.put("user_id", r.getUserId());
		m.put("company_id", r.getCompanyId());
		m.put("is_not_limit_num", isNotLimitNum);
		m.put("total_num", totalNum);
		m.put("total_consum_num", totalConsumNum);
		m.put("total_surplus_num", totalSurplusNum);
		m.put("end_time", r.getEndTime());
		m.put("is_valid", isValid);
		m.put("status", r.getStatus());
		return m;
	}
}
