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

package cn.shopex.ecshopx.wsugc.service.post;

import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UgcPostOpenIdResolveService {

	private final MemberAccountService memberAccountService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final WechatUsersMapper wechatUsersMapper;

	public UgcPostOpenIdResolveService(
			MemberAccountService memberAccountService,
			MembersAssociationsMapper membersAssociationsMapper,
			WechatUsersMapper wechatUsersMapper) {
		this.memberAccountService = memberAccountService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.wechatUsersMapper = wechatUsersMapper;
	}

	public String resolveOpenId(long userId, long companyId) {
		Map<String, Object> memberRow = memberAccountService.getMemberInfo(userId, companyId);
		Object appIdRaw = memberRow.get("wxa_appid");
		String appId = appIdRaw == null ? "" : String.valueOf(appIdRaw).trim();
		if (!StringUtils.hasText(appId)) {
			return "";
		}

		MembersAssociations assoc = membersAssociationsMapper.selectOne(new LambdaQueryWrapper<MembersAssociations>()
				.eq(MembersAssociations::getUserId, userId)
				.eq(MembersAssociations::getCompanyId, companyId)
				.eq(MembersAssociations::getUserType, "wechat")
				.last("LIMIT 1"));
		if (assoc == null || !StringUtils.hasText(assoc.getUnionid())) {
			return "";
		}
		String unionid = assoc.getUnionid().trim();

		WechatUsers wu = wechatUsersMapper.selectOne(new LambdaQueryWrapper<WechatUsers>()
				.eq(WechatUsers::getCompanyId, companyId)
				.eq(WechatUsers::getAuthorizerAppid, appId)
				.eq(WechatUsers::getUnionid, unionid)
				.last("LIMIT 1"));
		if (wu == null || !StringUtils.hasText(wu.getOpenId())) {
			return "";
		}
		return wu.getOpenId().trim();
	}
}
