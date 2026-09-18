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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RegionauthListService {

	private final RegionauthMapper regionauthMapper;

	public RegionauthListService(RegionauthMapper regionauthMapper) {
		this.regionauthMapper = regionauthMapper;
	}

	public Map<String, Object> getlist(
			long companyId,
			Integer page,
			Integer pageSize,
			Long regionauthIdOrNull,
			Integer stateOrNull,
			boolean stateParamPresent) {
		LambdaQueryWrapper<Regionauth> w = new LambdaQueryWrapper<>();
		w.eq(Regionauth::getCompanyId, companyId);
		if (regionauthIdOrNull != null) {
			w.eq(Regionauth::getRegionauthId, regionauthIdOrNull);
		}
		if (!stateParamPresent) {
			w.in(Regionauth::getState, List.of(0, 1));
		} else {
			w.eq(Regionauth::getState, stateOrNull);
		}

		long total = regionauthMapper.selectCount(w);

		List<Regionauth> rows;
		if (total == 0L) {
			rows = Collections.emptyList();
		} else {
			w.orderByDesc(Regionauth::getCreated);
			if (pageSize != null && pageSize > 0) {
				int currentPage = (page != null && page > 0) ? page.intValue() : 1;
				Page<Regionauth> p = new Page<>(currentPage, pageSize.intValue(), false);
				regionauthMapper.selectPage(p, w);
				rows = p.getRecords();
			} else {
				rows = regionauthMapper.selectList(w);
			}
		}

		List<Map<String, Object>> listMaps = new ArrayList<>(rows.size());
		for (Regionauth entity : rows) {
			LinkedHashMap<String, Object> line = new LinkedHashMap<>();
			line.put("regionauth_id", entity.getRegionauthId());
			line.put("regionauth_name", entity.getRegionauthName());
			line.put("state", entity.getState());
			line.put("company_id", entity.getCompanyId());
			line.put("created", entity.getCreated());
			line.put("updated", entity.getUpdated());
			listMaps.add(line);
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", Long.valueOf(total));
		data.put("list", listMaps);
		return data;
	}
}
