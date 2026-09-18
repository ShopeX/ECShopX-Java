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

import cn.shopex.ecshopx.adapay.domain.AdapayAlipayIndustryCategory;
import cn.shopex.ecshopx.adapay.domain.dto.AdapayAlipayIndustryCategoryTreeNodeDto;
import cn.shopex.ecshopx.adapay.mapper.AdapayAlipayIndustryCategoryMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdapayAlipayIndustryCategoryTreeService {

	private final AdapayAlipayIndustryCategoryMapper categoryMapper;

	public AdapayAlipayIndustryCategoryTreeService(AdapayAlipayIndustryCategoryMapper categoryMapper) {
		this.categoryMapper = categoryMapper;
	}

	public List<AdapayAlipayIndustryCategoryTreeNodeDto> getAlipayIndustryCatList() {
		List<AdapayAlipayIndustryCategory> all = categoryMapper.selectList(null);
		if (all == null) {
			return Collections.emptyList();
		}
		return buildTree(all, 0L);
	}

	private List<AdapayAlipayIndustryCategoryTreeNodeDto> buildTree(List<AdapayAlipayIndustryCategory> all, long pid) {
		List<AdapayAlipayIndustryCategoryTreeNodeDto> result = new ArrayList<>();
		for (AdapayAlipayIndustryCategory v : all) {
			long rowParentId = (v.getParentId() == null) ? 0L : v.getParentId();
			if (rowParentId != pid) {
				continue;
			}
			AdapayAlipayIndustryCategoryTreeNodeDto dto = new AdapayAlipayIndustryCategoryTreeNodeDto();
			dto.setId(v.getId());
			dto.setCategoryName(v.getCategoryName());
			dto.setParentId(v.getParentId());
			dto.setCategoryLevel(v.getCategoryLevel());
			dto.setAlipayClsId(v.getAlipayClsId());
			dto.setAlipayCategoryId(v.getAlipayCategoryId());

			if (v.getCategoryLevel() != null && v.getCategoryLevel() == 3) {
				dto.setChildren(null);
			} else {
				Long nodeId = v.getId();
				long childPid = nodeId == null ? 0L : nodeId;
				List<AdapayAlipayIndustryCategoryTreeNodeDto> childList = buildTree(all, childPid);
				dto.setChildren(childList);
			}
			result.add(dto);
		}
		return result;
	}
}
