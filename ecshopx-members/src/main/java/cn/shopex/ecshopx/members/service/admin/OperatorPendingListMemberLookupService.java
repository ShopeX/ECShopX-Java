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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.companys.operatorpending.OperatorPendingListMemberLookupPort;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.mapper.OperatorPendingListMemberLookupMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorPendingListMemberLookupService implements OperatorPendingListMemberLookupPort {

	private static final String[] MEMBER_NULLABLE_KEYS = {
		"name",
		"offline_card_code",
		"remarks",
		"third_data",
		"birthday",
		"address",
		"industry",
		"income",
		"edu_background",
		"fp_salesperson"
	};

	private final OperatorPendingListMemberLookupMapper operatorPendingListMemberLookupMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OperatorPendingListMemberLookupService(
			OperatorPendingListMemberLookupMapper operatorPendingListMemberLookupMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.operatorPendingListMemberLookupMapper = operatorPendingListMemberLookupMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Override
	public Map<Long, Map<String, Object>> listPendingDataMemberRows(long companyId, List<Long> userIds, int limit) {
		if (userIds == null || userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Map<String, Object>> baseRows =
				operatorPendingListMemberLookupMapper.selectMemberBaseRows(companyId, userIds, limit);
		Map<Long, Map<String, Object>> byUserId = new LinkedHashMap<>();
		for (Map<String, Object> raw : baseRows) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>(raw);
			Long uid = toLongBoxed(row.get("user_id"));
			if (uid == null || uid <= 0L) {
				continue;
			}
			Object mobileEnc = row.get("mobile");
			row.put("mobile", mobileEnc == null ? "" : sensitiveFieldEncryptor.decrypt(String.valueOf(mobileEnc)));
			Object usernameEnc = row.get("username");
			row.put("username", usernameEnc == null ? "" : sensitiveFieldEncryptor.decrypt(String.valueOf(usernameEnc)));
			row.put("nickname", "");
			for (String key : MEMBER_NULLABLE_KEYS) {
				if (!row.containsKey(key)) {
					row.put(key, null);
				}
			}
			byUserId.put(uid, row);
		}

		List<Map<String, Object>> assocRows =
				operatorPendingListMemberLookupMapper.selectMemberAssociationsForPendingList(companyId, userIds);
		if (assocRows == null || assocRows.isEmpty()) {
			for (Map<String, Object> row : byUserId.values()) {
				row.put("unionid", "");
				row.put("open_id", "");
			}
			return byUserId;
		}

		List<String> unionids = new ArrayList<>();
		LinkedHashSet<String> dedup = new LinkedHashSet<>();
		for (Map<String, Object> ar : assocRows) {
			Object u = ar.get("unionid");
			if (u != null && StringUtils.hasText(u.toString())) {
				String s = u.toString();
				if (dedup.add(s)) {
					unionids.add(s);
				}
			}
		}

		Map<String, String> openIdByUnionid = new LinkedHashMap<>();
		if (!unionids.isEmpty()) {
			List<Map<String, Object>> wxRows =
					operatorPendingListMemberLookupMapper.selectWechatOpenIdsForPendingList(companyId, unionids);
			if (wxRows != null) {
				for (Map<String, Object> wr : wxRows) {
					Object uk = wr.get("unionid");
					Object ok = wr.get("open_id");
					String unionKey = uk == null ? "" : uk.toString();
					openIdByUnionid.put(unionKey, ok == null ? "" : ok.toString());
				}
			}
		}

		Map<Long, WechatAssoc> assocByUserId = new LinkedHashMap<>();
		for (Map<String, Object> ar : assocRows) {
			Long u = toLongBoxed(ar.get("user_id"));
			if (u == null) {
				continue;
			}
			Object unionObj = ar.get("unionid");
			String unionid = unionObj == null ? "" : unionObj.toString();
			String openId = openIdByUnionid.getOrDefault(unionid, "");
			assocByUserId.put(u, new WechatAssoc(unionid, openId));
		}

		for (Map.Entry<Long, Map<String, Object>> e : byUserId.entrySet()) {
			WechatAssoc wa = assocByUserId.get(e.getKey());
			if (wa == null) {
				e.getValue().put("unionid", "");
				e.getValue().put("open_id", "");
			} else {
				e.getValue().put("unionid", wa.unionid == null ? "" : wa.unionid);
				e.getValue().put("open_id", wa.openId == null ? "" : wa.openId);
			}
		}
		return byUserId;
	}

	private static Long toLongBoxed(Object o) {
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

	private record WechatAssoc(String unionid, String openId) {}
}
