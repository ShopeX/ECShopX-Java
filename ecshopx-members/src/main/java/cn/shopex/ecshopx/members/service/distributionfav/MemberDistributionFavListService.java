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

import cn.shopex.ecshopx.common.members.port.MemberDistributionShopListByIdsPort;
import cn.shopex.ecshopx.members.domain.MemberDistributionFav;
import cn.shopex.ecshopx.members.mapper.MemberDistributionFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberDistributionFavListService {

	private final MemberDistributionFavMapper memberDistributionFavMapper;
	private final MemberDistributionShopListByIdsPort memberDistributionShopListByIdsPort;

	public MemberDistributionFavListService(
			MemberDistributionFavMapper memberDistributionFavMapper,
			MemberDistributionShopListByIdsPort memberDistributionShopListByIdsPort) {
		this.memberDistributionFavMapper = memberDistributionFavMapper;
		this.memberDistributionShopListByIdsPort = memberDistributionShopListByIdsPort;
	}

	public Object getDistributionFavList(
			long companyId, long userId, int page, int pageSize, String requestLang) {
		LambdaQueryWrapper<MemberDistributionFav> w = new LambdaQueryWrapper<>();
		w.eq(MemberDistributionFav::getCompanyId, companyId)
				.eq(MemberDistributionFav::getUserId, userId)
				.orderByDesc(MemberDistributionFav::getFavId);
		long favTotal = memberDistributionFavMapper.selectCount(w);
		Page<MemberDistributionFav> mp = new Page<>(page, pageSize, false);
		memberDistributionFavMapper.selectPage(mp, w);
		List<MemberDistributionFav> records = mp.getRecords();
		if (records == null || records.isEmpty()) {
			return Collections.emptyList();
		}

		List<Long> orderedIds = new ArrayList<>();
		for (MemberDistributionFav r : records) {
			Long did = r.getDistributorId();
			if (did != null && did > 0L) {
				orderedIds.add(did);
			}
		}

		if (orderedIds.isEmpty()) {
			return emptyShopBody(favTotal);
		}

		Map<String, Object> shopPayload =
				memberDistributionShopListByIdsPort.listValidShopByDistributorIds(
						companyId, orderedIds, requestLang);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) shopPayload.get("list");
		if (list == null) {
			list = List.of();
		}

		if (list.isEmpty()) {
			return emptyShopBody(favTotal);
		}

		Object tcObj = shopPayload.get("total_count");
		long shopTotalCount = tcObj instanceof Number ? ((Number) tcObj).longValue() : 0L;
		if (shopTotalCount > 0L) {
			for (Map<String, Object> row : list) {
				Object didObj = row.get("distributor_id");
				if (!(didObj instanceof Number)) {
					continue;
				}
				long distId = ((Number) didObj).longValue();
				LambdaQueryWrapper<MemberDistributionFav> c = new LambdaQueryWrapper<>();
				c.eq(MemberDistributionFav::getCompanyId, companyId)
						.eq(MemberDistributionFav::getDistributorId, distId);
				long favNum = memberDistributionFavMapper.selectCount(c);
				row.put("fav_num", favNum);
			}
		}

		return shopPayload;
	}

	private static Map<String, Object> emptyShopBody(long favTotal) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("total_count", favTotal);
		m.put("list", List.of());
		return m;
	}
}
