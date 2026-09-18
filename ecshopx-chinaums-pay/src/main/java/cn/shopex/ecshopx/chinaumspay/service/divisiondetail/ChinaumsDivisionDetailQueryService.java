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

package cn.shopex.ecshopx.chinaumspay.service.divisiondetail;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionDetail;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChinaumsDivisionDetailQueryService {

	public static final int DEFAULT_PAGE_SIZE = 500;

	private final ChinaumspayDivisionDetailMapper divisionDetailMapper;

	public ChinaumsDivisionDetailQueryService(ChinaumspayDivisionDetailMapper divisionDetailMapper) {
		this.divisionDetailMapper = divisionDetailMapper;
	}

	public int countByFilter(DivisionDetailExportFilter filter) {
		Long c = divisionDetailMapper.selectCount(toWrapper(filter));
		return c == null ? 0 : c.intValue();
	}

	public List<ChinaumspayDivisionDetail> listPageByFilter(DivisionDetailExportFilter filter, int pageNum,
			int pageSize) {
		LambdaQueryWrapper<ChinaumspayDivisionDetail> w = toWrapper(filter);
		w.orderByDesc(ChinaumspayDivisionDetail::getId);
		Page<ChinaumspayDivisionDetail> page = new Page<>(pageNum, pageSize, false);
		return divisionDetailMapper.selectPage(page, w).getRecords();
	}

	private static LambdaQueryWrapper<ChinaumspayDivisionDetail> toWrapper(DivisionDetailExportFilter filter) {
		LambdaQueryWrapper<ChinaumspayDivisionDetail> w = new LambdaQueryWrapper<>();
		w.eq(ChinaumspayDivisionDetail::getCompanyId, filter.getCompanyId());
		if (filter.getOrderId() != null) {
			w.eq(ChinaumspayDivisionDetail::getOrderId, filter.getOrderId());
		}
		if (filter.getDivisionId() != null) {
			w.eq(ChinaumspayDivisionDetail::getDivisionId, filter.getDivisionId());
		}
		if (filter.getDistributorId() != null) {
			w.eq(ChinaumspayDivisionDetail::getDistributorId, filter.getDistributorId());
		}
		if (StringUtils.hasText(filter.getCreateTimeBegin()) && StringUtils.hasText(filter.getCreateTimeEnd())) {
			w.apply("create_time >= {0}", filter.getCreateTimeBegin());
			w.apply("create_time <= {0}", filter.getCreateTimeEnd());
		}
		return w;
	}
}
