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

package cn.shopex.ecshopx.promotions.service.multilang;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class SeckillActivityOutsideMultiLangWriteService {

	private static final Logger log = LoggerFactory.getLogger(SeckillActivityOutsideMultiLangWriteService.class);

	private static final String TABLE_AND_MODULE = "promotions_seckill_activity";
	private static final List<String> FIELDS = List.of("activity_name", "description", "ad_pic");

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public SeckillActivityOutsideMultiLangWriteService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void updateForSeckill(long seckillId, long companyId, Map<String, Object> requestData, String requestLangTag) {
		if (seckillId <= 0L) {
			return;
		}
		String normalizedLang = normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (String field : FIELDS) {
			Object v = requestData == null ? null : requestData.get(field);
			Object boxed = v == null ? "" : v;
			String attrStr = toAttributeString(boxed);
			Integer exists =
					jdbcTemplate.queryForObject(
							"SELECT COUNT(1) FROM `"
									+ table
									+ "` WHERE company_id=? AND field=? AND table_name=? AND module_name=? AND data_id=? LIMIT 1",
							Integer.class,
							companyId,
							field,
							TABLE_AND_MODULE,
							TABLE_AND_MODULE,
							seckillId);
			int cnt = exists == null ? 0 : exists;
			if (cnt > 0) {
				jdbcTemplate.update(
						"UPDATE `"
								+ table
								+ "` SET attribute_value=?, updated=? WHERE company_id=? AND field=? AND table_name=? AND module_name=? AND data_id=?",
						attrStr,
						now,
						companyId,
						field,
						TABLE_AND_MODULE,
						TABLE_AND_MODULE,
						seckillId);
			} else {
				String sql =
						"INSERT INTO `"
								+ table
								+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
								+ "VALUES (?,?,?,?,?,?,?,?)";
				jdbcTemplate.update(
						sql, companyId, field, attrStr, TABLE_AND_MODULE, TABLE_AND_MODULE, seckillId, now, now);
			}
		}
	}

	public void addForNewSeckill(long seckillId, long companyId, Map<String, Object> requestData, String requestLangTag) {
		if (seckillId <= 0L) {
			return;
		}
		log.debug(
				"[DEBUG] SeckillActivityOutsideMultiLangWriteService outside_item_multi_lang seckillId={} companyId={}",
				seckillId,
				companyId);
		Map<String, Object> langBag = new LinkedHashMap<>();
		for (String f : FIELDS) {
			Object v = requestData == null ? null : requestData.get(f);
			langBag.put(f, v == null ? "" : v);
		}
		String normalizedLang = normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (Map.Entry<String, Object> e : langBag.entrySet()) {
			String field = e.getKey();
			String attrStr = toAttributeString(e.getValue());
			String sql = "INSERT INTO `"
					+ table
					+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
					+ "VALUES (?,?,?,?,?,?,?,?)";
			jdbcTemplate.update(
					sql, companyId, field, attrStr, TABLE_AND_MODULE, TABLE_AND_MODULE, seckillId, now, now);
		}
	}

	private String toAttributeString(Object attributeValue) {
		if (attributeValue == null) {
			return "";
		}
		if (attributeValue instanceof List<?> || attributeValue instanceof Map<?, ?>) {
			try {
				return objectMapper.writeValueAsString(attributeValue);
			} catch (Exception ex) {
				return attributeValue.toString();
			}
		}
		return attributeValue.toString();
	}

	public static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}
}
