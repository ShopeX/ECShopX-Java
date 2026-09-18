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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.stereotype.Service;

@Service
public class MembersContactByUserIdsLookupService {

	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public MembersContactByUserIdsLookupService(
			MembersMapper membersMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<Long, Map<String, String>> loadDecryptedContactsByUserIds(List<Long> userIds, int limit) {
		if (userIds == null || userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<MemberContactRow> rows = membersMapper.selectMembersWithInfoForUserIds(userIds, limit);
		Map<Long, Map<String, String>> out = new LinkedHashMap<>();
		for (MemberContactRow r : rows) {
			if (r.getUserId() == null) {
				continue;
			}
			String mobilePlain =
					r.getMobileEnc() == null ? "" : sensitiveFieldEncryptor.decrypt(r.getMobileEnc());
			String usernamePlain =
					r.getUsernameEnc() == null ? "" : sensitiveFieldEncryptor.decrypt(r.getUsernameEnc());
			out.put(r.getUserId(), Map.of("mobile", mobilePlain, "username", usernamePlain));
		}
		return out;
	}

	@Data
	public static class MemberContactRow {
		private Long userId;
		private String mobileEnc;
		private String usernameEnc;
	}
}
