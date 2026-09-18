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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.wechat.WorkWechatDistributorDepartmentSyncPort;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkWechatDistributorDepartmentSyncService implements WorkWechatDistributorDepartmentSyncPort {

	private final DistributorWriteRepository distributorWriteRepository;
	private final DistributorCreateService distributorCreateService;
	private final DistributorUpdateService distributorUpdateService;

	public WorkWechatDistributorDepartmentSyncService(
			DistributorWriteRepository distributorWriteRepository,
			DistributorCreateService distributorCreateService,
			DistributorUpdateService distributorUpdateService) {
		this.distributorWriteRepository = distributorWriteRepository;
		this.distributorCreateService = distributorCreateService;
		this.distributorUpdateService = distributorUpdateService;
	}

	@Override
	public List<Map<String, Object>> syncDepartmentToDistributor(
			long companyId, List<Map<String, Object>> departmentInfo, String requestLangTag) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> v : departmentInfo) {
			int wechatDeptId = parseWeChatDepartmentId(v);
			if (distributorWriteRepository.selectByCompanyIdAndWechatWorkDepartmentId(companyId, wechatDeptId).isPresent()) {
				continue;
			}
			int parentid = intField(v, "parentid");
			int order = intField(v, "order");
			String mobile =
					String.valueOf(wechatDeptId) + "-" + String.valueOf(parentid) + "-" + String.valueOf(order);
			String name = v.get("name") == null ? "" : String.valueOf(v.get("name"));
			Map<String, Object> merged = new LinkedHashMap<>();
			merged.put("company_id", companyId);
			merged.put("wechat_work_department_id", wechatDeptId);
			merged.put("name", name);
			merged.put("is_ziti", true);
			merged.put("is_valid", "true");
			merged.put("mobile", mobile);
			out.add(distributorCreateService.performInsertAndEvents(merged, Collections.emptyMap(), requestLangTag));
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> updateDepartmentToDistributor(
			long companyId, List<Map<String, Object>> departmentInfo, long distributorId, String requestLangTag) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> v : departmentInfo) {
			int wechatDeptId = parseWeChatDepartmentId(v);
			if (distributorWriteRepository.selectByCompanyIdAndWechatWorkDepartmentId(companyId, wechatDeptId).isPresent()) {
				distributorWriteRepository.unbindWechatWorkDepartmentByCompanyAndWechatDeptId(companyId, wechatDeptId);
			}
			Map<String, Object> merged = new LinkedHashMap<>();
			merged.put("company_id", companyId);
			merged.put("wechat_work_department_id", wechatDeptId);
			out.add(distributorUpdateService.performUpdateAndEvents(merged, distributorId, requestLangTag));
		}
		return out;
	}

	private static int parseWeChatDepartmentId(Map<String, Object> v) {
		Object idObj = v.get("id");
		if (idObj == null) {
			throw new BadRequestException("部门 id 无效");
		}
		if (idObj instanceof Number n) {
			int id = n.intValue();
			if (id <= 0) {
				throw new BadRequestException("部门 id 无效");
			}
			return id;
		}
		String s = idObj.toString().trim();
		if (s.isEmpty()) {
			throw new BadRequestException("部门 id 无效");
		}
		try {
			int id = Integer.parseInt(s);
			if (id <= 0) {
				throw new BadRequestException("部门 id 无效");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("部门 id 无效");
		}
	}

	private static int intField(Map<String, Object> v, String key) {
		Object o = v.get(key);
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
