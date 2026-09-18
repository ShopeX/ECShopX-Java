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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.domain.CommunityChiefApplyInfo;
import cn.shopex.ecshopx.community.mapper.CommunityChiefApplyInfoMapper;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsApplicationService;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommunityChiefApplySubmitService {

	private final CommunityChiefApplyInfoMapper communityChiefApplyInfoMapper;
	private final CommunityChiefService communityChiefService;
	private final ConfigRequestFieldsApplicationService configRequestFieldsApplicationService;
	private final ObjectMapper objectMapper;

	public CommunityChiefApplySubmitService(
			CommunityChiefApplyInfoMapper communityChiefApplyInfoMapper,
			CommunityChiefService communityChiefService,
			ConfigRequestFieldsApplicationService configRequestFieldsApplicationService,
			ObjectMapper objectMapper) {
		this.communityChiefApplyInfoMapper = communityChiefApplyInfoMapper;
		this.communityChiefService = communityChiefService;
		this.configRequestFieldsApplicationService = configRequestFieldsApplicationService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> submit(
			long companyId,
			long userId,
			String authMobile,
			int distributorId,
			Map<String, Object> mergedInput,
			String acceptLanguageHeader) {
		long pending =
				communityChiefApplyInfoMapper.selectCount(
						new LambdaQueryWrapper<CommunityChiefApplyInfo>()
								.eq(CommunityChiefApplyInfo::getCompanyId, companyId)
								.eq(CommunityChiefApplyInfo::getUserId, userId)
								.eq(CommunityChiefApplyInfo::getDistributorId, distributorId)
								.ne(CommunityChiefApplyInfo::getApproveStatus, 2));
		if (pending > 0L) {
			throw new ResourceException("不能重复申请");
		}
		if (communityChiefService.countByCompanyAndUserId(companyId, userId) > 0L) {
			throw new ResourceException("已经是团长，不能再申请");
		}

		LinkedHashMap<String, Object> params = new LinkedHashMap<>(mergedInput);
		int companyIdInt = toIntCompanyId(companyId);
		configRequestFieldsApplicationService.checkIsNeedInit(
				companyIdInt, ConfigRequestFieldsConstants.MODULE_TYPE_CHIEF_INFO, distributorId);
		List<Map<String, Object>> fieldDefs =
				configRequestFieldsApplicationService.listAllChiefApplyFieldsHandled(
						companyIdInt, distributorId, acceptLanguageHeader);
		configRequestFieldsApplicationService.transformChiefApplyValuesByDesc(fieldDefs, params);
		configRequestFieldsApplicationService.validateChiefApplyLazy(fieldDefs, params);

		Map<String, Map<String, Object>> extraNested = buildExtraDataNested(fieldDefs, params);
		params.put("extra_data", extraNested);
		params.put("company_id", companyId);
		params.put("user_id", userId);
		params.put("distributor_id", distributorId);
		params.put("chief_mobile", authMobile);

		CommunityChiefApplyInfo entity = new CommunityChiefApplyInfo();
		entity.setCompanyId(companyId);
		entity.setUserId(userId);
		entity.setDistributorId(distributorId);
		Object chiefNameRaw = params.get("chief_name");
		entity.setChiefName(chiefNameRaw != null ? String.valueOf(chiefNameRaw).trim() : null);
		entity.setChiefMobile(authMobile);
		try {
			entity.setExtraData(objectMapper.writeValueAsString(extraNested));
		} catch (JsonProcessingException e) {
			throw new ResourceException("附加信息序列化失败");
		}
		entity.setApproveStatus(0);
		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);

		communityChiefApplyInfoMapper.insert(entity);
		Long applyId = entity.getApplyId();
		if (applyId == null || applyId <= 0L) {
			throw new ResourceException("申请提交失败");
		}
		return toResponseMap(entity, extraNested);
	}

	private static int toIntCompanyId(long companyId) {
		if (companyId <= 0L || companyId > Integer.MAX_VALUE) {
			throw new BadRequestException("参数错误");
		}
		return (int) companyId;
	}

	private static Map<String, Map<String, Object>> buildExtraDataNested(
			List<Map<String, Object>> fieldDefs, Map<String, Object> params) {
		Map<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> field : fieldDefs) {
			String keyName = stringVal(field.get("key_name"));
			if (keyName.isEmpty() || !params.containsKey(keyName)) {
				continue;
			}
			if (intVal(field.get("is_default"), 0) != 0) {
				continue;
			}
			LinkedHashMap<String, Object> cell = new LinkedHashMap<>();
			cell.put("label", stringVal(field.get("label")));
			cell.put("value", params.get(keyName));
			cell.put("type", intVal(field.get("field_type"), 0));
			out.put(keyName, cell);
		}
		return out;
	}

	private static Map<String, Object> toResponseMap(
			CommunityChiefApplyInfo entity, Map<String, Map<String, Object>> extraNested) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("apply_id", entity.getApplyId());
		m.put("company_id", entity.getCompanyId());
		m.put("distributor_id", entity.getDistributorId());
		m.put("user_id", entity.getUserId());
		m.put("chief_name", entity.getChiefName());
		m.put("chief_mobile", entity.getChiefMobile());
		m.put("extra_data", extraNested);
		m.put("approve_status", entity.getApproveStatus());
		m.put("refuse_reason", entity.getRefuseReason());
		return m;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static int intVal(Object v, int defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
