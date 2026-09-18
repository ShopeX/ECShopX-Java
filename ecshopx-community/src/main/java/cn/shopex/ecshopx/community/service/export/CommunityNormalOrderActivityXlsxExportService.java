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

package cn.shopex.ecshopx.community.service.export;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.community.domain.CommunityActivity;
import cn.shopex.ecshopx.community.dto.export.CommunityNormalOrderExportRow;
import cn.shopex.ecshopx.community.mapper.CommunityNormalOrderExportMapper;
import cn.shopex.ecshopx.community.service.CommunityActivityService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.espier.service.ExportXlsxFileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommunityNormalOrderActivityXlsxExportService {

	private static final Logger log = LoggerFactory.getLogger(CommunityNormalOrderActivityXlsxExportService.class);

	public static final String EXPORT_TYPE = "normal_community_order";

	private static final DateTimeFormatter EXPORT_DT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final Map<String, String> RECEIPT_TYPE_LABEL =
			Map.of("logistics", "快递配送", "ziti", "上门自提", "dada", "同城配");

	private static final String[] SHEET1_HEADERS = {
		"订单号",
		"团购标题",
		"下单人",
		"下单时间",
		"订单状态",
		"团员备注",
		"跟团号",
		"商品",
		"规格",
		"数量",
		"商品金额",
		"优惠",
		"订单金额",
		"物流方式",
		"自提点",
		"自提点地址",
		"收货人",
		"联系电话",
		"详细地址",
		"团长",
		"团长手机号",
		"活动状态",
		"活动发货状态",
		"楼号",
		"房号"
	};

	private static final String[] SHEET2_HEADERS = {
		"团购标题",
		"所属团长",
		"团长手机号",
		"商品",
		"商品编号",
		"商品编码",
		"规格",
		"销售数量",
		"团当前单价",
		"商品总金额",
		"自提点",
		"自提点地址"
	};

	private final CommunityActivityService communityActivityService;
	private final CommunityNormalOrderExportMapper communityNormalOrderExportMapper;
	private final ExportXlsxFileService exportXlsxFileService;
	private final ExportLogCreateService exportLogCreateService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public CommunityNormalOrderActivityXlsxExportService(
			CommunityActivityService communityActivityService,
			CommunityNormalOrderExportMapper communityNormalOrderExportMapper,
			ExportXlsxFileService exportXlsxFileService,
			ExportLogCreateService exportLogCreateService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.communityActivityService = communityActivityService;
		this.communityNormalOrderExportMapper = communityNormalOrderExportMapper;
		this.exportXlsxFileService = exportXlsxFileService;
		this.exportLogCreateService = exportLogCreateService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public void runExport(CommunityOrderActivityExportContext ctx) {
		List<CommunityActivity> activities =
				communityActivityService.listActivitiesForAdminExport(
						ctx.companyId(), ctx.query(), ctx.optionalActivityId());

		List<List<String>> sheet1Rows = new ArrayList<>();
		sheet1Rows.add(List.of(SHEET1_HEADERS));

		Map<String, Sheet2Agg> sheet2ByKey = new TreeMap<>();

		for (CommunityActivity act : activities) {
			Long actId = act.getActivityId();
			if (actId == null) {
				continue;
			}
			String activityName = act.getActivityName() != null ? act.getActivityName() : "";
			String actStatusLabel =
					communityActivityService.exportActivityStatusLabel(act.getActivityStatus());
			String deliveryLabel =
					communityActivityService.exportActivityDeliveryLabel(act.getDeliveryStatus());

			List<CommunityNormalOrderExportRow> rows = communityNormalOrderExportMapper.selectRowsForActivity(actId);
			for (CommunityNormalOrderExportRow row : rows) {
				Map<String, String> extra = parseExtraData(row.getExtraData());
				List<String> line = buildSheet1Line(row, activityName, actStatusLabel, deliveryLabel, extra, ctx.datapassAllowed());
				sheet1Rows.add(line);

				String itemKey = actId + "_" + nullSafeItemId(row.getItemId());
				Sheet2Agg agg = sheet2ByKey.computeIfAbsent(itemKey, k -> new Sheet2Agg());
				agg.activityName = activityName;
				agg.chiefName = nullToEmpty(row.getChiefName());
				agg.chiefMobile = maybeMaskMobile(nullToEmpty(row.getChiefMobile()), ctx.datapassAllowed());
				agg.itemName = nullToEmpty(row.getItemName());
				agg.itemId = row.getItemId();
				agg.itemBn = formatItemBnForSheet2(row.getItemBn());
				agg.itemSpecDesc = nullToEmpty(row.getItemSpecDesc());
				agg.num += row.getNum() != null ? row.getNum() : 0;
				if (row.getPrice() != null) {
					agg.lastPriceCents = row.getPrice();
				}
				agg.itemFeeCentsSum += row.getItemFee() != null ? row.getItemFee() : 0;
				agg.zitiName = nullToEmpty(row.getZitiName());
				agg.zitiAddress = nullToEmpty(row.getZitiAddress());
			}
		}

		List<List<String>> sheet2Rows = new ArrayList<>();
		sheet2Rows.add(List.of(SHEET2_HEADERS));
		for (Sheet2Agg agg : sheet2ByKey.values()) {
			sheet2Rows.add(agg.toRow());
		}

		String baseName =
				"community-"
						+ java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
								.withZone(ZoneId.systemDefault())
								.format(Instant.now())
						+ ctx.companyId();

		XSSFWorkbook wb = new XSSFWorkbook();
		try {
			fillSheet(wb.createSheet("顾客购买明细表"), sheet1Rows);
			fillSheet(wb.createSheet("商品汇总表"), sheet2Rows);
		} catch (Exception e) {
			log.debug("队列导出: 执行导出时失败", e);
			try {
				wb.close();
			} catch (IOException ignored) {
				// ignore
			}
			return;
		}
		Map<String, String> uploaded = exportXlsxFileService.exportXlsx(baseName, wb);
		String url = uploaded.get("url");
		if (!StringUtils.hasText(url)) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		String filename = uploaded.getOrDefault("filename", baseName + ".xlsx");
		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				0L,
				0L,
				EXPORT_TYPE,
				filename,
				url,
				Instant.now().getEpochSecond());
	}

	private void fillSheet(Sheet sheet, List<List<String>> rows) {
		for (int r = 0; r < rows.size(); r++) {
			Row row = sheet.createRow(r);
			List<String> cells = rows.get(r);
			for (int c = 0; c < cells.size(); c++) {
				Cell cell = row.createCell(c);
				String v = cells.get(c);
				cell.setCellValue(v != null ? v : "");
			}
		}
	}

	private List<String> buildSheet1Line(
			CommunityNormalOrderExportRow row,
			String activityName,
			String actStatusLabel,
			String actDeliveryLabel,
			Map<String, String> extra,
			boolean datapassAllowed) {
		String username = tryDecrypt(nullToEmpty(row.getUsername()));
		String receiverName = tryDecrypt(nullToEmpty(row.getReceiverName()));
		String receiverMobile = tryDecrypt(nullToEmpty(row.getReceiverMobile()));
		String detailAddr = tryDecrypt(nullToEmpty(row.getReceiverAddress()));
		String receiverAddress =
				nullToEmpty(row.getReceiverState())
						+ nullToEmpty(row.getReceiverCity())
						+ nullToEmpty(row.getReceiverDistrict())
						+ detailAddr;
		if (!datapassAllowed) {
			receiverMobile = DataMasking.maskUname(receiverMobile);
			username = DataMasking.maskUname(username);
		}
		String chiefMobile = maybeMaskMobile(nullToEmpty(row.getChiefMobile()), datapassAllowed);
		int createSec = row.getCreateTime() != null ? row.getCreateTime() : 0;
		String createStr = createSec > 0 ? EXPORT_DT.format(Instant.ofEpochSecond(createSec)) : "";

		List<String> line = new ArrayList<>();
		line.add(formatOrderIdCell(row.getOrderId()));
		line.add(activityName);
		line.add(username);
		line.add(createStr);
		line.add(CommunityNormalOrderExportStatusFormatter.format(row));
		line.add(nullToEmpty(row.getRemark()));
		line.add(nullToEmpty(row.getActivityTradeNo()));
		line.add(nullToEmpty(row.getItemName()));
		line.add(nullToEmpty(row.getItemSpecDesc()));
		line.add(row.getNum() != null ? String.valueOf(row.getNum()) : "");
		line.add(centsToYuan(row.getItemFee()));
		line.add(centsToYuan(row.getDiscountFee()));
		line.add(centsToYuan(row.getTotalFee()));
		line.add(receiptLabel(row.getReceiptType()));
		line.add(nullToEmpty(row.getZitiName()));
		line.add(nullToEmpty(row.getZitiAddress()));
		line.add(receiverName);
		line.add(receiverMobile);
		line.add(receiverAddress);
		line.add(nullToEmpty(row.getChiefName()));
		line.add(chiefMobile);
		line.add(actStatusLabel);
		line.add(actDeliveryLabel);
		line.add(extra.getOrDefault("楼号", ""));
		line.add(extra.getOrDefault("房号", ""));
		return line;
	}

	private static String maybeMaskMobile(String mobile, boolean datapassAllowed) {
		if (datapassAllowed) {
			return mobile;
		}
		return DataMasking.maskUname(mobile);
	}

	private String tryDecrypt(String enc) {
		if (!StringUtils.hasText(enc)) {
			return "";
		}
		try {
			return sensitiveFieldEncryptor.decrypt(enc.trim());
		} catch (Exception e) {
			return enc;
		}
	}

	private Map<String, String> parseExtraData(String json) {
		if (!StringUtils.hasText(json)) {
			return Map.of();
		}
		try {
			Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
			Map<String, String> out = new LinkedHashMap<>();
			putExtra(out, raw, "楼号");
			putExtra(out, raw, "房号");
			return out;
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static void putExtra(Map<String, String> out, Map<String, Object> raw, String key) {
		Object v = raw.get(key);
		out.put(key, v == null ? "" : String.valueOf(v));
	}

	private static String formatOrderIdCell(Long orderId) {
		if (orderId == null) {
			return "";
		}
		return "'" + orderId;
	}

	private static String receiptLabel(String code) {
		if (!StringUtils.hasText(code)) {
			return "";
		}
		return RECEIPT_TYPE_LABEL.getOrDefault(code.trim(), code);
	}

	private static String centsToYuan(Integer cents) {
		if (cents == null) {
			return "";
		}
		return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String centsToYuan(long cents) {
		return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}

	private static String nullSafeItemId(Long itemId) {
		return itemId != null ? String.valueOf(itemId) : "0";
	}

	private static String formatItemBnForSheet2(String itemBn) {
		if (!StringUtils.hasText(itemBn)) {
			return "";
		}
		String t = itemBn.trim();
		if (isNumericItemBn(t)) {
			return "'" + t;
		}
		return t;
	}

	private static boolean isNumericItemBn(String bn) {
		try {
			new BigDecimal(bn);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static final class Sheet2Agg {
		String activityName;
		String chiefName;
		String chiefMobile;
		String itemName;
		Long itemId;
		String itemBn;
		String itemSpecDesc;
		int num;
		int lastPriceCents;
		long itemFeeCentsSum;
		String zitiName;
		String zitiAddress;

		List<String> toRow() {
			List<String> line = new ArrayList<>();
			line.add(activityName != null ? activityName : "");
			line.add(chiefName != null ? chiefName : "");
			line.add(chiefMobile != null ? chiefMobile : "");
			line.add(itemName != null ? itemName : "");
			line.add(itemId != null ? String.valueOf(itemId) : "");
			line.add(itemBn != null ? itemBn : "");
			line.add(itemSpecDesc != null ? itemSpecDesc : "");
			line.add(String.valueOf(num));
			line.add(centsToYuan(lastPriceCents));
			line.add(centsToYuan(itemFeeCentsSum));
			line.add(zitiName != null ? zitiName : "");
			line.add(zitiAddress != null ? zitiAddress : "");
			return line;
		}
	}
}
