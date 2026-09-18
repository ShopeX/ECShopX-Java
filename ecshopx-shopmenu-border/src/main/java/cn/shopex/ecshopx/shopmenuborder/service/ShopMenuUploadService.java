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

package cn.shopex.ecshopx.shopmenuborder.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.shopmenuborder.config.ShopmenuAuditLogProperties;
import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import cn.shopex.ecshopx.superadmin.domain.ShopMenuRelType;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShopMenuUploadService {

	private static final int BORDER_COMPANY_ID = 0;

	private final ShopMenuService shopMenuService;
	private final ShopMenuMapper shopMenuMapper;
	private final ShopMenuRelTypeMapper shopMenuRelTypeMapper;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;
	private final ShopmenuAuditLogProperties auditLogProperties;
	private final ObjectMapper objectMapper;

	public ShopMenuUploadService(
			ShopMenuService shopMenuService,
			ShopMenuMapper shopMenuMapper,
			ShopMenuRelTypeMapper shopMenuRelTypeMapper,
			CommonLangModWriteService commonLangModWriteService,
			LangueProperties langueProperties,
			ShopmenuAuditLogProperties auditLogProperties,
			ObjectMapper objectMapper) {
		this.shopMenuService = shopMenuService;
		this.shopMenuMapper = shopMenuMapper;
		this.shopMenuRelTypeMapper = shopMenuRelTypeMapper;
		this.commonLangModWriteService = commonLangModWriteService;
		this.langueProperties = langueProperties;
		this.auditLogProperties = auditLogProperties;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void uploadMenus(List<JsonNode> rows) {
		try {
			doUploadMenus(rows);
		} catch (BadRequestException e) {
			throw e;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	private void doUploadMenus(List<JsonNode> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}

		for (JsonNode row : rows) {
			if (!row.isObject()) {
				throw new BadRequestException("菜单数据须为 JSON 对象数组");
			}
		}

		Set<Integer> versions = new LinkedHashSet<>();
		for (JsonNode row : rows) {
			JsonNode v = row.get("version");
			if (v != null && !v.isNull() && v.isNumber()) {
				versions.add(v.intValue());
			}
		}

		List<String> langList = langueProperties.getList();
		if (langList == null) {
			langList = List.of();
		}

		if (!versions.isEmpty()) {
			List<Long> shopMenuIdList = new ArrayList<>();
			int pageNum = 1;
			List<Integer> versionList = new ArrayList<>(versions);
			while (true) {
				Page<ShopMenu> page = new Page<>(pageNum, 5000);
				LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
				w.in(ShopMenu::getVersion, versionList).eq(ShopMenu::getCompanyId, BORDER_COMPANY_ID);
				Page<ShopMenu> result = shopMenuMapper.selectPage(page, w);
				List<ShopMenu> rec = result.getRecords();
				if (rec.isEmpty()) {
					break;
				}
				for (ShopMenu m : rec) {
					shopMenuIdList.add(m.getShopmenuId());
				}
				if (rec.size() < 5000) {
					break;
				}
				pageNum++;
			}

			for (int i = 0; i < shopMenuIdList.size(); i += 2000) {
				int end = Math.min(i + 2000, shopMenuIdList.size());
				List<Long> chunk = shopMenuIdList.subList(i, end);
				LambdaQueryWrapper<ShopMenuRelType> rw = new LambdaQueryWrapper<>();
				rw.in(ShopMenuRelType::getShopmenuId, chunk);
				shopMenuRelTypeMapper.delete(rw);
			}

			for (Long shopmenuId : shopMenuIdList) {
				for (String langue : langList) {
					commonLangModWriteService.deleteLang(
							BORDER_COMPANY_ID, "shop_menu", shopmenuId, "shop_menu", langue);
				}
			}

			LambdaQueryWrapper<ShopMenu> delMain = new LambdaQueryWrapper<>();
			delMain.in(ShopMenu::getVersion, versionList).eq(ShopMenu::getCompanyId, BORDER_COMPANY_ID);
			shopMenuMapper.delete(delMain);
		}

		Map<Long, Long> menuIdMapping = new LinkedHashMap<>();
		int now = (int) (System.currentTimeMillis() / 1000L);

		for (JsonNode row : rows) {
			long fileMenuId = row.path("shopmenu_id").asLong(0L);
			long newId = getMaxMenuId() + 1;

			long pidRaw = row.path("pid").asLong(0L);
			long pidNew = 0L;
			if (pidRaw != 0L) {
				Long mapped = menuIdMapping.get(pidRaw);
				pidNew = mapped != null ? mapped : 0L;
			}

			menuIdMapping.put(fileMenuId, newId);

			String rowName = textOrEmpty(row, "name");
			String aliasName = textOrEmpty(row, "alias_name");
			if (!StringUtils.hasText(aliasName)) {
				throw new ResourceException(rowName + "菜单唯一标识不能为空");
			}

			boolean isShow = jsonTruthy(row.get("is_show"));
			boolean isMenu = jsonTruthy(row.get("is_menu"));
			boolean disabled = jsonTruthy(row.get("disabled"));

			if (!isShow) {
				appendIsShowHiddenAuditLog(newId, row, rowName, aliasName);
			}

			ShopMenu entity = new ShopMenu();
			entity.setShopmenuId(newId);
			entity.setCompanyId(BORDER_COMPANY_ID);
			entity.setName(row.path("name").asText(null));
			entity.setUrl(row.path("url").asText(null));
			entity.setSort(row.path("sort").isNumber() ? row.get("sort").intValue() : null);
			entity.setPid(pidNew);
			entity.setApis(row.path("apis").asText(null));
			entity.setIcon(row.path("icon").asText(null));
			entity.setAliasName(aliasName);
			entity.setIsMenu(isMenu);
			entity.setIsShow(isShow);
			entity.setDisabled(disabled);
			entity.setVersion(row.path("version").isNumber() ? row.get("version").intValue() : 1);
			entity.setCreated(now);
			entity.setUpdated(now);

			int inserted = shopMenuMapper.insert(entity);
			if (inserted != 1) {
				throw new ResourceException("导入失败");
			}

			List<String> menuTypeNames = parseMenuTypeNames(row.get("menu_type"));
			List<Integer> typeIds = shopMenuService.menuTypeNamesToIds(menuTypeNames);
			for (Integer menuTypeId : typeIds) {
				ShopMenuRelType rel = new ShopMenuRelType();
				rel.setShopmenuId(newId);
				rel.setMenuType(menuTypeId);
				rel.setCompanyId(BORDER_COMPANY_ID);
				rel.setCreated(now);
				rel.setUpdated(now);
				shopMenuRelTypeMapper.insert(rel);
			}

			JsonNode nameLang = row.get("name_lang");
			if (nameLang != null && !nameLang.isNull()) {
				if (!nameLang.isObject()) {
					throw new BadRequestException("name_lang 须为 JSON 对象");
				}
				for (String langue : langList) {
					JsonNode valNode = nameLang.get(langue);
					if (valNode == null || valNode.isNull()) {
						continue;
					}
					String translated = valNode.asText("");
					if (!StringUtils.hasText(translated)) {
						continue;
					}
					Map<String, String> bag = new LinkedHashMap<>();
					bag.put("name", translated);
					commonLangModWriteService.saveLang(
							BORDER_COMPANY_ID, bag, "shop_menu", (int) newId, "shop_menu", langue);
				}
			}
		}
	}

	private List<String> parseMenuTypeNames(JsonNode mtNode) {
		if (mtNode == null || mtNode.isNull()) {
			return List.of();
		}
		if (!mtNode.isArray()) {
			throw new BadRequestException("menu_type 须为 JSON 字符串数组");
		}
		List<String> out = new ArrayList<>();
		for (JsonNode n : mtNode) {
			if (!n.isTextual()) {
				throw new BadRequestException("menu_type 须为 JSON 字符串数组");
			}
			out.add(n.asText());
		}
		return out;
	}

	private long getMaxMenuId() {
		LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
		w.orderByDesc(ShopMenu::getShopmenuId).last("LIMIT 1");
		ShopMenu one = shopMenuMapper.selectOne(w);
		if (one == null || one.getShopmenuId() == null) {
			return 0L;
		}
		return one.getShopmenuId();
	}

	private static boolean jsonTruthy(JsonNode n) {
		if (n == null || n.isNull()) {
			return false;
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isNumber()) {
			return n.intValue() != 0;
		}
		String t = n.asText("").trim().toLowerCase();
		return t.equals("true") || t.equals("1") || t.equals("yes");
	}

	private static String textOrEmpty(JsonNode row, String field) {
		JsonNode n = row.get(field);
		if (n == null || n.isNull()) {
			return "";
		}
		return n.asText("");
	}

	private void appendIsShowHiddenAuditLog(long newShopmenuId, JsonNode row, String name, String aliasName) {
		try {
			ObjectNode log = objectMapper.createObjectNode();
			log.put("timestamp", java.time.Instant.now().toString());
			log.put("action", "upload");
			log.put("shopmenu_id", newShopmenuId);
			log.put("company_id", BORDER_COMPANY_ID);
			JsonNode ver = row.get("version");
			log.put("version", ver != null && ver.isNumber() ? ver.intValue() : 1);
			log.put("alias_name", aliasName);
			log.put("name", name);
			log.put("new_value", false);
			log.set("request_data", row);

			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && StringUtils.hasText(auth.getName())) {
				log.put("user_id", auth.getName());
			}

			String line = objectMapper.writeValueAsString(log) + "\n";
			Path path = Path.of(auditLogProperties.getIsShowAuditLogPath());
			if (path.getParent() != null) {
				Files.createDirectories(path.getParent());
			}
			Files.writeString(
					path,
					line,
					StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);
		} catch (Exception ignored) {
		}
	}
}
