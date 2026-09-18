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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.OperatorResponseAssembler;
import cn.shopex.ecshopx.companys.service.OperatorsCommandService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DealerListService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final OperatorsMapper operatorsMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final OperatorsCommandService operatorsCommandService;
	private final OperatorResponseAssembler operatorResponseAssembler;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public DealerListService(
			OperatorsMapper operatorsMapper,
			OperatorsQueryService operatorsQueryService,
			OperatorsCommandService operatorsCommandService,
			OperatorResponseAssembler operatorResponseAssembler,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.operatorsMapper = operatorsMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.operatorsCommandService = operatorsCommandService;
		this.operatorResponseAssembler = operatorResponseAssembler;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> dealerList(
			long companyId,
			Map<String, Object> jwtMap,
			String username,
			String contact,
			String mobile,
			String timeStart,
			String timeEnd,
			String openAccountStart,
			String openAccountEnd,
			int page,
			int pageSize) {
		if (page < 1) {
			throw new BadRequestException("page 须为正整数", 400);
		}
		if (pageSize < 1) {
			throw new BadRequestException("page_size 须为正整数", 400);
		}

		OperatorContext ctx = resolveOperatorContext(jwtMap);
		long jwtOperatorId = parseLongClaim(jwtMap.get("operator_id"));
		if ("dealer".equals(ctx.operatorTypeTrimmed())) {
			operatorsCommandService.initDealerParentIdForCurrentDealer(jwtOperatorId);
		}

		LambdaQueryWrapper<Operators> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Operators::getCompanyId, companyId).eq(Operators::getOperatorType, "dealer");

		if ("dealer".equals(ctx.operatorTypeTrimmed())) {
			wrapper.eq(Operators::getDealerParentId, String.valueOf(ctx.operatorIdForFilter()));
			wrapper.orderByAsc(Operators::getCreated);
		} else {
			wrapper.eq(Operators::getIsDealerMain, true);
			wrapper.orderByDesc(Operators::getCreated);
		}

		if (ValuePresence.hasEffectiveValue((Object) username)) {
			String pat = "%" + escapeSqlLike(username.trim()) + "%";
			wrapper.like(Operators::getUsername, pat);
		}
		if (ValuePresence.hasEffectiveValue((Object) contact)) {
			wrapper.eq(Operators::getContact, sensitiveFieldEncryptor.encrypt(contact.trim()));
		}
		if (ValuePresence.hasEffectiveValue((Object) mobile)) {
			wrapper.eq(Operators::getMobile, sensitiveFieldEncryptor.encrypt(mobile.trim()));
		}
		if (ValuePresence.hasEffectiveValue((Object) timeStart) && ValuePresence.hasEffectiveValue((Object) timeEnd)) {
			int startEpoch = parseTimeBoundary(timeStart.trim(), true);
			int endEpoch = parseTimeBoundary(timeEnd.trim(), false);
			wrapper.ge(Operators::getCreated, startEpoch).le(Operators::getCreated, endEpoch);
		}
		if (ValuePresence.hasEffectiveValue((Object) openAccountStart)
				&& ValuePresence.hasEffectiveValue((Object) openAccountEnd)) {
			wrapper.apply(
					"adapay_open_account_time >= {0} AND adapay_open_account_time <= {1}",
					openAccountStart.trim(),
					openAccountEnd.trim());
		}

		Page<Operators> mpPage = new Page<>(page, pageSize);
		IPage<Operators> result = operatorsMapper.selectPage(mpPage, wrapper);
		long total = result.getTotal();
		List<Map<String, Object>> list = new ArrayList<>();
		for (Operators op : result.getRecords()) {
			list.add(operatorResponseAssembler.toStatusMap(op));
		}
		int totalInt = (int) Math.min(total, Integer.MAX_VALUE);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalInt);
		data.put("list", list);
		data.put("count", totalInt);
		return data;
	}

	private record OperatorContext(String operatorTypeTrimmed, long operatorIdForFilter) {}

	private OperatorContext resolveOperatorContext(Map<String, Object> jwtMap) {
		String operatorTypeTrimmed =
				jwtMap.get("operator_type") == null ? "" : jwtMap.get("operator_type").toString().trim();
		long operatorIdForFilter = parseLongClaim(jwtMap.get("operator_id"));
		if ("distributor".equals(operatorTypeTrimmed)) {
			operatorIdForFilter = parseLongClaim(jwtMap.get("distributor_id"));
		}
		if ("dealer".equals(operatorTypeTrimmed)) {
			Map<String, Object> filter = new HashMap<>(2);
			filter.put("operator_id", operatorIdForFilter);
			Map<String, Object> operatorInfo = operatorsQueryService.getInfo(filter);
			if (operatorInfo == null || operatorInfo.isEmpty()) {
				throw new ResourceException("没有账号信息");
			}
			if (isDealerSubAccount(operatorInfo)) {
				long parentId = parseLongClaim(operatorInfo.get("dealer_parent_id"));
				if (parentId <= 0L) {
					throw new ResourceException("没有账号信息");
				}
				operatorIdForFilter = parentId;
			}
		}
		return new OperatorContext(operatorTypeTrimmed, operatorIdForFilter);
	}

	private static boolean isDealerSubAccount(Map<String, Object> operatorInfo) {
		if (!operatorInfo.containsKey("is_dealer_main")) {
			return false;
		}
		Object v = operatorInfo.get("is_dealer_main");
		if (v == null) {
			return false;
		}
		return !isTruthyDealerMain(v);
	}

	private static boolean isTruthyDealerMain(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim().toLowerCase(Locale.ROOT);
		if (s.isEmpty()) {
			return false;
		}
		if ("0".equals(s) || "false".equals(s)) {
			return false;
		}
		return true;
	}

	private static int parseTimeBoundary(String s, boolean startOfDayIfDateOnly) {
		try {
			LocalDateTime ldt = LocalDateTime.parse(s, DATE_TIME);
			return (int) ldt.atZone(SHANGHAI).toEpochSecond();
		} catch (DateTimeParseException e) {
			try {
				LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
				if (startOfDayIfDateOnly) {
					return (int) d.atStartOfDay(SHANGHAI).toEpochSecond();
				}
				return (int) d.atTime(23, 59, 59).atZone(SHANGHAI).toEpochSecond();
			} catch (DateTimeParseException e2) {
				throw new BadRequestException("时间参数格式不正确");
			}
		}
	}

	private static String escapeSqlLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static long parseLongClaim(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
