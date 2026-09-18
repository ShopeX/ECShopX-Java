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

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.dto.AftersalesAdminListQuery;
import cn.shopex.ecshopx.aftersales.dto.AftersalesLogExportQuery;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.supplier.SupplierOperatorInfoReadPort;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.members.domain.MembersDeleteRecord;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesAdminListService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
	private static final DateTimeFormatter UPDATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final ObjectMapper JWT_JSON = new ObjectMapper();
	private static final TypeReference<List<Object>> JWT_DIST_LIST_TYPE = new TypeReference<>() {};

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AftersalesRefundService aftersalesRefundService;
	private final MembersDeleteRecordMapper membersDeleteRecordMapper;
	private final OperatorsMapper operatorsMapper;
	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;
	private final DistributorDefaultAftersalesAddressReadPort distributorDefaultAftersalesAddressReadPort;
	private final SupplierOperatorInfoReadPort supplierOperatorInfoReadPort;
	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final AftersalesAdminAppInfoBuilder aftersalesAdminAppInfoBuilder;

	public AftersalesAdminListService(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesRefundService aftersalesRefundService,
			MembersDeleteRecordMapper membersDeleteRecordMapper,
			OperatorsMapper operatorsMapper,
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort,
			DistributorDefaultAftersalesAddressReadPort distributorDefaultAftersalesAddressReadPort,
			SupplierOperatorInfoReadPort supplierOperatorInfoReadPort,
			OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort,
			AftersalesAdminAppInfoBuilder aftersalesAdminAppInfoBuilder) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesRefundService = aftersalesRefundService;
		this.membersDeleteRecordMapper = membersDeleteRecordMapper;
		this.operatorsMapper = operatorsMapper;
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
		this.distributorDefaultAftersalesAddressReadPort = distributorDefaultAftersalesAddressReadPort;
		this.supplierOperatorInfoReadPort = supplierOperatorInfoReadPort;
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
		this.aftersalesAdminAppInfoBuilder = aftersalesAdminAppInfoBuilder;
	}

	public Map<String, Object> getAftersalesList(AftersalesAdminListQuery q, HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		mergeOperatorJwt(request, merged);
		long companyId = longVal(merged.get("company_id"));
		String operatorType = str(merged.get("operator_type"));

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		if ("merchant".equalsIgnoreCase(operatorType)) {
			Object mid = merged.get("merchant_id");
			if (mid != null && StringUtils.hasText(String.valueOf(mid))) {
				filter.put("merchant_id", longVal(mid));
			}
		}
		if ("supplier".equalsIgnoreCase(operatorType)) {
			filter.put("supplier_id", (int) longVal(merged.get("operator_id")));
		}

		if (StringUtils.hasText(q.getTimeStartBegin())) {
			filter.put("create_time|gte", parseEpochOrZero(q.getTimeStartBegin()));
			filter.put("create_time|lte", parseEpochOrZero(q.getTimeStartEnd()));
		}
		if (q.getAftersalesStatus() != null && StringUtils.hasText(q.getAftersalesStatus())) {
			filter.put("aftersales_status", q.getAftersalesStatus().trim());
		}
		if (StringUtils.hasText(q.getAftersalesType())) {
			filter.put("aftersales_type", q.getAftersalesType().trim());
		}
		if (StringUtils.hasText(q.getAftersalesBn())) {
			String bn = q.getAftersalesBn().trim();
			if (bn.length() < 15) {
				filter.put("aftersales_bn|contains", bn);
			} else {
				filter.put("aftersales_bn", bn);
			}
		}
		if (StringUtils.hasText(q.getItemId())) {
			filter.put("item_id", q.getItemId().trim());
		}
		if (StringUtils.hasText(q.getItemBn())) {
			filter.put("item_bn|contains", q.getItemBn().trim());
		}
		if (StringUtils.hasText(q.getOrderId())) {
			String oid = q.getOrderId().trim();
			if (oid.length() < 16) {
				filter.put("order_id|contains", oid);
			} else {
				filter.put("order_id", oid);
			}
		}
		if (StringUtils.hasText(q.getReceiverMobile())) {
			applyReceiverMobileFilter(companyId, q.getReceiverMobile().trim(), filter);
		}
		if (StringUtils.hasText(q.getShopId())) {
			filter.put("shop_id", q.getShopId().trim());
		}
		if (StringUtils.hasText(q.getUserId())) {
			filter.put("user_id", q.getUserId().trim());
		}
		if (StringUtils.hasText(q.getMobile())) {
			filter.put("mobile", q.getMobile().trim());
		}

		applyDistributorRequestAndJwt(filter, q, request);

		if (q.isPrescriptionOrderFilterActive()) {
			filter.put("is_prescription_order", q.getIsPrescriptionOrderValue() == null ? "" : q.getIsPrescriptionOrderValue());
		}

		if (StringUtils.hasText(q.getOrderClass())) {
			applyOrderClassFilter(companyId, q.getOrderClass().trim(), filter);
		}

		if (StringUtils.hasText(q.getItemName())) {
			filter.put("item_name", q.getItemName().trim());
		}

		filter.put("need_order", true);

		boolean prescriptionJoin = q.isPrescriptionOrderFilterActive();
		processItemNameFilter(companyId, filter);

		if (isDistributorScopeEmpty(filter)) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}

		Map<String, Object> sqlFilter = buildSqlFilter(filter, q, prescriptionJoin);
		int limit = q.getPageSize();
		long offset = (long) (q.getPage() - 1) * limit;

		long total;
		List<Aftersales> mainRows;
		if (prescriptionJoin) {
			total = aftersalesMapper.countAdminListJoinOrder(sqlFilter);
			mainRows =
					total > 0
							? aftersalesMapper.selectAdminListJoinOrder(sqlFilter, offset, limit)
							: List.of();
		} else {
			total = aftersalesMapper.countAdminList(sqlFilter);
			mainRows = total > 0 ? aftersalesMapper.selectAdminList(sqlFilter, offset, limit) : List.of();
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (Aftersales row : mainRows) {
			list.add(aftersalesToRowMap(row));
		}

		if (!list.isEmpty()) {
			enrichServiceInternals(companyId, list, request);
			enrichActionBatch(companyId, list);
			applyFinalFormatting(list, request);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	public long countForLogExport(AftersalesLogExportQuery q, HttpServletRequest request) {
		LinkedHashMap<String, Object> business = buildLogExportBusinessFilter(q, request);
		if (business == null) {
			return 0L;
		}
		AftersalesAdminListQuery stub = logExportListQueryStub();
		return aftersalesMapper.countAdminList(buildSqlFilter(business, stub, false));
	}

	/**
	 * SQL 层 filter，与 {@link #countForLogExport} 使用同一套拼装，供异步导出复用。
	 *
	 * @return 当分销商筛选为空集时返回 null
	 */
	public LinkedHashMap<String, Object> buildLogExportSqlFilter(AftersalesLogExportQuery q, HttpServletRequest request) {
		LinkedHashMap<String, Object> business = buildLogExportBusinessFilter(q, request);
		if (business == null) {
			return null;
		}
		AftersalesAdminListQuery stub = logExportListQueryStub();
		return new LinkedHashMap<>(buildSqlFilter(business, stub, false));
	}

	public AdminOperatorSnapshot readAdminOperatorSnapshot(HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		mergeOperatorJwt(request, merged);
		return new AdminOperatorSnapshot(longVal(merged.get("company_id")), longVal(merged.get("operator_id")));
	}

	public record AdminOperatorSnapshot(long companyId, long operatorId) {}

	private static AftersalesAdminListQuery logExportListQueryStub() {
		AftersalesAdminListQuery stub = new AftersalesAdminListQuery();
		stub.setPage(1);
		stub.setPageSize(1);
		stub.setOrderByCreateTimeAsc(false);
		stub.setPrescriptionOrderFilterActive(false);
		return stub;
	}

	private LinkedHashMap<String, Object> buildLogExportBusinessFilter(AftersalesLogExportQuery q, HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		mergeOperatorJwt(request, merged);
		long companyId = longVal(merged.get("company_id"));
		String operatorType = str(merged.get("operator_type"));

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		if ("merchant".equalsIgnoreCase(operatorType)) {
			Object mid = merged.get("merchant_id");
			if (mid != null && StringUtils.hasText(String.valueOf(mid))) {
				filter.put("merchant_id", longVal(mid));
			}
		}
		if ("supplier".equalsIgnoreCase(operatorType)) {
			filter.put("supplier_id", (int) longVal(merged.get("operator_id")));
		}

		if (!isLooseEmpty(q.getTimeStartBegin()) && !isLooseEmpty(q.getTimeStartEnd())) {
			filter.put("create_time|gte", parseEpochOrZero(q.getTimeStartBegin().trim()));
			filter.put("create_time|lte", parseEpochOrZero(q.getTimeStartEnd().trim()));
		}

		if (StringUtils.hasText(q.getOrderId())) {
			String oid = q.getOrderId().trim();
			if (oid.length() < 16) {
				filter.put("order_id|contains", oid);
			} else {
				filter.put("order_id", oid);
			}
		}
		if (StringUtils.hasText(q.getReceiverMobile())) {
			applyReceiverMobileFilter(companyId, q.getReceiverMobile().trim(), filter);
		}
		if (StringUtils.hasText(q.getMobile())) {
			filter.put("mobile", q.getMobile().trim());
		}
		if (StringUtils.hasText(q.getAftersalesBn())) {
			String bn = q.getAftersalesBn().trim();
			if (bn.length() < 15) {
				filter.put("aftersales_bn|contains", bn);
			} else {
				filter.put("aftersales_bn", bn);
			}
		}
		if (q.getAftersalesStatus() != null && StringUtils.hasText(q.getAftersalesStatus())) {
			filter.put("aftersales_status", q.getAftersalesStatus().trim());
		}
		if (!isLooseEmpty(q.getAftersalesType())) {
			filter.put("aftersales_type", q.getAftersalesType().trim());
		}
		if (StringUtils.hasText(q.getOrderClass())) {
			applyOrderClassFilter(companyId, q.getOrderClass().trim(), filter);
		}

		applyDistributorRequestAndJwtForLogExport(filter, q, request);

		if (isDistributorScopeEmpty(filter)) {
			return null;
		}
		return filter;
	}

	private void applyDistributorRequestAndJwtForLogExport(
			LinkedHashMap<String, Object> filter, AftersalesLogExportQuery q, HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		List<Long> jwtDist = List.of();
		if (attr instanceof Map<?, ?> jwt) {
			jwtDist = extractOperatorDistributorIds(jwt.get("distributor_ids"));
		}

		putDistributorFilterFromRequest(
				filter,
				request,
				q.getDistributorId() != null && StringUtils.hasText(q.getDistributorId()) && !isLooseEmpty(q.getDistributorId())
						? q.getDistributorId().trim()
						: null,
				null);

		if (!jwtDist.isEmpty()) {
			Object cur = filter.get("distributor_id");
			if (cur instanceof Collection<?> c && !c.isEmpty()) {
				List<Long> req = c.stream().map(AftersalesAdminListService::longVal).distinct().toList();
				List<Long> inter = req.stream().filter(jwtDist::contains).toList();
				if (inter.isEmpty()) {
					filter.put("distributor_id", List.of());
				} else {
					filter.put("distributor_id", inter);
				}
			} else if (cur != null && StringUtils.hasText(String.valueOf(cur))) {
				long one = longVal(cur);
				if (!jwtDist.contains(one)) {
					filter.remove("distributor_id");
				}
			} else {
				filter.put("distributor_id", jwtDist);
			}
		}

		Object dist = filter.get("distributor_id");
		if (dist != null) {
			if (dist instanceof Collection<?> dc) {
				if (dc.isEmpty()) {
					filter.put("distributor_or_empty", true);
				} else {
					List<Long> ids = dc.stream().map(AftersalesAdminListService::longVal).distinct().toList();
					filter.remove("distributor_id");
					filter.put("distributor_or_ids", ids);
				}
			} else if (StringUtils.hasText(String.valueOf(dist))) {
				long id = longVal(dist);
				if (id != 0L) {
					filter.remove("distributor_id");
					filter.put("distributor_or_ids", List.of(id));
				}
			}
		}
	}

	private static boolean isLooseEmpty(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof Boolean b) {
			return !b;
		}
		if (o instanceof Number n) {
			if (o instanceof Double d) {
				return d == 0.0;
			}
			if (o instanceof Float f) {
				return f == 0.0f;
			}
			return n.longValue() == 0L;
		}
		if (o instanceof CharSequence s) {
			String t = s.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (o instanceof Character c) {
			return c == '0' || c == 0;
		}
		if (o instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (o instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (o instanceof Object[] arr) {
			return arr.length == 0;
		}
		return false;
	}

	private void mergeOperatorJwt(HttpServletRequest request, LinkedHashMap<String, Object> merged) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		merged.put("company_id", longVal(companyIdObj));
		merged.put("operator_type", str(jwt.get("operator_type")));
		merged.put("operator_id", longVal(jwt.get("operator_id")));
		merged.put("merchant_id", jwt.get("merchant_id"));
	}

	private void applyDistributorRequestAndJwt(
			LinkedHashMap<String, Object> filter, AftersalesAdminListQuery q, HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		List<Long> jwtDist = List.of();
		if (attr instanceof Map<?, ?> jwt) {
			jwtDist = extractOperatorDistributorIds(jwt.get("distributor_ids"));
		}

		putDistributorFilterFromRequest(filter, request, q.getDistributorId(), q.getDistributorIds());

		if (!jwtDist.isEmpty()) {
			Object cur = filter.get("distributor_id");
			if (cur instanceof Collection<?> c && !c.isEmpty()) {
				List<Long> req = c.stream().map(AftersalesAdminListService::longVal).distinct().toList();
				List<Long> inter = req.stream().filter(jwtDist::contains).toList();
				if (inter.isEmpty()) {
					filter.put("distributor_id", List.of());
				} else {
					filter.put("distributor_id", inter);
				}
			} else if (cur != null && StringUtils.hasText(String.valueOf(cur))) {
				long one = longVal(cur);
				if (!jwtDist.contains(one)) {
					filter.remove("distributor_id");
				}
			} else {
				filter.put("distributor_id", jwtDist);
			}
		}

		Object dist = filter.get("distributor_id");
		if (dist != null) {
			if (dist instanceof Collection<?> dc) {
				if (dc.isEmpty()) {
					filter.put("distributor_or_empty", true);
				} else {
					List<Long> ids = dc.stream().map(AftersalesAdminListService::longVal).distinct().toList();
					filter.remove("distributor_id");
					filter.put("distributor_or_ids", ids);
				}
			} else if (StringUtils.hasText(String.valueOf(dist))) {
				long id = longVal(dist);
				if (id != 0L) {
					filter.remove("distributor_id");
					filter.put("distributor_or_ids", List.of(id));
				}
			}
		}
	}

	private static boolean isDistributorScopeEmpty(LinkedHashMap<String, Object> filter) {
		return Boolean.TRUE.equals(filter.get("distributor_or_empty"));
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
					attrCollection.stream().map(AftersalesAdminListService::longVal).filter(id -> id > 0L).distinct().toList();
			if (!ids.isEmpty()) {
				filter.put("distributor_id", ids);
				return;
			}
		}
		if (queryDistributorId != null) {
			String raw = queryDistributorId.trim();
			if (!raw.isEmpty()) {
				filter.put("distributor_id", longVal(raw));
				return;
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

	private void applyOrderClassFilter(long companyId, String orderClass, LinkedHashMap<String, Object> filter) {
		List<Long> ids = aftersalesMapper.selectOrderIdsByCompanyAndOrderClass(companyId, orderClass);
		if (ids.isEmpty()) {
			filter.put("order_id", List.of(-1L));
		} else {
			filter.put("order_id", ids);
		}
	}

	private void applyReceiverMobileFilter(long companyId, String plainMobile, LinkedHashMap<String, Object> filter) {
		String enc = LegacyFixedMobileEncrypt.fixedEncryptMobile(plainMobile);
		List<Long> orderIds =
				aftersalesMapper.selectOrderIdsByCompanyAndReceiverMobile(companyId, plainMobile, enc).stream()
						.filter(Objects::nonNull)
						.distinct()
						.toList();
		if (orderIds.isEmpty()) {
			filter.put("aftersales_bn", List.of(-1L));
			return;
		}
		Object ex = filter.get("order_id");
		if (ex == null) {
			filter.put("order_id", orderIds);
		} else if (ex instanceof Collection<?> c) {
			Set<Long> fromFilter = new LinkedHashSet<>();
			for (Object o : c) {
				fromFilter.add(longVal(o));
			}
			Set<Long> fromMobile = new LinkedHashSet<>(orderIds);
			List<Long> inter =
					fromFilter.stream().filter(fromMobile::contains).distinct().toList();
			if (inter.isEmpty()) {
				filter.put("aftersales_bn", List.of(-1L));
			} else {
				filter.put("order_id", inter);
			}
		} else {
			long single = longVal(ex);
			if (!orderIds.contains(single)) {
				filter.put("aftersales_bn", List.of(-1L));
			} else {
				filter.put("order_id", single);
			}
		}
	}

	private void processItemNameFilter(long companyId, LinkedHashMap<String, Object> filter) {
		Object rawName = filter.remove("item_name");
		if (rawName == null || !StringUtils.hasText(String.valueOf(rawName))) {
			return;
		}
		String itemName = String.valueOf(rawName).trim();
		List<Long> itemIds =
				aftersalesMapper.selectItemIdsByCompanyAndItemNameContains(companyId, "%" + itemName + "%");
		if (itemIds.isEmpty()) {
			filter.put("aftersales_bn", List.of(-1L));
			return;
		}
		List<AftersalesDetail> details =
				aftersalesDetailMapper.selectList(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.in(AftersalesDetail::getItemId, itemIds));
		if (details.isEmpty()) {
			filter.put("aftersales_bn", List.of(-1L));
			return;
		}
		List<Long> bns =
				details.stream()
						.map(AftersalesDetail::getAftersalesBn)
						.filter(Objects::nonNull)
						.distinct()
						.toList();
		intersectAftersalesBn(filter, bns);
	}

	private static void intersectAftersalesBn(LinkedHashMap<String, Object> filter, List<Long> candidateBns) {
		Object ex = filter.get("aftersales_bn");
		if (ex == null) {
			filter.put("aftersales_bn", candidateBns);
			return;
		}
		Set<Long> cand = new LinkedHashSet<>(candidateBns);
		if (ex instanceof Collection<?> c) {
			Set<Long> keep = new LinkedHashSet<>();
			for (Object o : c) {
				long bn = longVal(o);
				if (cand.contains(bn)) {
					keep.add(bn);
				}
			}
			filter.put("aftersales_bn", keep.isEmpty() ? List.of(-1L) : new ArrayList<>(keep));
		} else {
			long one = longVal(ex);
			if (cand.contains(one)) {
				filter.put("aftersales_bn", one);
			} else {
				filter.put("aftersales_bn", List.of(-1L));
			}
		}
	}

	private Map<String, Object> buildSqlFilter(LinkedHashMap<String, Object> business, AftersalesAdminListQuery q, boolean join) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", longVal(business.get("company_id")));
		Object gte = business.get("create_time|gte");
		if (gte != null) {
			m.put("create_time_gte", (int) longVal(gte));
		}
		Object lte = business.get("create_time|lte");
		if (lte != null) {
			m.put("create_time_lte", (int) longVal(lte));
		}
		Object st = business.get("aftersales_status");
		if (st != null && StringUtils.hasText(String.valueOf(st))) {
			m.put("aftersales_status", String.valueOf(st).trim());
		}
		Object at = business.get("aftersales_type");
		if (at != null && StringUtils.hasText(String.valueOf(at))) {
			m.put("aftersales_type", String.valueOf(at).trim());
		}

		Object ab = business.get("aftersales_bn");
		if (ab != null) {
			if (ab instanceof Collection<?> c) {
				List<Long> bns = c.stream().map(AftersalesAdminListService::longVal).toList();
				if (bns.size() == 1 && bns.get(0) == -1L) {
					m.put("aftersales_impossible", true);
				} else {
					m.put("aftersales_bn_in", bns);
				}
			} else {
				long v = longVal(ab);
				if (v == -1L) {
					m.put("aftersales_impossible", true);
				} else {
					String s = String.valueOf(ab).trim();
					if (s.length() < 15) {
						m.put("aftersales_bn_like", "%" + s + "%");
					} else {
						m.put("aftersales_bn_eq", v);
					}
				}
			}
		}
		Object abc = business.get("aftersales_bn|contains");
		if (abc != null && StringUtils.hasText(String.valueOf(abc))) {
			m.put("aftersales_bn_like", "%" + String.valueOf(abc).trim() + "%");
		}

		Object iid = business.get("item_id");
		if (iid != null && StringUtils.hasText(String.valueOf(iid))) {
			m.put("item_id", String.valueOf(iid).trim());
		}
		Object ibc = business.get("item_bn|contains");
		if (ibc != null && StringUtils.hasText(String.valueOf(ibc))) {
			m.put("item_bn_like", "%" + String.valueOf(ibc).trim() + "%");
		}

		Object oid = business.get("order_id");
		if (oid != null) {
			if (oid instanceof Collection<?> c) {
				List<Long> ids = c.stream().map(AftersalesAdminListService::longVal).toList();
				if (ids.size() == 1 && ids.get(0) == -1L) {
					m.put("order_impossible", true);
				} else {
					m.put("order_id_in", ids);
				}
			} else {
				m.put("order_id_eq", longVal(oid));
			}
		}
		Object oc = business.get("order_id|contains");
		if (oc != null && StringUtils.hasText(String.valueOf(oc))) {
			m.put("order_id_like", "%" + String.valueOf(oc).trim() + "%");
		}

		Object sid = business.get("shop_id");
		if (sid != null && StringUtils.hasText(String.valueOf(sid))) {
			m.put("shop_id", String.valueOf(sid).trim());
		}
		Object uid = business.get("user_id");
		if (uid != null && StringUtils.hasText(String.valueOf(uid))) {
			m.put("user_id", String.valueOf(uid).trim());
		}
		Object mob = business.get("mobile");
		if (mob != null && StringUtils.hasText(String.valueOf(mob))) {
			m.put("mobile_cipher", LegacyFixedMobileEncrypt.fixedEncryptMobile(String.valueOf(mob).trim()));
		}
		Object mid = business.get("merchant_id");
		if (mid != null && StringUtils.hasText(String.valueOf(mid))) {
			m.put("merchant_id", longVal(mid));
		}
		Object sup = business.get("supplier_id");
		if (sup != null && StringUtils.hasText(String.valueOf(sup))) {
			m.put("supplier_id", (int) longVal(sup));
		}

		@SuppressWarnings("unchecked")
		List<Long> dor = (List<Long>) business.get("distributor_or_ids");
		if (dor != null && !dor.isEmpty()) {
			m.put("distributor_or_ids", dor);
		}

		m.put("order_by_create_time_asc", q.isOrderByCreateTimeAsc());

		if (join) {
			String pv = business.get("is_prescription_order") == null ? "" : String.valueOf(business.get("is_prescription_order"));
			if ("0".equals(pv)) {
				m.put("o_prescription_eq0", true);
			} else if ("1".equals(pv)) {
				m.put("o_prescription_gt0", true);
			}
		}
		return m;
	}

	private void enrichServiceInternals(long companyId, List<Map<String, Object>> rows, HttpServletRequest request) {
		Set<Long> selfOpIds =
				rows.stream()
						.map(r -> longVal(r.get("self_delivery_operator_id")))
						.filter(id -> id > 0L)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, Operators> opById = new HashMap<>();
		if (!selfOpIds.isEmpty()) {
			List<Operators> ops =
					operatorsMapper.selectList(
							new LambdaQueryWrapper<Operators>().in(Operators::getOperatorId, selfOpIds));
			for (Operators o : ops) {
				opById.put(o.getOperatorId(), o);
			}
		}

		Set<Long> distIdsForAddr =
				rows.stream()
						.map(r -> longVal(r.get("distributor_id")))
						.filter(id -> id > 0L)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, Map<String, Object>> defaultAddrByDist = new HashMap<>();
		for (Long did : distIdsForAddr) {
			Optional<Map<String, Object>> def = distributorDefaultAftersalesAddressReadPort.findDefaultAddress(companyId, did);
			def.ifPresent(map -> defaultAddrByDist.put(did, map));
		}

		Set<Long> allDistIds =
				rows.stream()
						.map(r -> longVal(r.get("distributor_id")))
						.filter(id -> id >= 0L)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, Map<String, Object>> distInfoById = new HashMap<>();
		for (Long did : allDistIds) {
			if (did <= 0L) {
				continue;
			}
			distInfoById.put(did, distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(companyId, did));
		}

		List<Long> pageOrderIds =
				rows.stream().map(r -> longVal(r.get("order_id"))).filter(oid -> oid > 0L).distinct().toList();
		Map<Long, List<Map<String, Object>>> orderItemsByOrderId = new HashMap<>();
		if (!pageOrderIds.isEmpty()) {
			List<Map<String, Object>> allOi =
					aftersalesMapper.selectNormalOrderItemsByCompanyAndOrderIds(companyId, pageOrderIds);
			for (Map<String, Object> oi : allOi) {
				long oid = longVal(oi.get("order_id"));
				orderItemsByOrderId.computeIfAbsent(oid, k -> new ArrayList<>()).add(oi);
			}
		}

		Map<Long, Optional<Map<String, Object>>> orderHeaderCache = new HashMap<>();

		applyUserDeleteFlags(companyId, rows);

		for (Map<String, Object> row : rows) {
			long selfId = longVal(row.get("self_delivery_operator_id"));
			if (selfId > 0L && opById.containsKey(selfId)) {
				Operators op = opById.get(selfId);
				row.put("self_delivery_operator_mobile", op.getMobile() == null ? "" : op.getMobile());
				row.put("self_delivery_operator_name", op.getUsername() == null ? "" : op.getUsername());
			} else {
				row.put("self_delivery_operator_mobile", "");
				row.put("self_delivery_operator_name", "");
			}

			Object addrFieldObj = row.get("aftersales_address");
			String addrForRule =
					addrFieldObj == null
							? null
							: (addrFieldObj instanceof String s ? s : String.valueOf(addrFieldObj));
			long distributorId = longVal(row.get("distributor_id"));
			boolean needAddr = needDefaultAddress(addrForRule);
			if (needAddr && distributorId > 0L && defaultAddrByDist.containsKey(distributorId)) {
				row.put("aftersales_address", defaultAddrByDist.get(distributorId));
			}

			long abn = longVal(row.get("aftersales_bn"));
			List<AftersalesDetail> detailRows =
					aftersalesDetailMapper.selectList(
							new LambdaQueryWrapper<AftersalesDetail>()
									.eq(AftersalesDetail::getCompanyId, companyId)
									.eq(AftersalesDetail::getAftersalesBn, abn)
									.eq(AftersalesDetail::getUserId, longVal(row.get("user_id"))));

			List<Map<String, Object>> detailMaps = new ArrayList<>();
			for (AftersalesDetail d : detailRows) {
				detailMaps.add(detailToMap(d));
			}

			long orderId = longVal(row.get("order_id"));
			if (orderId > 0L) {
				attachDetail(companyId, orderId, detailMaps, orderItemsByOrderId);
			}
			row.put("detail", detailMaps);
			row.put(
					"app_info",
					aftersalesAdminAppInfoBuilder.buildAppInfo(row, orderId, companyId, orderHeaderCache, false));

			long did = longVal(row.get("distributor_id"));
			if (did <= 0L) {
				Map<String, Object> di = new LinkedHashMap<>();
				di.put("name", "平台自营");
				row.put("distributor_info", di);
			} else {
				row.put("distributor_info", distInfoById.getOrDefault(did, Map.of()));
			}
		}

		fillDetailPrescription(companyId, rows);
	}

	private void fillDetailPrescription(long companyId, List<Map<String, Object>> rows) {
		List<Long> subIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> details = (List<Map<String, Object>>) row.get("detail");
			if (details == null) {
				continue;
			}
			for (Map<String, Object> d : details) {
				Object sid = d.get("sub_order_id");
				if (sid != null) {
					long id = longVal(sid);
					if (id > 0L) {
						subIds.add(id);
					}
				}
			}
		}
		if (subIds.isEmpty()) {
			return;
		}
		List<Map<String, Object>> prRows = aftersalesMapper.selectSubOrderPrescriptionBySubIds(companyId, subIds);
		Map<Long, Object> prescBySubId = new HashMap<>();
		for (Map<String, Object> pr : prRows) {
			prescBySubId.put(longVal(pr.get("id")), pr.get("is_prescription"));
		}
		for (Map<String, Object> row : rows) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> details = (List<Map<String, Object>>) row.get("detail");
			if (details == null) {
				continue;
			}
			for (Map<String, Object> d : details) {
				long sid = longVal(d.get("sub_order_id"));
				if (prescBySubId.containsKey(sid)) {
					d.put("is_prescription", prescBySubId.get(sid));
				}
			}
		}
	}

	private void attachDetail(
			long companyId,
			long orderId,
			List<Map<String, Object>> detailMaps,
			Map<Long, List<Map<String, Object>>> orderItemsByOrderId) {
		List<Map<String, Object>> orderitemList = orderItemsByOrderId.getOrDefault(orderId, List.of());
		Map<Long, Map<String, Object>> byItemId = new HashMap<>();
		Set<Long> supplierIds = new LinkedHashSet<>();
		for (Map<String, Object> oi : orderitemList) {
			byItemId.put(longVal(oi.get("item_id")), oi);
			long sup = longVal(oi.get("supplier_id"));
			if (sup > 0L) {
				supplierIds.add(sup);
			}
		}
		// PHP attachDetail: supplier_name on detail/orderItem is the full row {operator_id, supplier_name}
		Map<Long, Object> supplierNameByOp = new HashMap<>();
		for (Long sid : supplierIds) {
			Map<String, Object> inf = supplierOperatorInfoReadPort.getInfo(companyId, sid);
			if (inf == null || inf.isEmpty()) {
				continue;
			}
			Map<String, Object> slim = new LinkedHashMap<>(2);
			slim.put("operator_id", inf.get("operator_id"));
			slim.put("supplier_name", inf.get("supplier_name"));
			supplierNameByOp.put(sid, slim);
		}
		for (Map<String, Object> d : detailMaps) {
			long iid = longVal(d.get("item_id"));
			Map<String, Object> oi = byItemId.get(iid);
			if (oi != null) {
				Map<String, Object> oiMap = orderItemRowToMap(oi);
				long supOp = longVal(oi.get("supplier_id"));
				oiMap.put("supplier_name", supOp > 0L ? supplierNameByOp.getOrDefault(supOp, "") : "");
				d.put("orderItem", oiMap);
				d.put("supplier_name", oiMap.get("supplier_name"));
			} else {
				d.put("orderItem", null);
				d.put("supplier_name", "");
			}
		}
	}

	private static Map<String, Object> orderItemRowToMap(Map<String, Object> oi) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", oi.get("id"));
		m.put("order_id", oi.get("order_id"));
		m.put("item_id", oi.get("item_id"));
		m.put("item_bn", oi.get("item_bn"));
		m.put("item_name", oi.get("item_name"));
		m.put("supplier_id", oi.get("supplier_id"));
		m.put("num", oi.get("num"));
		return m;
	}

	private void applyUserDeleteFlags(long companyId, List<Map<String, Object>> rows) {
		List<Long> userIds =
				rows.stream()
						.map(r -> longVal(r.get("user_id")))
						.filter(id -> id > 0L)
						.distinct()
						.toList();
		if (userIds.isEmpty()) {
			for (Map<String, Object> r : rows) {
				r.put("user_delete", false);
			}
			return;
		}
		List<MembersDeleteRecord> recs =
				membersDeleteRecordMapper.selectList(
						new LambdaQueryWrapper<MembersDeleteRecord>()
								.eq(MembersDeleteRecord::getCompanyId, companyId)
								.in(MembersDeleteRecord::getUserId, userIds));
		Set<Long> deleted = recs.stream().map(MembersDeleteRecord::getUserId).collect(Collectors.toSet());
		for (Map<String, Object> r : rows) {
			long uid = longVal(r.get("user_id"));
			r.put("user_delete", uid > 0L && deleted.contains(uid));
		}
	}

	private void enrichActionBatch(long companyId, List<Map<String, Object>> rows) {
		List<Long> orderIds =
				rows.stream().map(r -> longVal(r.get("order_id"))).filter(id -> id > 0L).distinct().toList();
		Map<Long, Map<String, Object>> orderById = new HashMap<>();
		if (!orderIds.isEmpty()) {
			List<Map<String, Object>> ords =
					aftersalesMapper.selectNormalOrderHeadersByOrderIds(companyId, orderIds);
			for (Map<String, Object> o : ords) {
				orderById.put(longVal(o.get("order_id")), o);
			}
		}

		Set<Long> supplierIds =
				rows.stream()
						.map(r -> longVal(r.get("supplier_id")))
						.filter(id -> id > 0L)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, String> supplierName = new HashMap<>();
		for (Long sid : supplierIds) {
			Map<String, Object> inf = supplierOperatorInfoReadPort.getInfo(companyId, sid);
			supplierName.put(sid, str(inf.get("supplier_name")));
		}

		List<Long> aftersalesBns =
				rows.stream().map(r -> longVal(r.get("aftersales_bn"))).filter(b -> b > 0L).distinct().toList();
		List<AftersalesRefund> refunds = aftersalesRefundService.listByCompanyAndAftersalesBns(companyId, aftersalesBns);
		Map<Long, AftersalesRefund> refundByBn = new HashMap<>();
		for (AftersalesRefund r : refunds) {
			if (r.getAftersalesBn() != null) {
				refundByBn.put(r.getAftersalesBn(), r);
			}
		}

		Set<Long> salesmanIds = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			Map<String, Object> o = orderById.get(longVal(row.get("order_id")));
			if (o != null) {
				long sm = longVal(o.get("salesman_id"));
				if (sm > 0L) {
					salesmanIds.add(sm);
				}
			}
		}
		Map<Long, String> workUserBySalesId = new HashMap<>();
		if (!salesmanIds.isEmpty()) {
			List<Map<String, Object>> sprows =
					aftersalesMapper.selectShoppingGuideWorkUseridBySalespersonIds(companyId, new ArrayList<>(salesmanIds));
			for (Map<String, Object> sp : sprows) {
				long spid = longVal(sp.get("salesperson_id"));
				workUserBySalesId.put(spid, sp.get("work_userid") == null ? "" : String.valueOf(sp.get("work_userid")));
			}
		}

		Map<Long, Map<String, Object>> itemRowByBn = new HashMap<>();
		if (!aftersalesBns.isEmpty()) {
			List<Map<String, Object>> raw = aftersalesMapper.selectItemNamesByAftersalesBns(companyId, aftersalesBns);
			for (Map<String, Object> m : raw) {
				long bn = longVal(m.get("aftersales_bn"));
				itemRowByBn.put(bn, m);
			}
		}

		for (Map<String, Object> row : rows) {
			long oid = longVal(row.get("order_id"));
			Map<String, Object> ord = orderById.get(oid);
			if (ord != null) {
				row.put("receiver_mobile", ord.get("receiver_mobile") == null ? "" : String.valueOf(ord.get("receiver_mobile")));
				row.put("order_holder", ord.get("order_holder") == null ? "" : String.valueOf(ord.get("order_holder")));
				long sm = longVal(ord.get("salesman_id"));
				row.put("salesman_name", workUserBySalesId.getOrDefault(sm, ""));
			} else {
				row.put("receiver_mobile", "");
				row.put("order_holder", "");
				row.put("salesman_name", "");
			}
			long sup = longVal(row.get("supplier_id"));
			row.put("supplier_name", sup > 0L ? supplierName.getOrDefault(sup, "") : "");
			long bn = longVal(row.get("aftersales_bn"));
			AftersalesRefund rf = refundByBn.get(bn);
			row.put("refunded_fee", rf == null || rf.getRefundedFee() == null ? 0 : rf.getRefundedFee());
			row.put("refunded_point", rf == null || rf.getRefundedPoint() == null ? 0 : rf.getRefundedPoint());
			Map<String, Object> it = itemRowByBn.get(bn);
			row.put("item_name", it == null || it.get("item_name") == null ? "" : String.valueOf(it.get("item_name")));
			row.put("item_bn", it == null || it.get("item_bn") == null ? "" : String.valueOf(it.get("item_bn")));
		}
	}

	private void applyFinalFormatting(List<Map<String, Object>> rows, HttpServletRequest request) {
		boolean mask = datapassTruthy(request);
		for (Map<String, Object> row : rows) {
			Object fee = row.get("refund_fee");
			if (fee instanceof Number n) {
				BigDecimal yuan =
						BigDecimal.valueOf(n.longValue()).divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
				row.put("refund_fee", yuan);
			}
			Object ut = row.get("update_time");
			if (ut instanceof Number n) {
				long sec = n.longValue();
				if (sec > 0) {
					row.put(
							"update_time",
							Instant.ofEpochSecond(sec).atZone(DEFAULT_ZONE).format(UPDATE_TIME_FMT));
				}
			}
			if (mask) {
				Object mob = row.get("mobile");
				row.put("mobile", DataMasking.maskMobile(mob == null ? "" : String.valueOf(mob)));
			}
		}
	}

	private static boolean datapassTruthy(HttpServletRequest request) {
		String h = request.getHeader("x-datapass-block");
		if (StringUtils.hasText(h)) {
			return true;
		}
		String p = request.getParameter("x-datapass-block");
		if (StringUtils.hasText(p)) {
			return true;
		}
		Object a = request.getAttribute("x-datapass-block");
		return a != null && StringUtils.hasText(String.valueOf(a));
	}

	private static Map<String, Object> detailToMap(AftersalesDetail d) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("detail_id", d.getDetailId());
		m.put("company_id", d.getCompanyId());
		m.put("distributor_id", d.getDistributorId());
		m.put("user_id", d.getUserId());
		m.put("aftersales_bn", d.getAftersalesBn());
		m.put("order_id", d.getOrderId());
		m.put("sub_order_id", d.getSubOrderId());
		m.put("goods_id", d.getGoodsId());
		m.put("item_id", d.getItemId());
		m.put("item_bn", d.getItemBn());
		m.put("item_name", d.getItemName());
		m.put("order_item_type", d.getOrderItemType());
		m.put("item_pic", d.getItemPic());
		m.put("num", d.getNum());
		m.put("refund_fee", d.getRefundFee());
		m.put("refund_point", d.getRefundPoint());
		m.put("aftersales_type", d.getAftersalesType());
		m.put("progress", d.getProgress());
		m.put("aftersales_status", d.getAftersalesStatus());
		m.put("create_time", d.getCreateTime());
		m.put("update_time", d.getUpdateTime());
		m.put("auto_refuse_time", d.getAutoRefuseTime());
		m.put("refunded_num", d.getRefundedNum());
		return m;
	}

	private static Map<String, Object> aftersalesToRowMap(Aftersales a) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_bn", a.getAftersalesBn());
		m.put("order_id", a.getOrderId());
		m.put("company_id", a.getCompanyId());
		m.put("user_id", a.getUserId());
		m.put("salesman_id", a.getSalesmanId());
		m.put("item_bn", a.getItemBn());
		m.put("shop_id", a.getShopId());
		m.put("distributor_id", a.getDistributorId());
		m.put("supplier_id", a.getSupplierId());
		m.put("aftersales_type", a.getAftersalesType());
		m.put("aftersales_status", a.getAftersalesStatus());
		m.put("progress", a.getProgress());
		m.put("refund_fee", a.getRefundFee());
		m.put("refund_point", a.getRefundPoint());
		m.put("reason", a.getReason());
		m.put("description", a.getDescription());
		m.put("evidence_pic", a.getEvidencePic());
		m.put("refuse_reason", a.getRefuseReason());
		m.put("memo", a.getMemo());
		m.put("sendback_data", a.getSendbackData());
		m.put("sendconfirm_data", a.getSendconfirmData());
		m.put("third_data", a.getThirdData());
		m.put("aftersales_address", a.getAftersalesAddress());
		m.put("distributor_remark", a.getDistributorRemark());
		m.put("create_time", a.getCreateTime());
		m.put("update_time", a.getUpdateTime());
		m.put("contact", a.getContact());
		m.put("mobile", a.getMobile());
		m.put("merchant_id", a.getMerchantId());
		m.put("is_partial_cancel", a.getIsPartialCancel());
		m.put("return_type", a.getReturnType());
		m.put("return_distributor_id", a.getReturnDistributorId());
		m.put("self_delivery_operator_id", a.getSelfDeliveryOperatorId());
		m.put("freight", a.getFreight());
		m.put("freight_type", a.getFreightType());
		return m;
	}

	private static long parseEpochOrZero(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		return LeadingNumberParser.parseAsLong(raw.trim());
	}

	private static List<Long> extractOperatorDistributorIds(Object raw) {
		List<Long> out = new ArrayList<>();
		List<?> list = distributorIdsRawToList(raw);
		for (Object row : list) {
			if (row instanceof Map<?, ?> m) {
				Object id = m.get("distributor_id");
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

	/**
	 * True when the stored aftersales address should be replaced with the distributor default: null, blank, or the literal "0" after trimming.
	 */
	private static boolean needDefaultAddress(String addr) {
		if (addr == null) {
			return true;
		}
		if (addr.isEmpty()) {
			return true;
		}
		return "0".equals(addr.trim());
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

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
