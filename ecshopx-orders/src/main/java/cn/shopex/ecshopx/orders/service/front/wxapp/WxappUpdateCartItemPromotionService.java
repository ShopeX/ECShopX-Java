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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.orders.port.WxappCartGoodsPromotionApplyRulesPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Locale;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappUpdateCartItemPromotionService {

	private final CartMapper cartMapper;
	private final WxappCartGoodsPromotionApplyRulesPort applyRulesPort;

	public WxappUpdateCartItemPromotionService(
			CartMapper cartMapper, WxappCartGoodsPromotionApplyRulesPort applyRulesPort) {
		this.cartMapper = cartMapper;
		this.applyRulesPort = applyRulesPort;
	}

	public void updateCartItemPromotion(
			long companyId,
			long userId,
			@Nullable String wxappAppidFromAuth,
			String shopTypeRaw,
			long cartId,
			long marketingId) {
		String st = shopTypeRaw == null ? "" : shopTypeRaw.trim().toLowerCase(Locale.ROOT);
		if (!StringUtils.hasText(st)) {
			st = "distributor";
		}
		if (!"distributor".equals(st)) {
			throw new ResourceException("无此购车类型");
		}
		LambdaQueryWrapper<Cart> qw = ownershipWhere(companyId, userId, st, cartId, wxappAppidFromAuth);
		Cart row = cartMapper.selectOne(qw);
		if (row == null) {
			throw new ResourceException("购物车数据有误");
		}
		long itemId = row.getItemId() == null ? 0L : row.getItemId();
		long cartShopId = row.getShopId() == null ? 0L : row.getShopId();
		applyRulesPort.assertUserCanApplyGoodsPromotion(companyId, userId, marketingId, itemId, cartShopId);
		int nowSeconds = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Cart> uw = new LambdaUpdateWrapper<>();
		uw.eq(Cart::getCartId, cartId)
				.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getShopType, st);
		if (StringUtils.hasText(wxappAppidFromAuth)) {
			uw.eq(Cart::getWxaAppid, wxappAppidFromAuth.trim());
		}
		uw.set(Cart::getActivityId, marketingId)
				.set(Cart::getActivityType, "goods_promotion")
				.set(Cart::getUpdated, nowSeconds);
		cartMapper.update(null, uw);
	}

	private static LambdaQueryWrapper<Cart> ownershipWhere(
			long companyId,
			long userId,
			String shopType,
			long cartId,
			@Nullable String wxappAppidFromAuth) {
		LambdaQueryWrapper<Cart> w =
				new LambdaQueryWrapper<Cart>()
						.eq(Cart::getCartId, cartId)
						.eq(Cart::getCompanyId, companyId)
						.eq(Cart::getUserId, userId)
						.eq(Cart::getShopType, shopType);
		if (StringUtils.hasText(wxappAppidFromAuth)) {
			w.eq(Cart::getWxaAppid, wxappAppidFromAuth.trim());
		}
		return w;
	}
}
