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

package cn.shopex.ecshopx.crossborder.service;

import cn.shopex.ecshopx.crossborder.api.admin.v1.dto.CrossBorderSetInfoDto;
import cn.shopex.ecshopx.crossborder.domain.CrossBorderSet;
import cn.shopex.ecshopx.crossborder.mapper.CrossBorderSetMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CrossBorderSetInfoQueryService {

	private final CrossBorderSetMapper crossBorderSetMapper;

	public CrossBorderSetInfoQueryService(CrossBorderSetMapper crossBorderSetMapper) {
		this.crossBorderSetMapper = crossBorderSetMapper;
	}

	public Optional<CrossBorderSetInfoDto> findByCompanyId(long companyId) {
		CrossBorderSet row = crossBorderSetMapper.selectOne(
				Wrappers.<CrossBorderSet>lambdaQuery()
						.eq(CrossBorderSet::getCompanyId, companyId)
						.last("LIMIT 1"));
		if (row == null) {
			return Optional.empty();
		}
		CrossBorderSetInfoDto dto = new CrossBorderSetInfoDto();
		dto.setTaxRate(row.getTaxRate());
		dto.setQuotaTip(row.getQuotaTip());
		dto.setCrossborderShow(row.getCrossborderShow());
		dto.setLogistics(row.getLogistics());
		return Optional.of(dto);
	}
}
