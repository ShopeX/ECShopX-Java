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
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.browse.MemberBrowseHistoryListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1MemberBrowseHistoryListService {

	private static final Logger log =
			LoggerFactory.getLogger(OpenapiThirdApiV1MemberBrowseHistoryListService.class);
	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");
	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MemberAccountService memberAccountService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MemberBrowseHistoryListService memberBrowseHistoryListService;
	private final LangueProperties langueProperties;

	public OpenapiThirdApiV1MemberBrowseHistoryListService(
			MemberAccountService memberAccountService,
			MembersAssociationsMapper membersAssociationsMapper,
			MemberBrowseHistoryListService memberBrowseHistoryListService,
			LangueProperties langueProperties) {
		this.memberAccountService = memberAccountService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.memberBrowseHistoryListService = memberBrowseHistoryListService;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> executeOpenapiMemberBrowseHistoryList(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			String externalMemberIdQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			boolean externalMemberIdPresent,
			String externalMemberIdRaw,
			boolean externalMemberIdTruthy,
			boolean unionidPresent,
			String unionidRaw,
			int page,
			int pageSize) {
		MemberFilterKey filterKey =
				resolveMemberFilter(
						companyId,
						mobileQueryParam,
						unionidQueryParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						externalMemberIdRaw,
						externalMemberIdTruthy,
						unionidRaw);

		log.info("[geMembertBrowseList] filter={}", filterKey);

		String languageTag = langueProperties.getDefaultLang();
		Map<String, Object> result =
				memberBrowseHistoryListService.getBrowseHistory(
						companyId, filterKey.userId(), page, pageSize, languageTag);

		return trimBrowseHistoryForOpenApi(result);
	}

	private MemberFilterKey resolveMemberFilter(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			String externalMemberIdRaw,
			boolean externalMemberIdTruthy,
			String unionidRaw) {
		log.info("[getFilter] mobilePresent={}, mobileTruthy={}", mobilePresent, mobileTruthy);

		if (externalMemberIdTruthy) {
			return new MemberFilterKey(companyId, externalMemberIdRaw);
		}

		if (body != null
				&& body.containsKey("unionid")
				&& body.get("unionid") != null
				&& !(body.get("unionid") instanceof String)) {
			throw new ResourceException("请填写unionid");
		}

		if (mobilePresent && (mobileRaw == null || !MOBILE_PATTERN.matcher(mobileRaw).matches())) {
			throw new ResourceException("请填写正确的手机号");
		}

		if (isPhpEmptyInline(mobileQueryParam, body, "mobile")
				&& isPhpEmptyInline(unionidQueryParam, body, "unionid")) {
			throw new ResourceException("unionid或者手机号必填");
		}

		if (mobileTruthy) {
			Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileRaw);
			if (member == null) {
				throw new ResourceException("会员信息获取失败");
			}
			return new MemberFilterKey(companyId, member.getUserId());
		}

		MembersAssociations assoc =
				membersAssociationsMapper.selectOne(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUnionid, unionidRaw)
								.eq(MembersAssociations::getUserType, "wechat")
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("会员信息获取失败");
		}
		return new MemberFilterKey(companyId, assoc.getUserId());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> trimBrowseHistoryForOpenApi(Map<String, Object> result) {
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		if (list == null || list.isEmpty()) {
			return result;
		}
		for (int i = 0; i < list.size(); i++) {
			list.set(i, trimSingleBrowseRow(list.get(i)));
		}
		return result;
	}

	private static Map<String, Object> trimSingleBrowseRow(Map<String, Object> row) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(row);
		out.put("create_time", formatEpoch(row.get("created")));
		Object itemData = row.get("itemData");
		if (itemData instanceof Map<?, ?> map && !map.isEmpty()) {
			out.put("itemData", trimItemData(map));
		} else {
			out.put("itemData", Collections.emptyList());
		}
		return out;
	}

	private static Map<String, Object> trimItemData(Map<?, ?> map) {
		LinkedHashMap<String, Object> trimmed = new LinkedHashMap<>();
		trimmed.put("item_id", map.get("item_id"));
		trimmed.put("item_bn", map.get("item_bn"));
		trimmed.put("item_name", map.get("item_name"));
		trimmed.put("pics", map.get("pics"));
		trimmed.put("price", map.get("price"));
		return trimmed;
	}

	private record MemberFilterKey(long companyId, Object userId) {}

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

	private static String formatEpoch(Object raw) {
		long sec = longVal(raw);
		if (sec <= 0L) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(sec));
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
