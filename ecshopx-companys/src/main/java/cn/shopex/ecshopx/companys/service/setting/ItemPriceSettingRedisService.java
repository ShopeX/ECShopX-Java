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

package cn.shopex.ecshopx.companys.service.setting;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Redis key {@code ItemPriceSetting:{companyId}}: JSON with {@code cart_page}, {@code order_page},
 * {@code item_page}. Whole key replaced on write; no TTL.
 */
@Service
public class ItemPriceSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(ItemPriceSettingRedisService.class);

	private static final String KEY_PREFIX = "ItemPriceSetting:";

	private static final String PAGE_CART = "cart_page";
	private static final String PAGE_ORDER = "order_page";
	private static final String PAGE_ITEM = "item_page";

	private static final String FIELD_MARKET_PRICE = "market_price";
	private static final String FIELD_MEMBER_PRICE = "member_price";
	private static final String FIELD_SVIP_PRICE = "svip_price";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public ItemPriceSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private Map<String, Object> newLinkedDefaultStructure() {
		LinkedHashMap<String, Object> root = new LinkedHashMap<>();
		LinkedHashMap<String, Object> cart = new LinkedHashMap<>();
		cart.put(FIELD_MARKET_PRICE, Boolean.TRUE);
		LinkedHashMap<String, Object> order = new LinkedHashMap<>();
		order.put(FIELD_MARKET_PRICE, Boolean.TRUE);
		LinkedHashMap<String, Object> item = new LinkedHashMap<>();
		item.put(FIELD_MARKET_PRICE, Boolean.TRUE);
		item.put(FIELD_MEMBER_PRICE, Boolean.FALSE);
		item.put(FIELD_SVIP_PRICE, Boolean.FALSE);
		root.put(PAGE_CART, cart);
		root.put(PAGE_ORDER, order);
		root.put(PAGE_ITEM, item);
		return root;
	}

	/**
	 * Aligns with {@link GiftSettingRedisService}: {@code Boolean.TRUE} / {@code Boolean.FALSE} as
	 * booleans; otherwise {@code "true".equals(String.valueOf(raw))}.
	 */
	private static boolean normalizeMarketPriceBoolean(Object raw) {
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		return "true".equals(String.valueOf(raw));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> shallowMergePage(Map<?, ?> oldPage, Map<String, Object> patchPage) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (oldPage != null) {
			for (Map.Entry<?, ?> e : oldPage.entrySet()) {
				if (e.getKey() != null) {
					out.put(e.getKey().toString(), e.getValue());
				}
			}
		}
		out.putAll(patchPage);
		return out;
	}

	/**
	 * Ensures root has three pages in order {@code cart_page} → {@code order_page} → {@code item_page},
	 * each with required boolean keys and default values where missing.
	 */
	private Map<String, Object> ensureFullThreePageStructure(Map<String, Object> raw) {
		Map<String, Object> defaults = newLinkedDefaultStructure();
		LinkedHashMap<String, Object> root = new LinkedHashMap<>();
		root.put(PAGE_CART, mergePageDefaults((Map<?, ?>) raw.get(PAGE_CART), (Map<String, Object>) defaults.get(PAGE_CART)));
		root.put(PAGE_ORDER, mergePageDefaults((Map<?, ?>) raw.get(PAGE_ORDER), (Map<String, Object>) defaults.get(PAGE_ORDER)));
		root.put(PAGE_ITEM, mergeItemPageDefaults((Map<?, ?>) raw.get(PAGE_ITEM), (Map<String, Object>) defaults.get(PAGE_ITEM)));
		return root;
	}

	private Map<String, Object> mergePageDefaults(Map<?, ?> fromRedis, Map<String, Object> pageDefaults) {
		LinkedHashMap<String, Object> page = new LinkedHashMap<>(pageDefaults);
		if (fromRedis != null && fromRedis.containsKey(FIELD_MARKET_PRICE) && fromRedis.get(FIELD_MARKET_PRICE) != null) {
			page.put(FIELD_MARKET_PRICE, normalizeMarketPriceBoolean(fromRedis.get(FIELD_MARKET_PRICE)));
		}
		return page;
	}

	private Map<String, Object> mergeItemPageDefaults(Map<?, ?> fromRedis, Map<String, Object> pageDefaults) {
		LinkedHashMap<String, Object> page = new LinkedHashMap<>(pageDefaults);
		if (fromRedis == null) {
			return page;
		}
		if (fromRedis.containsKey(FIELD_MARKET_PRICE) && fromRedis.get(FIELD_MARKET_PRICE) != null) {
			page.put(FIELD_MARKET_PRICE, normalizeMarketPriceBoolean(fromRedis.get(FIELD_MARKET_PRICE)));
		}
		if (fromRedis.containsKey(FIELD_MEMBER_PRICE) && fromRedis.get(FIELD_MEMBER_PRICE) != null) {
			page.put(FIELD_MEMBER_PRICE, normalizeMarketPriceBoolean(fromRedis.get(FIELD_MEMBER_PRICE)));
		}
		if (fromRedis.containsKey(FIELD_SVIP_PRICE) && fromRedis.get(FIELD_SVIP_PRICE) != null) {
			page.put(FIELD_SVIP_PRICE, normalizeMarketPriceBoolean(fromRedis.get(FIELD_SVIP_PRICE)));
		}
		return page;
	}

	public Map<String, Object> getItemPriceSetting(long companyId) {
		String json;
		try {
			json = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (DataAccessException e) {
			log.warn("Failed to read item price setting from Redis for companyId={}", companyId, e);
			throw new ResourceException("读取商品价格显示配置失败");
		}
		if (!StringUtils.hasText(json)) {
			return newLinkedDefaultStructure();
		}
		Map<String, Object> parsed;
		try {
			parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			log.warn("Invalid item price setting JSON for companyId={}", companyId, e);
			throw new ResourceException("商品价格显示配置数据格式无效");
		}
		return ensureFullThreePageStructure(parsed);
	}

	/**
	 * H5 read path: Redis string is returned as parsed JSON without merging defaults or normalizing
	 * booleans; blank Redis value yields the default three-page structure; invalid JSON or JSON
	 * {@code null} yields {@code null}.
	 */
	public Object getItemPriceSettingForH5(long companyId) {
		String json;
		try {
			json = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (DataAccessException e) {
			log.warn("Failed to read item price setting from Redis for companyId={}", companyId, e);
			throw new ResourceException("读取商品价格显示配置失败");
		}
		if (!StringUtils.hasText(json)) {
			return newLinkedDefaultStructure();
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(json);
		} catch (JsonProcessingException e) {
			log.warn("H5 item price setting JSON parse failed, companyId={}", companyId, e);
			return null;
		}
		if (root == null || root.isNull()) {
			return null;
		}
		if (root.isObject()) {
			return objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
		}
		return objectMapper.convertValue(root, Object.class);
	}

	public Map<String, Object> saveItemPriceSetting(long companyId, Map<String, Object> mergedRequest) {
		Map<String, Object> merged = mergedRequest == null ? Map.of() : mergedRequest;
		LinkedHashMap<String, Map<String, Object>> patch = new LinkedHashMap<>();

		Object cartRaw = merged.get(PAGE_CART);
		if (cartRaw != null && cartRaw instanceof Map<?, ?> cartMap) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cm = (Map<String, Object>) cartMap;
			if (cm.containsKey(FIELD_MARKET_PRICE) && cm.get(FIELD_MARKET_PRICE) != null) {
				LinkedHashMap<String, Object> p = new LinkedHashMap<>();
				p.put(FIELD_MARKET_PRICE, normalizeMarketPriceBoolean(cm.get(FIELD_MARKET_PRICE)));
				patch.put(PAGE_CART, p);
			}
		}

		Object orderRaw = merged.get(PAGE_ORDER);
		if (orderRaw != null && orderRaw instanceof Map<?, ?> orderMap) {
			@SuppressWarnings("unchecked")
			Map<String, Object> om = (Map<String, Object>) orderMap;
			if (om.containsKey(FIELD_MARKET_PRICE) && om.get(FIELD_MARKET_PRICE) != null) {
				LinkedHashMap<String, Object> p = new LinkedHashMap<>();
				p.put(FIELD_MARKET_PRICE, normalizeMarketPriceBoolean(om.get(FIELD_MARKET_PRICE)));
				patch.put(PAGE_ORDER, p);
			}
		}

		Object itemRaw = merged.get(PAGE_ITEM);
		if (itemRaw != null && itemRaw instanceof Map<?, ?> itemMap) {
			@SuppressWarnings("unchecked")
			Map<String, Object> im = (Map<String, Object>) itemMap;
			LinkedHashMap<String, Object> p = new LinkedHashMap<>();
			if (im.containsKey(FIELD_MARKET_PRICE) && im.get(FIELD_MARKET_PRICE) != null) {
				p.put(FIELD_MARKET_PRICE, normalizeMarketPriceBoolean(im.get(FIELD_MARKET_PRICE)));
			}
			if (im.containsKey(FIELD_MEMBER_PRICE) && im.get(FIELD_MEMBER_PRICE) != null) {
				p.put(FIELD_MEMBER_PRICE, normalizeMarketPriceBoolean(im.get(FIELD_MEMBER_PRICE)));
			}
			if (im.containsKey(FIELD_SVIP_PRICE) && im.get(FIELD_SVIP_PRICE) != null) {
				p.put(FIELD_SVIP_PRICE, normalizeMarketPriceBoolean(im.get(FIELD_SVIP_PRICE)));
			}
			if (!p.isEmpty()) {
				patch.put(PAGE_ITEM, p);
			}
		}

		Map<String, Object> data = getItemPriceSetting(companyId);
		for (String pageKey : new String[] {PAGE_CART, PAGE_ORDER, PAGE_ITEM}) {
			if (patch.containsKey(pageKey)) {
				@SuppressWarnings("unchecked")
				Map<?, ?> oldPage = (Map<?, ?>) data.get(pageKey);
				data.put(pageKey, shallowMergePage(oldPage, patch.get(pageKey)));
			}
		}

		try {
			String outJson = objectMapper.writeValueAsString(data);
			companysRedisTemplate.opsForValue().set(key(companyId), outJson);
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize item price setting for companyId={}", companyId, e);
			throw new ResourceException("保存商品价格显示配置失败");
		} catch (DataAccessException e) {
			log.warn("Failed to write item price setting to Redis for companyId={}", companyId, e);
			throw new ResourceException("写入商品价格显示配置失败");
		}
		return data;
	}
}
