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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WechatUserListByUserIdsService {

	private final WechatUsersMapper wechatUsersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public WechatUserListByUserIdsService(WechatUsersMapper wechatUsersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.wechatUsersMapper = wechatUsersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<Long, Map<String, Object>> mapByUserId(long companyId, Collection<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Long> ids = new ArrayList<>();
		for (Long uid : userIds) {
			if (uid != null && uid != 0L) {
				ids.add(uid);
			}
		}
		if (ids.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Map<String, Object>> rows = wechatUsersMapper.selectWechatNicknameRowsByCompanyAndUserIds(companyId, ids);
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			Object uidObj = row.get("userId");
			if (uidObj == null) {
				uidObj = row.get("userid");
			}
			long userId = uidObj instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(uidObj));
			Object nick = row.get("nickname");
			String nicknamePlain = nick == null ? "" : sensitiveFieldEncryptor.decrypt(String.valueOf(nick));
			Map<String, Object> cell = new LinkedHashMap<>();
			cell.put("nickname", nicknamePlain);
			out.put(userId, cell);
		}
		return out;
	}
}
