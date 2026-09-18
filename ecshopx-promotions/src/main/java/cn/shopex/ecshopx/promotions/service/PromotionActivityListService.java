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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.domain.PromotionActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionActivityMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionActivityListService {

	private final PromotionActivityMapper promotionActivityMapper;
	private final PromotionActivityCreateService promotionActivityCreateService;
	private final PromotionActivityMultiLangReadService promotionActivityMultiLangReadService;
	private final MessageSource messageSource;

	public PromotionActivityListService(
			PromotionActivityMapper promotionActivityMapper,
			PromotionActivityCreateService promotionActivityCreateService,
			PromotionActivityMultiLangReadService promotionActivityMultiLangReadService,
			MessageSource messageSource) {
		this.promotionActivityMapper = promotionActivityMapper;
		this.promotionActivityCreateService = promotionActivityCreateService;
		this.promotionActivityMultiLangReadService = promotionActivityMultiLangReadService;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getActivityList(
			long companyId,
			String activityStatusRaw,
			String pageRaw,
			String pageSizeRaw,
			String countryCodeRaw,
			Locale locale) {
		boolean validTab =
				Objects.equals("valid", activityStatusRaw == null ? null : activityStatusRaw.trim());
		LambdaQueryWrapper<PromotionActivity> w = new LambdaQueryWrapper<>();
		if (validTab) {
			w.eq(PromotionActivity::getCompanyId, companyId)
					.eq(PromotionActivity::getActivityStatus, "valid")
					.gt(PromotionActivity::getEndTime, Instant.now().getEpochSecond());
		} else {
			w.eq(PromotionActivity::getCompanyId, companyId)
					.eq(PromotionActivity::getActivityStatus, "invalid");
		}
		w.orderByDesc(PromotionActivity::getCreated);

		int page = parsePageOrDefault(pageRaw, locale);
		int pageSize = parsePageSizeOrDefault(pageSizeRaw, locale);

		Page<PromotionActivity> p = new Page<>(page, pageSize);
		promotionActivityMapper.selectPage(p, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (PromotionActivity e : p.getRecords()) {
			list.add(promotionActivityCreateService.toActivityListRow(e));
		}

		String countryTrimmed = countryCodeRaw == null ? "" : countryCodeRaw.trim();
		String langTag = StringUtils.hasText(countryTrimmed) ? countryTrimmed : "zh-CN";
		promotionActivityMultiLangReadService.applyTitles(list, langTag);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", p.getTotal());
		out.put("list", list);
		return out;
	}

	private int parsePageOrDefault(String pageRaw, Locale locale) {
		if (pageRaw == null || pageRaw.trim().isEmpty()) {
			return 1;
		}
		return parseStrictPositiveInt(pageRaw.trim(), locale);
	}

	private int parsePageSizeOrDefault(String pageSizeRaw, Locale locale) {
		if (pageSizeRaw == null || pageSizeRaw.trim().isEmpty()) {
			return 10;
		}
		return parseStrictPositiveInt(pageSizeRaw.trim(), locale);
	}

	private int parseStrictPositiveInt(String s, Locale locale) {
		try {
			long v = Long.parseLong(s);
			if (v < 1L || v > Integer.MAX_VALUE) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.active_article.pagination_invalid", null, locale));
			}
			return (int) v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.pagination_invalid", null, locale));
		}
	}
}
