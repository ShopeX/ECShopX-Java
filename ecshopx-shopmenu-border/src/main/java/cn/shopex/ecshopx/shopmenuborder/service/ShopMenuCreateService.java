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

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.shopmenuborder.config.ShopmenuAuditLogProperties;
import cn.shopex.ecshopx.shopmenuborder.service.dto.BorderShopMenuCreateInput;
import cn.shopex.ecshopx.shopmenuborder.service.dto.ShopMenuCreateAuditContext;
import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import cn.shopex.ecshopx.superadmin.domain.ShopMenuRelType;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ShopMenuCreateService {

	private static final int BORDER_COMPANY_ID = 0;

	private final ShopMenuService shopMenuService;
	private final ShopMenuMapper shopMenuMapper;
	private final ShopMenuRelTypeMapper shopMenuRelTypeMapper;
	private final ShopmenuAuditLogProperties auditLogProperties;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;
	private final ShopMenuOutsideLangWriteService shopMenuOutsideLangWriteService;
	private final LangueProperties langueProperties;

	public ShopMenuCreateService(
			ShopMenuService shopMenuService,
			ShopMenuMapper shopMenuMapper,
			ShopMenuRelTypeMapper shopMenuRelTypeMapper,
			ShopmenuAuditLogProperties auditLogProperties,
			ObjectMapper objectMapper,
			PlatformTransactionManager transactionManager,
			ShopMenuOutsideLangWriteService shopMenuOutsideLangWriteService,
			LangueProperties langueProperties) {
		this.shopMenuService = shopMenuService;
		this.shopMenuMapper = shopMenuMapper;
		this.shopMenuRelTypeMapper = shopMenuRelTypeMapper;
		this.auditLogProperties = auditLogProperties;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.shopMenuOutsideLangWriteService = shopMenuOutsideLangWriteService;
		this.langueProperties = langueProperties;
	}

	public ShopMenu createFromBorderRequest(BorderShopMenuCreateInput input, ShopMenuCreateAuditContext ctx) {
		int companyId = input.companyId() != null ? input.companyId() : BORDER_COMPANY_ID;
		int sortVal = input.sort() != null ? input.sort() : 1;
		int versionVal = input.version() != null ? input.version() : 1;
		long pidVal = input.pid() != null ? input.pid() : 0L;

		if (!StringUtils.hasText(input.aliasName())) {
			throw new ResourceException("菜单唯一标识不能为空");
		}

		List<String> menuTypeNames = input.menuTypeNames() != null ? input.menuTypeNames() : List.of();
		List<Integer> menuTypeList = shopMenuService.menuTypeNamesToIds(menuTypeNames);

		long shopmenuId;
		if (input.shopmenuId() != null) {
			shopmenuId = input.shopmenuId();
		} else {
			shopmenuId = nextShopmenuIdForCompany(companyId);
		}

		shopMenuService.assertAliasNameNotConflict(
				input.aliasName(), versionVal, companyId, null, menuTypeNames);

		if (pidVal != 0L) {
			LambdaQueryWrapper<ShopMenu> parentW = new LambdaQueryWrapper<>();
			parentW.eq(ShopMenu::getShopmenuId, pidVal).eq(ShopMenu::getCompanyId, companyId);
			ShopMenu parent = shopMenuMapper.selectOne(parentW);
			if (parent == null) {
				throw new ResourceException("上级菜单不存在");
			}
			if (!Boolean.TRUE.equals(parent.getIsMenu())) {
				throw new ResourceException("功能菜单下不能有子菜单");
			}
			LambdaQueryWrapper<ShopMenuRelType> pRelW = new LambdaQueryWrapper<>();
			pRelW.eq(ShopMenuRelType::getShopmenuId, pidVal).eq(ShopMenuRelType::getCompanyId, companyId);
			List<ShopMenuRelType> parentRels = shopMenuRelTypeMapper.selectList(pRelW);
			List<Integer> parentTypeList =
					parentRels.stream().map(ShopMenuRelType::getMenuType).collect(Collectors.toList());
			shopMenuService.checkParentMenuType(parentTypeList, menuTypeList);
		}

		if (shouldAppendIsShowHiddenAudit(ctx.requestData())) {
			appendIsShowHiddenAuditLogForCreate(ctx, shopmenuId, companyId, versionVal, input);
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		ShopMenu entity = buildEntityForInsert(
				input, shopmenuId, companyId, sortVal, versionVal, pidVal, now);

		return transactionTemplate.execute(status -> {
			int inserted = shopMenuMapper.insert(entity);
			if (inserted != 1) {
				throw new ResourceException("创建菜单失败");
			}
			for (Integer menuTypeId : menuTypeList) {
				ShopMenuRelType rel = new ShopMenuRelType();
				rel.setShopmenuId(shopmenuId);
				rel.setMenuType(menuTypeId);
				rel.setCompanyId(companyId);
				rel.setCreated(now);
				rel.setUpdated(now);
				shopMenuRelTypeMapper.insert(rel);
			}
			if (input.hasName()) {
				shopMenuOutsideLangWriteService.upsertName(
						companyId, shopmenuId, input.name(), input.requestLang());
			}
			return entity;
		});
	}

	private long nextShopmenuIdForCompany(int companyId) {
		LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
		w.eq(ShopMenu::getCompanyId, companyId).orderByDesc(ShopMenu::getShopmenuId).last("LIMIT 1");
		ShopMenu one = shopMenuMapper.selectOne(w);
		if (one == null || one.getShopmenuId() == null) {
			return 1L;
		}
		return one.getShopmenuId() + 1;
	}

	private ShopMenu buildEntityForInsert(
			BorderShopMenuCreateInput input,
			long shopmenuId,
			int companyId,
			int sortVal,
			int versionVal,
			long pidVal,
			int now) {
		ShopMenu entity = new ShopMenu();
		entity.setShopmenuId(shopmenuId);
		entity.setCompanyId(companyId);
		entity.setSort(sortVal);
		entity.setVersion(versionVal);
		entity.setPid(pidVal);
		entity.setAliasName(input.aliasName());
		entity.setCreated(now);
		entity.setUpdated(now);

		// 默认语种写主表 name；非默认语种仅写多语言表（对齐 PHP createLangue）
		if (input.hasName()
				&& StringUtils.hasText(input.name())
				&& langueProperties.isDefaultLang(input.requestLang())) {
			entity.setName(input.name());
		}
		if (input.hasUrl() && StringUtils.hasText(input.url())) {
			entity.setUrl(input.url());
		}
		if (input.hasApis()) {
			entity.setApis(input.apis());
		}
		if (input.hasIcon() && StringUtils.hasText(input.icon())) {
			entity.setIcon(input.icon());
		}

		entity.setIsMenu(computeIsMenuDb(input.hasIsMenu(), input.isMenuRaw()));
		entity.setIsShow(computeIsShowDb(input.hasIsShow(), input.isShowRaw()));
		entity.setDisabled(computeDisabledDb(input.hasDisabled(), input.disabledRaw()));
		return entity;
	}

	/**
	 * 请求未传 {@code is_menu} 时默认为菜单项；传入时仅当值为“真”（布尔 true、非零数值、或忽略大小写的
	 * {@code true}/{@code 1}/{@code yes}）时为菜单项，其余为功能项。
	 */
	private static boolean computeIsMenuDb(boolean hasKey, Object raw) {
		if (!hasKey) {
			return true;
		}
		return isTruthyLikeUpload(raw);
	}

	/**
	 * 请求未传 {@code is_show} 时默认为展示；传入时仅当值为“真”（布尔 true、非零数值、或忽略大小写的
	 * {@code true}/{@code 1}/{@code yes}）时为展示，否则为隐藏。
	 */
	private static boolean computeIsShowDb(boolean hasKey, Object raw) {
		if (!hasKey) {
			return true;
		}
		return isTruthyLikeUpload(raw);
	}

	/**
	 * 请求未传 {@code disabled} 时默认为未禁用；传入时仅当值的字符串形式（去首尾空白）等于 {@code true} 时为禁用。
	 */
	private static boolean computeDisabledDb(boolean hasKey, Object raw) {
		if (!hasKey) {
			return false;
		}
		return "true".equals(trimToString(raw));
	}

	private static String trimToString(Object o) {
		if (o == null) {
			return "";
		}
		return o.toString().trim();
	}

	/**
	 * 在请求体包含 {@code is_show} 且其值按展示语义为假时写入审计（与落库的展示标志一致）。
	 */
	private boolean shouldAppendIsShowHiddenAudit(java.util.Map<String, Object> requestData) {
		if (requestData == null || !requestData.containsKey("is_show")) {
			return false;
		}
		return !isTruthyLikeUpload(requestData.get("is_show"));
	}

	private static boolean isTruthyLikeUpload(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.intValue() != 0;
		}
		String t = o.toString().trim().toLowerCase();
		return t.equals("true") || t.equals("1") || t.equals("yes");
	}

	private void appendIsShowHiddenAuditLogForCreate(
			ShopMenuCreateAuditContext ctx,
			long shopmenuId,
			int companyId,
			int versionVal,
			BorderShopMenuCreateInput input) {
		try {
			ObjectNode log = objectMapper.createObjectNode();
			log.put("timestamp", java.time.Instant.now().toString());
			log.put("action", "create");
			log.put("shopmenu_id", shopmenuId);
			log.put("company_id", companyId);
			log.put("version", versionVal);
			log.put("alias_name", input.aliasName() != null ? input.aliasName() : "");
			log.put("name", input.hasName() && input.name() != null ? input.name() : "");
			log.put("new_value", false);
			if (ctx.requestData() != null) {
				log.set("request_data", objectMapper.valueToTree(ctx.requestData()));
			}
			if (StringUtils.hasText(ctx.userId())) {
				log.put("user_id", ctx.userId());
			}
			if (StringUtils.hasText(ctx.clientIp())) {
				log.put("ip", ctx.clientIp());
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
