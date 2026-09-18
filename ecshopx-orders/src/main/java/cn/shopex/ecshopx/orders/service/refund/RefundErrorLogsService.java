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

package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.domain.RefundErrorLogs;
import cn.shopex.ecshopx.orders.mapper.RefundErrorLogsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
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
public class RefundErrorLogsService {

	private final RefundErrorLogsMapper refundErrorLogsMapper;
	private final AftersalesRefundDoRefundService aftersalesRefundDoRefundService;
	private final ObjectMapper objectMapper;

	public RefundErrorLogsService(
			RefundErrorLogsMapper refundErrorLogsMapper,
			AftersalesRefundDoRefundService aftersalesRefundDoRefundService,
			ObjectMapper objectMapper) {
		this.refundErrorLogsMapper = refundErrorLogsMapper;
		this.aftersalesRefundDoRefundService = aftersalesRefundDoRefundService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getList(Map<String, Object> jwtMap, HttpServletRequest request) {
		LambdaQueryWrapper<RefundErrorLogs> wrapper = new LambdaQueryWrapper<>();

		Optional<String> distRawOpt = optionalRawQueryDistributorId(request);
		Set<Long> jwtDistIds = parseJwtDistributorIds(jwtMap);
		if (jwtDistIds.isEmpty()) {
			distRawOpt.ifPresent(raw -> wrapper.apply("distributor_id = {0}", raw));
		} else {
			if (distRawOpt.isPresent()) {
				String raw = distRawOpt.get();
				if (inArrayLooseForDistributor(jwtDistIds, raw)) {
					wrapper.apply("distributor_id = {0}", raw);
				}
			} else {
				wrapper.in(RefundErrorLogs::getDistributorId, jwtDistIds);
			}
		}

		applyCompanyIdFromJwt(wrapper, jwtMap);
		applyOperatorTypeScopeFromJwt(wrapper, jwtMap);

		if (request.getParameter("status") != null) {
			String statusTrimmed =
					EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("status"));
			if (statusTrimmed != null) {
				if ("waiting".equals(statusTrimmed)) {
					wrapper.eq(RefundErrorLogs::getIsResubmit, Boolean.FALSE);
				} else if ("is_resubmit".equals(statusTrimmed)) {
					wrapper.eq(RefundErrorLogs::getIsResubmit, Boolean.TRUE);
				}
			}
		}

		if (request.getParameter("order_id") != null) {
			String t = request.getParameter("order_id").trim();
			if (!t.isEmpty() && !"0".equals(t)) {
				wrapper.apply("order_id = {0}", t);
			}
		}

		String startParam = request.getParameter("start_time");
		String endParam = request.getParameter("end_time");
		if (startParam != null && endParam != null) {
			String tStart = startParam.trim();
			String tEnd = endParam.trim();
			if (!tStart.isEmpty()
					&& !"0".equals(tStart)
					&& !tEnd.isEmpty()
					&& !"0".equals(tEnd)) {
				wrapper.apply("create_time >= {0}", tStart);
				wrapper.apply("create_time <= {0}", tEnd);
			}
		}

		wrapper.orderByDesc(RefundErrorLogs::getId);

		int page = resolvePage(request);
		int pageSize = resolvePageSize(request);
		long total = refundErrorLogsMapper.selectCount(wrapper);
		List<Map<String, Object>> list;
		if (total == 0L) {
			list = new ArrayList<>();
		} else {
			Page<RefundErrorLogs> p = new Page<>(page, pageSize, false);
			refundErrorLogsMapper.selectPage(p, wrapper);
			list = new ArrayList<>(p.getRecords().size());
			for (RefundErrorLogs entity : p.getRecords()) {
				list.add(RefundErrorLogsRowResponseSupport.row(entity));
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", list);
		return data;
	}

	/**
	 * Re-runs refund capture for a logged failure. AdaPay paths record order process logs from the
	 * payment-reverse branch inside the dispatch stack; Chinaums paths publish the same event from
	 * the channel executor after the gateway response is parsed. Alipay (and other standard online
	 * pay) refunds publish the same order-process-log event once from {@link
	 * cn.shopex.ecshopx.payment.service.orderrefund.StandardOnlinePayAftersalesRefundExecutor} via
	 * {@link cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort} after the channel
	 * refund result is known, without a second publish from {@link AftersalesRefundDoRefundService}.
	 * Point refunds publish that event once from the point branch inside {@link
	 * OrdersRefundPaymentDispatchService} (after points are credited) through the same
	 * {@link cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort}; this method does not
	 * publish order process logs directly. Deposit ({@code deposit}) resubmits publish the same
	 * order-process-log event from the deposit branch inside {@link OrdersRefundPaymentDispatchService}
	 * after the in-app deposit refund succeeds, not from this service.
	 *
	 * <p>BSPay ({@code bspay}, 斗拱) behaves like Alipay and other standard online refunds: after the
	 * channel response is interpreted in {@link
	 * cn.shopex.ecshopx.payment.service.orderrefund.StandardOnlinePayAftersalesRefundExecutor}, the
	 * order-process-log event is published once through {@link
	 * cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort} ({@code detail} includes the
	 * 「斗拱支付渠道」 wording). That path is not opened from this service, and the
	 * {@link OrderProcessLogPublishPort} wired on {@link AftersalesRefundDoRefundService} does not
	 * form a second publish path for this branch.
	 *
	 * <p>WeChat Pay ({@code wxpay}, {@code wxpayjs}, {@code wxpayh5}, and other {@code wxpay}-prefixed
	 * pay types) follows the same pattern as Alipay and BSPay: after the gateway response is
	 * interpreted in {@link
	 * cn.shopex.ecshopx.payment.service.orderrefund.StandardOnlinePayAftersalesRefundExecutor}, the
	 * order-process-log event is published once through {@link
	 * cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort} ({@code detail} includes the
	 * 「微信支付渠道」 wording). That path is not opened from this service, and the {@link
	 * cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort} wired on {@link
	 * AftersalesRefundDoRefundService} does not form a second publish path for this branch.
	 */
	public Map<String, Object> resubmitRefund(String pathIdRaw) {
		String s = pathIdRaw == null ? "" : pathIdRaw.trim();
		if (s.isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		if (!s.matches("^\\d{1,19}$")) {
			throw new BadRequestException("参数错误");
		}
		long id = Long.parseLong(s);
		if (id <= 0L) {
			throw new BadRequestException("参数错误");
		}

		RefundErrorLogs logRow = refundErrorLogsMapper.selectById(id);
		if (logRow == null) {
			throw new ResourceException("记录不存在");
		}

		String dataJson = logRow.getDataJson();
		if (!StringUtils.hasText(dataJson)) {
			throw new BadRequestException("参数错误");
		}
		Map<String, Object> root;
		try {
			root =
					objectMapper.readValue(
							dataJson, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数错误");
		}
		if (root == null) {
			throw new BadRequestException("参数错误");
		}

		Object bnObj = root.get("refund_bn");
		Object cyObj = root.get("company_id");
		if (bnObj == null || cyObj == null) {
			throw new BadRequestException("参数错误");
		}
		long refundBn = toPositiveLong(bnObj);
		long companyId = toPositiveLong(cyObj);
		if (refundBn <= 0L || companyId <= 0L) {
			throw new BadRequestException("参数错误");
		}

		aftersalesRefundDoRefundService.doRefund(companyId, refundBn, true);

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<RefundErrorLogs> u =
				new LambdaUpdateWrapper<RefundErrorLogs>()
						.eq(RefundErrorLogs::getId, logRow.getId())
						.set(RefundErrorLogs::getIsResubmit, Boolean.TRUE)
						.set(RefundErrorLogs::getUpdateTime, now);
		int n = refundErrorLogsMapper.update(null, u);
		if (n != 1) {
			throw new ResourceException("未查询到更新数据");
		}

		RefundErrorLogs fresh = refundErrorLogsMapper.selectById(id);
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return RefundErrorLogsRowResponseSupport.row(fresh);
	}

	private static long toPositiveLong(Object o) {
		if (o == null) {
			return -1L;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : -1L;
		}
		String t = String.valueOf(o).trim();
		if (t.isEmpty()) {
			return -1L;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : -1L;
		} catch (NumberFormatException e) {
			return -1L;
		}
	}

	private static Set<Long> parseJwtDistributorIds(Map<String, Object> jwtMap) {
		Object raw = jwtMap.get("distributor_ids");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return Set.of();
		}
		Set<Long> out = new LinkedHashSet<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				continue;
			}
			Object did = m.get("distributor_id");
			if (did == null) {
				continue;
			}
			Long v = parseLongObject(did instanceof String s ? s : String.valueOf(did));
			if (v != null && v > 0L) {
				out.add(v);
			}
		}
		return out;
	}

	private static Long parseLongObject(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			try {
				return new BigDecimal(s).longValue();
			} catch (NumberFormatException e2) {
				return null;
			}
		}
	}

	private static Optional<String> optionalRawQueryDistributorId(HttpServletRequest request) {
		if (request.getParameter("distributor_id") == null) {
			return Optional.empty();
		}
		String raw = request.getParameter("distributor_id");
		if (raw.trim().isEmpty()) {
			return Optional.empty();
		}
		if (raw.trim().equals("0")) {
			return Optional.empty();
		}
		return Optional.of(raw);
	}

	private static boolean inArrayLooseForDistributor(Set<Long> distributorIdSet, String raw) {
		for (Long id : distributorIdSet) {
			if (id != null && raw != null && id.toString().equals(raw)) {
				return true;
			}
			try {
				if (id != null && raw != null && id.equals(Long.parseLong(raw.trim()))) {
					return true;
				}
			} catch (NumberFormatException ignored) {
				// loose: non-numeric raw only matches string form above
			}
		}
		return false;
	}

	private static void applyCompanyIdFromJwt(
			LambdaQueryWrapper<RefundErrorLogs> wrapper, Map<String, Object> jwtMap) {
		applyJwtScalarEq(wrapper, jwtMap.get("company_id"), RefundErrorLogs::getCompanyId, "company_id");
	}

	private static void applyOperatorTypeScopeFromJwt(
			LambdaQueryWrapper<RefundErrorLogs> wrapper, Map<String, Object> jwtMap) {
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(jwtMap.get("operator_type"));
		if (operatorType == null) {
			return;
		}
		if ("merchant".equalsIgnoreCase(operatorType)) {
			applyJwtScalarEq(
					wrapper, jwtMap.get("merchant_id"), RefundErrorLogs::getMerchantId, "merchant_id");
		} else if ("supplier".equalsIgnoreCase(operatorType)) {
			applyJwtScalarEq(
					wrapper, jwtMap.get("operator_id"), RefundErrorLogs::getSupplierId, "supplier_id");
		}
	}

	private static void applyJwtScalarEq(
			LambdaQueryWrapper<RefundErrorLogs> wrapper,
			Object v,
			SFunction<RefundErrorLogs, ?> column,
			String columnNameForApply) {
		if (v == null) {
			wrapper.isNull(column);
			return;
		}
		if (v instanceof Number n) {
			wrapper.eq(column, n.longValue());
			return;
		}
		String s;
		if (v instanceof String str) {
			s = str.trim();
		} else {
			s = String.valueOf(v).trim();
		}
		if (s.isEmpty()) {
			wrapper.isNull(column);
			return;
		}
		try {
			wrapper.eq(column, Long.parseLong(s));
		} catch (NumberFormatException e) {
			wrapper.apply(columnNameForApply + " = {0}", s);
		}
	}

	private static int resolvePage(HttpServletRequest request) {
		String raw = EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("page"));
		if (raw == null) {
			return 1;
		}
		try {
			int p = Integer.parseInt(raw);
			return p < 1 ? 1 : p;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int resolvePageSize(HttpServletRequest request) {
		String raw = EspierAdminJwtControllerSupport.optionalTrimmedString(request.getParameter("pageSize"));
		if (raw == null) {
			return 20;
		}
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException e) {
			return 20;
		}
	}
}
