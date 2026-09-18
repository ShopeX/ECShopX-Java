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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1CouponGetCouponListService {

	private final DiscountCardsMapper discountCardsMapper;

	public OpenapiThirdApiV1CouponGetCouponListService(DiscountCardsMapper discountCardsMapper) {
		this.discountCardsMapper = discountCardsMapper;
	}

	public Map<String, Object> executeOpenapiGetCouponList(
			long companyId,
			boolean applyDistributorFilter,
			String distributorId,
			boolean pageNoPresent,
			String pageNoRaw,
			boolean pageSizePresent,
			String pageSizeRaw) {
		LambdaQueryWrapper<DiscountCards> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(DiscountCards::getCompanyId, companyId);
		wrapper.select(
				DiscountCards::getCardId,
				DiscountCards::getTitle,
				DiscountCards::getDescription,
				DiscountCards::getDiscount,
				DiscountCards::getDistributorId,
				DiscountCards::getCardType);
		wrapper.orderByDesc(DiscountCards::getCreated);

		if (applyDistributorFilter) {
			String[] storeIds = distributorId.split(",");
			wrapper.and(w -> {
				w.eq(DiscountCards::getDistributorId, ",");
				for (String storeId : storeIds) {
					w.or().like(DiscountCards::getDistributorId, "%," + storeId + ",%");
				}
			});
		}

		if (pageNoPresent && pageSizePresent) {
			int pageSize = parseRawInt(pageSizeRaw);
			int pageNo = parseRawInt(pageNoRaw);
			int offset = (pageNo - 1) * pageSize;
			wrapper.last("LIMIT " + offset + "," + pageSize);
		}

		List<DiscountCards> entities;
		try {
			entities = discountCardsMapper.selectList(wrapper);
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		for (DiscountCards entity : entities) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("card_id", entity.getCardId());
			row.put("title", entity.getTitle() != null ? entity.getTitle() : "");
			row.put("description", entity.getDescription() != null ? entity.getDescription() : "");
			row.put("discount", entity.getDiscount() != null ? entity.getDiscount() : 0);
			row.put("distributor_id", entity.getDistributorId() != null ? entity.getDistributorId() : ",");
			row.put("card_type", entity.getCardType());
			rows.add(row);
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", rows);
		return data;
	}

	private static int parseRawInt(String raw) {
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new OpenapiLegacyZeroCodeFailException("A non-numeric value encountered");
		}
	}
}
