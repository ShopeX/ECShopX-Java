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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.dispatch.BspayTradeDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.bspay.service.export.BspayTradeDataExportContext;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BspayTradeExportDataService {

	private final BsPayOperatorResolveService bsPayOperatorResolveService;
	private final BspayTradeListQueryService bspayTradeListQueryService;
	private final BspayTradeDataExportFileJobDispatchPublisher bspayTradeDataExportFileJobDispatchPublisher;
	private final DistributorListQueryService distributorListQueryService;

	public BspayTradeExportDataService(
			BsPayOperatorResolveService bsPayOperatorResolveService,
			BspayTradeListQueryService bspayTradeListQueryService,
			BspayTradeDataExportFileJobDispatchPublisher bspayTradeDataExportFileJobDispatchPublisher,
			DistributorListQueryService distributorListQueryService) {
		this.bsPayOperatorResolveService = bsPayOperatorResolveService;
		this.bspayTradeListQueryService = bspayTradeListQueryService;
		this.bspayTradeDataExportFileJobDispatchPublisher = bspayTradeDataExportFileJobDispatchPublisher;
		this.distributorListQueryService = distributorListQueryService;
	}

	public void exportTradeData(Map<String, Object> jwtMap, HttpServletRequest request) {
		Object rawCompany = jwtMap.get("company_id");
		if (rawCompany == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseLong(rawCompany);
		if (companyId <= 0) {
			throw new UnauthorizedException("未登录");
		}
		long jwtOperatorId = parseLong(jwtMap.get("operator_id"));
		String rawOperatorType = str(jwtMap.get("operator_type")).trim();
		String outputOperatorType = "staff".equalsIgnoreCase(rawOperatorType) ? "admin" : rawOperatorType;

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		String st = request.getParameter("status");
		if (StringUtils.hasText(st)) {
			filter.put("status", st.trim().toUpperCase(Locale.ROOT));
		}
		String feeMode = request.getParameter("bspay_fee_mode");
		if (StringUtils.hasText(feeMode)) {
			filter.put("bspay_fee_mode", feeMode.trim().toUpperCase(Locale.ROOT));
		}
		String divStatus = request.getParameter("bspay_div_status");
		if (StringUtils.hasText(divStatus)) {
			filter.put("bspay_div_status", divStatus.trim().toUpperCase(Locale.ROOT));
		}
		String payChannel = request.getParameter("pay_channel");
		if (StringUtils.hasText(payChannel)) {
			filter.put("pay_channel", payChannel.trim());
		}
		String orderId = request.getParameter("order_id");
		if (StringUtils.hasText(orderId)) {
			filter.put("order_id", orderId.trim());
		}
		String tradeId = request.getParameter("trade_id");
		if (StringUtils.hasText(tradeId)) {
			filter.put("trade_id", tradeId.trim());
		}

		if (request.getParameterMap().containsKey("can_div")) {
			String rawCanDiv = request.getParameter("can_div");
			boolean value = rawCanDiv != null && "true".equalsIgnoreCase(rawCanDiv.trim());
			filter.put("can_div", value);
		}

		bspayTradeListQueryService.applyTimeStartRangeFromRequest(filter, request);

		String operatorTypeLower = rawOperatorType.toLowerCase(Locale.ROOT);
		if ("distributor".equals(operatorTypeLower)) {
			long did = parseLong(jwtMap.get("distributor_id"));
			if (did <= 0) {
				throw new ResourceException("导出有误,暂无数据导出");
			}
			filter.put("distributor_id", did);
		} else if ("merchant".equals(operatorTypeLower)) {
			long mid = bsPayOperatorResolveService.resolveMerchantIdForBspayTradeExportOrThrow(jwtOperatorId);
			filter.put("merchant_id", mid);
		}

		String dn = request.getParameter("distributor_name");
		if (dn != null && StringUtils.hasText(dn.trim()) && !"0".equals(dn.trim())) {
			List<Long> ids =
					distributorListQueryService.listDistributorIdsByCompanyAndNameContains(companyId, dn.trim());
			if (ids.isEmpty()) {
				throw new ResourceException("导出有误,暂无数据导出");
			}
			filter.put("distributor_id", ids);
		}

		filter.put("operator_type", rawOperatorType);

		Map<String, Object> exportFilter = new LinkedHashMap<>(filter);
		bspayTradeListQueryService.prepareFilterForTradeExport(exportFilter);
		long count = bspayTradeListQueryService.countGroupedTrades(exportFilter);
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (count > 15000) {
			throw new ResourceException("导出有误，当前导出数据为 " + count + " 条，最高导出 15000 条数据");
		}

		BspayTradeDataExportContext ctx =
				new BspayTradeDataExportContext(companyId, jwtOperatorId, outputOperatorType, exportFilter);
		bspayTradeDataExportFileJobDispatchPublisher.enqueueBspayTradeDataExport(ctx);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long parseLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
