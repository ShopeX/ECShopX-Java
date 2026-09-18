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

package cn.shopex.ecshopx.supplier.service.multilang;

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
public class SupplierOutsideMultiLangWriteService {

	private static final Logger log = LoggerFactory.getLogger(SupplierOutsideMultiLangWriteService.class);

	private static final String TABLE_AND_MODULE = "supplier";
	private static final List<String> FIELDS = List.of("supplier_name", "contact", "business_license", "bank_name");

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public SupplierOutsideMultiLangWriteService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void addForNewSupplier(long supplierId, long companyId, Map<String, Object> requestData, String requestLangTag) {
		if (supplierId <= 0L) {
			return;
		}
		log.debug(
				"[DEBUG] SupplierOutsideMultiLangWriteService outside_item_multi_lang supplierId={} companyId={}",
				supplierId,
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
					sql, companyId, field, attrStr, TABLE_AND_MODULE, TABLE_AND_MODULE, supplierId, now, now);
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
