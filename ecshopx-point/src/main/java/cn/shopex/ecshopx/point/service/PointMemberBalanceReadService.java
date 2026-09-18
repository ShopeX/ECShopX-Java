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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.point.domain.PointMember;
import cn.shopex.ecshopx.point.mapper.PointMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

/**
 * Reads the member point balance from {@code point_member} only. Optional OEM or external CRM overlays
 * are not applied in this version.
 */
@Service
public class PointMemberBalanceReadService {

	private final PointMemberMapper pointMemberMapper;

	public PointMemberBalanceReadService(PointMemberMapper pointMemberMapper) {
		this.pointMemberMapper = pointMemberMapper;
	}

	public long getPointBalance(long companyId, long userId) {
		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getCompanyId, companyId)
								.eq(PointMember::getUserId, userId)
								.last("LIMIT 1"));
		if (row == null || row.getPoint() == null) {
			return 0L;
		}
		return row.getPoint();
	}
}
