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
import org.springframework.stereotype.Service;

@Service
public class SalemanCustomerComplaintListService {

	private final SalemanCustomerComplaintMapper salemanCustomerComplaintMapper;

	public SalemanCustomerComplaintListService(SalemanCustomerComplaintMapper salemanCustomerComplaintMapper) {
		this.salemanCustomerComplaintMapper = salemanCustomerComplaintMapper;
	}

	public Map<String, Object> getSalemanCustomerComplaintsList(long companyId, String userNameContains,
			String userMobileContains, String salemanNameContains, String salemanMobileContains,
			String replyStatusEquals, String pageTrimmed, String pageSizeTrimmed) {
		LambdaQueryWrapper<SalemanCustomerComplaint> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SalemanCustomerComplaint::getCompanyId, (int) companyId);
		if (userNameContains != null) {
			wrapper.like(SalemanCustomerComplaint::getUserName, "%" + userNameContains + "%");
		}
		if (userMobileContains != null) {
			wrapper.like(SalemanCustomerComplaint::getUserMobile, "%" + userMobileContains + "%");
		}
		if (salemanNameContains != null) {
			wrapper.like(SalemanCustomerComplaint::getSalemanName, "%" + salemanNameContains + "%");
		}
		if (salemanMobileContains != null) {
			wrapper.like(SalemanCustomerComplaint::getSalemanMobile, "%" + salemanMobileContains + "%");
		}
		if (replyStatusEquals != null) {
			wrapper.eq(SalemanCustomerComplaint::getReplyStatus, parseReplyStatus(replyStatusEquals));
		}

		long total = salemanCustomerComplaintMapper.selectCount(wrapper);
		if (total == 0) {
			return Map.of("total_count", 0, "list", Collections.emptyList());
		}

		int pageVal = trimmedStringToIntOrZero(pageTrimmed);
		int pageSizeVal = trimmedStringToIntOrZero(pageSizeTrimmed);

		List<SalemanCustomerComplaint> entities;
		if (pageSizeVal > 0) {
			Page<SalemanCustomerComplaint> mpPage = new Page<>(pageVal, pageSizeVal, false);
			IPage<SalemanCustomerComplaint> result = salemanCustomerComplaintMapper.selectPage(mpPage, wrapper);
			entities = result.getRecords();
		} else {
			entities = salemanCustomerComplaintMapper.selectList(wrapper);
		}

		List<Map<String, Object>> rows = new ArrayList<>(entities.size());
		for (SalemanCustomerComplaint e : entities) {
			rows.add(SalemanCustomerComplaintSwgRowConverter.toRow(e));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", (int) total);
		out.put("list", rows);
		return out;
	}

	/**
	 * Aligns with filter literal for reply_status; only call when a non-empty query value is present.
	 */
	private static Boolean parseReplyStatus(String raw) {
		if ("0".equals(raw)) {
			return Boolean.FALSE;
		}
		if ("1".equals(raw)) {
			return Boolean.TRUE;
		}
		try {
			int parsed = Integer.parseInt(raw);
			return parsed != 0;
		} catch (NumberFormatException e) {
			return Boolean.FALSE;
		}
	}

	private static int trimmedStringToIntOrZero(String s) {
		if (s == null) {
			return 0;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
