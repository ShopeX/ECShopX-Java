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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberLookupByUserIdOnlyService {

	private final MembersMapper membersMapper;

	public MemberLookupByUserIdOnlyService(MembersMapper membersMapper) {
		this.membersMapper = membersMapper;
	}

	public Map<String, Object> requireMemberRowForFirstHop(long userId) {
		Members row = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getUserId, userId)
				.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("会员信息有误");
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", row.getUserId());
		String mobile = row.getMobile();
		m.put("mobile", StringUtils.hasText(mobile) ? mobile : "");
		if (row.getAuthorizerAppid() != null) {
			m.put("woa_appid", row.getAuthorizerAppid());
		} else {
			m.put("woa_appid", "");
		}
		if (row.getWxaAppid() != null) {
			m.put("wxapp_appid", row.getWxaAppid());
		} else {
			m.put("wxapp_appid", "");
		}
		return m;
	}
}
