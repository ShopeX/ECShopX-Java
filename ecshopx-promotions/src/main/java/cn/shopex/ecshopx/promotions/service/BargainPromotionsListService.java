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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.BargainPromotionMultiLangReadService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class BargainPromotionsListService {

	private final BargainPromotionsMapper bargainPromotionsMapper;
	private final BargainPromotionCreateService bargainPromotionCreateService;
	private final BargainPromotionMultiLangReadService bargainPromotionMultiLangReadService;
	private final MessageSource messageSource;

	public BargainPromotionsListService(
			BargainPromotionsMapper bargainPromotionsMapper,
			BargainPromotionCreateService bargainPromotionCreateService,
			BargainPromotionMultiLangReadService bargainPromotionMultiLangReadService,
			MessageSource messageSource) {
		this.bargainPromotionsMapper = bargainPromotionsMapper;
		this.bargainPromotionCreateService = bargainPromotionCreateService;
		this.bargainPromotionMultiLangReadService = bargainPromotionMultiLangReadService;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getBargainList(long companyId, int page, int pageSize, String requestLangTag) {
		long nowEpochSeconds = System.currentTimeMillis() / 1000L;
		long offset = (long) (page - 1) * (long) pageSize;
		int total = bargainPromotionsMapper.countBargainListActiveForFront(companyId, nowEpochSeconds);
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("total_count", total);
		res.put("list", Collections.emptyList());
		if (total > 0) {
			List<BargainPromotions> entities =
					bargainPromotionsMapper.selectBargainListPageActiveForFront(
							companyId, nowEpochSeconds, offset, pageSize);
			List<Map<String, Object>> rows = new ArrayList<>();
			for (BargainPromotions e : entities) {
				rows.add(bargainPromotionCreateService.toBargainListRow(e, nowEpochSeconds));
			}
			if (!rows.isEmpty()) {
				bargainPromotionMultiLangReadService.applyTitleAndAdPic(rows, requestLangTag);
			}
			res.put("list", rows);
		}
		return res;
	}

	public Map<String, Object> getBargainList(
			long companyId,
			String itemNameContains,
			String titleContains,
			int page,
			int pageSize,
			String requestLangTag) {
		long offset = (long) (page - 1) * (long) pageSize;
		int total = bargainPromotionsMapper.countBargainList(companyId, itemNameContains, titleContains);
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("total_count", total);
		res.put("list", Collections.emptyList());
		if (total > 0) {
			List<BargainPromotions> entities =
					bargainPromotionsMapper.selectBargainListPage(
							companyId, itemNameContains, titleContains, offset, pageSize);
			long now = System.currentTimeMillis() / 1000L;
			List<Map<String, Object>> rows = new ArrayList<>();
			for (BargainPromotions e : entities) {
				rows.add(bargainPromotionCreateService.toBargainListRow(e, now));
			}
			if (!rows.isEmpty()) {
				bargainPromotionMultiLangReadService.applyTitleAndAdPic(rows, requestLangTag);
			}
			res.put("list", rows);
		}
		return res;
	}

	public Map<String, Object> getBargainDetail(long companyId, long bargainId, String acceptLanguage) {
		BargainPromotions entity = bargainPromotionsMapper.selectById(bargainId);
		if (entity == null
				|| entity.getCompanyId() == null
				|| !Objects.equals(entity.getCompanyId(), companyId)) {
			Locale locale =
					Optional.ofNullable(LocaleContextHolder.getLocale())
							.orElse(Locale.SIMPLIFIED_CHINESE);
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.can_only_get_your_bargain_detail", null, locale));
		}
		long now = System.currentTimeMillis() / 1000L;
		Map<String, Object> row = bargainPromotionCreateService.toBargainListRow(entity, now);
		List<Map<String, Object>> one = List.of(row);
		bargainPromotionMultiLangReadService.applyTitleAndAdPic(one, acceptLanguage);
		return row;
	}
}
