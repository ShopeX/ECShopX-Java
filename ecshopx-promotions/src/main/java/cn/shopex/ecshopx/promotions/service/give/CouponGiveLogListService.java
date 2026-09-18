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

package cn.shopex.ecshopx.promotions.service.give;

import cn.shopex.ecshopx.promotions.domain.CouponGiveLog;
import cn.shopex.ecshopx.promotions.mapper.CouponGiveLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CouponGiveLogListService {

	private final CouponGiveLogMapper couponGiveLogMapper;

	public CouponGiveLogListService(CouponGiveLogMapper couponGiveLogMapper) {
		this.couponGiveLogMapper = couponGiveLogMapper;
	}

	public Map<String, Object> getGiveLog(long companyId, long distributorIdFromJwt, int page, int pageSize) {
		LambdaQueryWrapper<CouponGiveLog> w = new LambdaQueryWrapper<>();
		w.eq(CouponGiveLog::getCompanyId, companyId);
		if (distributorIdFromJwt != 0L) {
			w.eq(CouponGiveLog::getDistributorId, distributorIdFromJwt);
		}
		w.orderByDesc(CouponGiveLog::getCreated);
		long total = couponGiveLogMapper.selectCount(w);
		List<Map<String, Object>> list;
		if (total == 0) {
			list = new ArrayList<>();
		} else {
			Page<CouponGiveLog> p = new Page<>(page, pageSize, false);
			couponGiveLogMapper.selectPage(p, w);
			list = new ArrayList<>();
			for (CouponGiveLog e : p.getRecords()) {
				list.add(toRow(e));
			}
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private static Map<String, Object> toRow(CouponGiveLog e) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("give_id", e.getGiveLogId());
		row.put("company_id", e.getCompanyId());
		row.put("distributor_id", e.getDistributorId());
		row.put("sender", e.getSender());
		row.put("number", e.getNumber());
		row.put("error", e.getError());
		row.put("created", e.getCreated());
		return row;
	}
}
