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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayRegionsThird;
import cn.shopex.ecshopx.adapay.domain.dto.AdapayRegionsListRowDto;
import cn.shopex.ecshopx.adapay.mapper.AdapayRegionsThirdMapper;
import cn.shopex.ecshopx.adapay.util.AdapayRegionPidParsing;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdapayRegionsThirdQueryService {

	private final AdapayRegionsThirdMapper adapayRegionsThirdMapper;

	public AdapayRegionsThirdQueryService(AdapayRegionsThirdMapper adapayRegionsThirdMapper) {
		this.adapayRegionsThirdMapper = adapayRegionsThirdMapper;
	}

	public List<AdapayRegionsListRowDto> getRegionsThirdLists(String pidParam) {
		long pidLong = AdapayRegionPidParsing.parseLoosePid(pidParam);

		LambdaQueryWrapper<AdapayRegionsThird> w =
				new LambdaQueryWrapper<AdapayRegionsThird>()
						.eq(AdapayRegionsThird::getPid, pidLong)
						.orderByAsc(AdapayRegionsThird::getId);
		List<AdapayRegionsThird> rows = adapayRegionsThirdMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}

		List<AdapayRegionsListRowDto> out = new ArrayList<>(rows.size());
		for (AdapayRegionsThird e : rows) {
			AdapayRegionsListRowDto dto = new AdapayRegionsListRowDto();
			dto.setId(e.getId());
			dto.setAreaName(e.getAreaName());
			dto.setPid(e.getPid());
			dto.setAreaCode(e.getAreaCode());
			out.add(dto);
		}
		return out;
	}
}
