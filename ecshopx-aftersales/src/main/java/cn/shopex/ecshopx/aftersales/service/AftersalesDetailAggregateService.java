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

	public int getAppliedNumByType(
			long companyId, String orderId, long subOrderId, String... aftersalesTypes) {
		if (aftersalesTypes == null || aftersalesTypes.length == 0) {
			return 0;
		}
		QueryWrapper<AftersalesDetail> qw = new QueryWrapper<>();
		qw.select("COALESCE(SUM(num),0) AS total_num");
		qw.eq("company_id", companyId);
		qw.eq("order_id", orderId);
		qw.eq("sub_order_id", subOrderId);
		qw.in("aftersales_status", 0, 5, 1, 2);
		qw.in("aftersales_type", (Object[]) aftersalesTypes);
		List<Map<String, Object>> maps = aftersalesDetailMapper.selectMaps(qw);
		if (maps == null || maps.isEmpty()) {
			return 0;
		}
		return intFromSum(maps.get(0).get("total_num"));
	}

	public OrderItemAftersalesQuota calculateItemQuota(
			long companyId,
			String orderId,
			long subOrderId,
			int num,
			int deliveryItemNum,
			int cancelItemNum) {
		int appliedNum = getAppliedNum(companyId, orderId, subOrderId);
		int appliedReturnExchangeNum =
				getAppliedNumByType(companyId, orderId, subOrderId, "REFUND_GOODS", "EXCHANGING_GOODS");
		int appliedRefundOnlyNum =
				getAppliedNumByType(companyId, orderId, subOrderId, "ONLY_REFUND");
		return calculateItemQuota(
				appliedNum, appliedReturnExchangeNum, appliedRefundOnlyNum, num, deliveryItemNum, cancelItemNum);
	}

	public AppliedRefundAmount getAppliedRefundAmount(
			long companyId, String orderId, long subOrderId) {
		QueryWrapper<AftersalesDetail> qw = new QueryWrapper<>();
		qw.select(
				"COALESCE(SUM(CASE WHEN aftersales_status IN (0,1,2) THEN refund_fee ELSE 0 END),0) AS refund_fee",
				"COALESCE(SUM(CASE WHEN aftersales_status IN (0,1,2) THEN refund_point ELSE 0 END),0) AS refund_point",
				"MAX(CASE WHEN aftersales_status IN (0,1,2) THEN 1 ELSE 0 END) AS applied_exists");
		qw.eq("company_id", companyId);
		qw.eq("order_id", orderId);
		qw.eq("sub_order_id", subOrderId);
		List<Map<String, Object>> maps = aftersalesDetailMapper.selectMaps(qw);
		if (maps == null || maps.isEmpty()) {
			return new AppliedRefundAmount(0, 0, false);
		}
		Map<String, Object> row = maps.get(0);
		return new AppliedRefundAmount(
				intFromSum(row.get("refund_fee")),
				intFromSum(row.get("refund_point")),
				intFromSum(row.get("applied_exists")) > 0);
	}

	static OrderItemAftersalesQuota calculateItemQuota(
			int appliedNum,
			int appliedReturnExchangeNum,
			int appliedRefundOnlyNum,
			int num,
			int deliveryItemNum,
			int cancelItemNum) {
		int leftAftersalesNum = Math.max(0, deliveryItemNum - appliedReturnExchangeNum);
		int leftRefundOnlyNum =
				Math.max(0, num - cancelItemNum - deliveryItemNum - appliedRefundOnlyNum);
		return new OrderItemAftersalesQuota(appliedNum, leftAftersalesNum, leftRefundOnlyNum);
	}

	public record OrderItemAftersalesQuota(
			int appliedNum, int leftAftersalesNum, int leftRefundOnlyNum) {}

	public record AppliedRefundAmount(int refundFee, int refundPoint, boolean exists) {}

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
