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

package cn.shopex.ecshopx.kaquan.integration.openapi;

import cn.shopex.ecshopx.common.kaquan.port.OpenapiVipGradeRelUserOpenapiPagePort;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OpenapiVipGradeRelUserOpenapiPagePortImpl implements OpenapiVipGradeRelUserOpenapiPagePort {

	private final VipGradeRelUserMapper vipGradeRelUserMapper;

	public OpenapiVipGradeRelUserOpenapiPagePortImpl(VipGradeRelUserMapper vipGradeRelUserMapper) {
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
	}

	@Override
	public PageResult listUserIdsByVipGrade(long companyId, long vipGradeId, int page, int pageSize) {
		LambdaQueryWrapper<VipGradeRelUser> base =
				new LambdaQueryWrapper<VipGradeRelUser>()
						.eq(VipGradeRelUser::getCompanyId, (int) companyId)
						.eq(VipGradeRelUser::getVipGradeId, vipGradeId);
		long totalCount = vipGradeRelUserMapper.selectCount(base);

		LambdaQueryWrapper<VipGradeRelUser> listWrapper =
				base.clone()
						.select(VipGradeRelUser::getUserId)
						.orderByDesc(VipGradeRelUser::getUserId);
		Page<VipGradeRelUser> pageRequest = new Page<>(page, pageSize, false);
		vipGradeRelUserMapper.selectPage(pageRequest, listWrapper);
		List<Long> userIds =
				pageRequest.getRecords().stream().map(VipGradeRelUser::getUserId).toList();
		return new PageResult(totalCount, userIds);
	}
}
