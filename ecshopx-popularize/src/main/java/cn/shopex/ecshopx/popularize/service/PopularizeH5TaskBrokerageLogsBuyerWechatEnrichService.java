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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeH5TaskBrokerageLogsBuyerWechatEnrichService {

	private final MembersAssociationsMapper membersAssociationsMapper;
	private final WechatUsersMapper wechatUsersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public PopularizeH5TaskBrokerageLogsBuyerWechatEnrichService(
			MembersAssociationsMapper membersAssociationsMapper,
			WechatUsersMapper wechatUsersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.wechatUsersMapper = wechatUsersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public void attachBuyerUsernameAndAvatar(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> seen = new LinkedHashSet<>();
		List<Long> buyUserIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Long uid = extractLongFlexible(row.get("buy_user_id"));
			if (uid != null && uid > 0L && seen.add(uid)) {
				buyUserIds.add(uid);
			}
		}
		Map<Long, WechatUsers> wechatByBuyUserId =
				buyUserIds.isEmpty()
						? Collections.emptyMap()
						: loadBestWechatUsersByUserIds(companyId, buyUserIds);
		for (Map<String, Object> row : rows) {
			Long buyUid = extractLongFlexible(row.get("buy_user_id"));
			WechatUsers wu = buyUid == null ? null : wechatByBuyUserId.get(buyUid);
			if (wu == null) {
				row.put("username", "");
				row.put("avatar", "");
			} else {
				String plainNickname =
						wu.getNickname() == null
								? ""
								: sensitiveFieldEncryptor.decrypt(String.valueOf(wu.getNickname()));
				Object head = wu.getHeadimgurl();
				row.put("username", plainNickname);
				row.put("avatar", head == null ? "" : String.valueOf(head));
			}
		}
	}

	private Map<Long, WechatUsers> loadBestWechatUsersByUserIds(long companyId, List<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<MembersAssociations> assocs =
				membersAssociationsMapper.selectList(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.in(MembersAssociations::getUserId, userIds)
								.isNotNull(MembersAssociations::getUnionid));
		if (assocs.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, MembersAssociations> bestAssocByUserId = new HashMap<>();
		for (MembersAssociations a : assocs) {
			String uni = a.getUnionid();
			if (uni == null || uni.trim().isEmpty()) {
				continue;
			}
			Long uid = a.getUserId();
			if (uid == null || uid <= 0L) {
				continue;
			}
			MembersAssociations prev = bestAssocByUserId.get(uid);
			if (prev == null || compareMembersAssociationsTupleMax(a, prev) > 0) {
				bestAssocByUserId.put(uid, a);
			}
		}
		Map<Long, String> userIdToUnionid = new LinkedHashMap<>();
		for (Map.Entry<Long, MembersAssociations> e : bestAssocByUserId.entrySet()) {
			String u = e.getValue().getUnionid();
			if (u != null && !u.trim().isEmpty()) {
				userIdToUnionid.put(e.getKey(), u.trim());
			}
		}
		LinkedHashSet<String> distinctUnionidSet = new LinkedHashSet<>();
		for (String u : userIdToUnionid.values()) {
			distinctUnionidSet.add(u);
		}
		List<String> distinctUnionids = new ArrayList<>(distinctUnionidSet);
		if (distinctUnionids.isEmpty()) {
			return Collections.emptyMap();
		}
		List<WechatUsers> wrows =
				wechatUsersMapper.selectList(
						new LambdaQueryWrapper<WechatUsers>()
								.eq(WechatUsers::getCompanyId, companyId)
								.in(WechatUsers::getUnionid, distinctUnionids));
		Map<String, WechatUsers> bestByUnionid = new HashMap<>();
		for (WechatUsers wu : wrows) {
			String uni = wu.getUnionid();
			if (!StringUtils.hasText(uni)) {
				continue;
			}
			String uniKey = uni.trim();
			WechatUsers cur = bestByUnionid.get(uniKey);
			if (cur == null || compareWechatUsersForSameUnionid(wu, cur) > 0) {
				bestByUnionid.put(uniKey, wu);
			}
		}
		LinkedHashMap<Long, WechatUsers> out = new LinkedHashMap<>();
		for (Long uid : userIds) {
			if (uid == null || uid <= 0L || out.containsKey(uid)) {
				continue;
			}
			String unionid = userIdToUnionid.get(uid);
			if (!StringUtils.hasText(unionid)) {
				continue;
			}
			WechatUsers wu = bestByUnionid.get(unionid);
			if (wu == null) {
				continue;
			}
			out.put(uid, wu);
		}
		return out;
	}

	private static int compareMembersAssociationsTupleMax(MembersAssociations x, MembersAssociations y) {
		int c = nullToEmpty(x.getUserType()).compareTo(nullToEmpty(y.getUserType()));
		if (c != 0) {
			return c;
		}
		return nullToEmpty(x.getUnionid()).compareTo(nullToEmpty(y.getUnionid()));
	}

	private static int compareWechatUsersForSameUnionid(WechatUsers a, WechatUsers b) {
		int c = compareLongNullLowDesc(a.getUpdated(), b.getUpdated());
		if (c != 0) {
			return c;
		}
		c = compareLongNullLowDesc(a.getCreated(), b.getCreated());
		if (c != 0) {
			return c;
		}
		return wechatAuthOpenTieKey(a).compareTo(wechatAuthOpenTieKey(b));
	}

	private static int compareLongNullLowDesc(Long a, Long b) {
		long la = a == null ? Long.MIN_VALUE : a.longValue();
		long lb = b == null ? Long.MIN_VALUE : b.longValue();
		return Long.compare(la, lb);
	}

	private static String wechatAuthOpenTieKey(WechatUsers w) {
		String app = w.getAuthorizerAppid() == null ? "" : w.getAuthorizerAppid();
		String oid = w.getOpenId() == null ? "" : w.getOpenId();
		return app + '\u0000' + oid;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static Long extractLongFlexible(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String t = String.valueOf(o).trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
