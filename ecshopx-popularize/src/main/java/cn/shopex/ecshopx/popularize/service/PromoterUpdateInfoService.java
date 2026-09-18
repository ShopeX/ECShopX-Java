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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromoterUpdateInfoService {

	private final PromoterMapper promoterMapper;
	private final PromoterShopStatusUpdateService promoterShopStatusUpdateService;

	public PromoterUpdateInfoService(
			PromoterMapper promoterMapper, PromoterShopStatusUpdateService promoterShopStatusUpdateService) {
		this.promoterMapper = promoterMapper;
		this.promoterShopStatusUpdateService = promoterShopStatusUpdateService;
	}

	public Map<String, Object> updatePromoterInfo(long companyId, long userId, Map<String, Object> mergedInput) {
		LinkedHashMap<String, String> patch = new LinkedHashMap<>();
		putTruthyStringPatch(patch, mergedInput, "alipay_name");
		putTruthyStringPatch(patch, mergedInput, "brief");
		putTruthyStringPatch(patch, mergedInput, "shop_pic");
		putTruthyStringPatch(patch, mergedInput, "alipay_account");
		putTruthyStringPatch(patch, mergedInput, "shop_name");

		boolean shopBranch =
				PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterObject(mergedInput.get("shop_status"));
		if (shopBranch) {
			promoterShopStatusUpdateService.updatePromoterShop(companyId, String.valueOf(userId), "2", null);
		}

		if (patch.isEmpty()) {
			if (shopBranch) {
				return Map.of("status", Boolean.TRUE);
			}
			return Collections.emptyMap();
		}

		LambdaUpdateWrapper<Promoter> uw = new LambdaUpdateWrapper<Promoter>().eq(Promoter::getUserId, userId);
		for (Map.Entry<String, String> e : patch.entrySet()) {
			String col = e.getKey();
			String val = e.getValue();
			if ("alipay_name".equals(col)) {
				uw.set(Promoter::getAlipayName, val);
			} else if ("brief".equals(col)) {
				uw.set(Promoter::getBrief, val);
			} else if ("shop_pic".equals(col)) {
				uw.set(Promoter::getShopPic, val);
			} else if ("alipay_account".equals(col)) {
				uw.set(Promoter::getAlipayAccount, val);
			} else if ("shop_name".equals(col)) {
				uw.set(Promoter::getShopName, val);
			}
		}
		promoterMapper.update(null, uw);

		Promoter refreshed =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>().eq(Promoter::getUserId, userId).last("LIMIT 1"));

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", 1L);
		if (refreshed != null) {
			out.put("list", Collections.singletonList(promoterRowAsDbMap(refreshed)));
		} else {
			out.put("list", Collections.singletonList(Boolean.TRUE));
		}
		return out;
	}

	private static void putTruthyStringPatch(
			LinkedHashMap<String, String> patch, Map<String, Object> mergedInput, String key) {
		Object raw = mergedInput.get(key);
		if (PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterObject(raw)) {
			patch.put(key, String.valueOf(raw).trim());
		}
	}

	/** Row map for list responses: core table columns only (no derived member fields). */
	private static Map<String, Object> promoterRowAsDbMap(Promoter p) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", p.getId());
		row.put("promoter_id", p.getId() != null ? String.valueOf(p.getId()) : "0");
		row.put("company_id", p.getCompanyId());
		row.put("user_id", p.getUserId());
		row.put("identity_id", p.getIdentityId());
		row.put("is_subordinates", p.getIsSubordinates());
		long pidOut = p.getPid() == null ? 0L : p.getPid();
		row.put("pid", pidOut);
		row.put("pmobile", p.getPmobile() != null ? p.getPmobile() : "");
		row.put("pname", p.getPname() != null ? p.getPname() : "");
		row.put("shop_name", p.getShopName() != null ? p.getShopName() : "");
		row.put("alipay_name", p.getAlipayName() != null ? p.getAlipayName() : "");
		row.put("brief", p.getBrief() != null ? p.getBrief() : "");
		row.put("shop_pic", p.getShopPic() != null ? p.getShopPic() : "");
		row.put("alipay_account", p.getAlipayAccount() != null ? p.getAlipayAccount() : "");
		row.put("grade_level", p.getGradeLevel());
		row.put("is_promoter", p.getIsPromoter());
		row.put("shop_status", p.getShopStatus());
		row.put("reason", p.getReason() != null ? p.getReason() : "");
		row.put("disabled", p.getDisabled());
		row.put("is_buy", p.getIsBuy());
		row.put("promoter_name", p.getPromoterName() != null ? p.getPromoterName() : "");
		row.put("regions_id", p.getRegionsId() != null ? p.getRegionsId() : "");
		row.put("address", p.getAddress() != null ? p.getAddress() : "");
		row.put("created", p.getCreated());
		row.put("updated", p.getUpdated());
		return row;
	}
}
