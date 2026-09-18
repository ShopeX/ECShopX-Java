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

package cn.shopex.ecshopx.bspay.service.export;

import cn.shopex.ecshopx.bspay.domain.WithdrawApply;
import cn.shopex.ecshopx.bspay.service.WithdrawApplyExportQueryService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.service.MerchantQueryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BspayWithdrawDataCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(BspayWithdrawDataCsvExportService.class);
	private static final String EXPORT_TYPE = "bspay_withdraw";
	private static final int BATCH = 1000;

	private static final LinkedHashMap<String, String> TITLE = new LinkedHashMap<>();

	private static final Map<String, String> APPLICANT_TYPE = Map.of(
			"distributor", "店铺",
			"merchant", "商户",
			"admin", "超级管理员",
			"staff", "员工");

	static {
		TITLE.put("applyId", "申请ID");
		TITLE.put("applyTime", "申请时间");
		TITLE.put("applicantType", "申请人类型");
		TITLE.put("applicantAccount", "申请人账号");
		TITLE.put("shopName", "申请店铺名称");
		TITLE.put("shopId", "申请店铺ID");
		TITLE.put("merchantName", "申请商户名称");
		TITLE.put("merchantId", "申请商户ID");
		TITLE.put("withdrawType", "提现类型");
		TITLE.put("amountYuan", "提现金额（元）");
		TITLE.put("invoice", "发票");
		TITLE.put("auditTime", "审核时间");
		TITLE.put("auditRemark", "审核备注");
		TITLE.put("auditorAccount", "审核人账号");
		TITLE.put("withdrawStatus", "提现状态");
		TITLE.put("processResult", "处理结果");
		TITLE.put("lastUpdated", "最后更新时间");
	}

	private final WithdrawApplyExportQueryService withdrawApplyExportQueryService;
	private final DistributorListQueryService distributorListQueryService;
	private final MerchantQueryService merchantQueryService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public BspayWithdrawDataCsvExportService(
			WithdrawApplyExportQueryService withdrawApplyExportQueryService,
			DistributorListQueryService distributorListQueryService,
			MerchantQueryService merchantQueryService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.withdrawApplyExportQueryService = withdrawApplyExportQueryService;
		this.distributorListQueryService = distributorListQueryService;
		this.merchantQueryService = merchantQueryService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(BspayWithdrawDataExportContext ctx) {
		Map<String, Object> f = new LinkedHashMap<>(ctx.exportFilter());
		f.put("company_id", Long.valueOf(ctx.companyId()));

		long count = withdrawApplyExportQueryService.countWithdrawApplies(f);
		if (count <= 0) {
			log.debug("bspay withdraw export: zero rows, skip file and skip export log");
			return;
		}

		String fileBase =
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(Instant.now())
						+ ctx.companyId()
						+ "斗拱提现记录";

		List<Map<String, String>> allRows = new ArrayList<>();
		int pages = (int) Math.ceil(count / (double) BATCH);
		Long companyIdObj = Long.valueOf(ctx.companyId());

		for (int p = 1; p <= pages; p++) {
			List<WithdrawApply> chunk = withdrawApplyExportQueryService.pageWithdrawAppliesForExport(f, p, BATCH);

			Set<Long> distIdSet = new LinkedHashSet<>();
			Set<Long> merchantIdSet = new LinkedHashSet<>();
			for (WithdrawApply wa : chunk) {
				Long did = wa.getDistributorId();
				if (did != null && did > 0) {
					distIdSet.add(did);
				}
				Long mid = wa.getMerchantId();
				if (mid != null && mid > 0) {
					merchantIdSet.add(mid);
				}
			}

			Map<Long, String> shopNameByDistId = new HashMap<>();
			if (!distIdSet.isEmpty()) {
				List<Distributor> distributors =
						distributorListQueryService.listByIdsAndCompany(ctx.companyId(), new ArrayList<>(distIdSet));
				for (Distributor d : distributors) {
					shopNameByDistId.put(d.getDistributorId(), emptyIfNull(d.getName()));
				}
			}

			Map<Long, String> merchantNameById = new HashMap<>();
			for (Long mid : merchantIdSet) {
				Merchant m = merchantQueryService.getInfo(companyIdObj, mid, false);
				merchantNameById.put(mid, m == null || m.getMerchantName() == null ? "" : m.getMerchantName());
			}

			for (WithdrawApply wa : chunk) {
				allRows.add(buildWithdrawCsvRow(wa, shopNameByDistId, merchantNameById));
			}
		}

		LinkedHashMap<String, String> titleCopy = new LinkedHashMap<>(TITLE);
		Map<String, String> fileMeta = exportCsvFileService.exportCsv(fileBase, titleCopy, allRows);
		if (fileMeta == null || fileMeta.isEmpty()) {
			log.debug("bspay withdraw export: csv service returned empty, skip export log");
			return;
		}

		String url = fileMeta.getOrDefault("url", "");
		String filename = fileMeta.getOrDefault("filename", fileBase + ".csv");
		long finish = Instant.now().getEpochSecond();
		exportLogCreateService.createFinishLog(
				ctx.companyId(), ctx.jwtOperatorId(), EXPORT_TYPE, filename, url, finish);
	}

	private static Map<String, String> buildWithdrawCsvRow(
			WithdrawApply wa, Map<Long, String> shopNameByDistId, Map<Long, String> merchantNameById) {
		Map<String, String> row = new LinkedHashMap<>();
		Long id = wa.getId();
		String idStr = id == null ? "" : String.valueOf(id);
		row.put("applyId", idStr.isEmpty() ? "" : "'" + idStr);
		row.put("applyTime", formatEpochSeconds(wa.getCreated()));
		row.put("applicantType", applicantTypeLabel(wa.getOperatorType()));
		row.put("applicantAccount", emptyIfNull(wa.getOperator()));
		Long did = wa.getDistributorId();
		if (did != null && did > 0) {
			row.put("shopName", shopNameByDistId.getOrDefault(did, ""));
			row.put("shopId", String.valueOf(did));
		} else {
			row.put("shopName", "");
			row.put("shopId", "");
		}
		Long mid = wa.getMerchantId();
		if (mid != null && mid > 0) {
			row.put("merchantName", merchantNameById.getOrDefault(mid, ""));
			row.put("merchantId", String.valueOf(mid));
		} else {
			row.put("merchantName", "");
			row.put("merchantId", "");
		}
		row.put("withdrawType", emptyIfNull(wa.getWithdrawType()));
		row.put("amountYuan", formatAmountYuan(wa.getAmount()));
		row.put("invoice", emptyIfNull(wa.getInvoiceFile()));
		row.put("auditTime", formatAuditTime(wa.getAuditTime()));
		row.put("auditRemark", emptyIfNull(wa.getAuditRemark()));
		row.put("auditorAccount", emptyIfNull(wa.getAuditor()));
		row.put("withdrawStatus", withdrawStatusLabel(wa.getStatus()));
		row.put("processResult", emptyIfNull(wa.getFailureReason()));
		row.put("lastUpdated", formatEpochSeconds(wa.getUpdated()));
		return row;
	}

	private static String emptyIfNull(String s) {
		return s == null ? "" : s;
	}

	private static String applicantTypeLabel(String operatorType) {
		if (operatorType == null || operatorType.isEmpty()) {
			return "";
		}
		return APPLICANT_TYPE.getOrDefault(operatorType, operatorType);
	}

	private static String withdrawStatusLabel(Integer status) {
		if (status == null) {
			return "未知状态";
		}
		return switch (status) {
			case 0 -> "审核中";
			case 1 -> "审核通过";
			case 2 -> "已拒绝";
			case 3 -> "处理中";
			case 4 -> "处理成功";
			case 5 -> "处理失败";
			default -> "未知状态";
		};
	}

	private static String formatAmountYuan(Integer amountCents) {
		int cents = amountCents == null ? 0 : amountCents;
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String formatEpochSeconds(Integer sec) {
		if (sec == null || sec <= 0) {
			return "";
		}
		return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
				.withZone(ZoneId.systemDefault())
				.format(Instant.ofEpochSecond(sec.intValue()));
	}

	private static String formatAuditTime(Integer auditTime) {
		if (auditTime == null || auditTime <= 0) {
			return "";
		}
		return formatEpochSeconds(auditTime);
	}
}
