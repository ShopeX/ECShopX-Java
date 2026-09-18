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

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.domain.PromoterIdentity;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PromoterFirstIdentityListService {

	private final PromoterMapper promoterMapper;
	private final MemberAccountService memberAccountService;
	private final PromoterIdentityMapper promoterIdentityMapper;

	public PromoterFirstIdentityListService(
			PromoterMapper promoterMapper,
			MemberAccountService memberAccountService,
			PromoterIdentityMapper promoterIdentityMapper) {
		this.promoterMapper = promoterMapper;
		this.memberAccountService = memberAccountService;
		this.promoterIdentityMapper = promoterIdentityMapper;
	}

	public Map<String, Object> getFirstIdentityPromoter(long companyId, int page, int pageSize) {
		LambdaQueryWrapper<Promoter> wrapper =
				new LambdaQueryWrapper<Promoter>()
						.eq(Promoter::getCompanyId, companyId)
						.eq(Promoter::getIsSubordinates, 1)
						.eq(Promoter::getIsPromoter, 1)
						.eq(Promoter::getDisabled, 0);

		Page<Promoter> pg = new Page<>(page, pageSize);
		Page<Promoter> pageResult = promoterMapper.selectPage(pg, wrapper);
		long total = pageResult.getTotal();
		List<Promoter> records = pageResult.getRecords();

		if (total == 0L) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}

		List<Long> userIds =
				records.stream()
						.map(Promoter::getUserId)
						.filter(Objects::nonNull)
						.filter(uid -> uid > 0L)
						.distinct()
						.toList();

		Map<Long, String> mobileByUserId = new LinkedHashMap<>();
		for (Map<String, Object> sum : memberAccountService.listMemberSummariesByUserIds(companyId, userIds)) {
			Object uidObj = sum.get("user_id");
			if (uidObj == null) {
				continue;
			}
			long uid =
					uidObj instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(uidObj).trim());
			mobileByUserId.put(
					uid, sum.get("mobile") != null ? String.valueOf(sum.get("mobile")) : "");
		}

		List<Long> distinctIdentityIds =
				records.stream()
						.map(Promoter::getIdentityId)
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.distinct()
						.toList();

		Map<Long, String> nameByIdentityId = new LinkedHashMap<>();
		if (!distinctIdentityIds.isEmpty()) {
			List<PromoterIdentity> identities = promoterIdentityMapper.selectBatchIds(distinctIdentityIds);
			if (identities != null) {
				for (PromoterIdentity pi : identities) {
					if (pi == null || pi.getId() == null) {
						continue;
					}
					String nm = pi.getName();
					nameByIdentityId.put(pi.getId(), nm != null ? nm : "");
				}
			}
		}

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (Promoter p : records) {
			long userId = p.getUserId() != null ? p.getUserId() : 0L;
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", p.getId());
			row.put("company_id", p.getCompanyId());
			row.put("user_id", p.getUserId());
			row.put("identity_id", p.getIdentityId());
			row.put("is_subordinates", p.getIsSubordinates());
			row.put("pid", p.getPid());
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
			row.put("mobile", userId > 0L ? mobileByUserId.getOrDefault(userId, "") : "");
			Long iid = p.getIdentityId();
			if (iid == null || iid <= 0L) {
				row.put("identity_name", "");
			} else {
				row.put("identity_name", nameByIdentityId.getOrDefault(iid, ""));
			}
			listMaps.add(row);
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", listMaps);
		return data;
	}
}
