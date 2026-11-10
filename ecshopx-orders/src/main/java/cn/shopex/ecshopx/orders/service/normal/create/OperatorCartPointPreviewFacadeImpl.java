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

package cn.shopex.ecshopx.orders.service.normal.create;

import cn.shopex.ecshopx.common.operatorcart.OperatorCartPointPreviewFacade;
import cn.shopex.ecshopx.point.service.PointMemberBalanceReadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * POS 购物车积分抵扣预览（ECX-11082 后续）：组装券后 od 调用下单同口径的
 * {@link NormalOrderCheckoutPointDeductService}（积分规则/上限/增值活动/行分摊全部复用），
 * 积分字段白名单合并回 shop 桶，total_fee 再减 point_fee。
 * 仅预览，不写订单数据；cartdatalist 专用，导购端与小程序链路不受影响。
 * 合并顺序与下单一致：满减 → 券 → 积分（积分基数 = 券后应付）。
 */
@Service
public class OperatorCartPointPreviewFacadeImpl implements OperatorCartPointPreviewFacade {

	/** 合并回 shop 桶的积分字段白名单（字段名与 POS 临时单信息接口一致，前端可复用）。 */
	private static final List<String> SHOP_POINT_FIELDS = List.of(
			"user_point",
			"max_point",
			"limit_point",
			"max_point_ziti",
			"max_uppoint",
			"deduct_point_rule",
			"is_open_deduct_point",
			"pointupvaluation",
			"point_use",
			"point_fee",
			"point_fee_item",
			"real_use_point",
			"real_use_point_ziti",
			"point",
			"before_point_deduction_total_fee");

	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final PointMemberBalanceReadService pointMemberBalanceReadService;
	private final NormalOrderCheckoutPointDeductService normalOrderCheckoutPointDeductService;

	public OperatorCartPointPreviewFacadeImpl(
			PointMemberRuleReadService pointMemberRuleReadService,
			PointMemberBalanceReadService pointMemberBalanceReadService,
			NormalOrderCheckoutPointDeductService normalOrderCheckoutPointDeductService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.pointMemberBalanceReadService = pointMemberBalanceReadService;
		this.normalOrderCheckoutPointDeductService = normalOrderCheckoutPointDeductService;
	}

	@Override
	public void applyPointPreview(
			long companyId, long userId, Map<String, Object> cartData, Integer pointUse) {
		if (cartData == null || userId <= 0L) {
			return;
		}
		if (!(cartData.get("valid_cart") instanceof List<?> shopBlocks) || shopBlocks.isEmpty()) {
			return;
		}
		if (!(shopBlocks.get(0) instanceof Map<?, ?> blockRaw)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> shop = (Map<String, Object>) blockRaw;
		if (!(shop.get("list") instanceof List<?> lines) || lines.isEmpty()) {
			return;
		}
		long totalFee = longVal(shop.get("total_fee"));
		if (totalFee <= 0L) {
			return;
		}

		Map<String, Object> od = new LinkedHashMap<>();
		od.put("total_fee", totalFee);
		od.put("items", lines);
		od.put("distributor_id", shop.get("shop_id"));
		if (pointUse != null && pointUse > 0) {
			od.put("point_use", pointUse);
		}
		Map<String, Object> params = new LinkedHashMap<>();

		Map<String, Object> pointRule = pointMemberRuleReadService.getPointRule(companyId);
		long memberPoint = pointMemberBalanceReadService.getPointBalance(companyId, userId);
		normalOrderCheckoutPointDeductService.applyCheckoutPointDeduct(
				od, params, companyId, userId, pointRule, memberPoint);

		for (String key : SHOP_POINT_FIELDS) {
			if (od.containsKey(key)) {
				shop.put(key, od.get(key));
			}
		}
		long pointFee = longVal(od.get("point_fee"));
		if (pointFee > 0L) {
			shop.put("total_fee", Math.max(0L, totalFee - pointFee));
		}
	}

	private static long longVal(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
