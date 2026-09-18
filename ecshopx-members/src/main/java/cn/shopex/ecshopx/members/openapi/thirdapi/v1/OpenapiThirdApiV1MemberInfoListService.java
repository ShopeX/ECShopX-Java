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

package cn.shopex.ecshopx.members.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.integration.orders.OpenapiMemberOrderListOrdersPort;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.browse.MemberBrowseHistoryListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1MemberInfoListService {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MemberAccountService memberAccountService;
	private final OpenapiMemberOrderListOrdersPort openapiMemberOrderListOrdersPort;
	private final MemberBrowseHistoryListService memberBrowseHistoryListService;
	private final LangueProperties langueProperties;

	public OpenapiThirdApiV1MemberInfoListService(
			MembersAssociationsMapper membersAssociationsMapper,
			MembersInfoMapper membersInfoMapper,
			MemberAccountService memberAccountService,
			OpenapiMemberOrderListOrdersPort openapiMemberOrderListOrdersPort,
			MemberBrowseHistoryListService memberBrowseHistoryListService,
			LangueProperties langueProperties) {
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.memberAccountService = memberAccountService;
		this.openapiMemberOrderListOrdersPort = openapiMemberOrderListOrdersPort;
		this.memberBrowseHistoryListService = memberBrowseHistoryListService;
		this.langueProperties = langueProperties;
	}

	public Object executeOpenapiMemberInfoList(
			long companyId,
			String unionidQueryParam,
			Map<String, Object> body,
			boolean unionidPresent,
			String unionidRaw,
			int associationsPage,
			Integer associationsPageSize) {
		validateUnionidParams(unionidQueryParam, body, unionidPresent);

		if (unionidRaw == null) {
			return Collections.emptyList();
		}

		List<String> unionids =
				Arrays.stream(unionidRaw.split(",")).map(String::valueOf).collect(Collectors.toList());

		LambdaQueryWrapper<MembersAssociations> assocW =
				new LambdaQueryWrapper<MembersAssociations>()
						.eq(MembersAssociations::getCompanyId, companyId)
						.eq(MembersAssociations::getUserType, "wechat")
						.in(MembersAssociations::getUnionid, unionids)
						.select(MembersAssociations::getUserId, MembersAssociations::getUnionid);

		List<MembersAssociations> associations;
		if (associationsPageSize != null && associationsPageSize > 0) {
			Page<MembersAssociations> assocPage =
					new Page<>(associationsPage, associationsPageSize, false);
			associations = membersAssociationsMapper.selectPage(assocPage, assocW).getRecords();
		} else {
			associations = membersAssociationsMapper.selectList(assocW);
		}

		if (associations == null || associations.isEmpty()) {
			return Collections.emptyList();
		}

		Map<Long, String> userIdToUnionid = new LinkedHashMap<>();
		for (MembersAssociations assoc : associations) {
			userIdToUnionid.put(assoc.getUserId(), assoc.getUnionid());
		}
		List<Long> userIds = new ArrayList<>(userIdToUnionid.keySet());

		LambdaQueryWrapper<MembersInfo> infoW =
				new LambdaQueryWrapper<MembersInfo>()
						.in(MembersInfo::getUserId, userIds)
						.orderByDesc(MembersInfo::getUserId);

		long totalCount = membersInfoMapper.selectCount(infoW);

		List<Map<String, Object>> list = new ArrayList<>();
		if (totalCount > 0) {
			String languageTag = langueProperties.getDefaultLang();
			Page<MembersInfo> infoPage = new Page<>(1, 100, false);
			membersInfoMapper.selectPage(infoPage, infoW);
			for (MembersInfo info : infoPage.getRecords()) {
				list.add(enrichMemberInfoRow(companyId, info, userIdToUnionid, languageTag));
			}
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("count", totalCount);
		result.put("list", list);
		return result;
	}

	private void validateUnionidParams(
			String unionidQueryParam, Map<String, Object> body, boolean unionidPresent) {
		if (!unionidPresent) {
			throw new ResourceException("unionid必填");
		}
		if (body != null && body.containsKey("unionid")) {
			Object raw = body.get("unionid");
			if (raw != null && !(raw instanceof String) && !(raw instanceof Number)) {
				throw new ResourceException("请填写unionid");
			}
		}
		if (unionidQueryParam != null && !(unionidQueryParam instanceof String)) {
			throw new ResourceException("请填写unionid");
		}
	}

	private Map<String, Object> enrichMemberInfoRow(
			long companyId,
			MembersInfo info,
			Map<Long, String> userIdToUnionid,
			String languageTag) {
		Map<String, Object> row = memberAccountService.toMembersInfoOpenapiListMap(info);
		Long userId = info.getUserId();
		row.put("total_amount", lookupSingleUserTotalFee(companyId, userId));
		row.put("unionid", userIdToUnionid.get(userId));

		Map<String, Object> browse =
				memberBrowseHistoryListService.getBrowseHistory(companyId, userId, 1, 1, languageTag);
		long browseCount = longVal(browse.get("total_count"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> browseList = (List<Map<String, Object>>) browse.get("list");
		if (browseCount > 0 && browseList != null && !browseList.isEmpty()) {
			Map<String, Object> latest = browseList.get(0);
			row.put("browse_count", browseCount);
			row.put("browse_time", formatEpoch(latest.get("updated")));
			Object itemData = latest.get("itemData");
			if (itemData instanceof Map<?, ?> itemMap) {
				row.put("browse_item_name", itemMap.get("item_name"));
			} else {
				row.put("browse_item_name", null);
			}
			row.put("browse_item_id", latest.get("item_id"));
		}
		return row;
	}

	private long lookupSingleUserTotalFee(long companyId, long userId) {
		Map<String, Object> orderFilter = new LinkedHashMap<>();
		orderFilter.put("company_id", companyId);
		orderFilter.put("user_id", userId);
		Map<Object, Long> totals = openapiMemberOrderListOrdersPort.sumTotalFeeByUserId(orderFilter);
		Long fee = totals.get(userId);
		if (fee == null) {
			fee = totals.get(String.valueOf(userId));
		}
		return fee != null ? fee : 0L;
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String formatEpoch(Object raw) {
		long sec = longVal(raw);
		if (sec <= 0L) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(sec));
	}
}
