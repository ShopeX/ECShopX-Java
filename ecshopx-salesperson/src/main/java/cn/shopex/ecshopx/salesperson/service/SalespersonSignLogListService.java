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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.salesperson.domain.SalespersonSignLog;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonSignLogMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonSignLogListService {

	private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final SalespersonSignLogMapper salespersonSignLogMapper;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public SalespersonSignLogListService(SalespersonSignLogMapper salespersonSignLogMapper,
			ShopSalespersonMapper shopSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.salespersonSignLogMapper = salespersonSignLogMapper;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	public Map<String, Object> getSignlogs(long companyId, String distributorIdRaw, String nameRaw, String mobileRaw,
			String timeStartBeginRaw, String timeStartEndRaw, String pageRaw, String pageSizeRaw) {
		int page = parsePageOrThrow(pageRaw);
		int pageSize = parsePageSizeOrThrow(pageSizeRaw);

		LambdaQueryWrapper<SalespersonSignLog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SalespersonSignLog::getCompanyId, companyId);

		if (distributorIdRaw != null && StringUtils.hasText(distributorIdRaw.trim())) {
			try {
				wrapper.eq(SalespersonSignLog::getDistributorId, Long.parseLong(distributorIdRaw.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("distributor_id 格式错误", 400);
			}
		}

		applyCreatedRangeIfBothPresent(wrapper, timeStartBeginRaw, timeStartEndRaw);

		boolean nameBlank = nameRaw == null || !StringUtils.hasText(nameRaw.trim());
		boolean mobileBlank = mobileRaw == null || !StringUtils.hasText(mobileRaw.trim());
		if (!nameBlank || !mobileBlank) {
			List<Long> salespersonIds = resolveSalespersonIdsByNameOrMobile(companyId, nameRaw, mobileRaw);
			if (salespersonIds.isEmpty()) {
				return emptyBody();
			}
			wrapper.in(SalespersonSignLog::getSalespersonId, salespersonIds);
		}

		long totalCount = salespersonSignLogMapper.selectCount(wrapper);
		if (totalCount == 0L) {
			return emptyBody();
		}

		wrapper.orderByDesc(SalespersonSignLog::getCreated).orderByDesc(SalespersonSignLog::getId);
		Page<SalespersonSignLog> mpPage = new Page<>(page, pageSize, false);
		List<SalespersonSignLog> logs = salespersonSignLogMapper.selectPage(mpPage, wrapper).getRecords();
		if (logs.isEmpty()) {
			return Map.of("total_count", totalCount, "list", List.of());
		}

		Set<Long> salespersonIdSet = new LinkedHashSet<>();
		Set<Long> distributorIdSet = new LinkedHashSet<>();
		for (SalespersonSignLog log : logs) {
			if (log.getSalespersonId() != null) {
				salespersonIdSet.add(log.getSalespersonId());
			}
			if (log.getDistributorId() != null) {
				distributorIdSet.add(log.getDistributorId());
			}
		}

		Map<Long, ShopSalesperson> salespersonById = loadSalespersonMap(companyId, salespersonIdSet);
		List<Long> distributorIdList = new ArrayList<>(distributorIdSet);
		List<Map<String, Object>> distributorRows = distributorRepositoryGetInfoSimpleService
				.listEasylistsByDistributorIds(companyId, distributorIdList);
		if (!distributorIdList.isEmpty() && distributorRows.isEmpty()) {
			return emptyBody();
		}

		Map<Long, String> shopNameByDistributorId = new LinkedHashMap<>();
		for (Map<String, Object> row : distributorRows) {
			Object did = row.get("distributor_id");
			if (did == null) {
				continue;
			}
			long id = did instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(did));
			Object name = row.get("name");
			shopNameByDistributorId.put(id, name != null ? String.valueOf(name) : "");
		}

		ZoneId zone = ZoneId.systemDefault();
		List<Map<String, Object>> list = new ArrayList<>(logs.size());
		for (SalespersonSignLog log : logs) {
			list.add(toRow(log, salespersonById, shopNameByDistributorId, zone));
		}

		return Map.of("total_count", totalCount, "list", list);
	}

	private static Map<String, Object> emptyBody() {
		return Map.of("total_count", 0L, "list", List.of());
	}

	private void applyCreatedRangeIfBothPresent(LambdaQueryWrapper<SalespersonSignLog> wrapper,
			String timeStartBeginRaw, String timeStartEndRaw) {
		String beginCheck = timeStartBeginRaw == null ? "" : timeStartBeginRaw.trim();
		String endCheck = timeStartEndRaw == null ? "" : timeStartEndRaw.trim();
		if (!StringUtils.hasText(beginCheck) || !StringUtils.hasText(endCheck)) {
			return;
		}
		String beginParse = timeStartBeginRaw.strip();
		String endParse = timeStartEndRaw.strip();
		long gteSec;
		long lteSec;
		try {
			gteSec = localDateTimeToEpochSecond(beginParse);
			lteSec = localDateTimeToEpochSecond(endParse);
		} catch (DateTimeParseException e) {
			throw new BadRequestException("时间格式须为 yyyy-MM-dd HH:mm:ss", 400);
		}
		wrapper.ge(SalespersonSignLog::getCreated, (int) gteSec).le(SalespersonSignLog::getCreated, (int) lteSec);
	}

	private static long localDateTimeToEpochSecond(String s) {
		LocalDateTime ldt = LocalDateTime.parse(s, LOG_TIME_FORMAT);
		return ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
	}

	private List<Long> resolveSalespersonIdsByNameOrMobile(long companyId, String nameRaw, String mobileRaw) {
		LambdaQueryWrapper<ShopSalesperson> w = new LambdaQueryWrapper<>();
		w.eq(ShopSalesperson::getCompanyId, companyId).eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.ne(ShopSalesperson::getIsValid, "delete");
		if (nameRaw != null && StringUtils.hasText(nameRaw.trim())) {
			String kw = nameRaw.trim();
			w.like(ShopSalesperson::getName, kw);
		}
		if (mobileRaw != null && StringUtils.hasText(mobileRaw.trim())) {
			w.eq(ShopSalesperson::getMobile, sensitiveFieldEncryptor.encrypt(mobileRaw.trim()));
		}
		List<ShopSalesperson> rows = shopSalespersonMapper.selectList(w);
		List<Long> ids = new ArrayList<>(rows.size());
		for (ShopSalesperson sp : rows) {
			if (sp.getSalespersonId() != null) {
				ids.add(sp.getSalespersonId());
			}
		}
		return ids;
	}

	private Map<Long, ShopSalesperson> loadSalespersonMap(long companyId, Set<Long> salespersonIds) {
		if (salespersonIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<ShopSalesperson> w = new LambdaQueryWrapper<>();
		w.eq(ShopSalesperson::getCompanyId, companyId).eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.ne(ShopSalesperson::getIsValid, "delete").in(ShopSalesperson::getSalespersonId, salespersonIds);
		List<ShopSalesperson> rows = shopSalespersonMapper.selectList(w);
		Map<Long, ShopSalesperson> m = new LinkedHashMap<>();
		for (ShopSalesperson sp : rows) {
			if (sp.getSalespersonId() != null) {
				m.put(sp.getSalespersonId(), sp);
			}
		}
		return m;
	}

	private Map<String, Object> toRow(SalespersonSignLog log, Map<Long, ShopSalesperson> salespersonById,
			Map<Long, String> shopNameByDistributorId, ZoneId zone) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", log.getId());
		row.put("company_id", log.getCompanyId());
		row.put("salesperson_id", log.getSalespersonId());
		row.put("distributor_id", log.getDistributorId());
		row.put("sign_type", mapSignType(log.getSignType()));
		row.put("created", formatEpoch(log.getCreated(), zone));
		row.put("updated", formatEpoch(log.getUpdated(), zone));

		ShopSalesperson sp = log.getSalespersonId() == null ? null : salespersonById.get(log.getSalespersonId());
		if (sp != null) {
			row.put("name", sp.getName() != null ? sensitiveFieldEncryptor.decrypt(sp.getName()) : "");
			row.put("mobile", sp.getMobile() != null ? sensitiveFieldEncryptor.decrypt(sp.getMobile()) : "");
		} else {
			row.put("name", "");
			row.put("mobile", "");
		}

		Long did = log.getDistributorId();
		String shopName = did == null ? "" : shopNameByDistributorId.getOrDefault(did, "");
		row.put("shop_name", shopName);
		return row;
	}

	private static String mapSignType(String signType) {
		if (signType == null) {
			return "";
		}
		return switch (signType) {
			case "signin" -> "签到";
			case "signout" -> "签退";
			case "forceout" -> "被动签退";
			default -> signType;
		};
	}

	private static String formatEpoch(Integer epochSec, ZoneId zone) {
		if (epochSec == null) {
			return "";
		}
		return Instant.ofEpochSecond(epochSec.longValue()).atZone(zone).toLocalDateTime().format(LOG_TIME_FORMAT);
	}

	private static int parsePageOrThrow(String pageRaw) {
		String s = pageRaw == null ? "" : pageRaw.trim();
		if (!StringUtils.hasText(s)) {
			return 1;
		}
		int v;
		try {
			v = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("page 须为正整数", 400);
		}
		if (v <= 0) {
			throw new BadRequestException("page 须为正整数", 400);
		}
		return v;
	}

	private static int parsePageSizeOrThrow(String pageSizeRaw) {
		String s = pageSizeRaw == null ? "" : pageSizeRaw.trim();
		if (!StringUtils.hasText(s)) {
			return 20;
		}
		int v;
		try {
			v = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("pageSize 须为正整数", 400);
		}
		if (v <= 0) {
			throw new BadRequestException("pageSize 须为正整数", 400);
		}
		return v;
	}
}
