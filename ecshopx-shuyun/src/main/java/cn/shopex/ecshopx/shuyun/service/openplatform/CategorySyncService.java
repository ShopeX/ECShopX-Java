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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D2：类目同步（仅 2/3 级）。 */
@Service
public class CategorySyncService {

	private static final Logger log = LoggerFactory.getLogger(CategorySyncService.class);
	private static final DateTimeFormatter DT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public CategorySyncService(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenGatewayClient gatewayClient,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.gatewayClient = gatewayClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	/** @return true 成功或跳过；false 网关失败 */
	public boolean syncCategory(long companyId, long categoryId) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			return false;
		}
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT category_id, category_name, category_level, parent_id, created, updated
						FROM items_category WHERE company_id=? AND category_id=? LIMIT 1
						""",
						companyId,
						categoryId);
		if (rows.isEmpty()) {
			return false;
		}
		Map<String, Object> row = rows.get(0);
		int level = toInt(row.get("category_level"));
		if (level != 2 && level != 3) {
			return true;
		}
		Map<String, Object> body = buildBody(companyId, row, level);
		try {
			gatewayClient.postJson(
					companyId, ShuyunOpenPlatformGatewayActions.PRODUCT_CATEGORY_SYNC, body, "offline");
			log.info("Shuyun category.sync ok companyId={} categoryId={}", companyId, categoryId);
			return true;
		} catch (Exception e) {
			log.error(
					"Shuyun category.sync failed companyId={} categoryId={} err={}",
					companyId,
					categoryId,
					e.getMessage());
			return false;
		}
	}

	private Map<String, Object> buildBody(long companyId, Map<String, Object> row, int level) {
		String name = stringVal(row.get("category_name"));
		String parentCategoryId;
		if (level == 2) {
			long parentId = toLong(row.get("parent_id"));
			if (parentId > 0) {
				String pname =
						jdbcTemplate.query(
								"""
								SELECT category_name FROM items_category
								WHERE company_id=? AND category_id=? LIMIT 1
								""",
								rs -> rs.next() ? rs.getString(1) : null,
								companyId,
								parentId);
				if (StringUtils.hasText(pname)) {
					name = pname.trim() + "/" + name;
				}
			}
			parentCategoryId = "0";
		} else {
			parentCategoryId = String.valueOf(toLong(row.get("parent_id")));
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("parent_category_id", parentCategoryId);
		body.put("category_name", name);
		body.put("category_id", String.valueOf(toLong(row.get("category_id"))));
		body.put("created", formatTs(row.get("created")));
		body.put("modified", formatTs(row.get("updated")));
		return body;
	}

	private static String formatTs(Object v) {
		int ts = toInt(v);
		long epoch = ts > 0 ? ts : Instant.now().getEpochSecond();
		return DT.format(Instant.ofEpochSecond(epoch));
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(stringVal(v));
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(stringVal(v));
		} catch (Exception e) {
			return 0;
		}
	}
}
