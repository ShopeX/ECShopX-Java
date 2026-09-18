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

package cn.shopex.ecshopx.kaquan.service.discount.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class UserDiscountNewGetCardListRequest {

	private final String amount;
	private final String items;
	private final String code;
	private final String cardId;
	private final String shopId;
	private final String itemId;
	private final int pageNo;
	private final int pageSize;
	private final String usePlatform;
	private final String pageType;
	private final String useScenes;
	private final String distributorId;
	private final String isCheckout;
	private final String cartType;
	private final String cxdid;
	private final String pointUse;
	private final String iscrossborder;
	private final String isShopScreen;
	private final String valid;

	public UserDiscountNewGetCardListRequest(String amount, String items, String code, String cardId, String shopId, String itemId,
			int pageNo, int pageSize, String usePlatform, String pageType, String useScenes, String distributorId,
			String isCheckout, String cartType, String cxdid, String pointUse, String iscrossborder, String isShopScreen,
			String valid) {
		this.amount = amount;
		this.items = items;
		this.code = code;
		this.cardId = cardId;
		this.shopId = shopId;
		this.itemId = itemId;
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.usePlatform = usePlatform;
		this.pageType = pageType;
		this.useScenes = useScenes;
		this.distributorId = distributorId;
		this.isCheckout = isCheckout;
		this.cartType = cartType;
		this.cxdid = cxdid;
		this.pointUse = pointUse;
		this.iscrossborder = iscrossborder;
		this.isShopScreen = isShopScreen;
		this.valid = valid;
	}

	public boolean isValidRequired() {
		return !isFalseLike(valid);
	}

	public boolean isCheckoutRequired() {
		if (isCheckout == null || isCheckout.isBlank()) {
			return false;
		}
		return !isFalseLike(isCheckout);
	}

	public long parseDistributorIdOrZero() {
		return parsePositiveLongOrZero(distributorId);
	}

	public long parseCxdidOrZero() {
		return parsePositiveLongOrZero(cxdid);
	}

	public Integer parseAmountLteOrNull() {
		if (amount == null || amount.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(amount.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public Long parseCardIdOrNull() {
		long v = parsePositiveLongOrZero(cardId);
		return v > 0L ? v : null;
	}

	public List<Map<String, Object>> parseItemsOrEmpty(ObjectMapper objectMapper) {
		if (items == null || items.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(items, new TypeReference<List<Map<String, Object>>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	private static boolean isFalseLike(String raw) {
		if (raw == null) {
			return false;
		}
		String s = raw.trim();
		return "false".equalsIgnoreCase(s) || "0".equals(s);
	}

	private static long parsePositiveLongOrZero(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return Math.max(v, 0L);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
