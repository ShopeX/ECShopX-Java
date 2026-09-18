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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AftersalesDetailAggregateService {

	private final AftersalesDetailMapper aftersalesDetailMapper;

	public AftersalesDetailAggregateService(AftersalesDetailMapper aftersalesDetailMapper) {
		this.aftersalesDetailMapper = aftersalesDetailMapper;
	}

	public int getAppliedNum(long companyId, String orderId, long subOrderId) {
		QueryWrapper<AftersalesDetail> qw = new QueryWrapper<>();
		qw.select("COALESCE(SUM(num),0) AS total_num");
		qw.eq("company_id", companyId);
		qw.eq("order_id", orderId);
		qw.eq("sub_order_id", subOrderId);
		qw.in("aftersales_status", 0, 5, 1, 2);
		List<Map<String, Object>> maps = aftersalesDetailMapper.selectMaps(qw);
		if (maps == null || maps.isEmpty()) {
			return 0;
		}
		return intFromSum(maps.get(0).get("total_num"));
	}

	private static int intFromSum(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
