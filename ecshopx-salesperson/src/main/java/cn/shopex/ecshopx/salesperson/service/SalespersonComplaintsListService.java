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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.SalemanCustomerComplaint;
import cn.shopex.ecshopx.salesperson.mapper.SalemanCustomerComplaintMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SalespersonComplaintsListService {

	private static final Logger log = LoggerFactory.getLogger(SalespersonComplaintsListService.class);

	private final SalemanCustomerComplaintMapper salemanCustomerComplaintMapper;

	public SalespersonComplaintsListService(SalemanCustomerComplaintMapper salemanCustomerComplaintMapper) {
		this.salemanCustomerComplaintMapper = salemanCustomerComplaintMapper;
	}

	public Map<String, Object> getSalespersonComplaintsList(long userId, long companyId, int page, int pageSize) {
		int userIdInt = Math.toIntExact(userId);
		int companyIdInt = Math.toIntExact(companyId);
		try {
			LambdaQueryWrapper<SalemanCustomerComplaint> wrapper = new LambdaQueryWrapper<>();
			wrapper.eq(SalemanCustomerComplaint::getUserId, userIdInt)
					.eq(SalemanCustomerComplaint::getCompanyId, companyIdInt);

			long total = salemanCustomerComplaintMapper.selectCount(wrapper);
			if (total == 0) {
				LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
				empty.put("total_count", 0);
				empty.put("list", Collections.emptyList());
				return empty;
			}

			wrapper.orderByDesc(SalemanCustomerComplaint::getCreated);
			Page<SalemanCustomerComplaint> mpPage = new Page<>(page, pageSize, false);
			IPage<SalemanCustomerComplaint> result = salemanCustomerComplaintMapper.selectPage(mpPage, wrapper);

			List<Map<String, Object>> rows = new ArrayList<>(result.getRecords().size());
			for (SalemanCustomerComplaint e : result.getRecords()) {
				rows.add(SalemanCustomerComplaintSwgRowConverter.toRow(e));
			}

			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", (int) total);
			out.put("list", rows);
			return out;
		} catch (Exception e) {
			log.error("salesperson complaints list query failed", e);
			String msg = e.getMessage();
			if (msg != null && !msg.isEmpty()) {
				throw new ResourceException(msg);
			}
			throw new ResourceException("查询失败");
		}
	}
}
