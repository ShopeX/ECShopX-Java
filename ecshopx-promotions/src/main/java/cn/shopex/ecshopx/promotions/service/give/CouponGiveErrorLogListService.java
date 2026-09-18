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

package cn.shopex.ecshopx.promotions.service.give;

import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import cn.shopex.ecshopx.promotions.mapper.CouponGiveErrorLogListMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CouponGiveErrorLogListService {

	private final CouponGiveErrorLogListMapper couponGiveErrorLogListMapper;
	private final MembersContactByUserIdsLookupService membersContactByUserIdsLookupService;

	public CouponGiveErrorLogListService(
			CouponGiveErrorLogListMapper couponGiveErrorLogListMapper,
			MembersContactByUserIdsLookupService membersContactByUserIdsLookupService) {
		this.couponGiveErrorLogListMapper = couponGiveErrorLogListMapper;
		this.membersContactByUserIdsLookupService = membersContactByUserIdsLookupService;
	}

	public Map<String, Object> getGiveErrorLog(long giveId, long companyId, int page, int pageSize) {
		long total = couponGiveErrorLogListMapper.countByGiveIdAndCompanyId(giveId, companyId);
		List<Map<String, Object>> list;
		if (total == 0L) {
			list = new ArrayList<>();
		} else {
			long offset = (long) pageSize * (long) (page - 1);
			List<Map<String, Object>> raw =
					couponGiveErrorLogListMapper.selectPageByGiveIdAndCompanyId(giveId, companyId, offset, pageSize);
			list = new ArrayList<>();
			for (Map<String, Object> src : raw) {
				list.add(normalizeRow(src));
			}
		}

		if (!list.isEmpty()) {
			Set<Long> uidOrder = new LinkedHashSet<>();
			for (Map<String, Object> row : list) {
				Long uid = toLong(row.get("uid"));
				if (uid != null) {
					uidOrder.add(uid);
				}
			}
			if (!uidOrder.isEmpty()) {
				List<Long> uidList = new ArrayList<>(uidOrder);
				Map<Long, Map<String, String>> contactByUserId =
						membersContactByUserIdsLookupService.loadDecryptedContactsByUserIds(uidList, pageSize);
				for (Map<String, Object> row : list) {
					Long uid = toLong(row.get("uid"));
					if (uid == null) {
						continue;
					}
					Map<String, String> contact = contactByUserId.get(uid);
					if (contact != null) {
						row.put("username", contact.get("username"));
						row.put("mobile", contact.get("mobile"));
					} else {
						row.put("username", "null");
						row.put("mobile", "null");
					}
				}
			}
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private static LinkedHashMap<String, Object> normalizeRow(Map<String, Object> src) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("give_log_id", toLong(src.get("give_log_id")));
		row.put("give_id", toLong(src.get("give_id")));
		row.put("uid", toLong(src.get("uid")));
		row.put("company_id", toLong(src.get("company_id")));
		row.put("card_id", toLong(src.get("card_id")));
		row.put("note", src.get("note") == null ? "" : String.valueOf(src.get("note")));
		row.put("created", toInt(src.get("created")));
		row.put("updated", toInt(src.get("updated")));
		Object title = src.get("title");
		row.put("title", title == null ? "" : String.valueOf(title));
		return row;
	}

	private static Long toLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer toInt(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
