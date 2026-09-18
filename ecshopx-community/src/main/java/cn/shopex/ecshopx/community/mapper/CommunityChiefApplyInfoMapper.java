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

package cn.shopex.ecshopx.community.mapper;

import cn.shopex.ecshopx.community.domain.CommunityChiefApplyInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommunityChiefApplyInfoMapper extends BaseMapper<CommunityChiefApplyInfo> {

	default CommunityChiefApplyInfo selectLatestByCompanyUserAndDistributor(
			long companyId, long userId, int distributorId) {
		LambdaQueryWrapper<CommunityChiefApplyInfo> w = new LambdaQueryWrapper<>();
		w.eq(CommunityChiefApplyInfo::getCompanyId, companyId)
				.eq(CommunityChiefApplyInfo::getUserId, userId)
				.eq(CommunityChiefApplyInfo::getDistributorId, distributorId)
				.orderByDesc(CommunityChiefApplyInfo::getCreatedAt)
				.last("LIMIT 1");
		List<CommunityChiefApplyInfo> list = selectList(w);
		return list.isEmpty() ? null : list.get(0);
	}
}
