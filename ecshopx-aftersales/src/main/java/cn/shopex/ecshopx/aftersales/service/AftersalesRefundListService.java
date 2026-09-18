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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.aftersales.domain.AftersalesOfflineRefund;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.dto.RefundAdminListQuery;
import cn.shopex.ecshopx.aftersales.dto.RefundLogExportQuery;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesOfflineRefundMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesRefundListService {

	public record RefundLogExportOperator(long companyId, long operatorId) {}

	private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
	private static final ObjectMapper JWT_JSON = new ObjectMapper();
	private static final TypeReference<List<Object>> JWT_DIST_LIST_TYPE = new TypeReference<>() {};

	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final AftersalesOfflineRefundMapper aftersalesOfflineRefundMapper;
	private final MembersUserIdByMobileLookupService membersUserIdByMobileLookupService;
	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;

	public AftersalesRefundListService(
			AftersalesRefundMapper aftersalesRefundMapper,
			AftersalesOfflineRefundMapper aftersalesOfflineRefundMapper,
			MembersUserIdByMobileLookupService membersUserIdByMobileLookupService,
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort) {
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.aftersalesOfflineRefundMapper = aftersalesOfflineRefundMapper;
		this.membersUserIdByMobileLookupService = membersUserIdByMobileLookupService;
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
	}

	public Object getRefundsDetail(String refundBn, HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		long companyId = longVal(companyIdObj);

		if (refundBn == null) {
			throw new BadRequestException("没有填写售后单号");
		}
		String t = refundBn.trim();
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			throw new BadRequestException("没有填写售后单号");
		}
		String refundBnForQuery = t;

		LambdaQueryWrapper<AftersalesRefund> w =
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.apply("refund_bn = {0}", refundBnForQuery);
		AftersalesRefund row = aftersalesRefundMapper.selectOne(w);
		if (row == null) {
			return List.of();
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>(refundToMap(row));

		if (!Objects.equals(String.valueOf(row.getRefundChannel()).trim(), "offline")) {
			return data;
		}

		LambdaQueryWrapper<AftersalesOfflineRefund> ow =
				new LambdaQueryWrapper<AftersalesOfflineRefund>()
						.apply("refund_bn = {0}", String.valueOf(row.getRefundBn()));
		AftersalesOfflineRefund off = aftersalesOfflineRefundMapper.selectOne(ow);
		if (off == null) {
			data.put("offline_refund", Collections.emptyList());
		} else {
			data.put("offline_refund", offlineRefundToMap(off));
		}
		return data;
	}

	public RefundLogExportOperator readRefundLogExportOperator(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		long companyId = longVal(companyIdObj);
		long operatorId = longVal(jwt.get("operator_id"));
		return new RefundLogExportOperator(companyId, operatorId);
	}

	public long countForRefundLogExport(RefundLogExportQuery q, HttpServletRequest request) {
		LinkedHashMap<String, Object> filter = buildRefundLogExportFilter(q, request);
		long companyId = longVal(filter.get("company_id"));
		return countByFilterMapForRefundLogExport(filter, companyId);
	}

	public long countByFilterMapForRefundLogExport(LinkedHashMap<String, Object> work, long companyId) {
		return aftersalesRefundMapper.selectCount(buildRefundLogExportWrapper(work, companyId));
	}

	public LinkedHashMap<String, Object> buildRefundLogExportFilter(RefundLogExportQuery q, HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		long companyId = longVal(companyIdObj);
		String operatorType = str(jwt.get("operator_type"));
		long operatorId = longVal(jwt.get("operator_id"));
		Object merchantIdObj = jwt.get("merchant_id");

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();

		if (ValuePresence.hasEffectiveValue(q.getTimeStartBegin()) && ValuePresence.hasEffectiveValue(q.getTimeStartEnd())) {
			filter.put("create_time|gte", q.getTimeStartBegin().trim());
			filter.put("create_time|lte", q.getTimeStartEnd().trim());
		}
		putIfHasText(filter, "refund_status", q.getRefundStatus());
		if (q.isRefundTypeParameterPresent()) {
			filter.put("refund_type", q.getRefundType());
		}
		putIfHasText(filter, "refund_bn", q.getRefundBn());
		putIfHasText(filter, "order_id", q.getOrderId());
		putIfHasText(filter, "refund_channel", q.getRefundChannel());

		putDistributorFilterFromRequest(
				filter,
				request,
				StringUtils.hasText(q.getDistributorIdRaw()) ? q.getDistributorIdRaw().trim() : null,
				null);
		if ("supplier".equalsIgnoreCase(operatorType)) {
			filter.put("supplier_id", (int) operatorId);
		}

		List<Long> jwtDist = extractOperatorDistributorIds(jwt.get("distributor_ids"));
		if (!jwtDist.isEmpty()) {
			Object cur = filter.get("distributor_id");
			if (cur instanceof Long one && one != 0L && !jwtDist.contains(one)) {
				filter.remove("distributor_id");
			} else if (cur instanceof List<?> list && !list.isEmpty()) {
				List<Long> inter =
						list.stream().map(AftersalesRefundListService::longVal).filter(jwtDist::contains).distinct().toList();
				if (inter.isEmpty()) {
					filter.remove("distributor_id");
				} else {
					filter.put("distributor_id", new ArrayList<>(inter));
				}
			} else if (cur == null) {
				filter.put("distributor_id", new ArrayList<>(jwtDist));
			}
		}
		filter.put("company_id", companyId);
		if ("merchant".equalsIgnoreCase(operatorType) && StringUtils.hasText(str(merchantIdObj))) {
			filter.put("merchant_id", longVal(merchantIdObj));
		}

		if (ValuePresence.hasEffectiveValue(q.getMobile())) {
			String mobilePlain = q.getMobile().trim();
			Long uid = membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(companyId, mobilePlain);
			filter.put("user_id", String.valueOf(uid != null ? uid : 0L));
		}

		return filter;
	}

	public LambdaQueryWrapper<AftersalesRefund> buildRefundLogExportWrapper(
			LinkedHashMap<String, Object> filter, long companyId) {
		LambdaQueryWrapper<AftersalesRefund> w = new LambdaQueryWrapper<>();
		w.eq(AftersalesRefund::getCompanyId, companyId);
		String rs = (String) filter.get("refund_status");
		if (StringUtils.hasText(rs)) {
			w.eq(AftersalesRefund::getRefundStatus, rs.trim());
		}
		if (filter.containsKey("refund_type")) {
			w.eq(AftersalesRefund::getRefundType, String.valueOf(filter.get("refund_type")));
		}
		eqLongFieldIfText(filter, w, "refund_bn", AftersalesRefund::getRefundBn);
		eqLongFieldIfText(filter, w, "aftersales_bn", AftersalesRefund::getAftersalesBn);
		eqLongFieldIfText(filter, w, "order_id", AftersalesRefund::getOrderId);
		eqLongFieldIfText(filter, w, "shop_id", AftersalesRefund::getShopId);
		eqLongFieldIfText(filter, w, "user_id", AftersalesRefund::getUserId);
		Object rc = filter.get("refund_channel");
		if (rc != null && StringUtils.hasText(String.valueOf(rc).trim())) {
			w.eq(AftersalesRefund::getRefundChannel, String.valueOf(rc).trim());
		}

		String gteRaw = (String) filter.get("create_time|gte");
		String lteRaw = (String) filter.get("create_time|lte");
		if (ValuePresence.hasEffectiveValue(gteRaw) && ValuePresence.hasEffectiveValue(lteRaw)) {
			int gteInt = parseTimeToEpochSeconds(gteRaw.trim());
			int lteInt = parseTimeToEpochSeconds(lteRaw.trim());
			w.ge(AftersalesRefund::getCreateTime, gteInt);
			w.le(AftersalesRefund::getCreateTime, lteInt);
		}

		if (filter.containsKey("merchant_id")) {
			w.eq(AftersalesRefund::getMerchantId, longVal(filter.get("merchant_id")));
		}
		if (filter.containsKey("supplier_id")) {
			w.eq(AftersalesRefund::getSupplierId, longVal(filter.get("supplier_id")));
		}
		Object dist = filter.get("distributor_id");
		if (dist instanceof Long dl) {
			w.eq(AftersalesRefund::getDistributorId, dl);
		} else if (dist instanceof List<?> list && !list.isEmpty()) {
			List<Long> ids = new ArrayList<>();
			for (Object o : list) {
				ids.add(longVal(o));
			}
			w.in(AftersalesRefund::getDistributorId, ids);
		}
		return w;
	}

	public Map<String, Object> refundEntityToMap(AftersalesRefund r) {
		return refundToMap(r);
	}

	public Map<String, Object> getRefundList(RefundAdminListQuery q, HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		long companyId = longVal(companyIdObj);
		String operatorType = str(jwt.get("operator_type"));
		long operatorId = longVal(jwt.get("operator_id"));
		Object merchantIdObj = jwt.get("merchant_id");

		int pageNum = 1;
		if (StringUtils.hasText(q.getPageRaw())) {
			try {
				int p = Integer.parseInt(q.getPageRaw().trim());
				pageNum = Math.max(1, p);
			} catch (NumberFormatException ignored) {
				pageNum = 1;
			}
		}
		int limit = 20;
		if (StringUtils.hasText(q.getPageSizeRaw())) {
			try {
				int parsed = Integer.parseInt(q.getPageSizeRaw().trim());
				limit = parsed <= 0 ? 20 : parsed;
			} catch (NumberFormatException ignored) {
				limit = 20;
			}
		}

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();

		if (ValuePresence.hasEffectiveValue(q.getTimeStartBegin())) {
			filter.put("create_time|gte", q.getTimeStartBegin().trim());
			filter.put("create_time|lte", q.getTimeStartEnd());
		}
		filter.put("refund_status", q.getRefundStatus());
		if (q.isRefundTypeParameterPresent()) {
			filter.put("refund_type", q.getRefundType());
		}
		putIfHasText(filter, "refund_bn", q.getRefundBn());
		putIfHasText(filter, "aftersales_bn", q.getAftersalesBn());
		putIfHasText(filter, "order_id", q.getOrderId());
		putIfHasText(filter, "shop_id", q.getShopId());
		putIfHasText(filter, "mobile", q.getMobile());
		putIfHasText(filter, "user_id", q.getUserId());
		putIfHasText(filter, "refund_channel", q.getRefundChannel());

		putDistributorFilterFromRequest(
				filter,
				request,
				StringUtils.hasText(q.getDistributorIdRaw()) ? q.getDistributorIdRaw().trim() : null,
				null);
		if ("supplier".equalsIgnoreCase(operatorType)) {
			filter.put("supplier_id", (int) operatorId);
		}

		List<Long> jwtDist = extractOperatorDistributorIds(jwt.get("distributor_ids"));
		if (!jwtDist.isEmpty()) {
			Object cur = filter.get("distributor_id");
			if (cur instanceof Long one && one != 0L && !jwtDist.contains(one)) {
				filter.remove("distributor_id");
			} else if (cur instanceof List<?> list && !list.isEmpty()) {
				List<Long> inter =
						list.stream().map(AftersalesRefundListService::longVal).filter(jwtDist::contains).distinct().toList();
				if (inter.isEmpty()) {
					filter.remove("distributor_id");
				} else {
					filter.put("distributor_id", new ArrayList<>(inter));
				}
			} else if (cur == null) {
				filter.put("distributor_id", new ArrayList<>(jwtDist));
			}
		}
		filter.put("company_id", companyId);
		if ("merchant".equalsIgnoreCase(operatorType) && StringUtils.hasText(str(merchantIdObj))) {
			filter.put("merchant_id", longVal(merchantIdObj));
		}

		if (filter.containsKey("mobile") && filter.containsKey("company_id")) {
			String mobilePlain = (String) filter.get("mobile");
			Long uid =
					membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(companyId, mobilePlain);
			if (uid == null || uid == 0L) {
				Map<String, Object> empty = new LinkedHashMap<>();
				empty.put("total_count", 0L);
				empty.put("list", List.of());
				return empty;
			}
			filter.remove("mobile");
			filter.put("user_id", String.valueOf(uid));
		}

		LambdaQueryWrapper<AftersalesRefund> wrapper = buildWrapper(filter, companyId);
		wrapper.orderByDesc(AftersalesRefund::getCreateTime).orderByDesc(AftersalesRefund::getRefundBn);

		long total = aftersalesRefundMapper.selectCount(wrapper);
		Page<AftersalesRefund> page = new Page<>(pageNum, limit, false);
		List<AftersalesRefund> rows = aftersalesRefundMapper.selectPage(page, wrapper).getRecords();

		List<Map<String, Object>> listOfMaps = new ArrayList<>();
		for (AftersalesRefund row : rows) {
			listOfMaps.add(refundToMap(row));
		}
		enrichDistributorInfo(companyId, listOfMaps);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", listOfMaps);
		return data;
	}

	public void enrichDistributorInfo(long companyId, List<Map<String, Object>> rows) {
		if (rows.isEmpty()) {
			return;
		}
		Set<Long> dids = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			dids.add(longVal(row.get("distributor_id")));
		}
		Map<Long, Map<String, Object>> distInfoById = new LinkedHashMap<>();
		for (Long did : dids) {
			if (did > 0L) {
				distInfoById.put(
						did, distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(companyId, did));
			}
		}
		for (Map<String, Object> row : rows) {
			long did = longVal(row.get("distributor_id"));
			if (did <= 0L) {
				Map<String, Object> di = new LinkedHashMap<>();
				di.put("name", "平台自营");
				row.put("distributor_info", di);
			} else {
				Map<String, Object> info = distInfoById.get(did);
				if (info != null && !info.isEmpty()) {
					row.put("distributor_info", info);
				} else {
					Map<String, Object> di = new LinkedHashMap<>();
					di.put("name", "平台自营");
					row.put("distributor_info", di);
				}
			}
		}
	}

	private static LambdaQueryWrapper<AftersalesRefund> buildWrapper(
			LinkedHashMap<String, Object> filter, long companyId) {
		LambdaQueryWrapper<AftersalesRefund> w = new LambdaQueryWrapper<>();
		w.eq(AftersalesRefund::getCompanyId, companyId);
		String rs = (String) filter.get("refund_status");
		if (StringUtils.hasText(rs)) {
			w.eq(AftersalesRefund::getRefundStatus, rs.trim());
		}
		if (filter.containsKey("refund_type")) {
			w.eq(AftersalesRefund::getRefundType, String.valueOf(filter.get("refund_type")));
		}
		eqLongFieldIfText(filter, w, "refund_bn", AftersalesRefund::getRefundBn);
		eqLongFieldIfText(filter, w, "aftersales_bn", AftersalesRefund::getAftersalesBn);
		eqLongFieldIfText(filter, w, "order_id", AftersalesRefund::getOrderId);
		eqLongFieldIfText(filter, w, "shop_id", AftersalesRefund::getShopId);
		eqLongFieldIfText(filter, w, "user_id", AftersalesRefund::getUserId);
		Object rc = filter.get("refund_channel");
		if (rc != null && StringUtils.hasText(String.valueOf(rc).trim())) {
			w.eq(AftersalesRefund::getRefundChannel, String.valueOf(rc).trim());
		}

		if (filter.containsKey("create_time|gte")) {
			String gteRaw = (String) filter.get("create_time|gte");
			if (ValuePresence.hasEffectiveValue(gteRaw)) {
				int gteInt = parseTimeToEpochSeconds(gteRaw.trim());
				w.ge(AftersalesRefund::getCreateTime, gteInt);
			}
		}
		String lteRaw = (String) filter.get("create_time|lte");
		if (ValuePresence.hasEffectiveValue(lteRaw)) {
			int lteInt = parseTimeToEpochSeconds(lteRaw.trim());
			w.le(AftersalesRefund::getCreateTime, lteInt);
		}

		if (filter.containsKey("merchant_id")) {
			w.eq(AftersalesRefund::getMerchantId, longVal(filter.get("merchant_id")));
		}
		if (filter.containsKey("supplier_id")) {
			w.eq(AftersalesRefund::getSupplierId, longVal(filter.get("supplier_id")));
		}
		Object dist = filter.get("distributor_id");
		if (dist instanceof Long dl) {
			w.eq(AftersalesRefund::getDistributorId, dl);
		} else if (dist instanceof List<?> list && !list.isEmpty()) {
			List<Long> ids = new ArrayList<>();
			for (Object o : list) {
				ids.add(longVal(o));
			}
			w.in(AftersalesRefund::getDistributorId, ids);
		}
		return w;
	}

	private static void eqLongFieldIfText(
			LinkedHashMap<String, Object> filter,
			LambdaQueryWrapper<AftersalesRefund> w,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<AftersalesRefund, Long> col) {
		Object v = filter.get(key);
		if (v == null) {
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		try {
			w.eq(col, Long.parseLong(s));
		} catch (NumberFormatException ignored) {
		}
	}

	private static int parseTimeToEpochSeconds(String raw) {
		Long sec = DateExpressionParser.parseToEpochSecond(raw, DEFAULT_ZONE);
		if (sec == null) {
			return 0;
		}
		long v = sec;
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}

	private static void putIfHasText(LinkedHashMap<String, Object> filter, String key, String value) {
		if (StringUtils.hasText(value)) {
			filter.put(key, value.trim());
		}
	}

	private static void putDistributorFilterFromRequest(
			LinkedHashMap<String, Object> filter,
			HttpServletRequest request,
			String queryDistributorId,
			List<String> queryDistributorIds) {
		Object attrId = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID);
		if (attrId != null && longVal(attrId) > 0L) {
			filter.put("distributor_id", longVal(attrId));
			return;
		}
		Object attrIds = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_IDS);
		if (attrIds instanceof Collection<?> attrCollection && !attrCollection.isEmpty()) {
			List<Long> ids =
					attrCollection.stream().map(AftersalesRefundListService::longVal).filter(id -> id > 0L).distinct().toList();
			if (!ids.isEmpty()) {
				filter.put("distributor_id", ids);
				return;
			}
		}
		if (queryDistributorId != null) {
			String raw = queryDistributorId.trim();
			if (!raw.isEmpty()) {
				long id = longVal(raw);
				if (id != 0L) {
					filter.put("distributor_id", id);
					return;
				}
			}
		}
		if (queryDistributorIds != null && !queryDistributorIds.isEmpty()) {
			List<Long> ids = new ArrayList<>();
			for (String s : queryDistributorIds) {
				if (s == null || s.isBlank()) {
					continue;
				}
				try {
					ids.add(Long.parseLong(s.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
			if (!ids.isEmpty()) {
				filter.put("distributor_id", ids);
			}
		}
	}

	private static Map<String, Object> offlineRefundToMap(AftersalesOfflineRefund o) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", o.getId());
		m.put("refund_bn", o.getRefundBn() == null ? null : String.valueOf(o.getRefundBn()));
		m.put("order_id", o.getOrderId());
		m.put("company_id", o.getCompanyId());
		m.put("refund_fee", o.getRefundFee());
		m.put("bank_account_name", o.getBankAccountName());
		m.put("bank_account_no", o.getBankAccountNo());
		m.put("bank_name", o.getBankName());
		m.put("refund_account_name", o.getRefundAccountName());
		m.put("refund_account_bank", o.getRefundAccountBank());
		m.put("refund_account_no", o.getRefundAccountNo());
		m.put("create_time", o.getCreateTime());
		m.put("update_time", o.getUpdateTime());
		return m;
	}

	private static Map<String, Object> refundToMap(AftersalesRefund r) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("refund_bn", r.getRefundBn() == null ? null : String.valueOf(r.getRefundBn()));
		m.put("aftersales_bn", r.getAftersalesBn());
		m.put("order_id", r.getOrderId());
		m.put("company_id", r.getCompanyId());
		m.put("user_id", r.getUserId());
		m.put("shop_id", r.getShopId());
		m.put("distributor_id", r.getDistributorId());
		m.put("refund_type", r.getRefundType());
		m.put("refund_channel", r.getRefundChannel());
		m.put("refund_status", r.getRefundStatus());
		m.put("refund_fee", r.getRefundFee());
		m.put("refunded_fee", r.getRefundedFee());
		m.put("refund_point", r.getRefundPoint());
		m.put("refunded_point", r.getRefundedPoint());
		m.put("return_point", r.getReturnPoint());
		m.put("return_freight", r.getReturnFreight());
		m.put("pay_type", r.getPayType());
		m.put("currency", r.getCurrency());
		m.put("refunds_memo", r.getRefundsMemo());
		m.put("refund_success_time", r.getRefundSuccessTime());
		m.put("refund_id", r.getRefundId());
		m.put("create_time", r.getCreateTime());
		m.put("update_time", r.getUpdateTime());
		m.put("cur_pay_fee", r.getCurPayFee());
		m.put("cur_fee_symbol", r.getCurFeeSymbol());
		m.put("cur_fee_rate", r.getCurFeeRate());
		m.put("cur_fee_type", r.getCurFeeType());
		m.put("hf_order_id", r.getHfOrderId());
		m.put("merchant_id", r.getMerchantId());
		m.put("freight", r.getFreight());
		m.put("freight_type", r.getFreightType());
		m.put("supplier_id", r.getSupplierId());
		m.put("trade_id", r.getTradeId());
		return m;
	}

	private static List<Long> extractOperatorDistributorIds(Object raw) {
		List<Long> out = new ArrayList<>();
		List<?> list = distributorIdsRawToList(raw);
		for (Object row : list) {
			if (row instanceof Map<?, ?> mm) {
				Object id = mm.get("distributor_id");
				if (id == null) {
					continue;
				}
				try {
					out.add(Long.parseLong(String.valueOf(id).trim()));
				} catch (NumberFormatException ignored) {
				}
			} else if (row instanceof Number n) {
				out.add(n.longValue());
			}
		}
		return out;
	}

	private static List<?> distributorIdsRawToList(Object raw) {
		if (raw instanceof List<?> list) {
			return list;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				List<Object> parsed = JWT_JSON.readValue(s, JWT_DIST_LIST_TYPE);
				return parsed != null ? parsed : List.of();
			} catch (JsonProcessingException e) {
				return List.of();
			}
		}
		return List.of();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
