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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.RegisterPromotions;
import cn.shopex.ecshopx.promotions.mapper.RegisterPromotionsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.RegisterPromotionMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.support.RegisterPromotionApiRowBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegisterPromotionsConfigReadService {

	private final RegisterPromotionsMapper registerPromotionsMapper;
	private final RegisterPromotionMultiLangReadService registerPromotionMultiLangReadService;
	private final RegisterPromotionApiRowBuilder registerPromotionApiRowBuilder;

	public RegisterPromotionsConfigReadService(
			RegisterPromotionsMapper registerPromotionsMapper,
			RegisterPromotionMultiLangReadService registerPromotionMultiLangReadService,
			RegisterPromotionApiRowBuilder registerPromotionApiRowBuilder) {
		this.registerPromotionsMapper = registerPromotionsMapper;
		this.registerPromotionMultiLangReadService = registerPromotionMultiLangReadService;
		this.registerPromotionApiRowBuilder = registerPromotionApiRowBuilder;
	}

	public Object getRegisterPromotionsConfig(long companyId, String registerType, String requestLangTag) {
		if ("all".equals(registerType)) {
			return getAllBranch(companyId, requestLangTag);
		}
		return getSingleTypeBranch(companyId, registerType, requestLangTag);
	}

	private Object getAllBranch(long companyId, String requestLangTag) {
		LambdaQueryWrapper<RegisterPromotions> countWrapper =
				new LambdaQueryWrapper<RegisterPromotions>()
						.eq(RegisterPromotions::getCompanyId, companyId)
						.orderByDesc(RegisterPromotions::getId);
		long total = registerPromotionsMapper.selectCount(countWrapper);
		if (total == 0L) {
			return new ArrayList<Object>();
		}
		List<RegisterPromotions> rows =
				registerPromotionsMapper.selectList(
						new LambdaQueryWrapper<RegisterPromotions>()
								.eq(RegisterPromotions::getCompanyId, companyId)
								.orderByDesc(RegisterPromotions::getId)
								.last("LIMIT 100"));
		LinkedHashMap<String, Map<String, Object>> tmpByType = new LinkedHashMap<>();
		for (RegisterPromotions row : rows) {
			String rt = row.getRegisterType();
			if (rt != null) {
				tmpByType.put(rt, registerPromotionApiRowBuilder.toRowMap(row));
			}
		}
		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		if (tmpByType.containsKey("general")) {
			info.put("general", tmpByType.get("general"));
		}
		if (tmpByType.containsKey("membercard")) {
			info.put("membercard", tmpByType.get("membercard"));
		}
		if (!info.containsKey("general")) {
			info.put("general", placeholderRowMap());
		}
		if (!info.containsKey("membercard")) {
			info.put("membercard", placeholderRowMap());
		}
		List<Map<String, Object>> toEnrich = new ArrayList<>(2);
		appendIfRealDbPromotionRow(info.get("general"), toEnrich);
		appendIfRealDbPromotionRow(info.get("membercard"), toEnrich);
		if (!toEnrich.isEmpty()) {
			registerPromotionMultiLangReadService.applyAdPicAndTitle(companyId, toEnrich, requestLangTag, false);
		}
		return info;
	}

	private static void appendIfRealDbPromotionRow(Object entry, List<Map<String, Object>> out) {
		if (!(entry instanceof Map)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> row = (Map<String, Object>) entry;
		Object idObj = row.get("id");
		if (idObj instanceof Number n && n.longValue() > 0L) {
			out.add(row);
		}
	}

	private Object getSingleTypeBranch(long companyId, String registerType, String requestLangTag) {
		String effective = StringUtils.hasText(registerType) ? registerType.trim() : "general";
		List<RegisterPromotions> picked =
				registerPromotionsMapper.selectList(
						new LambdaQueryWrapper<RegisterPromotions>()
								.eq(RegisterPromotions::getCompanyId, companyId)
								.eq(RegisterPromotions::getRegisterType, effective)
								.orderByAsc(RegisterPromotions::getId)
								.last("LIMIT 1"));
		if (picked == null || picked.isEmpty()) {
			return new ArrayList<Object>();
		}
		RegisterPromotions one = picked.get(0);
		Map<String, Object> rowMap = registerPromotionApiRowBuilder.toRowMap(one);
		if (!"distributor".equals(effective)) {
			registerPromotionMultiLangReadService.applyAdPicAndTitle(companyId, List.of(rowMap), requestLangTag, true);
		}
		return rowMap;
	}

	private Map<String, Object> placeholderRowMap() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("ad_pic", "");
		m.put("ad_title", "");
		m.put("company_id", "");
		m.put("id", "");
		m.put("is_open", "false");
		m.put("promotions_value", Collections.emptyList());
		m.put("register_type", "");
		return m;
	}
}
