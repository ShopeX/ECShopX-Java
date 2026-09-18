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

package cn.shopex.ecshopx.hfpay.service.cashrecord;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCashRecordMapper;
import cn.shopex.ecshopx.hfpay.service.export.HfpayWithdrawRecordExportContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class HfpayWithdrawRecordListService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CREATED_AT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final HfpayCashRecordMapper cashRecordMapper;
	private final OperatorsQueryService operatorsQueryService;

	public HfpayWithdrawRecordListService(
			HfpayCashRecordMapper cashRecordMapper,
			OperatorsQueryService operatorsQueryService) {
		this.cashRecordMapper = cashRecordMapper;
		this.operatorsQueryService = operatorsQueryService;
	}

	public Map<String, Object> getWithdrawList(
			long companyId, HfpayWithdrawRecordExportContext ctxFull, int page, int pageSize) {
		HfpayWithdrawRecordExportContext ctxBase =
				new HfpayWithdrawRecordExportContext(
						ctxFull.getCompanyId(),
						ctxFull.getOperatorId(),
						ctxFull.getStartDateTime(),
						ctxFull.getEndDateTime(),
						ctxFull.getDistributorId(),
						ctxFull.getOrderId(),
						false,
						null);

		long count = cashRecordMapper.countWithdrawExport(ctxFull);

		Map<String, Object> total = new LinkedHashMap<>();
		total.put("count", count);
		total.put("total_amt", cashRecordMapper.sumWithdrawTransAmtTotal(ctxBase));
		total.put("finish_total_amt", cashRecordMapper.sumWithdrawTransAmtFinish(ctxBase));
		total.put("total_amting", cashRecordMapper.sumWithdrawTransAmtInProgress(ctxBase));
		total.put("fail_total_amt", cashRecordMapper.sumWithdrawTransAmtFail(ctxBase));

		int offset = (page - 1) * pageSize;
		List<LinkedHashMap<String, Object>> data =
				cashRecordMapper.selectWithdrawListPage(ctxFull, offset, pageSize);

		if (!data.isEmpty()) {
			Set<Long> opIds = new HashSet<>();
			for (LinkedHashMap<String, Object> row : data) {
				Long oid = extractOperatorId(row);
				if (oid != null && oid != 0L) {
					opIds.add(oid);
				}
			}
			Map<Long, String> loginByOp =
					opIds.isEmpty()
							? Collections.emptyMap()
							: operatorsQueryService.mapLoginNameByOperatorIds(companyId, opIds);
			for (LinkedHashMap<String, Object> row : data) {
				Long oid = extractOperatorId(row);
				if (oid == null || oid == 0L) {
					row.put("login_name", "系统操作");
				} else {
					row.put("login_name", loginByOp.getOrDefault(oid, ""));
				}
				formatCreatedAtInPlace(row);
			}
		}

		Map<String, Object> list = new LinkedHashMap<>();
		list.put("total_count", count);
		list.put("data", data);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total", total);
		out.put("list", list);
		return out;
	}

	private static Long extractOperatorId(Map<String, Object> row) {
		Object v = row.get("operator_id");
		if (v == null) {
			v = row.get("operatorId");
		}
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void formatCreatedAtInPlace(Map<String, Object> row) {
		Object v = row.get("created_at");
		if (v == null) {
			v = row.get("createdAt");
		}
		if (v == null) {
			return;
		}
		String s = formatCreatedAtValue(v);
		if (s != null) {
			row.put("created_at", s);
		}
	}

	private static String formatCreatedAtValue(Object v) {
		if (v instanceof LocalDateTime ldt) {
			return CREATED_AT_FMT.format(ldt);
		}
		if (v instanceof Timestamp ts) {
			return CREATED_AT_FMT.format(ts.toLocalDateTime());
		}
		if (v instanceof java.util.Date d) {
			return CREATED_AT_FMT.format(
					LocalDateTime.ofInstant(d.toInstant(), CN));
		}
		if (v instanceof Instant ins) {
			return CREATED_AT_FMT.format(LocalDateTime.ofInstant(ins, CN));
		}
		if (v instanceof String str) {
			return str;
		}
		return null;
	}
}
