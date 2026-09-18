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

package cn.shopex.ecshopx.promotions.service.register;

import cn.shopex.ecshopx.promotions.mapper.RegisterPromotionItemRightsGrantMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RegisterPromotionMembercardItemsApplyService {

	private static final Logger log = LoggerFactory.getLogger(RegisterPromotionMembercardItemsApplyService.class);

	private final RegisterPromotionItemRightsGrantMapper registerPromotionItemRightsGrantMapper;

	public RegisterPromotionMembercardItemsApplyService(
			RegisterPromotionItemRightsGrantMapper registerPromotionItemRightsGrantMapper) {
		this.registerPromotionItemRightsGrantMapper = registerPromotionItemRightsGrantMapper;
	}

	public void applyItems(long companyId, long userId, String mobilePlain, Object itemsNode) {
		if (!(itemsNode instanceof Collection<?> coll) || coll.isEmpty()) {
			return;
		}
		for (Object el : coll) {
			Map<String, Object> rowMap = normalizeItemElement(el);
			if (rowMap == null) {
				log.debug("register promotion item rights skip: non-item element {}", el);
				continue;
			}
			try {
				grantRightsForOneItemRow(companyId, userId, mobilePlain, rowMap);
			} catch (Exception ex) {
				log.debug("register promotion item rights error: {}", ex.getMessage(), ex);
			}
		}
	}

	private Map<String, Object> normalizeItemElement(Object el) {
		if (el instanceof Map<?, ?> raw) {
			Map<String, Object> m = new HashMap<>();
			for (Map.Entry<?, ?> e : raw.entrySet()) {
				if (e.getKey() != null) {
					m.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			Long itemId = firstLong(m, "item_id", "id", "itemId");
			if (itemId == null || itemId <= 0L) {
				return null;
			}
			m.put("item_id", itemId);
			if (!m.containsKey("num") || m.get("num") == null) {
				m.put("num", 1L);
			}
			if (!m.containsKey("rights_from") || m.get("rights_from") == null) {
				m.put("rights_from", "注册赠送");
			}
			return m;
		}
		Long scalarId = parseScalarItemId(el);
		if (scalarId == null || scalarId <= 0L) {
			return null;
		}
		Map<String, Object> m = new HashMap<>(4);
		m.put("item_id", scalarId);
		m.put("num", 1L);
		m.put("rights_from", "注册赠送");
		return m;
	}

	private static Long parseScalarItemId(Object el) {
		if (el instanceof Number n) {
			return n.longValue();
		}
		if (el instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static Long firstLong(Map<String, Object> m, String... keys) {
		for (String k : keys) {
			Object v = m.get(k);
			if (v == null) {
				continue;
			}
			if (v instanceof Number n) {
				return n.longValue();
			}
			try {
				return Long.parseLong(String.valueOf(v).trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private void grantRightsForOneItemRow(
			long companyId, long userId, String mobilePlain, Map<String, Object> itemRow) {
		registerPromotionItemRightsGrantMapper.grantRightsForMembercardPromotionItem(
				companyId, userId, mobilePlain, itemRow);
	}
}
