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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.members.domain.MembersWhitelist;
import cn.shopex.ecshopx.members.mapper.MembersWhitelistMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembersWhitelistDeleteDataService {

	private final MembersWhitelistMapper membersWhitelistMapper;

	public MembersWhitelistDeleteDataService(MembersWhitelistMapper membersWhitelistMapper) {
		this.membersWhitelistMapper = membersWhitelistMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> deleteData(long companyId, String pathWhitelistId) {
		String trimmed = pathWhitelistId == null ? "" : pathWhitelistId.trim();
		if (trimmed.isEmpty()) {
			throw new BadRequestException("缺少id");
		}

		long whitelistId = LeadingNumberParser.parseAsLong(trimmed);

		LambdaQueryWrapper<MembersWhitelist> w = new LambdaQueryWrapper<MembersWhitelist>()
				.eq(MembersWhitelist::getCompanyId, companyId)
				.eq(MembersWhitelist::getWhitelistId, whitelistId);
		membersWhitelistMapper.delete(w);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}
}
