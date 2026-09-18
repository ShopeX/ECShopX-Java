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

package cn.shopex.ecshopx.chinaumspay.service.divisionlist;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivision;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChinaumsDivisionListExportQueryService {

	public static final int PAGE_SIZE = 500;

	private final ChinaumspayDivisionMapper divisionMapper;

	public ChinaumsDivisionListExportQueryService(ChinaumspayDivisionMapper divisionMapper) {
		this.divisionMapper = divisionMapper;
	}

	public int countByFilter(DivisionListExportFilter filter) {
		Long c = divisionMapper.selectCount(toWrapper(filter));
		return c == null ? 0 : c.intValue();
	}

	public List<ChinaumspayDivision> listPageByFilter(DivisionListExportFilter filter, int pageNum, int pageSize) {
		LambdaQueryWrapper<ChinaumspayDivision> w = toWrapper(filter);
		w.orderByDesc(ChinaumspayDivision::getId);
		Page<ChinaumspayDivision> page = new Page<>(pageNum, pageSize, false);
		return divisionMapper.selectPage(page, w).getRecords();
	}

	private static LambdaQueryWrapper<ChinaumspayDivision> toWrapper(DivisionListExportFilter filter) {
		LambdaQueryWrapper<ChinaumspayDivision> w = new LambdaQueryWrapper<>();
		w.eq(ChinaumspayDivision::getCompanyId, filter.getCompanyId());
		if (StringUtils.hasText(filter.getBackStatus())) {
			w.eq(ChinaumspayDivision::getBackStatus, filter.getBackStatus());
		}
		if (StringUtils.hasText(filter.getCreateTimeBegin()) && StringUtils.hasText(filter.getCreateTimeEnd())) {
			w.apply("create_time >= {0}", filter.getCreateTimeBegin());
			w.apply("create_time <= {0}", filter.getCreateTimeEnd());
		}
		if (filter.getDistributorId() != null) {
			w.eq(ChinaumspayDivision::getDistributorId, filter.getDistributorId());
		}
		return w;
	}
}
