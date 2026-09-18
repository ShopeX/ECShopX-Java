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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.domain.RightsTransferLogs;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.mapper.RightsTransferLogsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RightsAdminTransferRightsListService {

	private final RightsTransferLogsMapper rightsTransferLogsMapper;
	private final RightsMapper rightsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public RightsAdminTransferRightsListService(
			RightsTransferLogsMapper rightsTransferLogsMapper,
			RightsMapper rightsMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.rightsTransferLogsMapper = rightsTransferLogsMapper;
		this.rightsMapper = rightsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> transferRightsList(long companyId, HttpServletRequest request) {
		int pageNo = parsePageNo(request.getParameter("page"));
		int pageSize = parsePageSize(request.getParameter("pageSize"));

		LambdaQueryWrapper<RightsTransferLogs> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RightsTransferLogs::getCompanyId, companyId);

		String rightsIdRaw = request.getParameter("rights_id");
		if (isTruthyOptionalFilterValue(rightsIdRaw)) {
			try {
				long id = Long.parseLong(String.valueOf(rightsIdRaw).trim());
				wrapper.eq(RightsTransferLogs::getRightsId, id);
			} catch (NumberFormatException ignored) {
				// skip invalid filter
			}
		}

		String mobileRaw = request.getParameter("mobile");
		if (isTruthyOptionalFilterValue(mobileRaw)) {
			String plain = String.valueOf(mobileRaw).trim();
			String enc = sensitiveFieldEncryptor.encrypt(plain);
			wrapper.and(
					w ->
							w.eq(RightsTransferLogs::getMobile, enc)
									.or()
									.eq(RightsTransferLogs::getTransferMobile, enc));
		}

		String userIdRaw = request.getParameter("user_id");
		if (isTruthyOptionalFilterValue(userIdRaw)) {
			try {
				long uid = Long.parseLong(String.valueOf(userIdRaw).trim());
				wrapper.and(
						w ->
								w.eq(RightsTransferLogs::getUserId, uid)
										.or()
										.eq(RightsTransferLogs::getTransferUserId, uid));
			} catch (NumberFormatException ignored) {
				// skip invalid filter
			}
		}

		wrapper.orderByDesc(RightsTransferLogs::getCreated);

		long totalCount = rightsTransferLogsMapper.selectCount(wrapper);
		Page<RightsTransferLogs> page = new Page<>(pageNo, pageSize, false);
		rightsTransferLogsMapper.selectPage(page, wrapper);

		LinkedHashSet<Long> rightsIdSet = new LinkedHashSet<>();
		for (RightsTransferLogs log : page.getRecords()) {
			if (log.getRightsId() != null) {
				rightsIdSet.add(log.getRightsId());
			}
		}

		Map<Long, String> idToName;
		if (rightsIdSet.isEmpty()) {
			idToName = Map.of();
		} else {
			List<Long> distinctIds = new ArrayList<>(rightsIdSet);
			LambdaQueryWrapper<Rights> rw =
					Wrappers.<Rights>lambdaQuery()
							.eq(Rights::getCompanyId, companyId)
							.in(Rights::getRightsId, distinctIds)
							.orderByAsc(Rights::getEndTime);
			Page<Rights> namePage = new Page<>(1, 100, false);
			rightsMapper.selectPage(namePage, rw);
			idToName = new LinkedHashMap<>();
			for (Rights r : namePage.getRecords()) {
				idToName.put(r.getRightsId(), r.getRightsName());
			}
		}

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (RightsTransferLogs log : page.getRecords()) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", log.getId());
			row.put("rights_id", log.getRightsId());
			row.put("user_id", log.getUserId());
			row.put("transfer_user_id", log.getTransferUserId());
			row.put("mobile", log.getMobile());
			row.put("transfer_mobile", log.getTransferMobile());
			row.put("company_id", log.getCompanyId());
			row.put("remark", log.getRemark());
			row.put("operator_id", log.getOperatorId());
			row.put("operator", log.getOperator());
			row.put("created", log.getCreated());
			row.put(
					"rights_name",
					log.getRightsId() == null ? null : idToName.get(log.getRightsId()));
			listMaps.add(row);
		}

		if (!listMaps.isEmpty() && isDatapassBlocked(request)) {
			for (Map<String, Object> row : listMaps) {
				Object mob = row.get("mobile");
				if (mob != null) {
					row.put("mobile", maskMobileForDatapass(String.valueOf(mob)));
				}
				Object transferMob = row.get("transfer_mobile");
				if (transferMob != null) {
					row.put("transfer_mobile", maskMobileForDatapass(String.valueOf(transferMob)));
				}
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", listMaps);
		data.put("total_count", totalCount);
		return data;
	}

	private String maskMobileForDatapass(String raw) {
		String decrypted = sensitiveFieldEncryptor.decrypt(raw);
		if (!Objects.equals(raw, decrypted)) {
			return DataMasking.maskMobile(decrypted);
		}
		return DataMasking.maskMobile(raw);
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

	private static boolean isTruthyOptionalFilterValue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = String.valueOf(v).trim();
		return !s.isEmpty() && !"0".equals(s);
	}
}
