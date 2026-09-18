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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardEasyListService {

	private static final int MAX_PAGE_SIZE = 500;
	private static final DateTimeFormatter CREATED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final DiscountCardsMapper discountCardsMapper;
	private final DiscountCardsMultiLangReadService multiLangReadService;

	public DiscountCardEasyListService(DiscountCardsMapper discountCardsMapper,
			DiscountCardsMultiLangReadService multiLangReadService) {
		this.discountCardsMapper = discountCardsMapper;
		this.multiLangReadService = multiLangReadService;
	}

	/**
	 * 简易列表：未带有效 {@code card_type} 条件时返回该公司下全部券类型（在分页范围内）。
	 */
	public List<Map<String, Object>> list(long companyId, String cardTypeRaw, int page, int pageSize) {
		int effPage = Math.max(1, page);
		int effSize = pageSize <= 0 ? 30 : Math.min(pageSize, MAX_PAGE_SIZE);

		LambdaQueryWrapper<DiscountCards> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(DiscountCards::getCompanyId, companyId);
		String cardType = DiscountCardParamNormalize.stringVal(cardTypeRaw);
		if (StringUtils.hasText(cardType)) {
			wrapper.eq(DiscountCards::getCardType, cardType);
		}
		wrapper.select(
				DiscountCards::getCardId,
				DiscountCards::getTitle,
				DiscountCards::getCardType,
				DiscountCards::getCreated,
				DiscountCards::getEndDate);
		wrapper.orderByAsc(DiscountCards::getEndDate).orderByDesc(DiscountCards::getCardId);

		Page<DiscountCards> mpPage = new Page<>(effPage, effSize);
		Page<DiscountCards> pageOut = discountCardsMapper.selectPage(mpPage, wrapper);
		List<DiscountCards> records = pageOut.getRecords();
		if (records.isEmpty()) {
			return List.of();
		}

		ZoneId zone = ZoneId.systemDefault();
		List<Map<String, Object>> out = new ArrayList<>(records.size());
		for (DiscountCards entity : records) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("card_id", entity.getCardId());
			row.put("title", entity.getTitle());
			row.put("card_type", entity.getCardType());
			Integer created = entity.getCreated();
			if (created != null && created > 0) {
				row.put(
						"created_date",
						CREATED_FMT.format(LocalDateTime.ofInstant(Instant.ofEpochSecond(created.longValue()), zone)));
			}
			Long cardId = entity.getCardId();
			if (cardId != null) {
				multiLangReadService.overlay(companyId, cardId, row);
			}
			out.add(row);
		}
		return out;
	}
}
