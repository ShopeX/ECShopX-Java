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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesFrontRefundAmountService {

	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	private final AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService;

	public AftersalesFrontRefundAmountService(
			OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort,
			OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort,
			AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService) {
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
		this.orderNormalOrderItemsReadPort = orderNormalOrderItemsReadPort;
		this.aftersalesApplyDetailQueryService = aftersalesApplyDetailQueryService;
	}

	public long getRefundAmount(
			long companyId,
			long userId,
			String orderIdRaw,
			String itemIdRaw,
			int aftersalesItemNum,
			int up,
			String aftersalesBnRawOptional) {
		long orderIdLong;
		try {
			orderIdLong = Long.parseLong(orderIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("订单号必传");
		}

		Optional<Map<String, Object>> headOpt = orderNormalOrderHeaderReadPort.getHeader(companyId, orderIdLong);
		if (headOpt.isEmpty()) {
			throw new ResourceException("订单号为" + orderIdLong + "的订单不存在");
		}

		List<Map<String, Object>> lines = orderNormalOrderItemsReadPort.listItems(companyId, orderIdLong);
		String qItem = normalizeItemId(itemIdRaw);
		Map<String, Object> line = null;
		for (Map<String, Object> row : lines) {
			if (normalizeItemId(row.get("item_id")).equals(qItem)) {
				line = row;
				break;
			}
		}
		if (line == null) {
			throw new ResourceException("商品不存在");
		}

		int lineNum = intVal(line.get("num"));
		int totalFee = intVal(line.get("total_fee"));
		int itemFee = intVal(line.get("item_fee"));
		if (aftersalesItemNum > lineNum) {
			throw new ResourceException("超过购买数量");
		}

		long subOrderId = longVal(line.get("id"));
		Long excludeBn = null;
		if (up == 1 && StringUtils.hasText(aftersalesBnRawOptional)) {
			try {
				excludeBn = Long.parseLong(aftersalesBnRawOptional.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("售后单号无效");
			}
		}
		int appliedSum = aftersalesApplyDetailQueryService.sumAppliedNum(companyId, orderIdLong, subOrderId, excludeBn);
		int applyNum = lineNum - appliedSum;
		if (aftersalesItemNum > applyNum) {
			throw new ResourceException("超过申请数量");
		}

		if (totalFee == 0) {
			totalFee = itemFee;
		}

		if (aftersalesItemNum == lineNum) {
			return totalFee;
		}

		if (lineNum > 0 && totalFee % lineNum == 0) {
			return (long) (totalFee / lineNum) * aftersalesItemNum;
		}

		long unit = (long) Math.floor((double) totalFee / (double) lineNum);
		long aftersalesPrice = unit * aftersalesItemNum;

		if (aftersalesItemNum == applyNum) {
			int refunded = aftersalesApplyDetailQueryService.sumAppliedRefundFee(companyId, orderIdLong, subOrderId);
			return (long) totalFee - (long) refunded;
		}

		return aftersalesPrice;
	}

	private static String normalizeItemId(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
