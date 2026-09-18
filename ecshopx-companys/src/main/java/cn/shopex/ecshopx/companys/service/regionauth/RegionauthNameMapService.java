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

package cn.shopex.ecshopx.companys.service.regionauth;

import cn.shopex.ecshopx.companys.domain.Regionauth;
import cn.shopex.ecshopx.companys.mapper.RegionauthMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RegionauthNameMapService {

	private final RegionauthMapper regionauthMapper;

	public RegionauthNameMapService(RegionauthMapper regionauthMapper) {
		this.regionauthMapper = regionauthMapper;
	}

	public Map<Long, String> mapRegionauthIdToName(long companyId, Collection<Long> regionauthIds) {
		if (regionauthIds == null || regionauthIds.isEmpty()) {
			return new LinkedHashMap<>();
		}
		Set<Long> distinctPositive = new LinkedHashSet<>();
		for (Long id : regionauthIds) {
			if (id != null && id > 0L) {
				distinctPositive.add(id);
			}
		}
		if (distinctPositive.isEmpty()) {
			return new LinkedHashMap<>();
		}
		LambdaQueryWrapper<Regionauth> w = new LambdaQueryWrapper<>();
		w.eq(Regionauth::getCompanyId, companyId)
				.in(Regionauth::getRegionauthId, distinctPositive)
				.in(Regionauth::getState, List.of(0, 1))
				.select(Regionauth::getRegionauthId, Regionauth::getRegionauthName);
		Map<Long, String> out = new LinkedHashMap<>();
		for (Regionauth r : regionauthMapper.selectList(w)) {
			if (r.getRegionauthId() == null) {
				continue;
			}
			String name = r.getRegionauthName() == null ? "" : r.getRegionauthName();
			out.put(r.getRegionauthId(), name);
		}
		return out;
	}
}
