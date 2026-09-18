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
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.distribution.DistributorListRowsAppendInfoPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.members.domain.MembersDeleteRecord;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesFrontH5ListService {

	private static final char FW_COMMA = '\uFF0C';

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final MembersDeleteRecordMapper membersDeleteRecordMapper;
	private final OperatorsMapper operatorsMapper;
	private final DistributorDefaultAftersalesAddressReadPort distributorDefaultAftersalesAddressReadPort;
	private final DistributorListRowsAppendInfoPort distributorListRowsAppendInfoPort;

	public AftersalesFrontH5ListService(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			MembersDeleteRecordMapper membersDeleteRecordMapper,
			OperatorsMapper operatorsMapper,
			DistributorDefaultAftersalesAddressReadPort distributorDefaultAftersalesAddressReadPort,
			DistributorListRowsAppendInfoPort distributorListRowsAppendInfoPort) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.membersDeleteRecordMapper = membersDeleteRecordMapper;
		this.operatorsMapper = operatorsMapper;
		this.distributorDefaultAftersalesAddressReadPort = distributorDefaultAftersalesAddressReadPort;
		this.distributorListRowsAppendInfoPort = distributorListRowsAppendInfoPort;
	}

	public Map<String, Object> getAftersalesList(HttpServletRequest request) {
		Map<String, Object> auth = mergeAuth(request);
		Map<String, Object> params = FlexibleHttpServletParameterMap.toObjectMap(request);
		int page = parsePage(params);
		int limit = parsePageSize(params);
		long companyId = longVal(auth.get("company_id"));

		LinkedHashMap<String, Object> business = new LinkedHashMap<>();
		business.put("company_id", companyId);
		business.put("user_id", auth.get("user_id") == null ? "" : String.valueOf(auth.get("user_id")).trim());

		if (params.containsKey("aftersales_status")) {
			Object v = params.get("aftersales_status");
			String t = v == null ? "" : String.valueOf(v).trim();
			if (isWholeNumericString(t)) {
				business.put("aftersales_status", t);
			}
		}
		if (!isLooseEmpty(params.get("aftersales_type"))) {
			business.put("aftersales_type", String.valueOf(params.get("aftersales_type")).trim());
		}
		if (!isLooseEmpty(params.get("order_id"))) {
			business.put("order_id", String.valueOf(params.get("order_id")).trim());
		}
		if (!isLooseEmpty(params.get("order_class"))) {
			business.put("order_class", String.valueOf(params.get("order_class")).trim());
		}
		if (params.containsKey("is_prescription_order")) {
			Object v = params.get("is_prescription_order");
			business.put("is_prescription_order", v == null ? "" : String.valueOf(v));
		}

		validateCompanyAndUserIds(auth);

		if (params.containsKey("self_delivery_operator_id")) {
			Object raw = params.get("self_delivery_operator_id");
			if (ValuePresence.hasEffectiveValue(raw)) {
				business.put("self_delivery_operator_id", raw);
				business.remove("user_id");
			}
		}

		boolean join = params.containsKey("is_prescription_order") || !isLooseEmpty(business.get("order_class"));

		Map<String, Object> sqlFilter = buildFrontH5SqlFilter(business, join);
		long offset = (long) (page - 1) * limit;

		long total;
		List<Aftersales> mainRows;
		if (join) {
			total = aftersalesMapper.countFrontH5ListJoinOrder(sqlFilter);
			mainRows =
					total > 0 ? aftersalesMapper.selectFrontH5ListJoinOrder(sqlFilter, offset, limit) : List.of();
		} else {
			total = aftersalesMapper.countAdminList(sqlFilter);
			mainRows = total > 0 ? aftersalesMapper.selectAdminList(sqlFilter, offset, limit) : List.of();
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (Aftersales row : mainRows) {
			list.add(aftersalesToRowMap(row));
		}
		if (!list.isEmpty()) {
			enrichFrontH5Rows(companyId, list);
			distributorListRowsAppendInfoPort.appendDistributorInfo(companyId, list);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private Map<String, Object> mergeAuth(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return auth;
	}

	private static void validateCompanyAndUserIds(Map<String, Object> auth) {
		List<String> segments = new ArrayList<>();
		if (missingAuthId(auth.get("company_id"))) {
			segments.add("企业ID必填");
		}
		if (missingAuthId(auth.get("user_id"))) {
			segments.add("用户ID必填");
		}
		if (!segments.isEmpty()) {
			throw new ResourceException(String.join(String.valueOf(FW_COMMA), segments));
		}
	}

	private static boolean missingAuthId(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		return false;
	}

	private static int parsePage(Map<String, Object> params) {
		if (!params.containsKey("page")) {
			return 1;
		}
		Object p = params.get("page");
		String s = p == null ? "" : String.valueOf(p).trim();
		if (!StringUtils.hasText(s)) {
			return 1;
		}
		try {
			int v = Integer.parseInt(s);
			return Math.max(v, 1);
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parsePageSize(Map<String, Object> params) {
		if (!params.containsKey("pageSize")) {
			return 10;
		}
		Object p = params.get("pageSize");
		String s = p == null ? "" : String.valueOf(p).trim();
		if (!StringUtils.hasText(s)) {
			return 10;
		}
		try {
			int v = Integer.parseInt(s);
			return v < 1 ? 10 : v;
		} catch (NumberFormatException e) {
			return 10;
		}
	}

	private static boolean isWholeNumericString(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		return s.matches("^-?\\d+$");
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

	private Map<String, Object> buildFrontH5SqlFilter(LinkedHashMap<String, Object> business, boolean join) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", longVal(business.get("company_id")));

		Object st = business.get("aftersales_status");
		if (st != null && StringUtils.hasText(String.valueOf(st))) {
			m.put("aftersales_status", String.valueOf(st).trim());
		}
		Object at = business.get("aftersales_type");
		if (at != null && StringUtils.hasText(String.valueOf(at))) {
			m.put("aftersales_type", String.valueOf(at).trim());
		}

		Object oid = business.get("order_id");
		if (oid != null && StringUtils.hasText(String.valueOf(oid))) {
			m.put("order_id_eq", String.valueOf(oid).trim());
		}

		Object uid = business.get("user_id");
		if (uid != null && StringUtils.hasText(String.valueOf(uid))) {
			m.put("user_id", String.valueOf(uid).trim());
		}

		Object sdo = business.get("self_delivery_operator_id");
		if (sdo != null) {
			m.put("self_delivery_operator_id", sdo);
		}

		m.put("order_by_create_time_asc", false);

		if (join) {
			String pv =
					business.get("is_prescription_order") == null
							? ""
							: String.valueOf(business.get("is_prescription_order"));
			if ("0".equals(pv)) {
				m.put("o_prescription_eq0", true);
			} else if ("1".equals(pv)) {
				m.put("o_prescription_gt0", true);
			}
			Object oc = business.get("order_class");
			if (oc != null && StringUtils.hasText(String.valueOf(oc).trim())) {
				m.put("o_order_class", String.valueOf(oc).trim());
			}
		}
		return m;
	}

	private void enrichFrontH5Rows(long companyId, List<Map<String, Object>> rows) {
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
			Optional<Map<String, Object>> def =
					distributorDefaultAftersalesAddressReadPort.findDefaultAddress(companyId, did);
			def.ifPresent(map -> defaultAddrByDist.put(did, map));
		}

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
			row.put("detail", detailMaps);
		}

		fillDetailPrescription(companyId, rows);
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
}
