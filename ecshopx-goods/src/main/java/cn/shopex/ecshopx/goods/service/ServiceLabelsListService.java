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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.goods.domain.ServiceLabels;
import cn.shopex.ecshopx.goods.mapper.ServiceLabelsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ServiceLabelsListService {

	private final ServiceLabelsMapper mapper;

	public ServiceLabelsListService(ServiceLabelsMapper mapper) {
		this.mapper = mapper;
	}

	public Map<String, Object> getServiceLabelsList(
			long companyId,
			String serviceType,
			String labelNameExactOrNull,
			String keywordsContainsOrNull,
			long page,
			long pageSize) {
		long p = page < 1L ? 1L : page;
		long ps = pageSize;
		if (ps > 100L) {
			ps = 100L;
		}
		if (ps <= 0L) {
			ps = 10L;
		}

		LambdaQueryWrapper<ServiceLabels> w = Wrappers.lambdaQuery();
		w.eq(ServiceLabels::getCompanyId, companyId);
		w.eq(ServiceLabels::getServiceType, serviceType);
		if (StringUtils.hasText(keywordsContainsOrNull)) {
			w.like(ServiceLabels::getLabelName, keywordsContainsOrNull.trim());
		} else if (StringUtils.hasText(labelNameExactOrNull)) {
			w.eq(ServiceLabels::getLabelName, labelNameExactOrNull.trim());
		}
		w.orderByDesc(ServiceLabels::getLabelId);

		Page<ServiceLabels> mpPage = new Page<>(p, ps);
		mapper.selectPage(mpPage, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (ServiceLabels e : mpPage.getRecords()) {
			list.add(toListRow(e));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", mpPage.getTotal());
		out.put("list", list);
		return out;
	}

	private static Map<String, Object> toListRow(ServiceLabels e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("labelId", e.getLabelId());
		m.put("labelName", e.getLabelName());
		m.put("serviceType", e.getServiceType());
		m.put("companyId", e.getCompanyId());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("labelDesc", e.getLabelDesc() != null ? e.getLabelDesc() : "");
		m.put("labelPrice", e.getLabelPrice());
		return m;
	}
}
