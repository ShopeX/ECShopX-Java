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

import cn.shopex.ecshopx.adapay.domain.AdapayBankCodes;
import cn.shopex.ecshopx.adapay.domain.dto.AdapayBankCodesListRowDto;
import cn.shopex.ecshopx.adapay.mapper.AdapayBankCodesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayBankCodesQueryService {

	private final AdapayBankCodesMapper adapayBankCodesMapper;

	public AdapayBankCodesQueryService(AdapayBankCodesMapper adapayBankCodesMapper) {
		this.adapayBankCodesMapper = adapayBankCodesMapper;
	}

	public List<AdapayBankCodesListRowDto> getBanksLists(String bankName, Integer page, Integer pageSize) {
		int pageVal = (page == null) ? 1 : page.intValue();
		int pageSizeVal = (pageSize == null) ? 20 : pageSize.intValue();

		LambdaQueryWrapper<AdapayBankCodes> wrapper = new LambdaQueryWrapper<>();
		if (bankName != null && !bankName.isEmpty()) {
			wrapper.like(AdapayBankCodes::getBankName, bankName);
		}

		List<AdapayBankCodes> rows;
		if (pageSizeVal > 0) {
			Page<AdapayBankCodes> p = new Page<>(pageVal, pageSizeVal, false);
			adapayBankCodesMapper.selectPage(p, wrapper);
			rows = p.getRecords();
		} else {
			rows = adapayBankCodesMapper.selectList(wrapper);
		}

		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}

		List<AdapayBankCodesListRowDto> out = new ArrayList<>(rows.size());
		for (AdapayBankCodes e : rows) {
			AdapayBankCodesListRowDto dto = new AdapayBankCodesListRowDto();
			dto.setId(e.getId());
			dto.setBankName(e.getBankName());
			dto.setBankCode(e.getBankCode());
			out.add(dto);
		}
		return out;
	}

	public Map<String, Object> getBanksListsPost(String bankName, Integer page, Integer pageSize) {
		int pageVal = (page == null) ? 1 : page.intValue();
		int pageSizeVal = (pageSize == null) ? 20 : pageSize.intValue();

		LambdaQueryWrapper<AdapayBankCodes> wrapper = new LambdaQueryWrapper<>();
		if (StringUtils.hasText(bankName)) {
			wrapper.like(AdapayBankCodes::getBankName, bankName);
		}

		long total = adapayBankCodesMapper.selectCount(wrapper);

		List<AdapayBankCodesListRowDto> list;
		if (total == 0) {
			list = Collections.emptyList();
		} else if (pageSizeVal > 0) {
			Page<AdapayBankCodes> p = new Page<>(pageVal, pageSizeVal, false);
			adapayBankCodesMapper.selectPage(p, wrapper);
			List<AdapayBankCodes> rows = p.getRecords();
			if (rows == null || rows.isEmpty()) {
				list = Collections.emptyList();
			} else {
				list = new ArrayList<>(rows.size());
				for (AdapayBankCodes e : rows) {
					AdapayBankCodesListRowDto dto = new AdapayBankCodesListRowDto();
					dto.setId(e.getId());
					dto.setBankName(e.getBankName());
					dto.setBankCode(e.getBankCode());
					list.add(dto);
				}
			}
		} else {
			List<AdapayBankCodes> rows = adapayBankCodesMapper.selectList(wrapper);
			if (rows == null || rows.isEmpty()) {
				list = Collections.emptyList();
			} else {
				list = new ArrayList<>(rows.size());
				for (AdapayBankCodes e : rows) {
					AdapayBankCodesListRowDto dto = new AdapayBankCodesListRowDto();
					dto.setId(e.getId());
					dto.setBankName(e.getBankName());
					dto.setBankCode(e.getBankCode());
					list.add(dto);
				}
			}
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}
}
