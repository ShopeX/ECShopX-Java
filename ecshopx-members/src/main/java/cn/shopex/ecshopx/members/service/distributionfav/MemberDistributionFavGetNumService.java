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

package cn.shopex.ecshopx.members.service.distributionfav;

import cn.shopex.ecshopx.members.domain.MemberDistributionFav;
import cn.shopex.ecshopx.members.mapper.MemberDistributionFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class MemberDistributionFavGetNumService {

	private final MemberDistributionFavMapper memberDistributionFavMapper;

	public MemberDistributionFavGetNumService(MemberDistributionFavMapper memberDistributionFavMapper) {
		this.memberDistributionFavMapper = memberDistributionFavMapper;
	}

	public long getDistributionFavNum(long companyId, long userId, Long distributorIdExclusive) {
		LambdaQueryWrapper<MemberDistributionFav> w = new LambdaQueryWrapper<>();
		w.eq(MemberDistributionFav::getCompanyId, companyId);
		if (distributorIdExclusive != null) {
			w.eq(MemberDistributionFav::getDistributorId, distributorIdExclusive);
		} else {
			w.eq(MemberDistributionFav::getUserId, userId);
		}
		Long count = memberDistributionFavMapper.selectCount(w);
		if (count == null) {
			return 0L;
		}
		return count.longValue();
	}
}
