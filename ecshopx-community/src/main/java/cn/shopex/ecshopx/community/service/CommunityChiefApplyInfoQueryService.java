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

import cn.shopex.ecshopx.community.domain.CommunityChiefApplyInfo;
import cn.shopex.ecshopx.community.mapper.CommunityChiefApplyInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommunityChiefApplyInfoQueryService {

	private final CommunityChiefApplyInfoMapper communityChiefApplyInfoMapper;
	private final ObjectMapper objectMapper;

	public CommunityChiefApplyInfoQueryService(
			CommunityChiefApplyInfoMapper communityChiefApplyInfoMapper, ObjectMapper objectMapper) {
		this.communityChiefApplyInfoMapper = communityChiefApplyInfoMapper;
		this.objectMapper = objectMapper;
	}

	public Object getApplyInfoForAdmin(
			long companyId, String operatorType, Integer distributorIdFilter, long applyId) {
		LambdaQueryWrapper<CommunityChiefApplyInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(CommunityChiefApplyInfo::getCompanyId, companyId)
				.eq(CommunityChiefApplyInfo::getApplyId, applyId);
		if (!"distributor".equals(operatorType)) {
			wrapper.eq(CommunityChiefApplyInfo::getDistributorId, 0);
		} else if (distributorIdFilter == null) {
			wrapper.isNull(CommunityChiefApplyInfo::getDistributorId);
		} else {
			wrapper.eq(CommunityChiefApplyInfo::getDistributorId, distributorIdFilter);
		}
		wrapper.last("LIMIT 1");

		CommunityChiefApplyInfo entity = communityChiefApplyInfoMapper.selectOne(wrapper);
		if (entity == null) {
			return Collections.emptyList();
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("apply_id", entity.getApplyId());
		out.put("company_id", entity.getCompanyId());
		out.put("distributor_id", entity.getDistributorId());
		out.put("user_id", entity.getUserId());
		out.put("chief_name", entity.getChiefName());
		out.put("chief_mobile", entity.getChiefMobile());
		out.put("extra_data", parseExtraData(entity.getExtraData()));
		out.put("approve_status", entity.getApproveStatus());
		out.put("refuse_reason", entity.getRefuseReason());
		return out;
	}

	/**
	 * 管理端团长申请分页列表（只读 {@code community_chief_apply_info}）。
	 *
	 * @param distributorIdFilter 非 distributor 场景由调用方传 {@code 0}（此处不使用）；distributor 下 {@code null} 表示未传或解析失败，对应 {@code IS
	 *     NULL}
	 */
	public Map<String, Object> listApplyForAdmin(
			long companyId,
			String operatorType,
			Integer distributorIdFilter,
			int page,
			int pageSize,
			boolean restrictToPendingApproval,
			String chiefNameOrNull,
			String chiefMobileOrNull) {
		int pg = page < 1 ? 1 : page;
		int sz = pageSize <= 0 ? 10 : pageSize;

		LambdaQueryWrapper<CommunityChiefApplyInfo> countWrapper = new LambdaQueryWrapper<>();
		applyAdminApplyListFilters(
				countWrapper,
				companyId,
				operatorType,
				distributorIdFilter,
				restrictToPendingApproval,
				chiefNameOrNull,
				chiefMobileOrNull);
		long total = communityChiefApplyInfoMapper.selectCount(countWrapper);

		List<Map<String, Object>> rows = new ArrayList<>();
		if (total > 0) {
			LambdaQueryWrapper<CommunityChiefApplyInfo> listWrapper = new LambdaQueryWrapper<>();
			applyAdminApplyListFilters(
					listWrapper,
					companyId,
					operatorType,
					distributorIdFilter,
					restrictToPendingApproval,
					chiefNameOrNull,
					chiefMobileOrNull);
			listWrapper
					.select(
							CommunityChiefApplyInfo::getApplyId,
							CommunityChiefApplyInfo::getUserId,
							CommunityChiefApplyInfo::getChiefName,
							CommunityChiefApplyInfo::getChiefMobile,
							CommunityChiefApplyInfo::getApproveStatus,
							CommunityChiefApplyInfo::getCreatedAt)
					.orderByDesc(CommunityChiefApplyInfo::getCreatedAt);

			Page<CommunityChiefApplyInfo> mpPage = new Page<>(pg, sz, false);
			communityChiefApplyInfoMapper.selectPage(mpPage, listWrapper);
			for (CommunityChiefApplyInfo e : mpPage.getRecords()) {
				rows.add(toApplyListRow(e));
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", (int) total);
		data.put("list", rows);
		return data;
	}

	private static void applyAdminApplyListFilters(
			LambdaQueryWrapper<CommunityChiefApplyInfo> wrapper,
			long companyId,
			String operatorType,
			Integer distributorIdFilter,
			boolean restrictToPendingApproval,
			String chiefNameOrNull,
			String chiefMobileOrNull) {
		wrapper.eq(CommunityChiefApplyInfo::getCompanyId, companyId);
		if (!"distributor".equals(operatorType)) {
			wrapper.eq(CommunityChiefApplyInfo::getDistributorId, 0);
		} else if (distributorIdFilter == null) {
			wrapper.isNull(CommunityChiefApplyInfo::getDistributorId);
		} else {
			wrapper.eq(CommunityChiefApplyInfo::getDistributorId, distributorIdFilter);
		}
		if (restrictToPendingApproval) {
			wrapper.eq(CommunityChiefApplyInfo::getApproveStatus, 0);
		}
		if (StringUtils.hasText(chiefNameOrNull)) {
			wrapper.eq(CommunityChiefApplyInfo::getChiefName, chiefNameOrNull);
		}
		if (StringUtils.hasText(chiefMobileOrNull)) {
			wrapper.eq(CommunityChiefApplyInfo::getChiefMobile, chiefMobileOrNull);
		}
	}

	private static Map<String, Object> toApplyListRow(CommunityChiefApplyInfo e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("apply_id", e.getApplyId());
		m.put("user_id", e.getUserId());
		m.put("chief_name", e.getChiefName());
		m.put("chief_mobile", e.getChiefMobile());
		m.put("approve_status", e.getApproveStatus());
		m.put("created_at", e.getCreatedAt());
		return m;
	}

	private Object parseExtraData(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return null;
		}
		try {
			Object parsed = objectMapper.readValue(raw.trim(), Object.class);
			// json_decode style: JSON {} becomes [] (empty array), not associative array
			if (parsed instanceof Map<?, ?> m && m.isEmpty()) {
				return Collections.emptyList();
			}
			return parsed;
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
