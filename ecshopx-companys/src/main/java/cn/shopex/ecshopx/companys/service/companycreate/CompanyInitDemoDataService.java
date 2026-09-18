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

package cn.shopex.ecshopx.companys.service.companycreate;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Seeds operational defaults for a new company after {@code CompanyCreateEvent} (async, AFTER_COMMIT).
 * Uses {@link JdbcTemplate} so {@code ecshopx-companys} stays free of a Maven dependency on
 * {@code ecshopx-goods} / theme / wechat modules (those depend on companys and would cycle).
 */
@Service
public class CompanyInitDemoDataService {

	private static final Logger log = LoggerFactory.getLogger(CompanyInitDemoDataService.class);

	private static final String SMS_SCENE_RESOURCE = "company-init/sms_scene.json";

	private static final String DEFAULT_FEE_CONF =
			"[{\"add_fee\":\"\",\"start_fee\":\"\",\"add_standard\":\"\",\"start_standard\":\"\"}]";
	private static final String DEFAULT_FREE_CONF =
			"[{\"area\":\"0\",\"upmoney\":\"\",\"freetype\":\"1\",\"inweight\":\"\"}]";

	private static final String DEMO_BRAND_IMAGE =
			"https://preissue-b-img-cdn.yuanyuanke.cn/image/42/2021/09/27/07fcd2f4c93e1b1a27ab843a3bc4bef4hzBrebpk5dqu7Lp2paK94gwURNeQN4JH";

	private final CompanysMapper companysMapper;
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public CompanyInitDemoDataService(
			CompanysMapper companysMapper, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.companysMapper = companysMapper;
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void initialize(long companyId) {
		Companys c = companysMapper.selectById(companyId);
		if (c == null) {
			log.warn("CompanyInitDemoDataService skip: company not found companyId={}", companyId);
			return;
		}
		seedAliyunsmsScenes(companyId);
		ensureDefaultFreeShippingTemplate(companyId);
		ensureDemoBrandSpecParams(companyId);
		Long mainLeafCategoryId = ensureDemoCategories(companyId);
		if (mainLeafCategoryId != null) {
			linkMainLeafToSpecAndParams(companyId, mainLeafCategoryId);
		}
		/*
		 * Demo sellable SKU rows are intentionally not created here: a listing-ready item requires
		 * goods-domain orchestration (goods aggregate, item_rel_attributes, category relations, stock
		 * rows) implemented in ecshopx-goods; adding that module as a dependency of ecshopx-companys
		 * would introduce a Maven cycle because ecshopx-goods already depends on ecshopx-companys.
		 */
		/*
		 * Mini-program page templates and WeappSetting rows are owned by ecshopx-wechat / theme
		 * modules. Those stacks are not on the companys module classpath without new edges that risk
		 * dependency cycles; defer to a future listener in the owning module once a stable API exists.
		 */
		/*
		 * PC theme templates (ThemePcTemplateServices / ThemePcTemplateContent) live in the theme stack;
		 * not reachable from companys without cross-module coupling that is not yet defined for
		 * post-create hooks.
		 */
	}

	private void seedAliyunsmsScenes(long companyId) {
		try {
			Integer existing =
					jdbcTemplate.queryForObject(
							"SELECT COUNT(1) FROM aliyunsms_scene WHERE company_id = ?", Integer.class, companyId);
			if (existing != null && existing > 0) {
				return;
			}
			ClassPathResource res = new ClassPathResource(SMS_SCENE_RESOURCE);
			if (!res.exists()) {
				log.warn("CompanyInitDemoDataService skip SMS scenes: classpath:{} missing", SMS_SCENE_RESOURCE);
				return;
			}
			try (InputStream in = res.getInputStream()) {
				JsonNode root = objectMapper.readTree(in);
				if (!root.isArray()) {
					return;
				}
				int now = (int) (System.currentTimeMillis() / 1000L);
				for (JsonNode item : root) {
					String sceneName = textOrEmpty(item.get("scene_name"));
					String sceneTitle = textOrNull(item.get("scene_title"));
					String templateType = textOrEmpty(item.get("template_type"));
					String defaultTemplate = textOrNull(item.get("default_template"));
					String variablesJson = null;
					JsonNode vars = item.get("variables");
					if (vars != null && !vars.isNull()) {
						variablesJson = objectMapper.writeValueAsString(vars);
					}
					jdbcTemplate.update(
							"INSERT INTO aliyunsms_scene (company_id, scene_name, scene_title, template_type, status, "
									+ "default_template, variables, created, updated) VALUES (?,?,?,?,?,?,?,?,?)",
							companyId,
							sceneName,
							sceneTitle,
							templateType,
							"disabled",
							defaultTemplate,
							variablesJson,
							now,
							now);
				}
			}
		} catch (DataAccessException e) {
			log.warn(
					"CompanyInitDemoDataService aliyunsms_scene seed failed companyId={} msg={}",
					companyId,
					e.getMessage());
		} catch (Exception e) {
			log.warn(
					"CompanyInitDemoDataService aliyunsms_scene seed failed companyId={} msg={}",
					companyId,
					e.getMessage());
		}
	}

	private void ensureDefaultFreeShippingTemplate(long companyId) {
		try {
			Integer cnt =
					jdbcTemplate.queryForObject(
							"SELECT COUNT(1) FROM shipping_templates WHERE company_id = ?",
							Integer.class,
							companyId);
			if (cnt != null && cnt > 0) {
				return;
			}
			int now = (int) (System.currentTimeMillis() / 1000L);
			jdbcTemplate.update(
					"INSERT INTO shipping_templates (company_id, name, is_free, distributor_id, supplier_id, "
							+ "valuation, protect, protect_rate, minprice, status, fee_conf, nopost_conf, free_conf, "
							+ "create_time, update_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
					companyId,
					"包邮",
					"1",
					0L,
					0L,
					"1",
					false,
					java.math.BigDecimal.ZERO,
					java.math.BigDecimal.ZERO,
					true,
					DEFAULT_FEE_CONF,
					"[]",
					DEFAULT_FREE_CONF,
					now,
					now);
		} catch (DataAccessException e) {
			log.warn(
					"CompanyInitDemoDataService default shipping template not created companyId={} msg={}",
					companyId,
					e.getMessage());
		}
	}

	private void ensureDemoBrandSpecParams(long companyId) {
		try {
			insertBrandIfAbsent(companyId);
			insertSpecWithValuesIfAbsent(companyId);
			insertParamsWithValuesIfAbsent(companyId);
		} catch (DataAccessException e) {
			log.warn(
					"CompanyInitDemoDataService attributes seed failed companyId={} msg={}",
					companyId,
					e.getMessage());
		}
	}

	private void insertBrandIfAbsent(long companyId) {
		Integer n =
				jdbcTemplate.queryForObject(
						"SELECT COUNT(1) FROM items_attributes WHERE company_id = ? AND attribute_type = 'brand' AND attribute_name = ?",
						Integer.class,
						companyId,
						"ECshopX");
		if (n != null && n > 0) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		insertAttributeRow(
				companyId,
				"brand",
				"ECshopX",
				"",
				"true",
				"true",
				DEMO_BRAND_IMAGE,
				now);
	}

	private void insertSpecWithValuesIfAbsent(long companyId) {
		Integer n =
				jdbcTemplate.queryForObject(
						"SELECT COUNT(1) FROM items_attributes WHERE company_id = ? AND attribute_type = 'item_spec' AND attribute_name = ?",
						Integer.class,
						companyId,
						"颜色");
		if (n != null && n > 0) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		Long attributeId =
				insertAttributeRow(companyId, "item_spec", "颜色", "", "true", "false", "", now);
		if (attributeId == null) {
			return;
		}
		insertAttributeValueRow(companyId, attributeId, "红色", "2", "", now);
		insertAttributeValueRow(companyId, attributeId, "绿色", "1", "", now);
	}

	private void insertParamsWithValuesIfAbsent(long companyId) {
		Integer n =
				jdbcTemplate.queryForObject(
						"SELECT COUNT(1) FROM items_attributes WHERE company_id = ? AND attribute_type = 'item_params' AND attribute_name = ?",
						Integer.class,
						companyId,
						"原产地");
		if (n != null && n > 0) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		Long attributeId =
				insertAttributeRow(companyId, "item_params", "原产地", "", "true", "false", "", now);
		if (attributeId != null) {
			insertAttributeValueRow(companyId, attributeId, "中国", "1", "", now);
		}
	}

	private Long insertAttributeRow(
			long companyId,
			String attributeType,
			String attributeName,
			String attributeMemo,
			String isShow,
			String isImage,
			String imageUrl,
			int now) {
		KeyHolder kh = new GeneratedKeyHolder();
		jdbcTemplate.update(
				con -> {
					PreparedStatement ps =
							con.prepareStatement(
									"INSERT INTO items_attributes (company_id, shop_id, attribute_type, attribute_show, "
											+ "attribute_name, attribute_memo, attribute_sort, distributor_id, is_show, is_image, "
											+ "image_url, created, updated) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
									Statement.RETURN_GENERATED_KEYS);
					ps.setLong(1, companyId);
					ps.setLong(2, 0L);
					ps.setString(3, attributeType);
					ps.setString(4, "select");
					ps.setString(5, attributeName);
					ps.setString(6, attributeMemo);
					ps.setString(7, "1");
					ps.setLong(8, 0L);
					ps.setString(9, isShow);
					ps.setString(10, isImage);
					ps.setString(11, StringUtils.hasText(imageUrl) ? imageUrl : "");
					ps.setInt(12, now);
					ps.setInt(13, now);
					return ps;
				},
				kh);
		Number key = kh.getKey();
		return key != null ? key.longValue() : null;
	}

	private void insertAttributeValueRow(
			long companyId, long attributeId, String valueText, String sort, String imageUrl, int now) {
		jdbcTemplate.update(
				"INSERT INTO items_attribute_values (attribute_id, company_id, shop_id, attribuattribute_valuete_name, sort, image_url, created, updated) "
						+ "VALUES (?,?,?,?,?,?,?,?)",
				attributeId,
				companyId,
				0L,
				valueText,
				sort,
				imageUrl != null ? imageUrl : "",
				now,
				now);
	}

	/** @return main-category leaf id (level 3), or {@code null} if tree seed failed */
	private Long ensureDemoCategories(long companyId) {
		try {
			ensureSingleShopCategory(companyId);
			return ensureMainCategoryTree(companyId);
		} catch (DataAccessException e) {
			log.warn(
					"CompanyInitDemoDataService category seed failed companyId={} msg={}",
					companyId,
					e.getMessage());
			return null;
		}
	}

	private void ensureSingleShopCategory(long companyId) {
		Long existing =
				jdbcTemplate.query(
						"SELECT category_id FROM items_category WHERE company_id = ? AND is_main_category = 0 AND category_name = ? AND parent_id = 0 LIMIT 1",
						rs -> rs.next() ? rs.getLong(1) : null,
						companyId,
						"热销商品");
		if (existing != null) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		Long id = insertCategoryRow(companyId, 0L, "热销商品", 1, false, "", 0L, now);
		if (id != null) {
			jdbcTemplate.update("UPDATE items_category SET path = ? WHERE category_id = ? AND company_id = ?", id, id, companyId);
		}
	}

	/** Main tree: 热销商品 (L1) → 热销 (L2) → 爆品 (L3, leaf). */
	private Long ensureMainCategoryTree(long companyId) {
		Long leaf =
				jdbcTemplate.query(
						"SELECT category_id FROM items_category WHERE company_id = ? AND category_level = 3 AND is_main_category = 1 AND category_name = ? LIMIT 1",
						rs -> rs.next() ? rs.getLong(1) : null,
						companyId,
						"爆品");
		if (leaf != null) {
			return leaf;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		Long l1 = insertCategoryRow(companyId, 0L, "热销商品", 1, true, "", 0L, now);
		if (l1 == null) {
			return null;
		}
		jdbcTemplate.update("UPDATE items_category SET path = ? WHERE category_id = ? AND company_id = ?", l1, l1, companyId);
		Long l2 = insertCategoryRow(companyId, l1, "热销", 2, true, String.valueOf(l1), 0L, now);
		if (l2 == null) {
			return null;
		}
		String path2 = l1 + "," + l2;
		jdbcTemplate.update("UPDATE items_category SET path = ? WHERE category_id = ? AND company_id = ?", path2, l2, companyId);
		Long l3 = insertCategoryRow(companyId, l2, "爆品", 3, true, path2, 0L, now);
		if (l3 == null) {
			return null;
		}
		String path3 = path2 + "," + l3;
		jdbcTemplate.update("UPDATE items_category SET path = ? WHERE category_id = ? AND company_id = ?", path3, l3, companyId);
		return l3;
	}

	private Long insertCategoryRow(
			long companyId,
			long parentId,
			String name,
			int level,
			boolean main,
			String pathBeforeId,
			long sort,
			int now) {
		KeyHolder kh = new GeneratedKeyHolder();
		jdbcTemplate.update(
				con -> {
					PreparedStatement ps =
							con.prepareStatement(
									"INSERT INTO items_category (company_id, regionauth_id, category_name, category_code, parent_id, "
											+ "category_level, is_main_category, is_show_front, path, distributor_id, commission_ratio, sort, "
											+ "goods_params, goods_spec, image_url, customize_page_id, category_id_taobao, parent_id_taobao, created, updated) "
											+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
									Statement.RETURN_GENERATED_KEYS);
					ps.setLong(1, companyId);
					ps.setLong(2, 0L);
					ps.setString(3, name);
					ps.setString(4, "");
					ps.setLong(5, parentId);
					ps.setInt(6, level);
					ps.setBoolean(7, main);
					ps.setInt(8, 1);
					ps.setString(9, pathBeforeId != null ? pathBeforeId : "");
					ps.setLong(10, 0L);
					ps.setInt(11, 0);
					ps.setLong(12, sort);
					ps.setString(13, null);
					ps.setString(14, null);
					ps.setString(15, "");
					ps.setLong(16, 0L);
					ps.setLong(17, 0L);
					ps.setLong(18, 0L);
					ps.setInt(19, now);
					ps.setInt(20, now);
					return ps;
				},
				kh);
		Number key = kh.getKey();
		return key != null ? key.longValue() : null;
	}

	private void linkMainLeafToSpecAndParams(long companyId, long leafCategoryId) {
		try {
			Long specId =
					jdbcTemplate.query(
							"SELECT attribute_id FROM items_attributes WHERE company_id = ? AND attribute_type = 'item_spec' ORDER BY attribute_id ASC LIMIT 1",
							rs -> rs.next() ? rs.getLong(1) : null,
							companyId);
			Long paramsId =
					jdbcTemplate.query(
							"SELECT attribute_id FROM items_attributes WHERE company_id = ? AND attribute_type = 'item_params' ORDER BY attribute_id ASC LIMIT 1",
							rs -> rs.next() ? rs.getLong(1) : null,
							companyId);
			if (specId == null || paramsId == null) {
				return;
			}
			String specJson = "[" + specId + "]";
			String paramsJson = "[" + paramsId + "]";
			jdbcTemplate.update(
					"UPDATE items_category SET goods_spec = ?, goods_params = ?, updated = ? WHERE category_id = ? AND company_id = ?",
					specJson,
					paramsJson,
					(int) (System.currentTimeMillis() / 1000L),
					leafCategoryId,
					companyId);
		} catch (DataAccessException e) {
			log.warn(
					"CompanyInitDemoDataService link leaf category failed companyId={} msg={}",
					companyId,
					e.getMessage());
		}
	}

	private static String textOrEmpty(JsonNode n) {
		return n == null || n.isNull() ? "" : n.asText("");
	}

	private static String textOrNull(JsonNode n) {
		if (n == null || n.isNull()) {
			return null;
		}
		String t = n.asText();
		return StringUtils.hasText(t) ? t : null;
	}
}
