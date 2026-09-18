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

package cn.shopex.ecshopx.orders.service.offline.export;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OfflinePaymentCsvExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int PAGE_SIZE = 500;

	private final OfflinePaymentMapper offlinePaymentMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final ObjectMapper objectMapper;

	public OfflinePaymentCsvExportService(
			OfflinePaymentMapper offlinePaymentMapper,
			ExportCsvFileService exportCsvFileService,
			ObjectMapper objectMapper) {
		this.offlinePaymentMapper = offlinePaymentMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.objectMapper = objectMapper;
	}

	public Optional<Map<String, String>> runExport(LinkedHashMap<String, Object> filter, long operatorId) {
		long total = offlinePaymentMapper.selectCount(OfflinePaymentExportQuerySupport.toWrapper(filter));
		if (total == 0) {
			return Optional.empty();
		}

		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("order_id", "订单号");
		title.put("total_fee", "订单金额");
		title.put("bank_account_name", "收款账户名称");
		title.put("bank_name", "开户银行");
		title.put("bank_account_no", "银行账号");
		title.put("china_ums_no", "银联号");
		title.put("pay_account_bank", "付款银行");
		title.put("pay_account_no", "付款账号");
		title.put("pay_account_name", "付款账户名");
		title.put("pay_fee", "付款金额");
		title.put("voucher_pic", "付款凭证图片");
		title.put("transfer_remark", "转账备注");
		title.put("check_status", "审核状态");
		title.put("remark", "审核备注");
		title.put("create_time", "创建时间");
		title.put("update_time", "更新时间");

		LambdaQueryWrapper<OfflinePayment> wrapper = OfflinePaymentExportQuerySupport.toWrapper(filter);
		wrapper.orderByDesc(OfflinePayment::getId);

		List<Map<String, String>> rows = new ArrayList<>();
		int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE);
		for (int page = 1; page <= totalPages; page++) {
			Page<OfflinePayment> p = new Page<>(page, PAGE_SIZE, false);
			Page<OfflinePayment> result = offlinePaymentMapper.selectPage(p, wrapper);
			for (OfflinePayment op : result.getRecords()) {
				rows.add(toRow(op));
			}
		}

		String fileBaseName =
				"offline_payments_" + ZonedDateTime.now(SHANGHAI).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private Map<String, String> toRow(OfflinePayment op) {
		Map<String, String> row = new LinkedHashMap<>();
		row.put("order_id", formatOrderId(op.getOrderId()));
		row.put("total_fee", centsToYuan(op.getTotalFee()));
		row.put("bank_account_name", nullToEmpty(op.getBankAccountName()));
		row.put("bank_name", nullToEmpty(op.getBankName()));
		row.put("bank_account_no", tabSuffixIfNonBlank(op.getBankAccountNo()));
		row.put("china_ums_no", tabSuffixIfNonBlank(op.getChinaUmsNo()));
		row.put("pay_account_bank", nullToEmpty(op.getPayAccountBank()));
		row.put("pay_account_no", tabSuffixIfNonBlank(op.getPayAccountNo()));
		row.put("pay_account_name", nullToEmpty(op.getPayAccountName()));
		row.put("pay_fee", centsToYuan(op.getPayFee()));
		row.put("voucher_pic", voucherPicToCsvCell(op.getVoucherPic()));
		row.put("transfer_remark", nullToEmpty(op.getTransferRemark()));
		row.put("check_status", checkStatusLabel(op.getCheckStatus()));
		row.put("remark", nullToEmpty(op.getRemark()));
		row.put("create_time", formatEpochSeconds(op.getCreateTime()));
		row.put("update_time", updateTimeCell(op.getCheckStatus(), op.getUpdateTime()));
		return row;
	}

	private static String formatOrderId(Long orderId) {
		if (orderId == null) {
			return "\t";
		}
		return orderId + "\t";
	}

	private String centsToYuan(Long cents) {
		if (cents == null) {
			return "0.00";
		}
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String tabSuffixIfNonBlank(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		return s.trim() + "\t";
	}

	private String voucherPicToCsvCell(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		try {
			JsonNode node = objectMapper.readTree(raw.trim());
			if (node != null && node.isArray()) {
				List<String> parts = new ArrayList<>();
				for (JsonNode n : node) {
					parts.add(n.asText());
				}
				return String.join(",", parts);
			}
		} catch (Exception ignored) {
		}
		return "";
	}

	private static String checkStatusLabel(Integer checkStatus) {
		if (checkStatus == null) {
			return "待审核";
		}
		return switch (checkStatus) {
			case 1 -> "审核通过";
			case 2 -> "审核不通过";
			case 9 -> "已取消";
			default -> "待审核";
		};
	}

	private static String formatEpochSeconds(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch.longValue()), SHANGHAI).format(CSV_TIME);
	}

	private static String updateTimeCell(Integer checkStatus, Integer updateTime) {
		if (checkStatus != null && (checkStatus == 1 || checkStatus == 2)) {
			return formatEpochSeconds(updateTime);
		}
		return "";
	}
}
