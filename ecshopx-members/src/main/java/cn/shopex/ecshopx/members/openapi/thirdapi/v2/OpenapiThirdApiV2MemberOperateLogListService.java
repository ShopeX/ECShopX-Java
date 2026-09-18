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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.domain.MemberOperateLog;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberOperateLogMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberOperateLogListService {

	private static final String OPERATE_TYPE_INFO = "info";
	private static final String OPERATE_TYPE_MOBILE = "mobile";
	private static final String OPERATE_TYPE_GRADE_ID = "grade_id";

	private static final Map<String, String> OPERATE_TYPE_DESC_MAP =
			Map.of(
					OPERATE_TYPE_INFO, "修改会员信息",
					OPERATE_TYPE_MOBILE, "修改手机号",
					OPERATE_TYPE_GRADE_ID, "修改会员等级");

	private static final Map<String, Integer> TYPE_MAP =
			Map.of(
					OPERATE_TYPE_INFO, 1,
					OPERATE_TYPE_MOBILE, 2,
					OPERATE_TYPE_GRADE_ID, 3);

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenapiMemberOperateLogV2ListFilterBuilder filterBuilder;
	private final MemberOperateLogMapper memberOperateLogMapper;
	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberOperateLogListService(
			OpenapiMemberOperateLogV2ListFilterBuilder filterBuilder,
			MemberOperateLogMapper memberOperateLogMapper,
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.filterBuilder = filterBuilder;
		this.memberOperateLogMapper = memberOperateLogMapper;
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiList(
			long companyId,
			boolean mobilePresent,
			String mobileRaw,
			boolean startDatePresent,
			String startDateRaw,
			boolean endDatePresent,
			String endDateRaw,
			int page,
			int pageSize) {
		OpenapiMemberOperateLogV2ListFilterBuilder.FilterSpec spec =
				filterBuilder.build(
						companyId,
						mobilePresent,
						mobileRaw,
						startDatePresent,
						startDateRaw,
						endDatePresent,
						endDateRaw);

		LambdaQueryWrapper<MemberOperateLog> base =
				new LambdaQueryWrapper<MemberOperateLog>()
						.eq(MemberOperateLog::getCompanyId, spec.companyId());
		if (spec.userId() != null) {
			base.eq(MemberOperateLog::getUserId, spec.userId());
		}
		if (spec.createdGte() != null) {
			base.ge(MemberOperateLog::getCreated, spec.createdGte());
		}
		if (spec.createdLte() != null) {
			base.le(MemberOperateLog::getCreated, spec.createdLte());
		}

		long totalCount = memberOperateLogMapper.selectCount(base);

		LambdaQueryWrapper<MemberOperateLog> listWrapper =
				base.clone().orderByDesc(MemberOperateLog::getCreated);

		Page<MemberOperateLog> pageReq = new Page<>(page, pageSize, false);
		memberOperateLogMapper.selectPage(pageReq, listWrapper);
		List<MemberOperateLog> entities = pageReq.getRecords();

		List<Map<String, Object>> list = List.of();
		if (entities != null && !entities.isEmpty()) {
			Map<Long, String> mobileByUserId = appendMobileToList(companyId, entities);
			list = formatOperateLogRows(entities, mobileByUserId);
		}

		return OpenapiThirdApiV2MemberTagListService.formatListStruct(
				totalCount, list, page, pageSize);
	}

	private Map<Long, String> appendMobileToList(
			long companyId, List<MemberOperateLog> entities) {
		List<Long> userIds =
				entities.stream()
						.map(MemberOperateLog::getUserId)
						.filter(id -> id != null && id > 0)
						.distinct()
						.toList();
		if (userIds.isEmpty()) {
			return Map.of();
		}

		LambdaQueryWrapper<Members> wrapper =
				new LambdaQueryWrapper<Members>()
						.eq(Members::getCompanyId, companyId)
						.in(Members::getUserId, userIds)
						.select(Members::getUserId, Members::getMobile);
		List<Members> members = membersMapper.selectList(wrapper);

		Map<Long, String> mobileByUserId = new LinkedHashMap<>();
		for (Members member : members) {
			if (member.getUserId() == null) {
				continue;
			}
			String stored = member.getMobile();
			String mobile =
					stored != null && !stored.isEmpty()
							? sensitiveFieldEncryptor.decrypt(stored)
							: "";
			mobileByUserId.put(member.getUserId(), mobile);
		}
		return mobileByUserId;
	}

	private List<Map<String, Object>> formatOperateLogRows(
			List<MemberOperateLog> entities, Map<Long, String> mobileByUserId) {
		List<Map<String, Object>> rows = new ArrayList<>(entities.size());
		for (MemberOperateLog entity : entities) {
			String dbOperateType = entity.getOperateType();
			String formattedCreated = formatCreated(entity.getCreated());
			String oldDataStr = formatDataField(dbOperateType, entity.getOldData());
			String newDataStr = formatDataField(dbOperateType, entity.getNewData());

			Long userId = entity.getUserId();
			String mobile = "";
			if (userId != null && userId > 0) {
				mobile = mobileByUserId.getOrDefault(userId, "");
			}

			String description =
					String.format(
							"%s 于%s进行了%s的操作, 将 %s 改为 %s",
							stringOrEmpty(entity.getOperater()),
							formattedCreated,
							OPERATE_TYPE_DESC_MAP.getOrDefault(dbOperateType, ""),
							oldDataStr,
							newDataStr);

			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", entity.getId());
			row.put("operate_type", TYPE_MAP.getOrDefault(dbOperateType, 0));
			row.put("old_data", oldDataStr);
			row.put("new_data", newDataStr);
			row.put("operater", entity.getOperater());
			row.put("created", formattedCreated);
			row.put("description", description);
			row.put("mobile", mobile);
			rows.add(row);
		}
		return rows;
	}

	private String formatDataField(String dbOperateType, String raw) {
		if (OPERATE_TYPE_MOBILE.equals(dbOperateType)) {
			if (raw == null) {
				return "";
			}
			try {
				Map<String, Object> decoded =
						objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (decoded != null && decoded.containsKey("mobile")) {
					Object mobile = decoded.get("mobile");
					return mobile != null ? String.valueOf(mobile) : "";
				}
			} catch (Exception ignored) {
				// fall through to string cast
			}
			return raw;
		}
		return raw != null ? raw : "";
	}

	private String formatCreated(Long created) {
		if (created == null) {
			return "";
		}
		return DATETIME_FMT.format(Instant.ofEpochSecond(created));
	}

	private static String stringOrEmpty(String value) {
		return value != null ? value : "";
	}
}
