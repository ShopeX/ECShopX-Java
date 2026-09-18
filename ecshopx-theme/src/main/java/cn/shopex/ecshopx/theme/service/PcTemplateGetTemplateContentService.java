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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.service.pagestemplate.PcTemplateDecorationItemPriceResolveService;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplate;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplateContent;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateContentMapper;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateMapper;
import cn.shopex.ecshopx.theme.support.ThemePcTemplateContentRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PcTemplateGetTemplateContentService {

	private final ThemePcTemplateMapper themePcTemplateMapper;
	private final ThemePcTemplateContentMapper themePcTemplateContentMapper;
	private final ThemePcTemplateContentRowMapper themePcTemplateContentRowMapper;
	private final ThemePcTemplateContentLangReadService themePcTemplateContentLangReadService;
	private final PcTemplateDecorationItemPriceResolveService pcTemplateDecorationItemPriceResolveService;
	private final ObjectMapper objectMapper;

	public List<Map<String, Object>> getTemplateContent(
			long companyId,
			String requestLang,
			String rawThemePcTemplateId,
			String pageTypeForFrontOrNull,
			long userId) {
		if (pageTypeForFrontOrNull == null) {
			if (rawThemePcTemplateId == null || rawThemePcTemplateId.trim().isEmpty()) {
				return Collections.emptyList();
			}
			String trimmedId = rawThemePcTemplateId.trim();
			long themeId;
			try {
				themeId = Long.parseLong(trimmedId);
			} catch (NumberFormatException ex) {
				throw new BadRequestException("theme_pc_template_id 无效");
			}
			if (themeId <= 0L) {
				return Collections.emptyList();
			}
			return buildTemplateContentRows(companyId, themeId, requestLang, userId);
		}
		if (isUnsetThemePcTemplateId(rawThemePcTemplateId)) {
			String pageType =
					pageTypeForFrontOrNull.trim().isEmpty() ? "index" : pageTypeForFrontOrNull.trim();
			List<ThemePcTemplate> tpls =
					themePcTemplateMapper.selectList(
							new LambdaQueryWrapper<ThemePcTemplate>()
									.eq(ThemePcTemplate::getCompanyId, companyId)
									.eq(ThemePcTemplate::getPageType, pageType)
									.eq(ThemePcTemplate::getStatus, 1)
									.isNull(ThemePcTemplate::getDeletedAt)
									.orderByAsc(ThemePcTemplate::getThemePcTemplateId)
									.last("LIMIT 1"));
			if (tpls == null || tpls.isEmpty()) {
				return Collections.emptyList();
			}
			long themeId = tpls.get(0).getThemePcTemplateId();
			return buildTemplateContentRows(companyId, themeId, requestLang, userId);
		}
		String trimmedId = rawThemePcTemplateId.trim();
		long themeId;
		try {
			themeId = Long.parseLong(trimmedId);
		} catch (NumberFormatException ex) {
			return Collections.emptyList();
		}
		if (themeId <= 0L) {
			return Collections.emptyList();
		}
		return buildTemplateContentRows(companyId, themeId, requestLang, userId);
	}

	private static boolean isUnsetThemePcTemplateId(String raw) {
		if (raw == null) {
			return true;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return true;
		}
		if ("0".equals(t)) {
			return true;
		}
		try {
			return Long.parseLong(t) <= 0L;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	private List<Map<String, Object>> buildTemplateContentRows(
			long companyId, long themeId, String requestLang, long userId) {
		List<ThemePcTemplate> tpls =
				themePcTemplateMapper.selectList(
						new LambdaQueryWrapper<ThemePcTemplate>()
								.eq(ThemePcTemplate::getCompanyId, companyId)
								.eq(ThemePcTemplate::getThemePcTemplateId, themeId)
								.isNull(ThemePcTemplate::getDeletedAt));
		if (tpls == null || tpls.isEmpty()) {
			return Collections.emptyList();
		}

		List<ThemePcTemplateContent> rows =
				themePcTemplateContentMapper.selectList(
						new LambdaQueryWrapper<ThemePcTemplateContent>()
								.eq(ThemePcTemplateContent::getCompanyId, companyId)
								.eq(ThemePcTemplateContent::getThemePcTemplateId, themeId)
								.orderByAsc(ThemePcTemplateContent::getThemePcTemplateContentId));
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}

		List<Map<String, Object>> result = new ArrayList<>();
		for (ThemePcTemplateContent entity : rows) {
			Map<String, Object> row = themePcTemplateContentRowMapper.toRowMap(entity);
			themePcTemplateContentLangReadService.applyThemePcTemplateContentDetailLangOverlay(companyId, row, requestLang);
			String paramsStr = row.get("params") == null ? "" : String.valueOf(row.get("params"));
			JsonNode root;
			try {
				root = objectMapper.readTree(paramsStr);
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("装修数据不合法");
			}
			if (!root.isObject()) {
				if (root.isArray()) {
					throw new BadRequestException("装修数据不合法");
				}
				throw new BadRequestException("装修数据不合法");
			}
			ObjectNode config = (ObjectNode) root;
			List<Long> decorationItems = collectDecorationItemIds(config);
			Map<Long, JsonNode> priceByItemId =
					pcTemplateDecorationItemPriceResolveService.resolvePriceByItemIds(
							companyId, userId, decorationItems, requestLang);
			setItemPrice(config, priceByItemId);
			setItemCategory(config);

			String configJson;
			try {
				configJson = objectMapper.writeValueAsString(config);
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("装修数据不合法");
			}
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", entity.getThemePcTemplateContentId());
			item.put("name", entity.getName() == null ? "" : entity.getName());
			item.put("config", configJson);
			result.add(item);
		}
		return result;
	}

	private List<Long> collectDecorationItemIds(ObjectNode config) {
		LinkedHashSet<Long> acc = new LinkedHashSet<>();
		collectDecorationItemIdsInner(config, acc);
		return new ArrayList<>(acc);
	}

	private void collectDecorationItemIdsInner(ObjectNode config, LinkedHashSet<Long> out) {
		if (config == null) {
			return;
		}
		String t = textType(config);
		if ("W0002".equals(t)) {
			collectGoodsIdsFromDataArray(config.get("data"), out);
			return;
		}
		if ("W0005".equals(t)) {
			collectW0005(config, out);
			return;
		}
		if ("W0012".equals(t)) {
			collectW0012(config, out);
			return;
		}
		if ("W0015".equals(t) || "W0018".equals(t)) {
			collectW0015OrW0018(config, out);
			return;
		}
		if ("W0006".equals(t)) {
			JsonNode cw = config.get("childWidgets");
			if (cw == null || !cw.isArray()) {
				return;
			}
			for (JsonNode ch : cw) {
				if (ch != null && ch.isObject()) {
					collectDecorationItemIdsInner((ObjectNode) ch, out);
				}
			}
		}
	}

	private static void collectW0005(ObjectNode config, LinkedHashSet<Long> out) {
		JsonNode outer = config.get("data");
		if (outer == null || !outer.isArray()) {
			return;
		}
		for (JsonNode cellRow : outer) {
			if (cellRow == null || !cellRow.isObject()) {
				continue;
			}
			collectGoodsIdsFromDataArray(cellRow.get("data"), out);
		}
	}

	private void collectW0012(ObjectNode config, LinkedHashSet<Long> out) {
		JsonNode data = config.get("data");
		if (data == null || !data.isArray()) {
			return;
		}
		for (JsonNode slot : data) {
			if (slot != null && slot.isObject()) {
				Iterator<Map.Entry<String, JsonNode>> it = slot.fields();
				while (it.hasNext()) {
					maybeCollectGoodsPanel(it.next().getValue(), out);
				}
			} else if (slot != null && slot.isArray()) {
				for (JsonNode v : slot) {
					maybeCollectGoodsPanel(v, out);
				}
			}
		}
	}

	private static void maybeCollectGoodsPanel(JsonNode v, LinkedHashSet<Long> out) {
		if (v == null || !v.isObject()) {
			return;
		}
		ObjectNode o = (ObjectNode) v;
		if (!"goods".equals(textType(o))) {
			return;
		}
		collectGoodsIdsFromDataArray(o.get("data"), out);
	}

	private static void collectW0015OrW0018(ObjectNode config, LinkedHashSet<Long> out) {
		JsonNode data = config.get("data");
		if (data == null || !data.isArray()) {
			return;
		}
		for (JsonNode val : data) {
			if (val == null || !val.isObject()) {
				continue;
			}
			collectGoodsIdsFromDataArray(val.get("data"), out);
		}
	}

	private static void collectGoodsIdsFromDataArray(JsonNode dataArr, LinkedHashSet<Long> out) {
		if (dataArr == null || !dataArr.isArray()) {
			return;
		}
		for (JsonNode n : dataArr) {
			if (n == null || !n.isObject()) {
				continue;
			}
			long g = parseGoodsId(n.get("goods_id"));
			if (g > 0L) {
				out.add(g);
			}
		}
	}

	private ObjectNode setItemPrice(ObjectNode config, Map<Long, JsonNode> priceByItemId) {
		if (config == null) {
			return config;
		}
		String t = textType(config);
		if ("W0002".equals(t)) {
			applyPriceToDataArray(config.get("data"), priceByItemId);
			return config;
		}
		if ("W0005".equals(t)) {
			applyPriceW0005(config, priceByItemId);
			return config;
		}
		if ("W0012".equals(t)) {
			applyPriceW0012(config, priceByItemId);
			return config;
		}
		if ("W0015".equals(t) || "W0018".equals(t)) {
			applyPriceW0015OrW0018(config, priceByItemId);
			return config;
		}
		if ("W0006".equals(t)) {
			JsonNode cw = config.get("childWidgets");
			if (cw != null && cw.isArray()) {
				ArrayNode arr = (ArrayNode) cw;
				for (int i = 0; i < arr.size(); i++) {
					JsonNode el = arr.get(i);
					if (el != null && el.isObject()) {
						setItemPrice((ObjectNode) el, priceByItemId);
					}
				}
			}
		}
		return config;
	}

	private static void applyPriceToDataArray(JsonNode dataArr, Map<Long, JsonNode> priceByItemId) {
		if (dataArr == null || !dataArr.isArray()) {
			return;
		}
		ArrayNode arr = (ArrayNode) dataArr;
		for (int i = 0; i < arr.size(); i++) {
			JsonNode el = arr.get(i);
			if (el == null || !el.isObject()) {
				continue;
			}
			ObjectNode od = (ObjectNode) el;
			long gid = parseGoodsId(od.get("goods_id"));
			JsonNode pr = priceByItemId.get(gid);
			if (pr != null) {
				od.set("price", pr);
			}
		}
	}

	private static void applyPriceW0005(ObjectNode config, Map<Long, JsonNode> priceByItemId) {
		JsonNode outer = config.get("data");
		if (outer == null || !outer.isArray()) {
			return;
		}
		for (JsonNode cellRow : outer) {
			if (cellRow == null || !cellRow.isObject()) {
				continue;
			}
			applyPriceToDataArray(cellRow.get("data"), priceByItemId);
		}
	}

	private static void applyPriceW0012(ObjectNode config, Map<Long, JsonNode> priceByItemId) {
		JsonNode data = config.get("data");
		if (data == null || !data.isArray()) {
			return;
		}
		for (JsonNode slot : data) {
			if (slot != null && slot.isObject()) {
				ObjectNode oo = (ObjectNode) slot;
				Iterator<Map.Entry<String, JsonNode>> it = oo.fields();
				while (it.hasNext()) {
					applyPriceToGoodsPanel(it.next().getValue(), priceByItemId);
				}
			} else if (slot != null && slot.isArray()) {
				for (JsonNode v : slot) {
					applyPriceToGoodsPanel(v, priceByItemId);
				}
			}
		}
	}

	private static void applyPriceToGoodsPanel(JsonNode v, Map<Long, JsonNode> priceByItemId) {
		if (v == null || !v.isObject()) {
			return;
		}
		ObjectNode panel = (ObjectNode) v;
		if (!"goods".equals(textType(panel))) {
			return;
		}
		applyPriceToDataArray(panel.get("data"), priceByItemId);
	}

	private static void applyPriceW0015OrW0018(ObjectNode config, Map<Long, JsonNode> priceByItemId) {
		JsonNode data = config.get("data");
		if (data == null || !data.isArray()) {
			return;
		}
		for (JsonNode val : data) {
			if (val == null || !val.isObject()) {
				continue;
			}
			applyPriceToDataArray(val.get("data"), priceByItemId);
		}
	}

	private ObjectNode setItemCategory(ObjectNode config) {
		if (config == null) {
			return config;
		}
		String t = textType(config);
		if ("W0007".equals(t)) {
			trimW0007CategoryData(config);
		}
		if ("W0006".equals(t)) {
			JsonNode cw = config.get("childWidgets");
			if (cw != null && cw.isArray()) {
				ArrayNode arr = (ArrayNode) cw;
				for (int i = 0; i < arr.size(); i++) {
					JsonNode el = arr.get(i);
					if (el != null && el.isObject()) {
						arr.set(i, setItemCategory((ObjectNode) el));
					}
				}
			}
		}
		return config;
	}

	private void trimW0007CategoryData(ObjectNode config) {
		JsonNode cat = config.get("categoryData");
		if (cat == null || !cat.isArray()) {
			return;
		}
		ArrayNode catArr = (ArrayNode) cat;
		for (int x = 0; x < catArr.size(); x++) {
			JsonNode lv1Node = catArr.get(x);
			if (lv1Node == null || !lv1Node.isObject()) {
				continue;
			}
			ObjectNode lv1 = (ObjectNode) lv1Node;
			JsonNode children1 = lv1.get("children");
			if (children1 != null && children1.isArray()) {
				ArrayNode ch1 = (ArrayNode) children1;
				for (int y = 0; y < ch1.size(); y++) {
					JsonNode lv2Node = ch1.get(y);
					if (lv2Node == null || !lv2Node.isObject()) {
						continue;
					}
					ObjectNode lv2 = (ObjectNode) lv2Node;
					JsonNode children2 = lv2.get("children");
					if (children2 != null && children2.isArray()) {
						ArrayNode ch2 = (ArrayNode) children2;
						for (int z = 0; z < ch2.size(); z++) {
							JsonNode lv3Node = ch2.get(z);
							if (lv3Node == null || !lv3Node.isObject()) {
								continue;
							}
							ObjectNode lv3 = (ObjectNode) lv3Node;
							ObjectNode trimmed3 = objectMapper.createObjectNode();
							trimmed3.set("category_id", lv3.get("category_id"));
							trimmed3.set("category_name", lv3.get("category_name"));
							ch2.set(z, trimmed3);
						}
						ObjectNode newLv2 = objectMapper.createObjectNode();
						newLv2.set("category_id", lv2.get("category_id"));
						newLv2.set("category_name", lv2.get("category_name"));
						newLv2.set("children", ch2);
						ch1.set(y, newLv2);
					} else {
						ObjectNode newLv2 = objectMapper.createObjectNode();
						newLv2.set("category_id", lv2.get("category_id"));
						newLv2.set("category_name", lv2.get("category_name"));
						ch1.set(y, newLv2);
					}
				}
				ObjectNode newLv1 = objectMapper.createObjectNode();
				newLv1.set("category_id", lv1.get("category_id"));
				newLv1.set("category_name", lv1.get("category_name"));
				newLv1.set("image_url", lv1.get("image_url"));
				newLv1.set("children", ch1);
				catArr.set(x, newLv1);
			} else {
				ObjectNode newLv1 = objectMapper.createObjectNode();
				newLv1.set("category_id", lv1.get("category_id"));
				newLv1.set("category_name", lv1.get("category_name"));
				newLv1.set("image_url", lv1.get("image_url"));
				catArr.set(x, newLv1);
			}
		}
	}

	private static String textType(ObjectNode config) {
		JsonNode t = config.get("type");
		if (t == null || t.isNull()) {
			return "";
		}
		return t.asText("");
	}

	private static long parseGoodsId(JsonNode n) {
		if (n == null || n.isNull()) {
			return 0L;
		}
		if (n.isIntegralNumber()) {
			return n.longValue();
		}
		if (n.isNumber()) {
			return n.longValue();
		}
		if (n.isTextual()) {
			String s = n.asText().trim();
			if (s.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException ex) {
				return 0L;
			}
		}
		return 0L;
	}
}
