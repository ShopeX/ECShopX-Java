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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.domain.MemberDistributionFav;
import cn.shopex.ecshopx.members.mapper.MemberDistributionFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class MemberDistributionFavRemoveService {

	private final MemberDistributionFavMapper memberDistributionFavMapper;

	public MemberDistributionFavRemoveService(MemberDistributionFavMapper memberDistributionFavMapper) {
		this.memberDistributionFavMapper = memberDistributionFavMapper;
	}

	public boolean deleteDistributionFav(long companyId, long userId, Object distributorIdForEq) {
		if (companyId <= 0L || userId <= 0L) {
			throw new BadRequestException("参数有误", 422);
		}
		if (distributorIdForEq == null) {
			throw new BadRequestException("参数有误", 422);
		}
		if (distributorIdForEq instanceof Number n && n.longValue() == 0L) {
			throw new BadRequestException("参数有误", 422);
		}
		if (distributorIdForEq instanceof String s && s.trim().equals("0")) {
			throw new BadRequestException("参数有误", 422);
		}
		LambdaQueryWrapper<MemberDistributionFav> w = new LambdaQueryWrapper<>();
		w.eq(MemberDistributionFav::getCompanyId, companyId)
				.eq(MemberDistributionFav::getUserId, userId)
				.apply("distributor_id = {0}", distributorIdForEq);
		memberDistributionFavMapper.delete(w);
		return true;
	}
}
