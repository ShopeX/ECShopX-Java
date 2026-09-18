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

package cn.shopex.ecshopx.salesperson.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.distribution.port.DistributionCheckInRulePort;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.salesperson.service.SalespersonGetInfoService;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributionCheckInRulePortImpl implements DistributionCheckInRulePort {

	private static final Logger log = LoggerFactory.getLogger(DistributionCheckInRulePortImpl.class);

	private final SalespersonGetInfoService salespersonGetInfoService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final ObjectMapper objectMapper;

	public DistributionCheckInRulePortImpl(
			SalespersonGetInfoService salespersonGetInfoService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			MembersAssociationsMapper membersAssociationsMapper,
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			ObjectMapper objectMapper) {
		this.salespersonGetInfoService = salespersonGetInfoService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.objectMapper = objectMapper;
	}

	private static boolean isActiveValid(Object raw) {
		if (raw == null) {
			return false;
		}
		if (Boolean.TRUE.equals(raw)) {
			return true;
		}
		return "true".equalsIgnoreCase(String.valueOf(raw).trim());
	}

	@Override
	public Map<String, Object> checkInRule(long companyId, long userId, String workUserid) {
		if (StringUtils.hasText(workUserid)) {
			return checkInRuleBranchWorkUserid(companyId, workUserid.trim());
		}
		return checkInRuleBranchMember(companyId, userId);
	}

	private Map<String, Object> checkInRuleBranchWorkUserid(long companyId, String workUserid) {
		Map<String, Object> sp = salespersonGetInfoService.getShoppingGuideDetailByWorkUseridForInRuleCheck(companyId,
				workUserid);
		if (sp == null || sp.isEmpty() || !sp.containsKey("salesperson_id")) {
			throw new ResourceException("导购员不存在");
		}
		if (sp.containsKey("is_valid") && !isActiveValid(sp.get("is_valid"))) {
			throw new ResourceException("导购员失效");
		}
		Long parsedDistributorId = parsePositiveDistributorId(sp.get("distributor_id"));
		if (parsedDistributorId == null) {
			throw new ResourceException("店铺不存在");
		}
		Object dist = distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId,
				String.valueOf(parsedDistributorId));
		if (isDistributorResultEmpty(dist)) {
			throw new ResourceException("店铺不存在");
		}
		Map<String, Object> info = requireSingleDistributorMap(dist);
		if (info.containsKey("is_valid") && !isActiveValid(info.get("is_valid"))) {
			throw new ResourceException("店铺失效");
		}
		LinkedHashMap<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", true);
		ok.put("msg", "成功");
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("distributor_id", info.get("distributor_id"));
		data.put("distributor_ids", sp.get("distributor_ids"));
		data.put("salesperson_id", sp.get("salesperson_id"));
		ok.put("data", data);
		return ok;
	}

	private Map<String, Object> checkInRuleBranchMember(long companyId, long userId) {
		if (userId <= 0L) {
			throw new UnauthorizedException("请先登录");
		}
		List<Map<String, Object>> memberList = new ArrayList<>();
		LinkedHashMap<String, Object> firstMember = new LinkedHashMap<>();
		firstMember.put("user_id", userId);
		memberList.add(firstMember);
		batchGetBindSalesperson(companyId, memberList);
		try {
			log.info("david-batchGetBindSalesperson:{}", objectMapper.writeValueAsString(memberList));
		} catch (JsonProcessingException ex) {
			log.warn("david-batchGetBindSalesperson: json serialize failed", ex);
		}

		Map<String, Object> member0 = memberList.get(0);
		@SuppressWarnings("unchecked")
		Map<String, Object> salespersonInfo =
				(Map<String, Object>) member0.getOrDefault("salesperson_info", Map.of());
		@SuppressWarnings("unchecked")
		Map<String, Object> storeInfo = (Map<String, Object>) member0.getOrDefault("store_info", Map.of());

		Object salespersonIdRaw = salespersonInfo.get("salesperson_id");
		if (isMissingSalespersonId(salespersonIdRaw)) {
			LinkedHashMap<String, Object> neg = new LinkedHashMap<>();
			neg.put("status", false);
			neg.put("msg", "用户没有专属导购");
			neg.put("data", List.of());
			return neg;
		}
		long salespersonId = parsePositiveLongRequired(salespersonIdRaw, "导购员不存在");

		Map<String, Object> salespersonDetail =
				salespersonGetInfoService.getSalespersonDetailForUserSalespersonRelationship(companyId, salespersonId);
		if (salespersonDetail == null || salespersonDetail.isEmpty()
				|| !salespersonDetail.containsKey("salesperson_id")) {
			throw new ResourceException("导购员不存在");
		}
		if (salespersonDetail.containsKey("is_valid") && !isActiveValid(salespersonDetail.get("is_valid"))) {
			throw new ResourceException("导购员失效");
		}

		String storeBn = Optional.ofNullable(storeInfo.get("store_bn"))
				.map(String::valueOf)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.orElse("");

		Map<String, Object> distributorInfo = null;
		if (StringUtils.hasText(storeBn)) {
			Map<String, Object> byCode =
					distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(companyId, storeBn);
			if (byCode != null && !byCode.isEmpty()) {
				distributorInfo = byCode;
			}
		}
		if (distributorInfo == null) {
			Long fallbackDid = parsePositiveDistributorId(salespersonDetail.get("distributor_id"));
			if (fallbackDid == null) {
				throw new ResourceException("店铺不存在");
			}
			Object dist = distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId,
					String.valueOf(fallbackDid));
			if (isDistributorResultEmpty(dist)) {
				throw new ResourceException("店铺不存在");
			}
			distributorInfo = requireSingleDistributorMap(dist);
		}
		if (distributorInfo.containsKey("is_valid") && !isActiveValid(distributorInfo.get("is_valid"))) {
			throw new ResourceException("店铺失效");
		}

		LinkedHashMap<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", true);
		ok.put("msg", "成功");
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("distributor_id", distributorInfo.get("distributor_id"));
		data.put("distributor_ids", salespersonDetail.get("distributor_ids"));
		data.put("salesperson_id", salespersonDetail.get("salesperson_id"));
		ok.put("data", data);
		return ok;
	}

	private void batchGetBindSalesperson(long companyId, List<Map<String, Object>> memberList) {
		try {
			List<Long> userIds = memberList.stream()
					.map(DistributionCheckInRulePortImpl::extractUserId)
					.filter(id -> id != null && id > 0L)
					.distinct()
					.collect(Collectors.toList());
			if (userIds.isEmpty()) {
				log.info("[MemberService] 批量查询绑定导购：无有效数据 company_id={} member_count={}",
						companyId, memberList.size());
				return;
			}
			List<MembersAssociations> assocRows = membersAssociationsMapper.selectList(
					new LambdaQueryWrapper<MembersAssociations>()
							.eq(MembersAssociations::getCompanyId, companyId)
							.eq(MembersAssociations::getUserType, "wechat")
							.in(MembersAssociations::getUserId, userIds));
			Map<Long, String> unionidByUserId = new LinkedHashMap<>();
			for (MembersAssociations row : assocRows) {
				if (row == null || row.getUserId() == null) {
					continue;
				}
				String u = row.getUnionid();
				if (StringUtils.hasText(u)) {
					unionidByUserId.put(row.getUserId(), u.trim());
				}
			}
			for (Map<String, Object> m : memberList) {
				Long uid = extractUserId(m);
				if (uid == null) {
					continue;
				}
				String uni = unionidByUserId.get(uid);
				if (StringUtils.hasText(uni)) {
					m.put("unionid", uni);
				}
			}

			List<Map<String, Object>> batchData = new ArrayList<>();
			for (Map<String, Object> m : memberList) {
				Long uid = extractUserId(m);
				if (uid == null) {
					continue;
				}
				LinkedHashMap<String, Object> row = new LinkedHashMap<>();
				Object uniObj = m.get("unionid");
				if (uniObj != null && StringUtils.hasText(String.valueOf(uniObj))) {
					row.put("unionid", String.valueOf(uniObj).trim());
				} else {
					row.put("external_member_id", String.valueOf(uid));
				}
				batchData.add(row);
			}
			if (batchData.isEmpty()) {
				log.info("[MemberService] 批量查询绑定导购：无有效数据 company_id={} member_count={}",
						companyId, memberList.size());
				return;
			}

			log.info("[MemberService] 批量查询绑定导购：开始请求 company_id={} batch_count={} request_data={}",
					companyId, batchData.size(), batchData);

			Map<String, Object> parsed = marketingCenterOpenApiSignedFormClient.postReturningParsedDataWithJsonArrayData(
					companyId, "basics.member.getBindSalespersonBatch", batchData);

			LinkedHashMap<String, Object> resultStructure = new LinkedHashMap<>();
			Object resultsObj = parsed.get("results");
			boolean hasResults = resultsObj instanceof List<?> list && !list.isEmpty();
			resultStructure.put("has_results", hasResults);
			if (resultsObj instanceof List<?> list) {
				resultStructure.put("results_count", list.size());
				if (!list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
					resultStructure.put("first_result_keys", new ArrayList<>(stringKeys(first)));
				} else {
					resultStructure.put("first_result_keys", List.of());
				}
			} else {
				resultStructure.put("results_count", 0);
				resultStructure.put("first_result_keys", List.of());
			}
			log.info("[MemberService] 批量查询绑定导购：返回结果 company_id={} result_structure={}",
					companyId, resultStructure);

			if (!(resultsObj instanceof List<?> results) || results.isEmpty()) {
				log.info("[MemberService] 批量查询绑定导购：无返回结果 company_id={}", companyId);
				return;
			}

			int matched = 0;
			for (Object o : results) {
				if (!(o instanceof Map<?, ?> raw)) {
					continue;
				}
				int idx = parseRequestIndex(raw.get("request_index"));
				if (idx < 0 || idx >= memberList.size()) {
					log.warn("[MemberService] 批量查询绑定导购：无法匹配会员 company_id={} request_index={} member_list_count={}",
							companyId, idx, memberList.size());
					continue;
				}
				matched++;
				Map<String, Object> target = memberList.get(idx);
				Object spi = raw.get("salesperson_info");
				if (spi instanceof Map<?, ?> sm) {
					target.put("salesperson_info", shallowStringObjectMap(sm));
				}
				Object sti = raw.get("store_info");
				if (sti instanceof Map<?, ?> tm) {
					target.put("store_info", shallowStringObjectMap(tm));
				}
			}
			log.info("[MemberService] 批量查询绑定导购：匹配完成 company_id={} matched_count={} total_results={}",
					companyId, matched, results.size());
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			log.error("[MemberService] 批量查询绑定导购失败 company_id={} error={} trace={}",
					companyId, e.toString(), sw.toString());
		}
	}

	private static List<String> stringKeys(Map<?, ?> m) {
		List<String> keys = new ArrayList<>();
		for (Object k : m.keySet()) {
			keys.add(String.valueOf(k));
		}
		return keys;
	}

	private static Map<String, Object> shallowStringObjectMap(Map<?, ?> in) {
		LinkedHashMap<String, Object> o = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : in.entrySet()) {
			o.put(String.valueOf(e.getKey()), e.getValue());
		}
		return o;
	}

	private static int parseRequestIndex(Object raw) {
		if (raw == null) {
			return -1;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static Long extractUserId(Map<String, Object> m) {
		Object v = m.get("user_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isMissingSalespersonId(Object raw) {
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
		return false;
	}

	private static long parsePositiveLongRequired(Object raw, String missingMessage) {
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new ResourceException(missingMessage);
			}
			return v;
		}
		try {
			long v = Long.parseLong(String.valueOf(raw).trim());
			if (v <= 0L) {
				throw new ResourceException(missingMessage);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException(missingMessage);
		}
	}

	private static Long parsePositiveDistributorId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Boolean b && Boolean.FALSE.equals(b)) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		try {
			long v = Long.parseLong(String.valueOf(raw).trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isDistributorResultEmpty(Object dist) {
		if (dist == null) {
			return true;
		}
		if (dist instanceof List<?> list) {
			return list.isEmpty();
		}
		if (dist instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> requireSingleDistributorMap(Object dist) {
		if (dist instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		if (dist instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		throw new ResourceException("店铺不存在");
	}
}
