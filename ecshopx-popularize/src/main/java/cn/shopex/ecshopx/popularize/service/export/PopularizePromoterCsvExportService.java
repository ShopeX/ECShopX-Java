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

package cn.shopex.ecshopx.popularize.service.export;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.popularize.service.PromoterListQueryService;
import cn.shopex.ecshopx.popularize.service.PopularizeBrokerageCountReadService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizePromoterCsvExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);

	private final PromoterListQueryService promoterListQueryService;
	private final PopularizeBrokerageCountReadService popularizeBrokerageCountReadService;
	private final ExportCsvFileService exportCsvFileService;
	private final boolean oemShuyun;

	public PopularizePromoterCsvExportService(
			PromoterListQueryService promoterListQueryService,
			PopularizeBrokerageCountReadService popularizeBrokerageCountReadService,
			ExportCsvFileService exportCsvFileService,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.promoterListQueryService = promoterListQueryService;
		this.popularizeBrokerageCountReadService = popularizeBrokerageCountReadService;
		this.exportCsvFileService = exportCsvFileService;
		this.oemShuyun = oemShuyun;
	}

	public Optional<Map<String, String>> runExport(Map<String, Object> filter, boolean datapassBlock) {
		long companyId = longFrom(filter.get("company_id"));
		LinkedHashMap<String, String> fullTitle = buildFullTitle();

		Map<String, Object> probe =
				promoterListQueryService.getPromoterList(new LinkedHashMap<>(filter), 1, 1);
		long total = longFrom(probe.getOrDefault("total_count", 0L));
		if (total <= 0L) {
			return Optional.empty();
		}

		int limit = 500;
		int totalPage = (int) Math.ceil(total / (double) limit);
		List<Map<String, String>> allRows = new ArrayList<>();

		for (int page = 1; page <= totalPage; page++) {
			Map<String, Object> pageResult =
					promoterListQueryService.getPromoterList(new LinkedHashMap<>(filter), page, limit);
			long pageTotal = longFrom(pageResult.getOrDefault("total_count", 0L));
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) pageResult.get("list");
			if (pageTotal > 0L && list != null && !list.isEmpty()) {
				List<Long> userIds =
						list.stream()
								.map(r -> r.get("user_id"))
								.filter(v -> v != null)
								.map(PopularizePromoterCsvExportService::longFrom)
								.filter(id -> id > 0L)
								.toList();
				Map<Long, Map<String, Object>> counts =
						popularizeBrokerageCountReadService.batchPromoterBrokerageCountsByUserIds(
								companyId, userIds);
				for (int i = 0; i < list.size(); i++) {
					Map<String, Object> row = new LinkedHashMap<>(list.get(i));
					long uid = longFrom(row.get("user_id"));
					row.putAll(counts.getOrDefault(uid, emptyBrokerageStats()));
					list.set(i, row);
				}
			}

			if (list != null) {
				for (Map<String, Object> value : list) {
					allRows.add(buildCsvRow(value, datapassBlock, fullTitle));
				}
			}
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "popularize";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, fullTitle, allRows);
		if (uploaded == null || uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			return Optional.empty();
		}
		return Optional.of(uploaded);
	}

	private Map<String, String> buildCsvRow(
			Map<String, Object> value, boolean datapassBlock, LinkedHashMap<String, String> fullTitle) {
		String username = value.get("username") != null ? String.valueOf(value.get("username")) : "";
		String mobile = value.get("mobile") != null ? String.valueOf(value.get("mobile")) : "";
		if (datapassBlock) {
			if (!username.isEmpty()) {
				username = DataMasking.maskTruenameIfBlocked(username, 1);
			}
			if (!mobile.isEmpty()) {
				mobile = DataMasking.maskMobile(mobile);
			}
		}

		LinkedHashMap<String, String> out = new LinkedHashMap<>();
		if (oemShuyun) {
			out.put("username", username);
			out.put("mobile", mobile);
			out.put("cashWithdrawalRebate", centToYuanString(value.get("cashWithdrawalRebate")));
			out.put("payedRebate", centToYuanString(value.get("payedRebate")));
			out.put("freezeCashWithdrawalRebate", centToYuanString(value.get("freezeCashWithdrawalRebate")));
			out.put("noCloseRebate", centToYuanString(value.get("noCloseRebate")));
			out.put("rebateTotal", centToYuanString(value.get("rebateTotal")));
			out.put("itemTotalPrice", centToYuanString(value.get("itemTotalPrice")));
			out.put("noClosePoint", "");
			out.put("pointTotal", "");
		} else {
			for (String k : fullTitle.keySet()) {
				if ("username".equals(k)) {
					out.put(k, username);
				} else if ("mobile".equals(k)) {
					out.put(k, mobile);
				} else if ("noClosePoint".equals(k) || "pointTotal".equals(k)) {
					out.put(k, String.valueOf(longFrom(value.get(k))));
				} else {
					out.put(k, centToYuanString(value.get(k)));
				}
			}
		}
		for (String k : fullTitle.keySet()) {
			out.putIfAbsent(k, "");
		}
		return out;
	}

	private static String centToYuanString(Object v) {
		long cents = longFrom(v);
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static LinkedHashMap<String, String> buildFullTitle() {
		LinkedHashMap<String, String> fullTitle = new LinkedHashMap<>();
		fullTitle.put("username", "姓名");
		fullTitle.put("mobile", "手机号");
		fullTitle.put("cashWithdrawalRebate", "可提现");
		fullTitle.put("payedRebate", "已提现");
		fullTitle.put("freezeCashWithdrawalRebate", "申请提现");
		fullTitle.put("noCloseRebate", "未结算");
		fullTitle.put("rebateTotal", "佣金总额");
		fullTitle.put("itemTotalPrice", "商品总额");
		fullTitle.put("noClosePoint", "未结算积分");
		fullTitle.put("pointTotal", "积分总额");
		return fullTitle;
	}

	private static Map<String, Object> emptyBrokerageStats() {
		LinkedHashMap<String, Object> z = new LinkedHashMap<>();
		z.put("itemTotalPrice", 0L);
		z.put("rebateTotal", 0L);
		z.put("noCloseRebate", 0L);
		z.put("cashWithdrawalRebate", 0L);
		z.put("freezeCashWithdrawalRebate", 0L);
		z.put("rechargeRebate", 0L);
		z.put("payedRebate", 0L);
		z.put("noClosePoint", 0L);
		z.put("pointTotal", 0L);
		return z;
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
