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
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminMemberUpdateMemberInfoService {

	private final MembersMapper membersMapper;

	private final MembersInfoMapper membersInfoMapper;

	public AdminMemberUpdateMemberInfoService(
			MembersMapper membersMapper, MembersInfoMapper membersInfoMapper) {
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
	}

	public Map<String, Object> updateMemberInfo(long companyId, long userId, Map<String, Object> payload) {
		Members row =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("用户不存在");
		}

		boolean needMainUpdate = payload.containsKey("disabled") || payload.containsKey("remarks");
		if (needMainUpdate) {
			LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
			uw.eq(Members::getCompanyId, companyId).eq(Members::getUserId, userId);
			if (payload.containsKey("disabled")) {
				uw.set(Members::getDisabled, normalizeDisabledFlag(payload.get("disabled")));
			}
			if (payload.containsKey("remarks")) {
				Object rv = payload.get("remarks");
				uw.set(Members::getRemarks, rv == null ? null : String.valueOf(rv));
			}
			uw.set(Members::getUpdated, System.currentTimeMillis() / 1000L);
			int affected = membersMapper.update(null, uw);
			if (affected == 0) {
				throw new ResourceException("会员不存在");
			}
			row =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getUserId, userId)
									.last("LIMIT 1"));
		}

		if (payload.containsKey("name")) {
			MembersInfo info =
					membersInfoMapper.selectOne(
							new LambdaQueryWrapper<MembersInfo>()
									.eq(MembersInfo::getCompanyId, companyId)
									.eq(MembersInfo::getUserId, userId)
									.last("LIMIT 1"));
			if (info == null) {
				throw new ResourceException("未查询到更新数据");
			}
			String plainName = payload.get("name") == null ? null : String.valueOf(payload.get("name"));
			info.setName(LegacyFixedMobileEncrypt.fixedEncryptMobile(plainName));
			info.setUpdated(System.currentTimeMillis() / 1000L);
			membersInfoMapper.updateById(info);
		}

		return MemberAccountService.mapMembersTable(row);
	}

	private static boolean normalizeDisabledFlag(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			if ("1".equals(t) || "true".equalsIgnoreCase(t)) {
				return true;
			}
			return true;
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equalsIgnoreCase(t)) {
			return false;
		}
		return true;
	}
}
