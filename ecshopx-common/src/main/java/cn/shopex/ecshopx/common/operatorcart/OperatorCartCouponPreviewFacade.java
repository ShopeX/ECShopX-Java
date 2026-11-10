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

package cn.shopex.ecshopx.common.operatorcart;

import java.util.Map;

/**
 * POS 购物车券后预览门面（ECX-11082）：把指定券码的优惠按下单同口径合并进 cartdatalist 返回。
 * 行级分摊与店铺级 coupon_discount_fee / discount_fee（活动+券）/ total_fee / coupon_info 由实现完成。
 * 仅预览，不写订单数据；cartdatalist 专用，导购端与小程序链路不受影响。
 */
public interface OperatorCartCouponPreviewFacade {

	/**
	 * @param companyId   企业 id
	 * @param operatorId  店务操作员 id
	 * @param userId      会员 id（0 表示无会员，不应用券）
	 * @param cartData    购物车列表返回（valid_cart 为 shop 桶列表），原地更新券字段
	 * @param couponCode  券码（getUserCardList 返回的 code）；空表示无券预览
	 */
	void applyCouponPreview(
			long companyId, long operatorId, long userId, Map<String, Object> cartData, String couponCode);
}
