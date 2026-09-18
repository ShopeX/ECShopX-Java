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

import cn.shopex.ecshopx.adapay.dispatch.AdapayTradeDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.adapay.service.export.AdapayTradeDataExportContext;
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
public class AdapayTradeExportDataService {

	private final AdapayDealerOperatorResolveHelper adapayDealerOperatorResolveHelper;
	private final AdapayTradeListQueryService adapayTradeListQueryService;
	private final AdapayTradeExportOperationLogService adapayTradeExportOperationLogService;
	private final AdapayTradeDataExportFileJobDispatchPublisher adapayTradeDataExportFileJobDispatchPublisher;
	private final DistributorListQueryService distributorListQueryService;

	public AdapayTradeExportDataService(
			AdapayDealerOperatorResolveHelper adapayDealerOperatorResolveHelper,
			AdapayTradeListQueryService adapayTradeListQueryService,
			AdapayTradeExportOperationLogService adapayTradeExportOperationLogService,
			AdapayTradeDataExportFileJobDispatchPublisher adapayTradeDataExportFileJobDispatchPublisher,
			DistributorListQueryService distributorListQueryService) {
		this.adapayDealerOperatorResolveHelper = adapayDealerOperatorResolveHelper;
		this.adapayTradeListQueryService = adapayTradeListQueryService;
		this.adapayTradeExportOperationLogService = adapayTradeExportOperationLogService;
		this.adapayTradeDataExportFileJobDispatchPublisher = adapayTradeDataExportFileJobDispatchPublisher;
		this.distributorListQueryService = distributorListQueryService;
	}

	public void exportTradeData(Map<String, Object> jwtMap, HttpServletRequest request) {
		long companyId = requireCompanyId(jwtMap);
		long jwtOperatorId = parseLong(jwtMap.get("operator_id"));
		String rawOperatorType = str(jwtMap.get("operator_type")).trim();
		String outputOperatorType = "staff".equalsIgnoreCase(rawOperatorType) ? "admin" : rawOperatorType;

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		String st = request.getParameter("status");
		if (StringUtils.hasText(st)) {
			filter.put("status", st.trim().toUpperCase(Locale.ROOT));
		}
		if (request.getParameterMap().containsKey("can_div") && StringUtils.hasText(request.getParameter("can_div"))) {
			String raw = request.getParameter("can_div");
			filter.put("can_div", "true".equalsIgnoreCase(raw.trim()));
		}
		String feeMode = request.getParameter("adapay_fee_mode");
		if (StringUtils.hasText(feeMode)) {
			filter.put("adapay_fee_mode", feeMode.trim().toUpperCase(Locale.ROOT));
		}
		String divStatus = request.getParameter("adapay_div_status");
		if (StringUtils.hasText(divStatus)) {
			filter.put("adapay_div_status", divStatus.trim().toUpperCase(Locale.ROOT));
		}
		String payChannel = request.getParameter("pay_channel");
		if (StringUtils.hasText(payChannel)) {
			filter.put("pay_channel", payChannel.trim());
		}
		String tradeId = request.getParameter("trade_id");
		if (StringUtils.hasText(tradeId)) {
			filter.put("trade_id", tradeId.trim());
		}
		String timeBegin = request.getParameter("time_start_begin");
		if (StringUtils.hasText(timeBegin)) {
			filter.put("time_start|gte", timeBegin.trim());
			filter.put("time_start|lte", request.getParameter("time_start_end"));
		}

		String operatorType = rawOperatorType.toLowerCase(Locale.ROOT);
		if ("distributor".equals(operatorType)) {
			long distributorId = parseLong(jwtMap.get("distributor_id"));
			if (distributorId <= 0) {
				throw new ResourceException("导出有误,暂无数据导出");
			}
			filter.put("distributor_id", distributorId);
		} else if ("dealer".equals(operatorType)) {
			long dealerId = adapayDealerOperatorResolveHelper.resolveMainDealerOperatorIdOrThrow(jwtOperatorId);
			if (dealerId <= 0) {
				throw new ResourceException("导出有误,暂无数据导出");
			}
			filter.put("dealer_id", dealerId);
		}

		String distributorName = request.getParameter("distributor_name");
		if (distributorName != null
				&& StringUtils.hasText(distributorName.trim())
				&& !"0".equals(distributorName.trim())) {
			List<Long> ids = distributorListQueryService.listDistributorIdsByCompanyAndNameContains(
					companyId, distributorName.trim());
			if (ids.isEmpty()) {
				throw new ResourceException("导出有误,暂无数据导出");
			}
			filter.put("distributor_id", ids);
		}

		filter.put("operator_type", rawOperatorType);

		Map<String, Object> exportFilter = new LinkedHashMap<>(filter);
		adapayTradeListQueryService.prepareFilterForTradeExport(exportFilter);
		long count = adapayTradeListQueryService.countGroupedTrades(exportFilter);
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (count > 15000) {
			throw new ResourceException("导出有误，当前导出数据为 " + count + " 条，最高导出 15000 条数据");
		}

		adapayTradeExportOperationLogService.recordTradeExportRequest(companyId, jwtMap);

		AdapayTradeDataExportContext ctx =
				new AdapayTradeDataExportContext(companyId, jwtOperatorId, outputOperatorType, exportFilter);
		adapayTradeDataExportFileJobDispatchPublisher.enqueueAdapayTradeDataExport(ctx);
	}

	private static long requireCompanyId(Map<String, Object> jwtMap) {
		Object raw = jwtMap.get("company_id");
		if (raw == null) {
			throw new UnauthorizedException("未登录");
		}
		long c = parseLong(raw);
		if (c <= 0) {
			throw new UnauthorizedException("未登录");
		}
		return c;
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
