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

package cn.shopex.ecshopx.salesperson.service.export;

import cn.shopex.ecshopx.salesperson.domain.ProfitStatistics;
import cn.shopex.ecshopx.salesperson.mapper.ProfitStatisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProfitStatisticsExportQueryService {

	private final ProfitStatisticsMapper profitStatisticsMapper;

	public ProfitStatisticsExportQueryService(ProfitStatisticsMapper profitStatisticsMapper) {
		this.profitStatisticsMapper = profitStatisticsMapper;
	}

	public long countForFilter(long companyId, String dateYm, String profitUserTypeRaw) {
		return profitStatisticsMapper.selectCount(baseWrapper(companyId, dateYm, profitUserTypeRaw));
	}

	public List<ProfitStatistics> pageForFilter(long companyId, String dateYm, String profitUserTypeRaw,
			int pageOneBased, int pageSize) {
		LambdaQueryWrapper<ProfitStatistics> w = baseWrapper(companyId, dateYm, profitUserTypeRaw);
		w.orderByDesc(ProfitStatistics::getId);
		Page<ProfitStatistics> page = new Page<>(pageOneBased, pageSize, false);
		return profitStatisticsMapper.selectPage(page, w).getRecords();
	}

	private LambdaQueryWrapper<ProfitStatistics> baseWrapper(long companyId, String dateYm, String profitUserTypeRaw) {
		LambdaQueryWrapper<ProfitStatistics> w = new LambdaQueryWrapper<>();
		w.eq(ProfitStatistics::getCompanyId, companyId).eq(ProfitStatistics::getDate, dateYm);
		if (profitUserTypeRaw == null || profitUserTypeRaw.trim().isEmpty()) {
			w.isNull(ProfitStatistics::getProfitUserType);
		} else {
			try {
				long v = Long.parseLong(profitUserTypeRaw.trim());
				w.eq(ProfitStatistics::getProfitUserType, v);
			} catch (NumberFormatException e) {
				throw new IllegalStateException("profit_user_type");
			}
		}
		return w;
	}
}
