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

package cn.shopex.ecshopx.salesperson.service.support;

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Normalizes {@link ShopSalesperson} fields for operator API JSON parity.
 */
public final class ShopSalespersonApiFields {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CREATED_TIME_FALLBACK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private ShopSalespersonApiFields() {
	}

	/**
	 * {@code shop_id} for JSON: null when unset or zero-like (matches nullable / empty shop in DB).
	 */
	public static Object shopIdForJson(String shopId) {
		if (!ValuePresence.hasEffectiveValue(shopId)) {
			return null;
		}
		return shopId.trim();
	}

	/**
	 * {@code created_time} on detail payloads: DB string when present; otherwise formatted from {@code created} epoch.
	 */
	public static Object createdTimeForSalespersonDetail(ShopSalesperson row) {
		if (row == null) {
			return null;
		}
		String ct = row.getCreatedTime();
		if (ct != null) {
			String t = ct.trim();
			if (!t.isEmpty()) {
				return t;
			}
		}
		Long created = row.getCreated();
		if (created != null && created != 0L) {
			return CREATED_TIME_FALLBACK.format(Instant.ofEpochSecond(created).atZone(CN));
		}
		return null;
	}

	/**
	 * Parses {@code shop_id} for numeric lookups (store lists); zero and empty are absent.
	 */
	public static Long shopIdAsLongOrNull(String shopId) {
		if (!ValuePresence.hasEffectiveValue(shopId)) {
			return null;
		}
		try {
			return Long.parseLong(shopId.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
