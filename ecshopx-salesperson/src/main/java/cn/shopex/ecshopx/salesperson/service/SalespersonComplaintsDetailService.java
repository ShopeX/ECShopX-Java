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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SalespersonComplaintsDetailService {

	private static final Logger log = LoggerFactory.getLogger(SalespersonComplaintsDetailService.class);

	private final SalemanCustomerComplaintMapper salemanCustomerComplaintMapper;

	public SalespersonComplaintsDetailService(SalemanCustomerComplaintMapper salemanCustomerComplaintMapper) {
		this.salemanCustomerComplaintMapper = salemanCustomerComplaintMapper;
	}

	public Map<String, Object> getSalespersonComplaintsDetail(long userId, long companyId, int complaintId) {
		int userIdInt = Math.toIntExact(userId);
		int companyIdInt = Math.toIntExact(companyId);
		try {
			LambdaQueryWrapper<SalemanCustomerComplaint> w = new LambdaQueryWrapper<>();
			w.eq(SalemanCustomerComplaint::getUserId, userIdInt)
					.eq(SalemanCustomerComplaint::getCompanyId, companyIdInt)
					.eq(SalemanCustomerComplaint::getId, complaintId)
					.last("LIMIT 1");
			SalemanCustomerComplaint row = salemanCustomerComplaintMapper.selectOne(w);
			if (row == null) {
				return null;
			}
			return SalemanCustomerComplaintSwgRowConverter.toRow(row);
		} catch (Exception e) {
			log.error("salesperson complaints detail query failed", e);
			String msg = e.getMessage();
			if (msg != null && !msg.isEmpty()) {
				throw new ResourceException(msg);
			}
			throw new ResourceException("查询失败");
		}
	}
}
