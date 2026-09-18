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

package cn.shopex.ecshopx.members.service.wxapp;

import cn.shopex.ecshopx.common.members.port.MemberItemsFavListGoodsEnrichmentPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberInfoKaquanPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberPointRuleAndBalancePort;
import cn.shopex.ecshopx.common.members.port.WxappMemberStatisticalDistributorActivePort;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.members.domain.MemberItemsFav;
import cn.shopex.ecshopx.members.mapper.MemberItemsFavMapper;
import cn.shopex.ecshopx.members.service.articlefav.MemberArticleFavGetNumService;
import cn.shopex.ecshopx.members.service.distributionfav.MemberDistributionFavGetNumService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WxappMemberStatisticalService {

	private final WxappMemberInfoKaquanPort wxappMemberInfoKaquanPort;
	private final WxappMemberPointRuleAndBalancePort wxappMemberPointRuleAndBalancePort;
	private final MemberItemsFavMapper memberItemsFavMapper;
	private final MemberItemsFavListGoodsEnrichmentPort memberItemsFavListGoodsEnrichmentPort;
	private final WxappMemberStatisticalDistributorActivePort wxappMemberStatisticalDistributorActivePort;
	private final MemberArticleFavGetNumService memberArticleFavGetNumService;
	private final MemberDistributionFavGetNumService memberDistributionFavGetNumService;

	public Map<String, Object> getMemberStatistical(
			long companyId, long userId, int page, int pageSize, String acceptLanguageHeader) {
		long rawDiscount = wxappMemberInfoKaquanPort.countStatisticalValidUserDiscount(companyId, userId);
		int disc = (int) Math.min(rawDiscount, Integer.MAX_VALUE);

		Map<String, Object> pm = wxappMemberPointRuleAndBalancePort.loadPointMemberInfo(companyId, userId);
		long pointTotal = resolvePointTotal(pm);

		LambdaQueryWrapper<MemberItemsFav> w = new LambdaQueryWrapper<>();
		w.eq(MemberItemsFav::getCompanyId, companyId)
				.eq(MemberItemsFav::getUserId, userId)
				.orderByDesc(MemberItemsFav::getFavId);
		Page<MemberItemsFav> mp = new Page<>(page, pageSize);
		memberItemsFavMapper.selectPage(mp, w);
		long favAdjustedTotal = mp.getTotal();
		List<MemberItemsFav> records = mp.getRecords();
		if (records != null && !records.isEmpty()) {
			LinkedHashSet<Long> itemIds = new LinkedHashSet<>();
			for (MemberItemsFav r : records) {
				Long iid = r.getItemId();
				if (iid != null && iid > 0L) {
					itemIds.add(iid);
				}
			}
			if (!itemIds.isEmpty()) {
				String lang = acceptLanguageHeader != null ? acceptLanguageHeader : "zh-CN";
				List<Map<String, Object>> enriched =
						memberItemsFavListGoodsEnrichmentPort.loadEnrichedRows(
								companyId, userId, new ArrayList<>(itemIds), null, pageSize, lang);
				Map<Long, Map<String, Object>> enrichedByItemId = new LinkedHashMap<>();
				for (Map<String, Object> erow : enriched) {
					long eid = longVal(erow.get("item_id"));
					if (eid > 0L) {
						enrichedByItemId.put(eid, erow);
					}
				}
				for (MemberItemsFav rec : records) {
					Long itemId = rec.getItemId();
					if (itemId == null || itemId <= 0L) {
						continue;
					}
					Map<String, Object> itemRow = enrichedByItemId.get(itemId);
					if (itemRow == null) {
						continue;
					}
					if (!ValuePresence.hasEffectiveValue(itemRow.get("distributor_id"))) {
						continue;
					}
					long distId = longVal(itemRow.get("distributor_id"));
					if (!wxappMemberStatisticalDistributorActivePort.hasActiveDistributor(companyId, distId)) {
						favAdjustedTotal--;
					}
				}
			}
		}

		long articleCount = memberArticleFavGetNumService.getArticleFavNum(companyId, userId);
		long storeCount =
				memberDistributionFavGetNumService.getDistributionFavNum(companyId, userId, null);
		long sum = favAdjustedTotal + articleCount + storeCount;

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("discount_total_count", Integer.valueOf(disc));
		out.put("point_total_count", Long.valueOf(pointTotal));
		out.put("fav_total_count", Long.valueOf(sum));
		return out;
	}

	private static long resolvePointTotal(Map<String, Object> pm) {
		if (pm == null) {
			return 0L;
		}
		Object p = pm.get("point");
		if (p == null) {
			return 0L;
		}
		if (p instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(p).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
