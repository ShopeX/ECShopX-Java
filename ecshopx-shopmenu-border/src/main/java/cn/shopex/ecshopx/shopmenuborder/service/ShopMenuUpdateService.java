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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.shopmenuborder.config.ShopmenuAuditLogProperties;
import cn.shopex.ecshopx.shopmenuborder.service.dto.BorderShopMenuUpdateInput;
import cn.shopex.ecshopx.shopmenuborder.service.dto.ShopMenuCreateAuditContext;
import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import cn.shopex.ecshopx.superadmin.domain.ShopMenuRelType;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ShopMenuUpdateService {

	private static final int BORDER_COMPANY_ID = 0;

	private final ShopMenuService shopMenuService;
	private final ShopMenuMapper shopMenuMapper;
	private final ShopMenuRelTypeMapper shopMenuRelTypeMapper;
	private final ShopmenuAuditLogProperties auditLogProperties;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;
	private final ShopMenuOutsideLangWriteService shopMenuOutsideLangWriteService;
	private final LangueProperties langueProperties;

	public ShopMenuUpdateService(
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

	public ShopMenu updateFromBorderRequest(BorderShopMenuUpdateInput input, ShopMenuCreateAuditContext ctx) {
		long shopmenuId = input.shopmenuId();
		if (shopmenuId <= 0L) {
			throw new BadRequestException("未传菜单ID", 400);
		}

		List<Integer> menuTypeListForRel = null;
		ShopMenu currentRow = null;

		if (StringUtils.hasText(input.aliasName())) {
			LambdaQueryWrapper<ShopMenu> curW = new LambdaQueryWrapper<>();
			curW.eq(ShopMenu::getShopmenuId, shopmenuId).eq(ShopMenu::getCompanyId, BORDER_COMPANY_ID);
			currentRow = shopMenuMapper.selectOne(curW);

			int versionForDup;
			if (input.hasVersion() && input.version() != null) {
				versionForDup = input.version();
			} else if (currentRow != null && currentRow.getVersion() != null) {
				versionForDup = currentRow.getVersion();
			} else {
				versionForDup = 1;
			}

			shopMenuService.assertAliasNameNotConflict(
					input.aliasName(),
					versionForDup,
					BORDER_COMPANY_ID,
					shopmenuId,
					input.menuTypeNames());

			if (currentRow == null) {
				throw new ResourceException("无此菜单可更改");
			}

			List<Integer> menuTypeList = shopMenuService.menuTypeNamesToIds(input.menuTypeNames());
			menuTypeListForRel = menuTypeList;

			long pidVal = currentRow.getPid() != null ? currentRow.getPid() : 0L;
			if (pidVal != 0L) {
				LambdaQueryWrapper<ShopMenuRelType> pRelW = new LambdaQueryWrapper<>();
				pRelW.eq(ShopMenuRelType::getShopmenuId, pidVal).eq(ShopMenuRelType::getCompanyId, BORDER_COMPANY_ID);
				List<ShopMenuRelType> parentRels = shopMenuRelTypeMapper.selectList(pRelW);
				List<Integer> parentTypeList =
						parentRels.stream().map(ShopMenuRelType::getMenuType).collect(Collectors.toList());
				shopMenuService.checkParentMenuType(parentTypeList, menuTypeList);
			}

			LambdaQueryWrapper<ShopMenu> childW = new LambdaQueryWrapper<>();
			childW.eq(ShopMenu::getPid, shopmenuId).eq(ShopMenu::getCompanyId, BORDER_COMPANY_ID);
			List<ShopMenu> children = shopMenuMapper.selectList(childW);
			if (!children.isEmpty()) {
				List<Integer> sonTypeList = new ArrayList<>();
				for (ShopMenu child : children) {
					LambdaQueryWrapper<ShopMenuRelType> cRelW = new LambdaQueryWrapper<>();
					cRelW.eq(ShopMenuRelType::getShopmenuId, child.getShopmenuId())
							.eq(ShopMenuRelType::getCompanyId, BORDER_COMPANY_ID);
					List<ShopMenuRelType> childRels = shopMenuRelTypeMapper.selectList(cRelW);
					for (ShopMenuRelType r : childRels) {
						sonTypeList.add(r.getMenuType());
					}
				}
				shopMenuService.checkParentMenuType(menuTypeList, sonTypeList);
			}
		}

		ShopMenu oldRowForAudit;
		if (StringUtils.hasText(input.aliasName())) {
			oldRowForAudit = currentRow;
		} else {
			LambdaQueryWrapper<ShopMenu> ow = new LambdaQueryWrapper<>();
			ow.eq(ShopMenu::getShopmenuId, shopmenuId).eq(ShopMenu::getCompanyId, BORDER_COMPANY_ID);
			oldRowForAudit = shopMenuMapper.selectOne(ow);
		}

		if (input.hasIsShow()
				&& oldRowForAudit != null
				&& Boolean.TRUE.equals(oldRowForAudit.getIsShow())
				&& !isTruthyLikeUpload(input.isShowRaw())) {
			appendIsShowHiddenAuditLogForUpdate(ctx, oldRowForAudit);
		}

		final List<Integer> relRebuild = menuTypeListForRel;
		return transactionTemplate.execute(status -> {
			LambdaQueryWrapper<ShopMenu> loadW = new LambdaQueryWrapper<>();
			loadW.eq(ShopMenu::getShopmenuId, shopmenuId).eq(ShopMenu::getCompanyId, BORDER_COMPANY_ID);
			ShopMenu existing = shopMenuMapper.selectOne(loadW);
			if (existing == null) {
				throw new ResourceException("未查询到更新数据");
			}

			int now = (int) (System.currentTimeMillis() / 1000L);
			LambdaUpdateWrapper<ShopMenu> uw = new LambdaUpdateWrapper<>();
			uw.eq(ShopMenu::getShopmenuId, shopmenuId).eq(ShopMenu::getCompanyId, BORDER_COMPANY_ID);
			uw.set(ShopMenu::getUpdated, now);

			// 默认语种写主表 name；非默认语种仅写多语言表（对齐 PHP RepositoryLangInterceptor）
			if (input.hasName() && langueProperties.isDefaultLang(input.requestLang())) {
				uw.set(ShopMenu::getName, input.name());
			}
			if (input.hasUrl()) {
				uw.set(ShopMenu::getUrl, input.url());
			}
			if (input.hasSort()) {
				uw.set(ShopMenu::getSort, input.sort());
			}
			if (input.hasPid()) {
				uw.set(ShopMenu::getPid, input.pid() != null ? input.pid() : 0L);
			}
			if (input.hasAliasName() && StringUtils.hasText(input.aliasName())) {
				uw.set(ShopMenu::getAliasName, input.aliasName());
			}
			if (input.hasVersion()) {
				uw.set(ShopMenu::getVersion, input.version());
			}
			if (input.hasApis()) {
				uw.set(ShopMenu::getApis, input.apis());
			}
			if (input.hasIcon()) {
				uw.set(ShopMenu::getIcon, input.icon());
			}
			if (input.hasIsShow()) {
				uw.set(ShopMenu::getIsShow, computeIsShowDb(true, input.isShowRaw()));
			}
			if (input.hasIsMenu()) {
				uw.set(ShopMenu::getIsMenu, computeIsMenuDb(true, input.isMenuRaw()));
			}
			if (input.hasDisabled()) {
				uw.set(ShopMenu::getDisabled, computeDisabledDb(true, input.disabledRaw()));
			}

			shopMenuMapper.update(null, uw);

			if (StringUtils.hasText(input.aliasName()) && relRebuild != null) {
				LambdaQueryWrapper<ShopMenuRelType> delW = new LambdaQueryWrapper<>();
				delW.eq(ShopMenuRelType::getShopmenuId, shopmenuId)
						.eq(ShopMenuRelType::getCompanyId, BORDER_COMPANY_ID);
				shopMenuRelTypeMapper.delete(delW);
				for (Integer menuTypeId : relRebuild) {
					ShopMenuRelType rel = new ShopMenuRelType();
					rel.setShopmenuId(shopmenuId);
					rel.setMenuType(menuTypeId);
					rel.setCompanyId(BORDER_COMPANY_ID);
					rel.setCreated(now);
					rel.setUpdated(now);
					shopMenuRelTypeMapper.insert(rel);
				}
			}

			if (input.hasName()) {
				shopMenuOutsideLangWriteService.upsertName(
						BORDER_COMPANY_ID, shopmenuId, input.name(), input.requestLang());
			}

			return shopMenuMapper.selectOne(loadW);
		});
	}

	private void appendIsShowHiddenAuditLogForUpdate(ShopMenuCreateAuditContext ctx, ShopMenu oldRow) {
		try {
			ObjectNode log = objectMapper.createObjectNode();
			log.put("timestamp", java.time.Instant.now().toString());
			log.put("action", "update");
			log.put("shopmenu_id", oldRow.getShopmenuId());
			log.put("company_id", oldRow.getCompanyId() != null ? oldRow.getCompanyId() : BORDER_COMPANY_ID);
			log.put("version", oldRow.getVersion() != null ? oldRow.getVersion() : 0);
			log.put("alias_name", oldRow.getAliasName() != null ? oldRow.getAliasName() : "");
			log.put("name", oldRow.getName() != null ? oldRow.getName() : "");
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

	private static boolean computeIsMenuDb(boolean hasKey, Object raw) {
		if (!hasKey) {
			return true;
		}
		return isTruthyLikeUpload(raw);
	}

	private static boolean computeIsShowDb(boolean hasKey, Object raw) {
		if (!hasKey) {
			return true;
		}
		return isTruthyLikeUpload(raw);
	}

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
}
