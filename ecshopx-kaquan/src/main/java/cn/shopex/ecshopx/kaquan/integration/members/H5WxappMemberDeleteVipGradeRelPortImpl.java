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

import cn.shopex.ecshopx.common.members.h5.H5WxappMemberDeleteVipGradeRelPort;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("h5WxappMemberDeleteVipGradeRelPortImpl")
@RequiredArgsConstructor
public class H5WxappMemberDeleteVipGradeRelPortImpl implements H5WxappMemberDeleteVipGradeRelPort {

	private final VipGradeRelUserMapper vipGradeRelUserMapper;

	@Override
	public void deleteVipGradeRelForMember(long companyId, long userId) {
		vipGradeRelUserMapper.delete(
				new LambdaQueryWrapper<VipGradeRelUser>()
						.eq(VipGradeRelUser::getCompanyId, (int) companyId)
						.eq(VipGradeRelUser::getUserId, userId));
	}
}
