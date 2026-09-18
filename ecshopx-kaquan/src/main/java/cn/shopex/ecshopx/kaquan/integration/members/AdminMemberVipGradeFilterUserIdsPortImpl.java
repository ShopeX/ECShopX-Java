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

package cn.shopex.ecshopx.kaquan.integration.members;

import cn.shopex.ecshopx.common.members.admin.AdminMemberVipGradeFilterUserIdsPort;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service("adminMemberVipGradeFilterUserIdsPortImpl")
public class AdminMemberVipGradeFilterUserIdsPortImpl implements AdminMemberVipGradeFilterUserIdsPort {

	private final VipGradeRelUserMapper vipGradeRelUserMapper;

	public AdminMemberVipGradeFilterUserIdsPortImpl(VipGradeRelUserMapper vipGradeRelUserMapper) {
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
	}

	@Override
	public List<Long> listUserIdsMatchingVipGradeFilter(long companyId, Object vipGradeFilterRaw) {
		String raw = vipGradeFilterRaw == null ? "" : String.valueOf(vipGradeFilterRaw).trim();
		if (raw.isEmpty()) {
			return List.of();
		}
		long nowSec = Instant.now().getEpochSecond();
		LambdaQueryWrapper<VipGradeRelUser> w =
				new LambdaQueryWrapper<VipGradeRelUser>()
						.eq(VipGradeRelUser::getCompanyId, (int) companyId)
						.apply("CAST(end_date AS UNSIGNED) > {0}", nowSec);
		if (!"notvip".equalsIgnoreCase(raw)) {
			List<String> types =
					Arrays.stream(raw.split(","))
							.map(String::trim)
							.filter(s -> !s.isEmpty())
							.collect(Collectors.toList());
			if (!types.isEmpty()) {
				w.in(VipGradeRelUser::getVipType, types);
			}
		}
		List<VipGradeRelUser> rows = vipGradeRelUserMapper.selectList(w);
		return rows.stream().map(VipGradeRelUser::getUserId).distinct().collect(Collectors.toCollection(ArrayList::new));
	}
}
