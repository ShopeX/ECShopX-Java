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

package cn.shopex.ecshopx.orders.service.financial.export;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinancialSalesreportExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final long MAX_SPAN_SECONDS = 3600L * 24 * 31 * 3;

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final FinancialSalesreportItemIdConstraintResolver itemIdConstraintResolver;
	private final FinancialSalesreportExportAsyncExecutor financialSalesreportExportAsyncExecutor;
	private final FinancialSalesreportExportFileJobHandler financialSalesreportExportFileJobHandler;
	private final Environment environment;

	public FinancialSalesreportExportService(
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			FinancialSalesreportItemIdConstraintResolver itemIdConstraintResolver,
			FinancialSalesreportExportAsyncExecutor financialSalesreportExportAsyncExecutor,
			FinancialSalesreportExportFileJobHandler financialSalesreportExportFileJobHandler,
			Environment environment) {
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.itemIdConstraintResolver = itemIdConstraintResolver;
		this.financialSalesreportExportAsyncExecutor = financialSalesreportExportAsyncExecutor;
		this.financialSalesreportExportFileJobHandler = financialSalesreportExportFileJobHandler;
		this.environment = environment;
	}

	public void exportSalesreportData(long companyId, long operatorId, Map<String, String> queryParams) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("delivery_status", "DONE");

		String orderIdRaw = queryParams.get("order_id");
		if (StringUtils.hasText(orderIdRaw)) {
			String t = orderIdRaw.trim();
			if (!t.isEmpty()) {
				filter.put("order_id", t);
			}
		}

		String brandArg = queryParams.get("brand");
		String mainCatArg = queryParams.get("main_category");
		String brandOrNull = StringUtils.hasText(brandArg) ? brandArg.trim() : null;
		String mainCatOrNull = StringUtils.hasText(mainCatArg) ? mainCatArg.trim() : null;

		FinancialSalesreportItemIdResolution idRes = itemIdConstraintResolver.resolve(companyId, brandOrNull, mainCatOrNull);
		switch (idRes.kind()) {
			case NO_MATCH -> filter.put("item_id_impossible", true);
			case IDS -> filter.put("item_id_in", new ArrayList<>(idRes.itemIds()));
			case UNRESTRICTED -> {
				// no item_id constraint
			}
		}

		boolean hasOrderTimeBegin = timeBeginTruthy(queryParams.get("time_start_begin"));
		boolean hasDeliveryTimeBegin = timeBeginTruthy(queryParams.get("delivery_time_start_begin"));

		if (hasOrderTimeBegin) {
			long begin = parseEpochRequired(queryParams.get("time_start_begin"), "下单时间");
			String endRaw = queryParams.get("time_start_end");
			if (!StringUtils.hasText(endRaw)) {
				throw new BadRequestException("参数不完整");
			}
			long end = parseEpochRequired(endRaw, "下单时间");
			if (end - begin > MAX_SPAN_SECONDS) {
				throw new ResourceException("导出有误，下单日期不能超过3个月");
			}
			filter.put("create_time|gte", (int) begin);
			filter.put("create_time|lte", (int) end);
		}

		if (hasDeliveryTimeBegin) {
			long begin = parseEpochRequired(queryParams.get("delivery_time_start_begin"), "发货时间");
			String endRaw = queryParams.get("delivery_time_start_end");
			if (!StringUtils.hasText(endRaw)) {
				throw new BadRequestException("参数不完整");
			}
			long end = parseEpochRequired(endRaw, "发货时间");
			if (end - begin > MAX_SPAN_SECONDS) {
				throw new ResourceException("导出有误，发货日期不能超过3个月");
			}
			filter.put("delivery_time|gte", (int) begin);
			filter.put("delivery_time|lte", (int) end);
		}

		if (!hasOrderTimeBegin && !hasDeliveryTimeBegin) {
			ZonedDateTime now = ZonedDateTime.now(SHANGHAI);
			LocalDate today = now.toLocalDate();
			LocalDate from = today.minusDays(30);
			int gte = (int) from.atStartOfDay(SHANGHAI).toEpochSecond();
			int lte = (int) today.atTime(23, 59, 59).atZone(SHANGHAI).toEpochSecond();
			filter.put("create_time|gte", gte);
			filter.put("create_time|lte", lte);
		}

		long count = normalOrdersItemsMapper.selectCount(FinancialSalesreportQuerySupport.toWrapper(filter));
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}

		FinancialSalesreportExportJobContext ctx =
				new FinancialSalesreportExportJobContext(companyId, operatorId, new LinkedHashMap<>(filter));
		if (environment.acceptsProfiles(Profiles.of("local"))) {
			financialSalesreportExportFileJobHandler.run(ctx);
		} else {
			financialSalesreportExportAsyncExecutor.executeAsync(ctx);
		}
	}

	private static boolean timeBeginTruthy(String raw) {
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		try {
			Long.parseLong(t);
			return true;
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数格式错误");
		}
	}

	private static long parseEpochRequired(String raw, String label) {
		if (!StringUtils.hasText(raw)) {
			throw new BadRequestException("参数不完整");
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(label + "格式错误");
		}
	}
}
