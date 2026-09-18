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

package cn.shopex.ecshopx.members.service.h5.auth;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.client.ali.AliMiniProgramOAuthClient;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.h5.H5GenericUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class AliAppH5AuthStrategy implements H5AuthStrategy {

	private final AliMiniProgramOAuthClient aliMiniProgramOAuthClient;

	private final MembersAssociationsMapper membersAssociationsMapper;

	private final MembersMapper membersMapper;

	public AliAppH5AuthStrategy(
			AliMiniProgramOAuthClient aliMiniProgramOAuthClient,
			MembersAssociationsMapper membersAssociationsMapper,
			MembersMapper membersMapper) {
		this.aliMiniProgramOAuthClient = aliMiniProgramOAuthClient;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.membersMapper = membersMapper;
	}

	@Override
	public H5AuthType type() {
		return H5AuthType.ALIAPP;
	}

	@Override
	public Optional<H5GenericUser> resolve(Map<String, Object> credentials) {
		if (!StringUtils.hasText(stringVal(credentials.get("company_id")))) {
			throw new ResourceException("缺少参数！");
		}
		long companyId = toLong(credentials.get("company_id"));
		String alipayUserId;
		if (StringUtils.hasText(stringVal(credentials.get("alipay_user_id")))) {
			alipayUserId = stringVal(credentials.get("alipay_user_id"));
		} else {
			alipayUserId = aliMiniProgramOAuthClient.exchangeCodeForUserId(companyId, stringVal(credentials.get("code")));
		}
		MembersAssociations assoc = membersAssociationsMapper.selectOne(new LambdaQueryWrapper<MembersAssociations>()
				.eq(MembersAssociations::getCompanyId, companyId)
				.eq(MembersAssociations::getUserType, "ali")
				.eq(MembersAssociations::getUnionid, alipayUserId)
				.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("请您检查是否已经授权！");
		}
		Members member = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getUserId, assoc.getUserId())
				.eq(Members::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (member == null || member.getUserId() == null) {
			throw new ResourceException("请注册！");
		}
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("id", member.getUserId() + "_espier_alipay_espier_" + alipayUserId);
		attrs.put("user_id", member.getUserId());
		attrs.put("company_id", companyId);
		attrs.put("alipay_appid", member.getAlipayAppid());
		attrs.put("alipay_user_id", alipayUserId);
		attrs.put("operator_type", "user");
		return Optional.of(new H5GenericUser(attrs));
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
