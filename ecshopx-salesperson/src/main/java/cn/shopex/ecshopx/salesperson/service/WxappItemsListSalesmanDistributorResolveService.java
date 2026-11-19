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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappItemsListSalesmanDistributorResolveService {

	private final ShopSalespersonMapper shopSalespersonMapper;

	public WxappItemsListSalesmanDistributorResolveService(ShopSalespersonMapper shopSalespersonMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
	}

	/**
	 * 店铺推广分类：业务员场景下无显式店铺维度时跳过平台 JWT 分销商合并；无可用分销商时不中断接口。
	 */
	public static record PromoterCategoryDistributorResolution(
			boolean skipPlatformJwtDistributorMerge,
			boolean hasExplicitDistributorFilter,
			Long singleDistributorId,
			List<Long> distributorIdIn) {
	}

	public PromoterCategoryDistributorResolution resolvePromoterCategoryDistributorFilter(
			long companyId, long userId, String distributorIdRaw) {
		Long single = parseSinglePositiveDistributorId(distributorIdRaw);
		return new PromoterCategoryDistributorResolution(false, single != null, single, null);
	}

	/**
	 * 小店上架类目：当前登录会员在门店人员表中的 {@code shop_id}（分销商维度），与导购列表结果列 {@code shop_id} 一致，最多 {@code maxRows} 条。
	 */
	public List<Long> listRawSalespersonShopIdsForWxappUser(long companyId, long userId, int maxRows) {
		if (companyId <= 0L || userId <= 0L || maxRows <= 0) {
			return List.of();
		}
		int cap = Math.min(maxRows, 500);
		LambdaQueryWrapper<ShopSalesperson> sw = new LambdaQueryWrapper<>();
		sw.eq(ShopSalesperson::getCompanyId, companyId).apply("user_id = {0}", userId).eq(ShopSalesperson::getIsValid, "true")
				.last("LIMIT " + cap);
		List<ShopSalesperson> salesRows = shopSalespersonMapper.selectList(sw);
		LinkedHashSet<Long> ordered = new LinkedHashSet<>();
		for (ShopSalesperson s : salesRows) {
			if (s.getShopId() == null) {
				continue;
			}
			try {
				long sid = Long.parseLong(s.getShopId().trim());
				if (sid > 0L) {
					ordered.add(sid);
				}
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return new ArrayList<>(ordered);
	}

	private static Long parseSinglePositiveDistributorId(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return null;
		}
		String t = raw.trim();
		if (t.contains(",")) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public WxappItemsListSalesmanGateResult resolve(long companyId, long userId, LinkedHashMap<String, Object> params) {
		return WxappItemsListSalesmanGateResult.proceed(new LinkedHashMap<>());
	}
}