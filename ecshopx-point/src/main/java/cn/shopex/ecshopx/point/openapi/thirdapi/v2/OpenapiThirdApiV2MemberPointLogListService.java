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

package cn.shopex.ecshopx.point.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagListService;
import cn.shopex.ecshopx.point.domain.PointMemberLog;
import cn.shopex.ecshopx.point.mapper.PointMemberLogMapper;
import cn.shopex.ecshopx.point.service.PointMemberJournalTypeDescriptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberPointLogListService {

	private static final int JOURNAL_TYPE_OPENAPI = 13;

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenapiMemberPointLogV2ListFilterBuilder filterBuilder;
	private final PointMemberLogMapper pointMemberLogMapper;
	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OpenapiThirdApiV2MemberPointLogListService(
			OpenapiMemberPointLogV2ListFilterBuilder filterBuilder,
			PointMemberLogMapper pointMemberLogMapper,
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.filterBuilder = filterBuilder;
		this.pointMemberLogMapper = pointMemberLogMapper;
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
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
		OpenapiMemberPointLogV2ListFilterBuilder.FilterSpec spec =
				filterBuilder.build(
						companyId,
						mobilePresent,
						mobileRaw,
						startDatePresent,
						startDateRaw,
						endDatePresent,
						endDateRaw);

		LambdaQueryWrapper<PointMemberLog> base =
				new LambdaQueryWrapper<PointMemberLog>()
						.eq(PointMemberLog::getCompanyId, spec.companyId());
		if (spec.userId() != null) {
			base.eq(PointMemberLog::getUserId, spec.userId());
		}
		if (spec.createdGte() != null) {
			base.ge(PointMemberLog::getCreated, spec.createdGte());
		}
		if (spec.createdLte() != null) {
			base.le(PointMemberLog::getCreated, spec.createdLte());
		}

		long totalCount = pointMemberLogMapper.selectCount(base);

		LambdaQueryWrapper<PointMemberLog> listWrapper =
				base.clone().orderByDesc(PointMemberLog::getId);

		Page<PointMemberLog> pageReq = new Page<>(page, pageSize, false);
		pointMemberLogMapper.selectPage(pageReq, listWrapper);
		List<PointMemberLog> entities = pageReq.getRecords();

		List<Map<String, Object>> list = List.of();
		if (entities != null && !entities.isEmpty()) {
			Map<Long, String> mobileByUserId = appendMobileToList(companyId, entities);
			list = formatPointLogRows(entities, mobileByUserId);
		}

		return OpenapiThirdApiV2MemberTagListService.formatListStruct(
				totalCount, list, page, pageSize);
	}

	private Map<Long, String> appendMobileToList(
			long companyId, List<PointMemberLog> entities) {
		List<Long> userIds =
				entities.stream()
						.map(PointMemberLog::getUserId)
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

	private List<Map<String, Object>> formatPointLogRows(
			List<PointMemberLog> entities, Map<Long, String> mobileByUserId) {
		List<Map<String, Object>> rows = new ArrayList<>(entities.size());
		for (PointMemberLog entity : entities) {
			int type = entity.getJournalType() != null ? entity.getJournalType() : 0;
			int increasePoint = entity.getIncome() != null ? entity.getIncome() : 0;
			int decreasePoint = entity.getOutcome() != null ? entity.getOutcome() : 0;

			String record;
			if (type == JOURNAL_TYPE_OPENAPI) {
				record = stringOrEmpty(entity.getOperaterRemark());
			} else {
				record = stringOrEmpty(entity.getPointDesc());
			}

			Long userId = entity.getUserId();
			String mobile = userId != null ? mobileByUserId.getOrDefault(userId, "") : "";

			int createdUnix = entity.getCreated() != null ? entity.getCreated() : 0;
			String formattedCreated = formatCreated(entity.getCreated());

			String typeName = PointMemberJournalTypeDescriptions.forType(entity.getJournalType());
			if (typeName == null || typeName.isEmpty()) {
				typeName = "其他";
			}

			String point = "";
			if (increasePoint > 0) {
				point = "+" + increasePoint;
			} else if (decreasePoint > 0) {
				point = "-" + increasePoint;
			}

			String description =
					String.format(
							"系统 于%d 给 会员(%s) %s %s",
							createdUnix, mobile, typeName, point);

			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", entity.getId());
			row.put("mobile", mobile);
			row.put("operater", stringOrEmpty(entity.getOperater()));
			row.put("type", type);
			row.put("description", description);
			row.put("increase_point", increasePoint);
			row.put("decrease_point", decreasePoint);
			row.put("record", record);
			row.put("order_id", stringOrEmpty(entity.getOrderId()));
			row.put("external_id", stringOrEmpty(entity.getExternalId()));
			row.put("created", formattedCreated);
			rows.add(row);
		}
		return rows;
	}

	private String formatCreated(Integer created) {
		if (created == null || created <= 0) {
			return "";
		}
		return DATETIME_FMT.format(Instant.ofEpochSecond(created));
	}

	private static String stringOrEmpty(String value) {
		return value != null ? value : "";
	}
}
