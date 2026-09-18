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

package cn.shopex.ecshopx.hfpay.service.export;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCashRecordMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayWithdrawRecordCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(HfpayWithdrawRecordCsvExportService.class);
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final DateTimeFormatter ROW_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int PAGE_SIZE = 500;

	private final HfpayCashRecordMapper cashRecordMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public HfpayWithdrawRecordCsvExportService(
			HfpayCashRecordMapper cashRecordMapper,
			OperatorsQueryService operatorsQueryService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.cashRecordMapper = cashRecordMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(HfpayWithdrawRecordExportContext ctx) {
		long companyId = ctx.getCompanyId();
		long operatorId = ctx.getOperatorId();

		long total = cashRecordMapper.countWithdrawExport(ctx);
		int pageSize = PAGE_SIZE;
		int pages = total == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);

		LinkedHashMap<String, String> titles = buildTitles();
		List<Map<String, String>> allRows = new ArrayList<>();

		for (int page = 1; page <= pages; page++) {
			int offset = (page - 1) * pageSize;
			List<HfpayWithdrawRecordExportRow> chunk =
					cashRecordMapper.selectWithdrawExportPage(ctx, offset, pageSize);
			if (chunk.isEmpty()) {
				continue;
			}
			Set<Long> opIds = new HashSet<>();
			for (HfpayWithdrawRecordExportRow r : chunk) {
				Long oid = r.getOperatorId();
				if (oid != null && oid != 0L) {
					opIds.add(oid);
				}
			}
			Map<Long, String> loginByOp =
					operatorsQueryService.mapUsernameByOperatorIds(companyId, opIds);
			for (HfpayWithdrawRecordExportRow r : chunk) {
				allRows.add(toCsvRow(r, loginByOp));
			}
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "_店铺提现记录";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, titles, allRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}

		long finishSec = Instant.now().getEpochSecond();
		exportLogCreateService.createFinishLog(
				companyId,
				operatorId,
				0L,
				0L,
				HfpayWithdrawRecordExportFileJobTypes.TYPE_HFPAY_WITHDRAW_RECORD,
				uploaded.getOrDefault("filename", fileBaseName + ".csv"),
				uploaded.get("url"),
				finishSec);
	}

	private static LinkedHashMap<String, String> buildTitles() {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("created_at", "日期");
		m.put("order_id", "提现订单号");
		m.put("bind_card_id", "到账银行卡号");
		m.put("trans_amt", "提现金额");
		m.put("distributor_name", "店铺名称");
		m.put("login_name", "操作人");
		m.put("cash_status", "订单状态");
		m.put("resp_desc", "备注");
		return m;
	}

	private static Map<String, String> toCsvRow(HfpayWithdrawRecordExportRow r, Map<Long, String> loginByOp) {
		Map<String, String> line = new LinkedHashMap<>();
		line.put("created_at", formatCreatedAt(r.getCreatedAt()));
		line.put("order_id", excelSafeNumericId(r.getOrderId()));
		line.put("bind_card_id", excelSafeNumericId(r.getBindCardId()));
		line.put("trans_amt", fenToYuanPlain(r.getTransAmt()));
		line.put("distributor_name", nullToEmpty(r.getDistributorName()));
		Long opId = r.getOperatorId() != null ? r.getOperatorId() : 0L;
		if (opId == 0L) {
			line.put("login_name", "系统操作");
		} else {
			line.put("login_name", nullToEmpty(loginByOp.get(opId)));
		}
		line.put("cash_status", cashStatusLabel(r.getCashStatus()));
		line.put("resp_desc", nullToEmpty(r.getRespDesc()));
		return line;
	}

	private static String formatCreatedAt(LocalDateTime t) {
		if (t == null) {
			return "";
		}
		return ROW_DT.format(t);
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}

	private static String cashStatusLabel(Integer status) {
		if (status == null) {
			return "--";
		}
		if (status == 0 || status == 1) {
			return "提现中";
		}
		if (status == 2) {
			return "提现成功";
		}
		if (status == 3) {
			return "提现失败";
		}
		return "--";
	}

	private static String excelSafeNumericId(String raw) {
		if (raw == null) {
			return "";
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return "";
		}
		if (t.chars().allMatch(Character::isDigit)) {
			return "\t" + t;
		}
		return t;
	}

	private static String fenToYuanPlain(Object fenObj) {
		if (fenObj == null) {
			return "0";
		}
		BigDecimal fen;
		if (fenObj instanceof Number n) {
			fen = BigDecimal.valueOf(n.longValue());
		} else {
			try {
				fen = new BigDecimal(String.valueOf(fenObj).trim());
			} catch (Exception e) {
				return "0";
			}
		}
		return fen.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.stripTrailingZeros()
				.toPlainString();
	}
}
