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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.dto.PromoterAlipayInfoRow;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterAlipayInfoQueryService {

	private final PromoterMapper promoterMapper;

	public PromoterAlipayInfoQueryService(PromoterMapper promoterMapper) {
		this.promoterMapper = promoterMapper;
	}

	public Map<String, PromoterAlipayInfoRow> mapAlipayByCompanyAndUserIds(long companyId, List<String> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<Long> idSet = new LinkedHashSet<>();
		for (String uid : userIds) {
			if (!StringUtils.hasText(uid)) {
				continue;
			}
			try {
				idSet.add(Long.parseLong(uid.trim()));
			} catch (NumberFormatException ignored) {
				// skip non-numeric
			}
		}
		if (idSet.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Long> parsedLongs = new ArrayList<>(idSet);
		LambdaQueryWrapper<Promoter> w = new LambdaQueryWrapper<>();
		w.eq(Promoter::getCompanyId, companyId).in(Promoter::getUserId, parsedLongs);
		List<Promoter> rows = promoterMapper.selectList(w);
		Map<String, PromoterAlipayInfoRow> out = new LinkedHashMap<>();
		for (Promoter p : rows) {
			if (p.getUserId() == null) {
				continue;
			}
			PromoterAlipayInfoRow row = new PromoterAlipayInfoRow();
			row.setAlipayName(p.getAlipayName());
			row.setAlipayAccount(p.getAlipayAccount());
			out.put(String.valueOf(p.getUserId()), row);
		}
		return out;
	}
}
