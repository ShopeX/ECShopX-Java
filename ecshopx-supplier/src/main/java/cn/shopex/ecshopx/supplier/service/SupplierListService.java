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

package cn.shopex.ecshopx.supplier.service;

import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.domain.dto.OperatorLoginNameRow;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOperatorLoginNameMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierListService {

	private static final ZoneId RESPONSE_TIME_ZONE = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter RESPONSE_TIME_DATE_PREFIX =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final SupplierMapper supplierMapper;
	private final SupplierOperatorLoginNameMapper supplierOperatorLoginNameMapper;

	public SupplierListService(
			SupplierMapper supplierMapper,
			SupplierOperatorLoginNameMapper supplierOperatorLoginNameMapper) {
		this.supplierMapper = supplierMapper;
		this.supplierOperatorLoginNameMapper = supplierOperatorLoginNameMapper;
	}

	public Map<String, Object> getSupplierList(
			long companyId,
			String rawIsCheck,
			String supplierNameTrimmedOrNull,
			String mobileTrimmedOrNull,
			String pageRaw,
			String pageSizeRaw,
			String acceptLanguageTag) {
		int page = Math.max(1, (int) intvalLike(pageRaw, 1));
		int pageSize = Math.max(1, (int) intvalLike(pageSizeRaw, 20));

		LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<Supplier>().eq(Supplier::getCompanyId, companyId);

		String supplierLikeValue = null;
		if (StringUtils.hasText(supplierNameTrimmedOrNull)) {
			supplierLikeValue = supplierNameTrimmedOrNull.trim();
			wrapper.like(Supplier::getSupplierName, supplierLikeValue);
		}

		String mobileLikeValue = null;
		if (StringUtils.hasText(mobileTrimmedOrNull)) {
			mobileLikeValue = mobileTrimmedOrNull.trim();
			wrapper.like(Supplier::getMobile, mobileLikeValue);
		}

		String isCheckS = rawIsCheck == null ? "" : rawIsCheck.trim();
		Long filterIsCheckLong = null;
		if (!isCheckS.isEmpty()) {
			BigDecimal bd = tryParseNumericBigDecimal(isCheckS);
			if (bd != null) {
				Long isCheckLongForDb = bigDecimalToLongForDbOrNull(bd);
				if (isCheckLongForDb != null) {
					wrapper.eq(Supplier::getIsCheck, isCheckLongForDb);
					filterIsCheckLong = isCheckLongForDb;
				}
			}
		}

		wrapper.orderByDesc(Supplier::getId);

		Page<Supplier> mpPage = new Page<>(page, pageSize, true);
		Page<Supplier> result = supplierMapper.selectPage(mpPage, wrapper);
		long totalCount = result.getTotal();
		List<Supplier> rows = totalCount > 0 ? result.getRecords() : List.of();

		LinkedHashMap<String, Object> filterMap = new LinkedHashMap<>();
		filterMap.put("company_id", Long.valueOf(companyId));
		if (supplierLikeValue != null) {
			filterMap.put("supplier_name|like", supplierLikeValue);
		}
		if (mobileLikeValue != null) {
			filterMap.put("mobile|like", mobileLikeValue);
		}
		if (filterIsCheckLong != null) {
			filterMap.put("is_check", filterIsCheckLong);
		}

		Map<Long, String> loginById = buildLoginNameMap(companyId, rows);

		List<Map<String, Object>> listMaps = new ArrayList<>(rows.size());
		for (Supplier e : rows) {
			listMaps.add(buildRow(e, acceptLanguageTag, loginById));
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", Long.valueOf(totalCount));
		out.put("list", listMaps);
		out.put("filter", filterMap);
		return out;
	}

	private Map<Long, String> buildLoginNameMap(long companyId, List<Supplier> rows) {
		Set<Long> idSet = new LinkedHashSet<>();
		for (Supplier e : rows) {
			Long opId = e.getOperatorId();
			if (opId != null && opId > 0L) {
				idSet.add(opId);
			}
		}
		if (idSet.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = new ArrayList<>(idSet);
		List<OperatorLoginNameRow> nameRows =
				supplierOperatorLoginNameMapper.selectLoginNameByOperatorIds(companyId, ids);
		LinkedHashMap<Long, String> map = new LinkedHashMap<>();
		for (OperatorLoginNameRow r : nameRows) {
			if (r.operatorId() != null) {
				map.put(r.operatorId(), r.loginName());
			}
		}
		return map;
	}

	private LinkedHashMap<String, Object> buildRow(Supplier e, String acceptLanguageTag, Map<Long, String> loginById) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", e.getId());
		row.put("company_id", e.getCompanyId());
		row.put("supplier_name", e.getSupplierName());
		row.put("contact", e.getContact());
		row.put("mobile", e.getMobile());
		row.put("business_license", e.getBusinessLicense());
		row.put("wechat_qrcode", e.getWechatQrcode());
		row.put("service_tel", e.getServiceTel());
		row.put("bank_name", e.getBankName());
		row.put("bank_account", e.getBankAccount());
		row.put("is_check", e.getIsCheck());
		row.put("audit_remark", e.getAuditRemark());
		row.put("adapay_mch_id", e.getAdapayMchId());
		row.put("wx_openid", e.getWxOpenid());
		row.put("operator_id", e.getOperatorId());
		row.put("add_time", formatResponseTime(e.getAddTime()));
		row.put("modify_time", formatResponseTime(e.getModifyTime()));

		Long ic = e.getIsCheck();
		if (ic != null) {
			switch (ic.intValue()) {
				case 0 -> row.put("check_state", resolveCheckStateMessage(acceptLanguageTag, 0));
				case 1 -> row.put("check_state", resolveCheckStateMessage(acceptLanguageTag, 1));
				case 2 -> row.put("check_state", resolveCheckStateMessage(acceptLanguageTag, 2));
				default -> {
					// no check_state
				}
			}
		}

		Long opId = e.getOperatorId();
		if (opId == null || opId <= 0L) {
			row.put("login_name", "?");
		} else if (!loginById.containsKey(opId)) {
			row.put("login_name", "?");
		} else {
			String v = loginById.get(opId);
			if (v == null) {
				row.put("login_name", "?");
			} else {
				row.put("login_name", v);
			}
		}

		return row;
	}

	private static String resolveCheckStateMessage(String acceptLanguageTag, int state) {
		Locale locale = resolveSupplierListLocale(acceptLanguageTag);
		String key = "check_state." + state;
		ResourceBundle bundle = ResourceBundle.getBundle("i18n.supplier_list", locale);
		try {
			return bundle.getString(key);
		} catch (MissingResourceException ex) {
			ResourceBundle fb = ResourceBundle.getBundle("i18n.supplier_list", Locale.ROOT);
			return fb.getString(key);
		}
	}

	private static Locale resolveSupplierListLocale(String acceptLanguageTag) {
		String raw = acceptLanguageTag == null ? "" : acceptLanguageTag.trim();
		if (raw.isEmpty()) {
			return Locale.SIMPLIFIED_CHINESE;
		}
		String first = raw.split(",")[0].trim().split(";")[0].trim();
		if (first.isEmpty()) {
			return Locale.SIMPLIFIED_CHINESE;
		}
		Locale parsed = Locale.forLanguageTag(first.replace('_', '-'));
		if (parsed.getLanguage().isEmpty() || "und".equalsIgnoreCase(parsed.getLanguage())) {
			return Locale.SIMPLIFIED_CHINESE;
		}
		return parsed;
	}

	private static BigDecimal tryParseNumericBigDecimal(String s) {
		try {
			return new BigDecimal(s);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static Long bigDecimalToLongForDbOrNull(BigDecimal bigDecimal) {
		try {
			return bigDecimal.setScale(0, RoundingMode.DOWN).longValueExact();
		} catch (ArithmeticException ex) {
			return null;
		}
	}

	private static long intvalLike(String raw, int defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return defaultVal;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static String formatResponseTime(LocalDateTime t) {
		if (t == null) {
			return null;
		}
		ZonedDateTime zdt = t.atZone(RESPONSE_TIME_ZONE);
		return zdt.format(RESPONSE_TIME_DATE_PREFIX);
	}
}
