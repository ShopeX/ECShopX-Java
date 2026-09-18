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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberDistributionFav;
import cn.shopex.ecshopx.members.mapper.DistributionDistributorLookupMapper;
import cn.shopex.ecshopx.members.mapper.MemberDistributionFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberDistributionFavAddService {

	private final DistributionDistributorLookupMapper distributionDistributorLookupMapper;

	private final MemberDistributionFavMapper memberDistributionFavMapper;

	public MemberDistributionFavAddService(
			DistributionDistributorLookupMapper distributionDistributorLookupMapper,
			MemberDistributionFavMapper memberDistributionFavMapper) {
		this.distributionDistributorLookupMapper = distributionDistributorLookupMapper;
		this.memberDistributionFavMapper = memberDistributionFavMapper;
	}

	public Map<String, Object> addDistributionFav(long companyId, long userId, long distributorId) {
		Long foundId = distributionDistributorLookupMapper.selectDistributorIdById(distributorId);
		if (foundId == null) {
			throw new ResourceException("店铺信息有误");
		}

		LambdaQueryWrapper<MemberDistributionFav> q = new LambdaQueryWrapper<>();
		q.eq(MemberDistributionFav::getCompanyId, companyId)
				.eq(MemberDistributionFav::getUserId, userId)
				.eq(MemberDistributionFav::getDistributorId, distributorId)
				.last("LIMIT 1");
		MemberDistributionFav existing = memberDistributionFavMapper.selectOne(q);
		if (existing != null) {
			return toDistributionFavRowMap(existing);
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		MemberDistributionFav row = new MemberDistributionFav();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setDistributorId(distributorId);
		row.setCreated(nowSec);
		memberDistributionFavMapper.insert(row);
		return toDistributionFavRowMap(row);
	}

	private static Map<String, Object> toDistributionFavRowMap(MemberDistributionFav e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("fav_id", e.getFavId());
		m.put("company_id", e.getCompanyId());
		m.put("user_id", e.getUserId());
		m.put("distributor_id", e.getDistributorId());
		m.put("created", e.getCreated());
		return m;
	}
}
