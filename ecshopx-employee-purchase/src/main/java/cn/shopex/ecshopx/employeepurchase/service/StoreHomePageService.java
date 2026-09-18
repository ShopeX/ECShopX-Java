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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.StoreHomePage;
import cn.shopex.ecshopx.employeepurchase.mapper.StoreHomePageMapper;
import cn.shopex.ecshopx.wechat.service.CustomizePageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StoreHomePageService {

	public static final String CUSTOMIZE_PAGE_TYPE = "enterprise_store_home";

	private static final SecureRandom RANDOM = new SecureRandom();

	private final StoreHomePageMapper storeHomePageMapper;
	private final CustomizePageService customizePageService;

	public StoreHomePageService(StoreHomePageMapper storeHomePageMapper, CustomizePageService customizePageService) {
		this.storeHomePageMapper = storeHomePageMapper;
		this.customizePageService = customizePageService;
	}

	public static String internalCustomizePageName(long companyId, int distributorId) {
		return "ep_store_home_" + companyId + "_" + distributorId;
	}

	public static String uniqueInternalCustomizePageName(long companyId, int distributorId) {
		byte[] bytes = new byte[4];
		RANDOM.nextBytes(bytes);
		StringBuilder hex = new StringBuilder(8);
		for (byte b : bytes) {
			hex.append(String.format("%02x", b));
		}
		return internalCustomizePageName(companyId, distributorId) + "_" + hex;
	}

	public Map<String, Object> getList(
			long companyId, int authDistributorId, int page, int pageSize, Integer filterDistributorId) {
		LambdaQueryWrapper<StoreHomePage> w = new LambdaQueryWrapper<>();
		w.eq(StoreHomePage::getCompanyId, companyId);
		if (authDistributorId > 0) {
			w.eq(StoreHomePage::getDistributorId, authDistributorId);
		} else if (filterDistributorId != null && filterDistributorId > 0) {
			w.eq(StoreHomePage::getDistributorId, filterDistributorId);
		}
		w.orderByDesc(StoreHomePage::getId);

		Page<StoreHomePage> pageResult = storeHomePageMapper.selectPage(new Page<>(page, pageSize), w);
		List<Map<String, Object>> list = pageResult.getRecords().stream().map(this::toRowMap).toList();

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", pageResult.getTotal());
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createRow(long companyId, int authDistributorId, Map<String, Object> params) {
		int distributorId = authDistributorId;
		String templateName = trimToEmpty(params.get("template_name"));
		if (!StringUtils.hasText(templateName)) {
			throw new ResourceException("模版名称不能为空");
		}
		String pageName = trimToEmpty(params.get("page_name"));
		String pageDescription = trimToEmpty(params.get("page_description"));
		if (!StringUtils.hasText(pageName) || !StringUtils.hasText(pageDescription)) {
			throw new ResourceException("页面名称与描述不能为空");
		}

		boolean isOpen = normalizeIsOpen(params.get("is_open"));

		LinkedHashMap<String, Object> cpParams = new LinkedHashMap<>();
		cpParams.put("template_name", templateName);
		cpParams.put("page_name", uniqueInternalCustomizePageName(companyId, distributorId));
		cpParams.put("page_description", pageDescription);
		cpParams.put("page_share_title", nullToEmpty(params.get("page_share_title")));
		cpParams.put("page_share_desc", nullToEmpty(params.get("page_share_desc")));
		cpParams.put("page_share_imageUrl", nullToEmpty(params.get("page_share_imageUrl")));
		cpParams.put("is_open", isOpen);
		cpParams.put("page_type", CUSTOMIZE_PAGE_TYPE);
		cpParams.put("regionauth_id", 0);

		CustomizePageService.CustomizePageCreateOutcome outcome = customizePageService.createCustomizePage(companyId, cpParams);
		if (outcome.duplicateMy()) {
			throw new ResourceException("创建装修页失败");
		}
		Map<String, Object> cp = outcome.successData();
		long customizeId = readLongId(cp == null ? null : cp.get("id"));
		if (customizeId <= 0L) {
			throw new ResourceException("创建装修页失败");
		}

		int now = (int) (System.currentTimeMillis() / 1000);
		StoreHomePage entity = new StoreHomePage();
		entity.setCompanyId(companyId);
		entity.setDistributorId(distributorId);
		entity.setTemplateName(templateName);
		entity.setPageName(pageName);
		entity.setPageDescription(pageDescription);
		entity.setPageShareTitle(nullToNull(params.get("page_share_title")));
		entity.setPageShareDesc(nullToNull(params.get("page_share_desc")));
		entity.setPageShareImageUrl(nullToNull(params.get("page_share_imageUrl")));
		entity.setIsOpen(isOpen ? 1 : 0);
		entity.setWeappCustomizePageId(customizeId);
		entity.setCreated(now);
		entity.setUpdated(now);
		storeHomePageMapper.insert(entity);

		return toRowMap(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateRow(long companyId, int authDistributorId, long id, Map<String, Object> params) {
		StoreHomePage row = requireRow(companyId, id);
		StoreHomePageAccess.assertRowMatchesDealer(row, authDistributorId);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		for (String k : List.of(
				"page_name", "page_description", "page_share_title", "page_share_desc", "page_share_imageUrl", "template_name")) {
			if (params.containsKey(k)) {
				data.put(k, params.get(k));
			}
		}
		if (params.containsKey("is_open")) {
			data.put("is_open", normalizeIsOpen(params.get("is_open")) ? 1 : 0);
		}

		if (data.containsKey("page_name") && !StringUtils.hasText(trimToEmpty(data.get("page_name")))) {
			throw new ResourceException("页面名称不能为空");
		}
		if (data.containsKey("page_description") && !StringUtils.hasText(trimToEmpty(data.get("page_description")))) {
			throw new ResourceException("页面描述不能为空");
		}

		applyUpdateFields(row, data);
		row.setUpdated((int) (System.currentTimeMillis() / 1000));
		storeHomePageMapper.updateById(row);

		long customizeId = row.getWeappCustomizePageId() == null ? 0L : row.getWeappCustomizePageId();
		if (customizeId > 0L) {
			LinkedHashMap<String, Object> cpUpdate = new LinkedHashMap<>();
			if (data.containsKey("template_name")) {
				cpUpdate.put("template_name", data.get("template_name"));
			}
			if (data.containsKey("page_description")) {
				cpUpdate.put("page_description", data.get("page_description"));
			}
			if (data.containsKey("page_share_title")) {
				cpUpdate.put("page_share_title", data.get("page_share_title"));
			}
			if (data.containsKey("page_share_desc")) {
				cpUpdate.put("page_share_desc", data.get("page_share_desc"));
			}
			if (data.containsKey("page_share_imageUrl")) {
				cpUpdate.put("page_share_imageUrl", data.get("page_share_imageUrl"));
			}
			if (params.containsKey("is_open")) {
				cpUpdate.put("is_open", normalizeIsOpen(params.get("is_open")));
			}
			if (!cpUpdate.isEmpty()) {
				customizePageService.updateCustomizePage(companyId, customizeId, null, cpUpdate);
			}
		}

		return toRowMap(row);
	}

	public Map<String, Object> getById(long companyId, int authDistributorId, long id) {
		StoreHomePage row = requireRow(companyId, id);
		StoreHomePageAccess.assertRowMatchesDealer(row, authDistributorId);
		return toRowMap(row);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> deleteRow(long companyId, int authDistributorId, long id) {
		StoreHomePage row = requireRow(companyId, id);
		StoreHomePageAccess.assertRowMatchesDealer(row, authDistributorId);

		long customizeId = row.getWeappCustomizePageId() == null ? 0L : row.getWeappCustomizePageId();
		if (customizeId > 0L) {
			customizePageService.deleteCustomizePage(companyId, customizeId);
		}
		storeHomePageMapper.deleteById(id);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}

	private StoreHomePage requireRow(long companyId, long id) {
		StoreHomePage row =
				storeHomePageMapper.selectOne(
						new LambdaQueryWrapper<StoreHomePage>()
								.eq(StoreHomePage::getId, id)
								.eq(StoreHomePage::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("未查询到数据");
		}
		return row;
	}

	private void applyUpdateFields(StoreHomePage row, Map<String, Object> data) {
		if (data.containsKey("page_name")) {
			row.setPageName(trimToEmpty(data.get("page_name")));
		}
		if (data.containsKey("page_description")) {
			row.setPageDescription(trimToEmpty(data.get("page_description")));
		}
		if (data.containsKey("page_share_title")) {
			row.setPageShareTitle(nullToNull(data.get("page_share_title")));
		}
		if (data.containsKey("page_share_desc")) {
			row.setPageShareDesc(nullToNull(data.get("page_share_desc")));
		}
		if (data.containsKey("page_share_imageUrl")) {
			row.setPageShareImageUrl(nullToNull(data.get("page_share_imageUrl")));
		}
		if (data.containsKey("template_name")) {
			row.setTemplateName(trimToEmpty(data.get("template_name")));
		}
		if (data.containsKey("is_open")) {
			row.setIsOpen((Integer) data.get("is_open"));
		}
	}

	private Map<String, Object> toRowMap(StoreHomePage row) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("company_id", row.getCompanyId());
		out.put("distributor_id", row.getDistributorId());
		out.put("template_name", row.getTemplateName());
		out.put("page_name", row.getPageName());
		out.put("page_description", row.getPageDescription());
		out.put("page_share_title", row.getPageShareTitle());
		out.put("page_share_desc", row.getPageShareDesc());
		out.put("page_share_imageUrl", row.getPageShareImageUrl());
		out.put("is_open", row.getIsOpen());
		out.put("weapp_customize_page_id", row.getWeappCustomizePageId());
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		return out;
	}

	private static boolean normalizeIsOpen(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		String s = raw.toString().trim();
		return !("false".equalsIgnoreCase(s) || "0".equals(s));
	}

	private static String trimToEmpty(Object raw) {
		return raw == null ? "" : raw.toString().trim();
	}

	private static String nullToEmpty(Object raw) {
		return raw == null ? "" : raw.toString();
	}

	private static String nullToNull(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.toString();
		return s.isEmpty() ? null : s;
	}

	private static long readLongId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
