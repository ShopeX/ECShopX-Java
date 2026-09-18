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

package cn.shopex.ecshopx.members.integration.admin;

import cn.shopex.ecshopx.common.members.admin.MemberUnionidByUserIdLookupPort;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberUnionidByUserIdLookupPortImpl implements MemberUnionidByUserIdLookupPort {

	private final MembersAssociationsMapper membersAssociationsMapper;

	public MemberUnionidByUserIdLookupPortImpl(MembersAssociationsMapper membersAssociationsMapper) {
		this.membersAssociationsMapper = membersAssociationsMapper;
	}

	@Override
	public Optional<String> findUnionidByUserId(long companyId, long userId) {
		LambdaQueryWrapper<MembersAssociations> q = Wrappers.lambdaQuery();
		q.eq(MembersAssociations::getCompanyId, companyId)
				.eq(MembersAssociations::getUserId, userId)
				.orderByAsc(MembersAssociations::getUserType)
				.orderByAsc(MembersAssociations::getUnionid);
		List<MembersAssociations> rows = membersAssociationsMapper.selectList(q);
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String unionid = rows.get(0).getUnionid();
		if (!StringUtils.hasText(unionid)) {
			return Optional.empty();
		}
		return Optional.of(unionid.trim());
	}
}
