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
import cn.shopex.ecshopx.common.members.port.MemberBrowseHistoryItemLookupPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberFrequentItemsOrdersPort;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1MemberFrequentItemsService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MemberAccountService memberAccountService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final OpenapiMemberFrequentItemsOrdersPort frequentItemsOrdersPort;
	private final MemberBrowseHistoryItemLookupPort itemLookupPort;
	private final LangueProperties langueProperties;

	public OpenapiThirdApiV1MemberFrequentItemsService(
			MemberAccountService memberAccountService,
			MembersAssociationsMapper membersAssociationsMapper,
			OpenapiMemberFrequentItemsOrdersPort frequentItemsOrdersPort,
			MemberBrowseHistoryItemLookupPort itemLookupPort,
			LangueProperties langueProperties) {
		this.memberAccountService = memberAccountService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.frequentItemsOrdersPort = frequentItemsOrdersPort;
		this.itemLookupPort = itemLookupPort;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> executeOpenapiMemberFrequentItems(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			String externalMemberIdQueryParam,
			String timeRangeQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			boolean externalMemberIdPresent,
			String externalMemberIdRaw,
			boolean externalMemberIdTruthy,
			boolean unionidPresent,
			String unionidRaw,
			boolean timeRangePresent,
			String timeRangeRaw) {
		validateParams(
				mobileQueryParam,
				unionidQueryParam,
				externalMemberIdQueryParam,
				body,
				mobilePresent,
				mobileRaw,
				timeRangePresent,
				timeRangeRaw);

		MemberUserKey userKey =
				resolveFrequentItemsMember(
						companyId,
						mobileRaw,
						mobileTruthy,
						externalMemberIdRaw,
						externalMemberIdTruthy,
						unionidRaw);

		List<Map<String, Object>> aggregates =
				frequentItemsOrdersPort.getFrequentItemAggregates(
						companyId, userKey.userId(), timeRangeRaw);
		if (aggregates.isEmpty()) {
			return Map.of("count", 0, "list", List.of());
		}

		Map<Long, Aggregate> aggregateByItemId = new HashMap<>();
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : aggregates) {
			long itemId = longVal(row.get("item_id"));
			if (itemId <= 0L) {
				continue;
			}
			aggregateByItemId.put(
					itemId, new Aggregate(longVal(row.get("count")), longVal(row.get("sum"))));
			itemIds.add(itemId);
		}
		if (itemIds.isEmpty()) {
			return Map.of("count", 0, "list", List.of());
		}

		String languageTag = langueProperties.getDefaultLang();
		Map<Long, Map<String, Object>> itemRows =
				itemLookupPort.listItemRowsForBrowseHistory(itemIds, companyId, languageTag);

		itemIds.sort(Comparator.reverseOrder());

		List<Map<String, Object>> list = new ArrayList<>();
		for (Long itemId : itemIds) {
			Map<String, Object> itemRow = itemRows.get(itemId);
			if (itemRow == null || itemRow.isEmpty()) {
				continue;
			}
			Aggregate aggregate = aggregateByItemId.get(itemId);
			if (aggregate == null) {
				continue;
			}
			LinkedHashMap<String, Object> element = new LinkedHashMap<>();
			element.put("item_name", itemRow.get("item_name"));
			element.put("price", formatPriceYuan(itemRow.get("price")));
			element.put("buy_num", aggregate.count());
			element.put("pic", firstPicUrl(itemRow.get("pics")));
			element.put("sales_num", aggregate.sum());
			list.add(element);
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("count", list.size());
		return data;
	}

	private void validateParams(
			String mobileQueryParam,
			String unionidQueryParam,
			String externalMemberIdQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean timeRangePresent,
			String timeRangeRaw) {
		if (body != null
				&& body.containsKey("unionid")
				&& body.get("unionid") != null
				&& !(body.get("unionid") instanceof String)) {
			throw new ResourceException("请填写unionid");
		}

		if (mobilePresent && (mobileRaw == null || !MOBILE_PATTERN.matcher(mobileRaw).matches())) {
			throw new ResourceException("请填写正确的手机号");
		}

		if (body != null
				&& body.containsKey("timeRange")
				&& body.get("timeRange") != null
				&& !(body.get("timeRange") instanceof String)) {
			throw new ResourceException("请填写时间段");
		}

		if (!timeRangePresent || timeRangeRaw == null || timeRangeRaw.isEmpty()) {
			throw new ResourceException("请填写时间段");
		}

		if (isPhpEmptyInline(mobileQueryParam, body, "mobile")
				&& isPhpEmptyInline(unionidQueryParam, body, "unionid")
				&& isPhpEmptyInline(externalMemberIdQueryParam, body, "external_member_id")) {
			throw new ResourceException("unionid或者手机号必填");
		}
	}

	private MemberUserKey resolveFrequentItemsMember(
			long companyId,
			String mobileRaw,
			boolean mobileTruthy,
			String externalMemberIdRaw,
			boolean externalMemberIdTruthy,
			String unionidRaw) {
		if (mobileTruthy) {
			Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileRaw);
			if (member == null) {
				throw new ResourceException("参数无效");
			}
			return new MemberUserKey(member.getUserId());
		}

		if (externalMemberIdTruthy) {
			Map<String, Object> memberInfo =
					memberAccountService.getMemberInfoForOpenapiFilter(
							companyId, null, externalMemberIdRaw);
			if (memberInfo == null || memberInfo.isEmpty()) {
				throw new ResourceException("参数无效");
			}
			return new MemberUserKey(memberInfo.get("user_id"));
		}

		MembersAssociations assoc =
				membersAssociationsMapper.selectOne(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUnionid, unionidRaw)
								.eq(MembersAssociations::getUserType, "wechat")
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("参数无效");
		}
		return new MemberUserKey(assoc.getUserId());
	}

	private static String formatPriceYuan(Object priceRaw) {
		long priceFen = longVal(priceRaw);
		return BigDecimal.valueOf(priceFen)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String firstPicUrl(Object pics) {
		if (!(pics instanceof List<?> list) || list.isEmpty()) {
			return "";
		}
		return String.valueOf(list.get(0));
	}

	private record MemberUserKey(Object userId) {}

	private record Aggregate(long count, long sum) {}

	private static boolean isPhpEmptyInline(String queryParam, Map<String, Object> body, String key) {
		Object raw = null;
		if (body != null && body.containsKey(key)) {
			raw = body.get(key);
		} else if (queryParam != null) {
			raw = queryParam;
		}
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		return false;
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
}
