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

import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UgcPostUserIdResolveService {

	private final WechatUsersMapper wechatUsersMapper;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MembersMapper membersMapper;
	private final MemberAccountService memberAccountService;

	public UgcPostUserIdResolveService(
			WechatUsersMapper wechatUsersMapper,
			MembersAssociationsMapper membersAssociationsMapper,
			MembersMapper membersMapper,
			MemberAccountService memberAccountService) {
		this.wechatUsersMapper = wechatUsersMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.membersMapper = membersMapper;
		this.memberAccountService = memberAccountService;
	}

	public List<Long> userIdsByNicknameContains(String nickname) {
		if (!StringUtils.hasText(nickname)) {
			return List.of(-1L);
		}
		LambdaQueryWrapper<WechatUsers> w = new LambdaQueryWrapper<>();
		w.like(WechatUsers::getNickname, "%" + escapeLike(nickname.trim()) + "%");
		List<WechatUsers> rows = wechatUsersMapper.selectList(w);
		Set<String> unionids = new LinkedHashSet<>();
		for (WechatUsers r : rows) {
			if (r.getUnionid() != null && StringUtils.hasText(r.getUnionid())) {
				unionids.add(r.getUnionid());
			}
		}
		if (unionids.isEmpty()) {
			return List.of(-1L);
		}
		LambdaQueryWrapper<MembersAssociations> aw = new LambdaQueryWrapper<>();
		aw.eq(MembersAssociations::getUserType, "wechat").in(MembersAssociations::getUnionid, unionids);
		List<MembersAssociations> assoc = membersAssociationsMapper.selectList(aw);
		Set<Long> userIds = new LinkedHashSet<>();
		for (MembersAssociations a : assoc) {
			if (a.getUserId() != null) {
				userIds.add(a.getUserId());
			}
		}
		if (userIds.isEmpty()) {
			return List.of(-1L);
		}
		return new ArrayList<>(userIds);
	}

	public List<Long> userIdsByMobile(String mobile) {
		if (!StringUtils.hasText(mobile)) {
			return List.of(-1L);
		}
		String storedMobile = memberAccountService.encodeMobileForStorage(mobile.trim());
		LambdaQueryWrapper<Members> mw = new LambdaQueryWrapper<>();
		mw.eq(Members::getMobile, storedMobile).last("LIMIT 1");
		Members m = membersMapper.selectOne(mw);
		if (m == null || m.getUserId() == null) {
			return List.of(-1L);
		}
		return List.of(m.getUserId());
	}

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
