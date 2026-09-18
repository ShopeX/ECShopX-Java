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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierOrderListItemsAssembler {

	private static final Pattern SAFE_LANG_SUFFIX = Pattern.compile("^[a-zA-Z0-9_]+$");

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public SupplierOrderListItemsAssembler(
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public Map<Long, List<Map<String, Object>>> loadItemsGroupedByOrderId(
			long companyId, int supplierId, List<Long> orderIds, String langTag) {
		Map<Long, List<Map<String, Object>>> out = new HashMap<>();
		if (orderIds == null || orderIds.isEmpty()) {
			return out;
		}
		List<NormalOrdersItems> rows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.in(NormalOrdersItems::getOrderId, orderIds)
								.eq(NormalOrdersItems::getSupplierId, supplierId)
								.orderByAsc(NormalOrdersItems::getId));
		if (rows == null || rows.isEmpty()) {
			return out;
		}
		List<Map<String, Object>> maps = new ArrayList<>(rows.size());
		for (NormalOrdersItems it : rows) {
			maps.add(new LinkedHashMap<>(AdminOrderDetailPayloadMaps.itemToMap(it)));
		}
		applyItemNameTranslations(companyId, maps, langTag);
		replaceSpecDesc(companyId, maps, langTag);
		for (Map<String, Object> m : maps) {
			long oid = longLoose(m.get("order_id"));
			out.computeIfAbsent(oid, k -> new ArrayList<>()).add(m);
		}
		return out;
	}

	private void applyItemNameTranslations(long companyId, List<Map<String, Object>> itemMaps, String langTag) {
		if (itemMaps.isEmpty()) {
			return;
		}
		Set<Long> itemIds = new LinkedHashSet<>();
		for (Map<String, Object> m : itemMaps) {
			long id = longLoose(m.get("item_id"));
			if (id > 0L) {
				itemIds.add(id);
			}
		}
		if (itemIds.isEmpty()) {
			return;
		}
		String shard = langShardTable(langTag);
		String sql =
				"SELECT data_id, attribute_value FROM `"
						+ shard
						+ "` WHERE company_id = :companyId AND table_name = 'items' AND `field` = 'item_name' AND data_id IN (:ids)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("ids", itemIds);
		List<Map<String, Object>> langRows;
		try {
			langRows = namedParameterJdbcTemplate.queryForList(sql, p);
		} catch (Exception e) {
			return;
		}
		Map<Long, String> nameByItemId = new LinkedHashMap<>();
		for (Map<String, Object> lr : langRows) {
			Object did = lr.get("data_id");
			if (did == null) {
				did = lr.get("DATA_ID");
			}
			Object av = lr.get("attribute_value");
			if (av == null) {
				av = lr.get("ATTRIBUTE_VALUE");
			}
			if (did == null || av == null) {
				continue;
			}
			long dataId = did instanceof Number n ? n.longValue() : Long.parseLong(did.toString().trim());
			String val = av.toString();
			if (StringUtils.hasText(val)) {
				nameByItemId.put(dataId, val);
			}
		}
		for (Map<String, Object> m : itemMaps) {
			long iid = longLoose(m.get("item_id"));
			if (nameByItemId.containsKey(iid)) {
				m.put("item_name", nameByItemId.get(iid));
			}
		}
	}

	private void replaceSpecDesc(long companyId, List<Map<String, Object>> itemMaps, String langTag) {
		Set<Long> needSpec = new LinkedHashSet<>();
		for (Map<String, Object> m : itemMaps) {
			Object desc = m.get("item_spec_desc");
			if (desc != null && StringUtils.hasText(desc.toString())) {
				long iid = longLoose(m.get("item_id"));
				if (iid > 0L) {
					needSpec.add(iid);
				}
			}
		}
		if (needSpec.isEmpty()) {
			return;
		}
		String inList = needSpec.stream().map(String::valueOf).collect(Collectors.joining(","));
		String baseSql =
				"SELECT rel.item_id AS item_id, rel.attribute_id AS attribute_id, rel.attribute_value_id AS attribute_value_id,"
						+ " ia.attribute_name AS attribute_name, iv.attribuattribute_valuete_name AS attribute_value_name"
						+ " FROM items_rel_attributes rel"
						+ " JOIN items_attributes ia ON ia.attribute_id = rel.attribute_id"
						+ " JOIN items_attribute_values iv ON rel.attribute_value_id = iv.attribute_value_id"
						+ " WHERE ia.attribute_type = 'item_spec' AND rel.item_id IN ("
						+ inList
						+ ")";
		List<Map<String, Object>> specRows;
		try {
			specRows = namedParameterJdbcTemplate.queryForList(baseSql, new MapSqlParameterSource());
		} catch (Exception e) {
			return;
		}
		if (specRows.isEmpty()) {
			return;
		}
		applyAttributeNameLang(companyId, specRows, langTag);
		applyAttributeValueNameLang(companyId, specRows, langTag);
		Map<Long, List<Map<String, Object>>> linesByItemId = new LinkedHashMap<>();
		for (Map<String, Object> row : specRows) {
			long itemId = longLoose(row.get("item_id"));
			if (itemId <= 0L) {
				continue;
			}
			linesByItemId.computeIfAbsent(itemId, k -> new ArrayList<>()).add(row);
		}
		for (Map<String, Object> m : itemMaps) {
			Object desc = m.get("item_spec_desc");
			if (desc == null || !StringUtils.hasText(desc.toString())) {
				continue;
			}
			long itemId = longLoose(m.get("item_id"));
			List<Map<String, Object>> lines = linesByItemId.get(itemId);
			if (lines == null || lines.isEmpty()) {
				continue;
			}
			m.put("item_spec_desc", joinSpecDesc(lines));
		}
	}

	private void applyAttributeNameLang(long companyId, List<Map<String, Object>> specRows, String langTag) {
		Set<Long> attrIds = new LinkedHashSet<>();
		for (Map<String, Object> row : specRows) {
			long aid = longLoose(row.get("attribute_id"));
			if (aid > 0L) {
				attrIds.add(aid);
			}
		}
		if (attrIds.isEmpty()) {
			return;
		}
		Map<Long, String> langById = loadLangField(companyId, langTag, "items_attributes", "attribute_name", attrIds);
		for (Map<String, Object> row : specRows) {
			long aid = longLoose(row.get("attribute_id"));
			if (langById.containsKey(aid)) {
				row.put("attribute_name", langById.get(aid));
			}
		}
	}

	private void applyAttributeValueNameLang(long companyId, List<Map<String, Object>> specRows, String langTag) {
		Set<Long> valIds = new LinkedHashSet<>();
		for (Map<String, Object> row : specRows) {
			long vid = longLoose(row.get("attribute_value_id"));
			if (vid > 0L) {
				valIds.add(vid);
			}
		}
		if (valIds.isEmpty()) {
			return;
		}
		Map<Long, String> langById =
				loadLangField(companyId, langTag, "items_attribute_values", "attribuattribute_valuete_name", valIds);
		for (Map<String, Object> row : specRows) {
			long vid = longLoose(row.get("attribute_value_id"));
			if (langById.containsKey(vid)) {
				row.put("attribute_value_name", langById.get(vid));
			}
		}
	}

	private Map<Long, String> loadLangField(
			long companyId, String langTag, String tableName, String field, Set<Long> dataIds) {
		String shard = langShardTable(langTag);
		String sql =
				"SELECT data_id, attribute_value FROM `"
						+ shard
						+ "` WHERE company_id = :companyId AND table_name = :tableName AND `field` = :field AND data_id IN (:ids)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("tableName", tableName);
		p.addValue("field", field);
		p.addValue("ids", dataIds);
		try {
			List<Map<String, Object>> langRows = namedParameterJdbcTemplate.queryForList(sql, p);
			Map<Long, String> out = new LinkedHashMap<>();
			for (Map<String, Object> lr : langRows) {
				Object did = lr.get("data_id");
				if (did == null) {
					did = lr.get("DATA_ID");
				}
				Object av = lr.get("attribute_value");
				if (av == null) {
					av = lr.get("ATTRIBUTE_VALUE");
				}
				if (did == null || av == null) {
					continue;
				}
				long dataId = did instanceof Number n ? n.longValue() : Long.parseLong(did.toString().trim());
				String val = av.toString();
				if (StringUtils.hasText(val)) {
					out.put(dataId, val);
				}
			}
			return out;
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static String joinSpecDesc(List<Map<String, Object>> lines) {
		StringBuilder sb = new StringBuilder();
		for (Map<String, Object> row : lines) {
			Object an = row.get("attribute_name");
			Object vn = row.get("attribute_value_name");
			if (vn == null) {
				vn = row.get("attribuattribute_valuete_name");
			}
			String name = an == null ? "" : an.toString();
			String val = vn == null ? "" : vn.toString();
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(name).append(':').append(val);
		}
		return sb.toString();
	}

	private static String langShardTable(String langTag) {
		String lang = resolveAcceptLanguage(langTag);
		String suffix = lang.replace("-", "").replace("_", "");
		if (!StringUtils.hasText(suffix) || !SAFE_LANG_SUFFIX.matcher(suffix).matches()) {
			suffix = "zhCN";
		}
		return "item_multi_lang_mod_lang_" + suffix;
	}

	private static String resolveAcceptLanguage(String acceptLanguageHeader) {
		if (!StringUtils.hasText(acceptLanguageHeader)) {
			return "zh-CN";
		}
		String first = acceptLanguageHeader.split(",")[0].trim();
		int semi = first.indexOf(';');
		if (semi >= 0) {
			first = first.substring(0, semi).trim();
		}
		return StringUtils.hasText(first) ? first : "zh-CN";
	}

	private static long longLoose(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
