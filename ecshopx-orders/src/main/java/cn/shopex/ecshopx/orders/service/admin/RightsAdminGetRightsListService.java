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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.service.reservation.RightsTimesCardRowFactory;
import cn.shopex.ecshopx.orders.service.rights.export.RightsExportFilterAssembler;
import cn.shopex.ecshopx.orders.service.rights.export.RightsExportQuerySupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RightsAdminGetRightsListService {

	private final RightsExportFilterAssembler rightsExportFilterAssembler;
	private final RightsMapper rightsMapper;
	private final RightsTimesCardRowFactory rightsTimesCardRowFactory;

	public RightsAdminGetRightsListService(
			RightsExportFilterAssembler rightsExportFilterAssembler,
			RightsMapper rightsMapper,
			RightsTimesCardRowFactory rightsTimesCardRowFactory) {
		this.rightsExportFilterAssembler = rightsExportFilterAssembler;
		this.rightsMapper = rightsMapper;
		this.rightsTimesCardRowFactory = rightsTimesCardRowFactory;
	}

	public Map<String, Object> getRightsList(long companyId, HttpServletRequest request) {
		int pageNo = parsePageNo(request.getParameter("page"));
		int pageSize = parsePageSize(request.getParameter("pageSize"));

		String mobile = request.getParameter("mobile");
		String valid = request.getParameter("valid");
		String dateBegin = request.getParameter("date_begin");
		String dateEnd = request.getParameter("date_end");
		String rightsFrom = request.getParameter("rights_from");
		String shopId = request.getParameter("shop_id");

		RightsExportFilterAssembler.Assembly assembly =
				rightsExportFilterAssembler.assemble(
						companyId,
						request,
						mobile,
						null,
						valid,
						dateBegin,
						dateEnd,
						rightsFrom,
						null,
						shopId);

		if (assembly.emptyShopNoMembers()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", List.of());
			empty.put("total_count", 0L);
			return empty;
		}

		LinkedHashMap<String, Object> filter = assembly.filter();
		LambdaQueryWrapper<Rights> countW = RightsExportQuerySupport.toCountWrapper(companyId, filter);
		long totalCount = rightsMapper.selectCount(countW);

		Page<Rights> page = new Page<>(pageNo, pageSize, false);
		rightsMapper.selectPage(page, RightsExportQuerySupport.toRightsListPageWrapper(companyId, filter));

		int nowEpochSec = (int) (System.currentTimeMillis() / 1000L);
		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (Rights r : page.getRecords()) {
			Map<String, Object> row = rightsTimesCardRowFactory.toTimesCardRow(r, nowEpochSec);
			row.put("operator_desc", r.getOperatorDesc());
			row.put("created", r.getCreated());
			row.put("updated", r.getUpdated());
			if (r.getOrderId() == null) {
				row.put("order_id", 0L);
			} else {
				row.put("order_id", r.getOrderId());
			}
			listMaps.add(row);
		}

		if (!listMaps.isEmpty() && isDatapassBlocked(request)) {
			for (Map<String, Object> row : listMaps) {
				Object mob = row.get("mobile");
				if (mob != null) {
					row.put("mobile", DataMasking.maskMobile(String.valueOf(mob)));
				}
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", listMaps);
		data.put("total_count", totalCount);
		return data;
	}

	private static int parsePageNo(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return 1;
		}
		try {
			int p = Integer.parseInt(raw.trim());
			return p < 1 ? 1 : p;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parsePageSize(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return 20;
		}
		try {
			int ps = Integer.parseInt(raw.trim());
			if (ps <= 0) {
				return 20;
			}
			if (ps > 1000) {
				return 1000;
			}
			return ps;
		} catch (NumberFormatException e) {
			return 20;
		}
	}

	private static boolean isDatapassBlocked(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return true;
		}
		if (Boolean.TRUE.equals(attr)) {
			return true;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return true;
			}
		}
		String p = request.getParameter("x-datapass-block");
		if (p == null || p.trim().isEmpty() || "0".equals(p.trim()) || "false".equalsIgnoreCase(p.trim())) {
			return false;
		}
		return true;
	}
}
