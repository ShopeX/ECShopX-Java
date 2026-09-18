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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivityRelShop;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityRelShopMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityEasylistService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 20;

	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationActivityRelShopMapper registrationActivityRelShopMapper;
	private final FormTemplateMapper formTemplateMapper;
	private final DistributorMapper distributorMapper;
	private final RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;

	public RegistrationActivityEasylistService(
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationActivityRelShopMapper registrationActivityRelShopMapper,
			FormTemplateMapper formTemplateMapper,
			DistributorMapper distributorMapper,
			RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService) {
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationActivityRelShopMapper = registrationActivityRelShopMapper;
		this.formTemplateMapper = formTemplateMapper;
		this.distributorMapper = distributorMapper;
		this.registrationActivityOutsideMultiLangReadService = registrationActivityOutsideMultiLangReadService;
	}

	public Map<String, Object> getEasyDatalist(
			long companyId,
			String requestLangTag,
			String pageRaw,
			String pageSizeRaw,
			String startTimeRaw,
			String endTimeRaw,
			String statusRaw,
			String isValidRaw,
			String distributorIdRaw) {
		int page = parsePage(pageRaw);
		int pageSize = parsePageSize(pageSizeRaw);
		long distributorIdParam = parseDistributorIdIntvalStyle(distributorIdRaw);
		int now = (int) (System.currentTimeMillis() / 1000L);

		Optional<LambdaQueryWrapper<RegistrationActivity>> optional =
				buildActivityQueryWrapper(companyId, now, startTimeRaw, endTimeRaw, statusRaw, isValidRaw, distributorIdParam);
		if (optional.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0);
			empty.put("list", List.of());
			return empty;
		}

		LambdaQueryWrapper<RegistrationActivity> wrapper = optional.get();
		// Count must not reuse a wrapper that has partial column select(); clone keeps WHERE only (MyBatis-Plus).
		long total = registrationActivityMapper.selectCount(wrapper.clone());
		wrapper.orderByDesc(RegistrationActivity::getActivityId);
		wrapper.select(
				RegistrationActivity::getActivityId,
				RegistrationActivity::getActivityName,
				RegistrationActivity::getTempId);
		long offset = ((long) page - 1L) * (long) pageSize;
		List<RegistrationActivity> rows =
				registrationActivityMapper.selectList(wrapper.last("LIMIT " + pageSize + " OFFSET " + offset));

		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (RegistrationActivity row : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("activity_id", row.getActivityId());
			m.put("activity_name", row.getActivityName());
			m.put("temp_id", row.getTempId());
			list.add(m);
		}

		if (!list.isEmpty()) {
			registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(companyId, list, requestLangTag);
		}

		Map<Long, List<Long>> activityRelShopsByActivityId = new LinkedHashMap<>();
		List<Long> distributorIdsForQuery = new ArrayList<>();

		if (distributorIdParam > 0) {
			distributorIdsForQuery = List.of(distributorIdParam);
		} else {
			List<Long> activityIds = new ArrayList<>(list.size());
			for (Map<String, Object> row : list) {
				Object aid = row.get("activity_id");
				if (aid instanceof Number n) {
					activityIds.add(n.longValue());
				}
			}
			if (!activityIds.isEmpty()) {
				LambdaQueryWrapper<RegistrationActivityRelShop> relW = new LambdaQueryWrapper<>();
				relW.in(RegistrationActivityRelShop::getActivityId, activityIds);
				relW.select(RegistrationActivityRelShop::getActivityId, RegistrationActivityRelShop::getDistributorId);
				List<RegistrationActivityRelShop> rels = registrationActivityRelShopMapper.selectList(relW);
				Set<Long> distinctForQuery = new LinkedHashSet<>();
				for (RegistrationActivityRelShop rel : rels) {
					Long aid = rel.getActivityId();
					Long did = rel.getDistributorId();
					if (aid == null) {
						continue;
					}
					List<Long> bucket = activityRelShopsByActivityId.computeIfAbsent(aid, k -> new ArrayList<>());
					if (did != null && did > 0L && !bucket.contains(did)) {
						bucket.add(did);
						distinctForQuery.add(did);
					}
				}
				distributorIdsForQuery = new ArrayList<>(distinctForQuery);
			}
		}

		Map<Long, String> distributorIdToName = new LinkedHashMap<>();
		if (!distributorIdsForQuery.isEmpty()) {
			LambdaQueryWrapper<Distributor> distW = new LambdaQueryWrapper<>();
			distW.in(Distributor::getDistributorId, distributorIdsForQuery);
			distW.eq(Distributor::getCompanyId, companyId);
			distW.select(Distributor::getDistributorId, Distributor::getName);
			List<Distributor> distributors = distributorMapper.selectList(distW);
			for (Distributor d : distributors) {
				if (d.getDistributorId() == null) {
					continue;
				}
				String name = d.getName();
				if (name != null) {
					distributorIdToName.put(d.getDistributorId(), name);
				}
			}
		}

		List<Long> tempIdsInOrder = new ArrayList<>(list.size());
		for (Map<String, Object> row : list) {
			Object tid = row.get("temp_id");
			if (tid instanceof Number n) {
				tempIdsInOrder.add(n.longValue());
			} else {
				tempIdsInOrder.add(null);
			}
		}

		Map<Long, String> idToTemName = new LinkedHashMap<>();
		if (!tempIdsInOrder.isEmpty()) {
			LambdaQueryWrapper<FormTemplate> ftW = new LambdaQueryWrapper<>();
			ftW.in(FormTemplate::getId, tempIdsInOrder);
			ftW.eq(FormTemplate::getCompanyId, companyId);
			ftW.select(FormTemplate::getId, FormTemplate::getTemName);
			List<FormTemplate> templates = formTemplateMapper.selectList(ftW);
			for (FormTemplate t : templates) {
				if (t.getId() == null) {
					continue;
				}
				String temName = t.getTemName();
				if (temName != null) {
					idToTemName.put(t.getId(), temName);
				}
			}
		}

		for (Map<String, Object> row : list) {
			Object tempIdObj = row.get("temp_id");
			Long tempId = null;
			if (tempIdObj instanceof Number n) {
				tempId = n.longValue();
			}
			if (tempId == null) {
				row.put("tem_name", tempIdObj);
			} else if (idToTemName.containsKey(tempId)) {
				row.put("tem_name", idToTemName.get(tempId));
			} else {
				row.put("tem_name", tempIdObj);
			}

			List<String> names = new ArrayList<>();
			if (distributorIdParam == 0) {
				Object aidObj = row.get("activity_id");
				Long activityId = aidObj instanceof Number ? ((Number) aidObj).longValue() : null;
				if (activityId != null && activityRelShopsByActivityId.containsKey(activityId)) {
					for (Long relDistributorId : activityRelShopsByActivityId.get(activityId)) {
						if (distributorIdToName.containsKey(relDistributorId)) {
							names.add(distributorIdToName.get(relDistributorId));
						}
					}
				}
			}
			row.put("distributor_name", names);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);
		out.put("list", list);
		return out;
	}

	private static int parsePage(String pageRaw) {
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			return DEFAULT_PAGE;
		}
		try {
			int p = Integer.parseInt(pageRaw.trim());
			return p < 1 ? DEFAULT_PAGE : p;
		} catch (NumberFormatException e) {
			return DEFAULT_PAGE;
		}
	}

	private static int parsePageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			return DEFAULT_PAGE_SIZE;
		}
		try {
			int n = Integer.parseInt(pageSizeRaw.trim());
			return n <= 0 ? DEFAULT_PAGE_SIZE : n;
		} catch (NumberFormatException e) {
			return DEFAULT_PAGE_SIZE;
		}
	}

	/**
	 * 按 intval 风格解析分销商 ID：忽略前导空白，可选正负号后读取连续十进制数字，遇首个非数字停止；
	 * 空串或无法读出数字段时返回 0；数值超出 {@code long} 范围时钳位到边界。
	 * 不向调用栈抛出 {@link NumberFormatException}。
	 */
	private static long parseDistributorIdIntvalStyle(String raw) {
		if (raw == null) {
			return 0L;
		}
		String s = raw;
		int len = s.length();
		int i = 0;
		while (i < len && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		if (i >= len) {
			return 0L;
		}
		boolean negative = false;
		char c = s.charAt(i);
		if (c == '+') {
			i++;
		} else if (c == '-') {
			negative = true;
			i++;
		}
		if (i >= len) {
			return 0L;
		}
		int startDigits = i;
		while (i < len && Character.isDigit(s.charAt(i))) {
			i++;
		}
		if (i == startDigits) {
			return 0L;
		}
		String digitStr = s.substring(startDigits, i);
		try {
			BigInteger bi = new BigInteger(negative ? "-" + digitStr : digitStr);
			if (bi.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
				return Long.MAX_VALUE;
			}
			if (bi.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
				return Long.MIN_VALUE;
			}
			return bi.longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/** Unix 秒边界；非法/空白输入返回 null，永不抛业务 400。 */
	private static Integer parseEpochSecondOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return null;
		}
		if (!t.matches("^-?\\d+$")) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
				return null;
			}
			return (int) v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isTruthyIsValid(String isValidRaw) {
		return isValidRaw != null && !isValidRaw.isEmpty() && !"0".equals(isValidRaw);
	}

	private static boolean isTruthyQueryTimeParam(String raw) {
		return raw != null && !raw.isEmpty() && !"0".equals(raw);
	}

	private Optional<LambdaQueryWrapper<RegistrationActivity>> buildActivityQueryWrapper(
			long companyId,
			int now,
			String startTimeRaw,
			String endTimeRaw,
			String statusRaw,
			String isValidRaw,
			long distributorIdParam) {
		LambdaQueryWrapper<RegistrationActivity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RegistrationActivity::getCompanyId, companyId);

		if (StringUtils.hasText(statusRaw)) {
			String st = statusRaw.trim();
			switch (st) {
				case "waiting" -> {
					wrapper.ge(RegistrationActivity::getStartTime, now);
					wrapper.ge(RegistrationActivity::getEndTime, now);
				}
				case "ongoing" -> {
					wrapper.le(RegistrationActivity::getStartTime, now);
					wrapper.ge(RegistrationActivity::getEndTime, now);
				}
				case "end" -> {
					wrapper.le(RegistrationActivity::getStartTime, now);
					wrapper.le(RegistrationActivity::getEndTime, now);
				}
				default -> {
					// unknown status: no extra filter
				}
			}
		}

		if (isTruthyIsValid(isValidRaw)) {
			wrapper.le(RegistrationActivity::getStartTime, now);
			wrapper.ge(RegistrationActivity::getEndTime, now);
		}

		if (isTruthyQueryTimeParam(startTimeRaw) && isTruthyQueryTimeParam(endTimeRaw)) {
			Integer startSec = parseEpochSecondOrNull(startTimeRaw);
			Integer endSec = parseEpochSecondOrNull(endTimeRaw);
			if (startSec != null && endSec != null) {
				wrapper.ge(RegistrationActivity::getCreated, startSec);
				wrapper.le(RegistrationActivity::getCreated, endSec);
			}
		}

		if (distributorIdParam > 0) {
			LambdaQueryWrapper<RegistrationActivityRelShop> relW = new LambdaQueryWrapper<>();
			relW.eq(RegistrationActivityRelShop::getDistributorId, distributorIdParam);
			relW.select(RegistrationActivityRelShop::getActivityId);
			relW.last("LIMIT 100");
			List<RegistrationActivityRelShop> relHits = registrationActivityRelShopMapper.selectList(relW);
			if (relHits.isEmpty()) {
				return Optional.empty();
			}
			List<Long> activityIdList = new ArrayList<>(relHits.size());
			for (RegistrationActivityRelShop hit : relHits) {
				if (hit.getActivityId() != null) {
					activityIdList.add(hit.getActivityId());
				}
			}
			if (activityIdList.isEmpty()) {
				return Optional.empty();
			}
			wrapper.in(RegistrationActivity::getActivityId, activityIdList);
		}

		return Optional.of(wrapper);
	}
}
