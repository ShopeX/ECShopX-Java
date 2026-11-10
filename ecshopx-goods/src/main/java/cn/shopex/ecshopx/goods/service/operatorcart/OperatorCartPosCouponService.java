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

package cn.shopex.ecshopx.goods.service.operatorcart;

import cn.shopex.ecshopx.common.operatorcart.OperatorCartCouponPreviewFacade;
import cn.shopex.ecshopx.kaquan.service.order.normal.NormalOrderCheckoutCouponFacade;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * POS 购物车券后预览（ECX-11082）：按下单同口径把指定券码的优惠合并进 cartdatalist 返回。
 * 行级分摊由 {@link NormalOrderCheckoutCouponFacade#previewCouponDeduction} 原地完成，
 * 本服务同步店铺级 coupon_discount_fee / discount_fee（活动+券）/ total_fee / coupon_info。
 * 仅预览，不写订单数据；实现 common 的 {@link OperatorCartCouponPreviewFacade}，
 * cartdatalist 专用，导购端与小程序链路不受影响。
 */
@Service
public class OperatorCartPosCouponService implements OperatorCartCouponPreviewFacade {

	private final NormalOrderCheckoutCouponFacade checkoutCouponFacade;

	public OperatorCartPosCouponService(NormalOrderCheckoutCouponFacade checkoutCouponFacade) {
		this.checkoutCouponFacade = checkoutCouponFacade;
	}

	@Override
	public void applyCouponPreview(
			long companyId, long operatorId, long userId, Map<String, Object> cartData, String couponCode) {
		if (cartData == null || userId <= 0L || !StringUtils.hasText(couponCode)) {
			return;
		}
		if (!(cartData.get("valid_cart") instanceof List<?> shopBlocks) || shopBlocks.isEmpty()) {
			return;
		}
		for (Object blockObj : shopBlocks) {
			if (!(blockObj instanceof Map<?, ?> blockRaw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> shop = (Map<String, Object>) blockRaw;
			if (!(shop.get("list") instanceof List<?> listRaw) || listRaw.isEmpty()) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> lines = (List<Map<String, Object>>) listRaw;
			Map<String, Object> out = checkoutCouponFacade.previewCouponDeduction(
					companyId, operatorId, userId, longVal(shop.get("shop_id")), couponCode, lines);
			long deduct = longVal(out.get("deduct_fen"));
			if (deduct <= 0L) {
				continue;
			}
			shop.put("coupon_discount_fee", deduct);
			shop.put("coupon_info", out.get("coupon_info"));
			shop.put("discount_fee", longVal(shop.get("discount_fee")) + deduct);
			shop.put("total_fee", Math.max(0L, longVal(shop.get("total_fee")) - deduct));
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
