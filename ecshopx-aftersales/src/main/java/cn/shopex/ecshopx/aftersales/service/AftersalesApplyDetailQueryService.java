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

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AftersalesApplyDetailQueryService {

	private static final List<Integer> ACTIVE_DETAIL_STATUSES = Arrays.asList(0, 5, 1, 2);
	private static final List<Integer> ACTIVE_AFTERSALES_STATUSES = Arrays.asList(0, 5, 1, 2);

	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AftersalesMapper aftersalesMapper;

	public AftersalesApplyDetailQueryService(
			AftersalesDetailMapper aftersalesDetailMapper, AftersalesMapper aftersalesMapper) {
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesMapper = aftersalesMapper;
	}

	public int sumAppliedNum(long companyId, long orderId, long subOrderId) {
		return sumAppliedNum(companyId, orderId, subOrderId, null);
	}

	public int sumAppliedNum(long companyId, long orderId, long subOrderId, Long excludeAftersalesBn) {
		QueryWrapper<AftersalesDetail> q = new QueryWrapper<>();
		q.select("COALESCE(SUM(num),0) AS s");
		q.eq("company_id", companyId);
		q.eq("order_id", String.valueOf(orderId));
		q.eq("sub_order_id", subOrderId);
		q.in("aftersales_status", ACTIVE_DETAIL_STATUSES);
		if (excludeAftersalesBn != null) {
			q.ne("aftersales_bn", excludeAftersalesBn);
		}
		Map<String, Object> row = firstMapOrNull(aftersalesDetailMapper.selectMaps(q));
		return intFromSum(row == null ? null : row.get("s"));
	}

	public int sumAppliedRefundFee(long companyId, long orderId, long subOrderId) {
		QueryWrapper<AftersalesDetail> q = new QueryWrapper<>();
		q.select("COALESCE(SUM(refund_fee),0) AS s");
		q.eq("company_id", companyId);
		q.eq("order_id", String.valueOf(orderId));
		q.eq("sub_order_id", subOrderId);
		q.in("aftersales_status", ACTIVE_DETAIL_STATUSES);
		Map<String, Object> row = firstMapOrNull(aftersalesDetailMapper.selectMaps(q));
		return intFromSum(row == null ? null : row.get("s"));
	}

	public int sumAppliedRefundPoint(long companyId, long orderId, long subOrderId) {
		QueryWrapper<AftersalesDetail> q = new QueryWrapper<>();
		q.select("COALESCE(SUM(refund_point),0) AS s");
		q.eq("company_id", companyId);
		q.eq("order_id", String.valueOf(orderId));
		q.eq("sub_order_id", subOrderId);
		q.in("aftersales_status", ACTIVE_DETAIL_STATUSES);
		Map<String, Object> row = firstMapOrNull(aftersalesDetailMapper.selectMaps(q));
		return intFromSum(row == null ? null : row.get("s"));
	}

	public int sumAppliedFreightCash(long companyId, long orderId) {
		QueryWrapper<Aftersales> q = new QueryWrapper<>();
		q.select("COALESCE(SUM(freight),0) AS s");
		q.eq("company_id", companyId);
		q.eq("order_id", orderId);
		q.eq("freight_type", "cash");
		q.in("aftersales_status", ACTIVE_AFTERSALES_STATUSES);
		Map<String, Object> row = firstMapOrNull(aftersalesMapper.selectMaps(q));
		return intFromSum(row == null ? null : row.get("s"));
	}

	public int sumAppliedFreightPoint(long companyId, long orderId) {
		QueryWrapper<Aftersales> q = new QueryWrapper<>();
		q.select("COALESCE(SUM(freight),0) AS s");
		q.eq("company_id", companyId);
		q.eq("order_id", orderId);
		q.eq("freight_type", "point");
		q.in("aftersales_status", ACTIVE_AFTERSALES_STATUSES);
		Map<String, Object> row = firstMapOrNull(aftersalesMapper.selectMaps(q));
		return intFromSum(row == null ? null : row.get("s"));
	}

	public List<Map<String, Object>> listReturnPointRows(long companyId, long orderId, long subOrderId) {
		QueryWrapper<AftersalesDetail> q = new QueryWrapper<>();
		q.select("detail_id", "num");
		q.eq("company_id", companyId);
		q.eq("order_id", String.valueOf(orderId));
		q.eq("sub_order_id", subOrderId);
		q.in("aftersales_status", ACTIVE_DETAIL_STATUSES);
		return aftersalesDetailMapper.selectMaps(q);
	}

	private static Map<String, Object> firstMapOrNull(List<Map<String, Object>> maps) {
		if (maps == null || maps.isEmpty()) {
			return null;
		}
		return maps.get(0);
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
