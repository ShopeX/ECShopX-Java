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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.EntryApply;
import cn.shopex.ecshopx.bspay.domain.UserEnt;
import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import cn.shopex.ecshopx.bspay.support.BsPayEntTypeOptions;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BsPayUserAuditStateService {

	private static final Logger log = LoggerFactory.getLogger(BsPayUserAuditStateService.class);

	private final BsPayOperatorResolveService bsPayOperatorResolveService;
	private final EntryApplyMapper entryApplyMapper;
	private final UserEntMapper userEntMapper;
	private final UserIndvMapper userIndvMapper;

	public BsPayUserAuditStateService(
			BsPayOperatorResolveService bsPayOperatorResolveService,
			EntryApplyMapper entryApplyMapper,
			UserEntMapper userEntMapper,
			UserIndvMapper userIndvMapper) {
		this.bsPayOperatorResolveService = bsPayOperatorResolveService;
		this.entryApplyMapper = entryApplyMapper;
		this.userEntMapper = userEntMapper;
		this.userIndvMapper = userIndvMapper;
	}

	public Map<String, Object> getAuditState(Map<String, Object> jwtMap) {
		long companyId = parseLongSafe(jwtMap.get("company_id"));
		if (companyId <= 0) {
			throw new BadRequestException("缺少有效的 company_id");
		}

		BsPayOperatorResolveService.OperatorContext ctx = bsPayOperatorResolveService.resolve(jwtMap);
		int resolvedOperatorId = ctx.operatorId();
		String resolvedOperatorType = ctx.operatorType();

		LambdaQueryWrapper<EntryApply> w = new LambdaQueryWrapper<>();
		w.eq(EntryApply::getCompanyId, companyId);
		w.eq(EntryApply::getOperatorId, resolvedOperatorId);
		w.eq(EntryApply::getOperatorType, resolvedOperatorType);
		w.orderByDesc(EntryApply::getCreated).last("LIMIT 1");
		EntryApply apply = entryApplyMapper.selectOne(w);

		log.info(
				"getAuditState companyId={} operatorId={} operatorType={} entryApplyHit={}",
				companyId,
				resolvedOperatorId,
				resolvedOperatorType,
				apply != null);

		if (apply == null) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("audit_state", "D");
			empty.put("audit_desc", "待提交");
			empty.put("ent_type_options", BsPayEntTypeOptions.asMap());
			return empty;
		}

		String userType = apply.getUserType() == null ? "" : apply.getUserType().trim();
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("user_type", apply.getUserType());
		result.put("approved_time", apply.getUpdated());

		if ("ent".equals(userType)) {
			UserEnt u = userEntMapper.selectById(parseApplyUserId(apply.getUserId()));
			putUserFields(result, u);
		} else if ("indv".equals(userType)) {
			UserIndv u = userIndvMapper.selectById(parseApplyUserId(apply.getUserId()));
			putUserFieldsIndv(result, u);
		} else {
			throw new ResourceException("获取用户进件信息失败");
		}

		Object rawStateObj = result.get("audit_state");
		String raw = rawStateObj == null ? "" : rawStateObj.toString().trim();
		String mapped;
		if ("0".equals(raw) || "A".equalsIgnoreCase(raw)) {
			mapped = "A";
		} else if ("B".equalsIgnoreCase(raw)
				|| "C".equalsIgnoreCase(raw)
				|| "D".equalsIgnoreCase(raw)) {
			mapped = "B";
		} else if ("E".equalsIgnoreCase(raw)) {
			mapped = "C";
		} else {
			mapped = "A";
		}

		Integer approvedTime = (Integer) result.get("approved_time");
		String auditDescStr = nzObj(result.get("audit_desc"));
		Integer updatedVal = (Integer) result.get("updated");

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("audit_state", mapped);
		res.put("audit_desc", auditDescStr);
		res.put("update_time", approvedTime == null ? null : String.valueOf(approvedTime.intValue()));
		res.put("updated", updatedVal == null ? null : String.valueOf(updatedVal.intValue()));
		res.put("user_type", result.get("user_type"));
		res.put("ent_type_options", BsPayEntTypeOptions.asMap());
		return res;
	}

	private static void putUserFields(Map<String, Object> result, UserEnt u) {
		if (u != null) {
			result.put("audit_state", nz(u.getAuditState()));
			result.put("audit_desc", nz(u.getAuditDesc()));
			result.put("updated", u.getUpdated());
		} else {
			result.put("audit_state", "");
			result.put("audit_desc", "");
			result.put("updated", null);
		}
	}

	private static void putUserFieldsIndv(Map<String, Object> result, UserIndv u) {
		if (u != null) {
			result.put("audit_state", nz(u.getAuditState()));
			result.put("audit_desc", nz(u.getAuditDesc()));
			result.put("updated", u.getUpdated());
		} else {
			result.put("audit_state", "");
			result.put("audit_desc", "");
			result.put("updated", null);
		}
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static String nzObj(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long parseApplyUserId(String raw) {
		if (raw == null) {
			throw new ResourceException("获取用户进件信息失败");
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			throw new ResourceException("获取用户进件信息失败");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("获取用户进件信息失败");
		}
	}

	private static long parseLongSafe(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
