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

package cn.shopex.ecshopx.goods.service.cart;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsBarcode;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappScanCodeAddCartService {

	private final ItemsRepository itemsRepository;
	private final ItemsBarcodeRepository itemsBarcodeRepository;
	private final WxappDistributorCartAddCoreService wxappDistributorCartAddCoreService;

	public WxappScanCodeAddCartService(
			ItemsRepository itemsRepository,
			ItemsBarcodeRepository itemsBarcodeRepository,
			WxappDistributorCartAddCoreService wxappDistributorCartAddCoreService) {
		this.itemsRepository = itemsRepository;
		this.itemsBarcodeRepository = itemsBarcodeRepository;
		this.wxappDistributorCartAddCoreService = wxappDistributorCartAddCoreService;
	}

	public Map<String, Object> execute(long companyId, long userId, Map<String, Object> requestParams) {
		String normalizedBarcode = normalizeBarcode(requestParams.get("barcode"));
		if (!StringUtils.hasText(normalizedBarcode) || "0".equals(normalizedBarcode)) {
			throw new BadRequestException("条码不能为空");
		}
		long shopId = parseLongParam(requestParams.get("distributor_id"), 0L);
		long itemIdForCart = resolveItemIdForBarcode(companyId, shopId, normalizedBarcode);
		String isShopScreen = stringifyOptional(requestParams.get("isShopScreen"));

		Map<String, Object> coreResult =
				wxappDistributorCartAddCoreService.addCart(
						companyId,
						userId,
						itemIdForCart,
						"distributor",
						"normal",
						1,
						true,
						shopId,
						isShopScreen);

		if (userId <= 0L) {
			return Map.of("item_id", itemIdForCart);
		}
		if (coreResult.containsKey("cart_id") && coreResult.get("cart_id") != null) {
			Map<String, Object> ok = new LinkedHashMap<>();
			ok.put("status", true);
			ok.put("msg", "加入购物车成功");
			return ok;
		}
		Map<String, Object> fail = new LinkedHashMap<>();
		fail.put("status", false);
		fail.put("msg", "加入购物车失败");
		return fail;
	}

	private static String stringifyOptional(Object v) {
		if (v == null) {
			return null;
		}
		return v.toString();
	}

	private static String normalizeBarcode(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof Number n) {
			if (n.longValue() == 0L) {
				return "";
			}
			return String.valueOf(n.longValue());
		}
		return raw.toString().trim();
	}

	private long resolveItemIdForBarcode(long companyId, long distributorId, String barcode) {
		List<Items> fromItems =
				itemsRepository.listByCompanyIdAndDistributorIdAndBarcodeExact(companyId, distributorId, barcode);
		if (fromItems.size() > 1) {
			throw new ResourceException("商品数据异常");
		}
		if (fromItems.size() == 1) {
			Long id = fromItems.get(0).getItemId();
			if (id == null || id <= 0L) {
				throw new ResourceException("商品数据异常");
			}
			return id;
		}
		List<ItemsBarcode> ext =
				itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(companyId, distributorId, barcode);
		if (ext.size() > 1) {
			throw new ResourceException("商品数据异常");
		}
		if (ext.isEmpty()) {
			throw new ResourceException("商品未找到");
		}
		Long itemId = ext.get(0).getItemId();
		if (itemId == null || itemId <= 0L) {
			throw new ResourceException("商品数据异常");
		}
		return itemId;
	}

	private static long parseLongParam(Object v, long defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return defaultVal;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
