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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAgreedCouponRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderCancelUserDiscountRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AftersalesAgreedCouponRestoreService implements AftersalesAgreedCouponRestorePort {

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final OrderCancelUserDiscountRestorePort orderCancelUserDiscountRestorePort;
	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final ObjectMapper objectMapper;

	@Override
	public void restoreOnRefundAgreed(long companyId, long orderId) {
		try {
			restore(companyId, orderId);
		} catch (Exception e) {
			log.debug(
					"[aftersalesAgreedCouponRestore] restoreOnRefundAgreed failed, companyId={}, orderId={}",
					companyId,
					orderId,
					e);
		}
	}

	private void restore(long companyId, long orderId) {
		try {
			if (dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
				return;
			}
		} catch (Exception e) {
			log.debug(
					"[aftersalesAgreedCouponRestore] isPointIntegrationOpen failed, companyId={}",
					companyId,
					e);
			return;
		}

		List<NormalOrdersItems> orderLines =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));

		LinkedHashMap<String, List<NormalOrdersItems>> couponToLines = new LinkedHashMap<>();
		for (NormalOrdersItems line : orderLines) {
			if (line == null) {
				continue;
			}
			String itemType = line.getOrderItemType();
			if (itemType != null && "gift".equalsIgnoreCase(itemType.trim())) {
				continue;
			}
			List<Map<String, Object>> discountList = parseDiscountInfo(line.getDiscountInfo(), orderId);
			if (discountList == null || discountList.isEmpty()) {
				continue;
			}
			for (Map<String, Object> discountEntry : discountList) {
				if (discountEntry == null || discountEntry.isEmpty()) {
					continue;
				}
				for (String couponCode :
						NormalOrderCancelDiscountRestoreService.expandCouponCodes(
								discountEntry.get("coupon_code"))) {
					couponToLines.computeIfAbsent(couponCode, k -> new ArrayList<>()).add(line);
				}
			}
		}

		List<AftersalesDetail> aftersalesRows =
				aftersalesDetailMapper.selectList(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getOrderId, String.valueOf(orderId)));

		for (Map.Entry<String, List<NormalOrdersItems>> entry : couponToLines.entrySet()) {
			String couponCode = entry.getKey();
			List<NormalOrdersItems> linesForCoupon = new ArrayList<>();
			for (NormalOrdersItems line : entry.getValue()) {
				Integer num = line.getNum();
				if (num == null || num <= 0) {
					continue;
				}
				linesForCoupon.add(line);
			}
			if (linesForCoupon.isEmpty()) {
				continue;
			}
			boolean allLinesFullyAgreed = true;
			for (NormalOrdersItems line : linesForCoupon) {
				int purchaseQty = line.getNum();
				int agreedQty = sumAgreedQuantity(aftersalesRows, line.getId());
				if (agreedQty < purchaseQty) {
					allLinesFullyAgreed = false;
					break;
				}
			}
			if (!allLinesFullyAgreed) {
				continue;
			}
			try {
				orderCancelUserDiscountRestorePort.callbackUserCard(couponCode, orderId);
			} catch (Exception e) {
				log.debug(
						"[aftersalesAgreedCouponRestore] callbackUserCard failed, orderId={}, code={}",
						orderId,
						couponCode,
						e);
			}
		}
	}

	private static int sumAgreedQuantity(List<AftersalesDetail> aftersalesRows, Long subOrderId) {
		if (subOrderId == null || aftersalesRows == null || aftersalesRows.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (AftersalesDetail row : aftersalesRows) {
			if (row == null || !subOrderId.equals(row.getSubOrderId())) {
				continue;
			}
			if (!isAgreedRefundType(row.getAftersalesType())) {
				continue;
			}
			if (!isAgreedStatus(row.getAftersalesStatus(), row.getProgress())) {
				continue;
			}
			Integer num = row.getNum();
			total += num == null ? 0 : num;
		}
		return total;
	}

	private static boolean isAgreedRefundType(String aftersalesType) {
		if (aftersalesType == null) {
			return false;
		}
		String type = aftersalesType.trim();
		return "ONLY_REFUND".equals(type) || "REFUND_GOODS".equals(type);
	}

	private static boolean isAgreedStatus(Integer aftersalesStatus, Integer progress) {
		if (aftersalesStatus == null || progress == null) {
			return false;
		}
		return (aftersalesStatus == 1 && progress == 9) || (aftersalesStatus == 2 && progress == 4);
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseDiscountInfo(String discountInfoJson, long orderId) {
		if (!StringUtils.hasText(discountInfoJson)) {
			return List.of();
		}
		try {
			Object parsed = objectMapper.readValue(discountInfoJson, Object.class);
			if (parsed instanceof List<?> list) {
				List<Map<String, Object>> result = new ArrayList<>();
				for (Object item : list) {
					if (item instanceof Map<?, ?> m) {
						result.add((Map<String, Object>) m);
					}
				}
				return result;
			}
			if (parsed instanceof Map<?, ?> m) {
				return List.of((Map<String, Object>) m);
			}
			return List.of();
		} catch (Exception e) {
			log.debug(
					"[aftersalesAgreedCouponRestore] parse discount_info failed, orderId={}",
					orderId,
					e);
			return List.of();
		}
	}
}
