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

package cn.shopex.ecshopx.orders.service.tradeexport.csv;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.domain.TradeListDistributorRow;
import cn.shopex.ecshopx.orders.mapper.TradeListDistributorRowMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.tradeexport.support.TradeExportQuerySupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeDataExportCsvService {

	private static final int PAGE_SIZE = 500;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter PAY_DATE_OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter PAY_DATE_IN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final Pattern NUMERIC_ID = Pattern.compile("^[0-9]+$");

	private static final Map<String, String> TRADE_STATE_LABELS =
			Map.of(
					"SUCCESS", "支付成功",
					"REFUND", "转入退款",
					"NOTPAY", "未支付",
					"CLOSED", "已关闭",
					"REVOKED", "已撤销",
					"PAYERROR", "支付失败");

	private static final Map<String, String> TRADE_SOURCE_LABELS = new LinkedHashMap<>();

	static {
		TRADE_SOURCE_LABELS.put("membercard", "会员卡购买");
		TRADE_SOURCE_LABELS.put("normal", "实体订单购买");
		TRADE_SOURCE_LABELS.put("servers", "服务订单购买");
		TRADE_SOURCE_LABELS.put("service", "服务订单购买");
		TRADE_SOURCE_LABELS.put("groups", "拼团订单");
		TRADE_SOURCE_LABELS.put("seckill", "秒杀订单");
		TRADE_SOURCE_LABELS.put("normal_groups", "实体拼团订单");
		TRADE_SOURCE_LABELS.put("normal_seckill", "实体秒杀订单");
		TRADE_SOURCE_LABELS.put("normal_community", "社区订单购买");
		TRADE_SOURCE_LABELS.put("diposit", "预存款购买");
		TRADE_SOURCE_LABELS.put("order_pay", "买单购买");
	}

	private static final Map<String, String> PAY_TYPE_LABELS = new LinkedHashMap<>();

	static {
		PAY_TYPE_LABELS.put("wxpay", "微信支付");
		PAY_TYPE_LABELS.put("wxpayh5", "微信支付");
		PAY_TYPE_LABELS.put("wxpayjs", "微信支付");
		PAY_TYPE_LABELS.put("wxpayjsapi", "微信支付");
		PAY_TYPE_LABELS.put("wxpayapp", "微信支付");
		PAY_TYPE_LABELS.put("wxpaymini", "微信支付");
		PAY_TYPE_LABELS.put("wxpaypos", "微信支付");
		PAY_TYPE_LABELS.put("wxpaypc", "微信支付");
		PAY_TYPE_LABELS.put("alipay", "支付宝");
		PAY_TYPE_LABELS.put("alipayapp", "支付宝");
		PAY_TYPE_LABELS.put("alipaymini", "支付宝");
		PAY_TYPE_LABELS.put("alipaypos", "支付宝");
		PAY_TYPE_LABELS.put("alipayh5", "支付宝");
		PAY_TYPE_LABELS.put("offline_pay", "线下支付");
		PAY_TYPE_LABELS.put("point", "积分支付");
		PAY_TYPE_LABELS.put("deposit", "预存款");
		PAY_TYPE_LABELS.put("pos", "刷卡");
		PAY_TYPE_LABELS.put("bspay", "汇付支付");
		PAY_TYPE_LABELS.put("adapay", "汇付支付");
		PAY_TYPE_LABELS.put("paypal", "PayPal");
		PAY_TYPE_LABELS.put("chinaums", "银联商务");
		PAY_TYPE_LABELS.put("localpay", "线下支付");
	}

	private final TradeMapper tradeMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final MembersContactByUserIdsLookupService membersContactByUserIdsLookupService;
	private final WxShopsMapper wxShopsMapper;
	private final TradeListDistributorRowMapper tradeListDistributorRowMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;

	public TradeDataExportCsvService(
			TradeMapper tradeMapper,
			ExportCsvFileService exportCsvFileService,
			MembersContactByUserIdsLookupService membersContactByUserIdsLookupService,
			WxShopsMapper wxShopsMapper,
			TradeListDistributorRowMapper tradeListDistributorRowMapper,
			AftersalesRefundMapper aftersalesRefundMapper) {
		this.tradeMapper = tradeMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.membersContactByUserIdsLookupService = membersContactByUserIdsLookupService;
		this.wxShopsMapper = wxShopsMapper;
		this.tradeListDistributorRowMapper = tradeListDistributorRowMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
	}

	public Optional<Map<String, String>> export(long companyId, LinkedHashMap<String, Object> filter) {
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(filter);
		Object datapassRaw = work.remove("datapass_block");
		int datapassBlock = parseDatapassBlockFromFilter(datapassRaw);

		long count = tradeMapper.selectCount(TradeExportQuerySupport.toCountWrapper(companyId, work));
		if (count <= 0L) {
			return Optional.empty();
		}

		LinkedHashMap<String, String> title = buildTitle();
		List<Map<String, String>> rows = new ArrayList<>();
		int pageNum = 1;
		while (true) {
			Page<Trade> page = new Page<>(pageNum, PAGE_SIZE, false);
			IPage<Trade> result = tradeMapper.selectPage(page, TradeExportQuerySupport.toListWrapper(companyId, work));
			List<Trade> records = result.getRecords();
			if (records == null || records.isEmpty()) {
				break;
			}

			Map<Long, Map<String, String>> contacts = loadContacts(records);
			Map<Long, String> shopIdToName = loadShopNames(companyId, records);
			Map<Long, String> distributorIdToName = loadDistributorNames(companyId, records);
			Set<String> refundedTradeIds = loadRefundedTradeIds(companyId, records);

			for (Trade t : records) {
				rows.add(buildRow(t, contacts, shopIdToName, distributorIdToName, refundedTradeIds, datapassBlock));
			}
			if (records.size() < PAGE_SIZE) {
				break;
			}
			pageNum++;
		}

		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + "trade";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private Map<String, String> buildRow(
			Trade t,
			Map<Long, Map<String, String>> contacts,
			Map<Long, String> shopIdToName,
			Map<Long, String> distributorIdToName,
			Set<String> refundedTradeIds,
			int datapassBlock) {
		Map<String, String> row = new LinkedHashMap<>();
		Long uid = parseLongObject(t.getUserId());
		Map<String, String> inner = uid == null ? null : contacts.get(uid);
		String userName = inner != null && StringUtils.hasText(inner.get("username")) ? inner.get("username") : "--";
		String mobilePlain =
				inner != null && StringUtils.hasText(inner.get("mobile"))
						? inner.get("mobile")
						: nz(t.getMobile());

		String payTypeRaw = nz(t.getPayType());
		String payChannelRaw = nz(t.getPayChannel());
		Integer payFeeRaw = t.getPayFee() == null ? 0 : t.getPayFee();
		Integer totalFeeRaw = t.getTotalFee() == null ? 0 : t.getTotalFee();
		Integer discountFeeRaw = t.getDiscountFee() == null ? 0 : t.getDiscountFee();

		String payFeeOut;
		String payPointOut;
		if ("point".equalsIgnoreCase(payTypeRaw)) {
			payFeeOut = "0";
			payPointOut = String.valueOf(payFeeRaw);
		} else {
			payFeeOut = centsToYuan(payFeeRaw);
			payPointOut = "0";
		}

		String payDate = computePayDate(t.getTimeExpire());
		if (!StringUtils.hasText(payDate)) {
			payDate = "--";
		}

		Long distId = parseLongObject(t.getDistributorId());
		String storeName =
				distId != null && distId > 0L && distributorIdToName.containsKey(distId)
						? nzOrDash(distributorIdToName.get(distId))
						: "--";

		Long wxShopId = parseLongObject(t.getShopId());
		String shopName =
				wxShopId != null && wxShopId > 0L && shopIdToName.containsKey(wxShopId)
						? nzOrDash(shopIdToName.get(wxShopId))
						: "--";

		row.put("orderId", formatNumericIdCell(nz(t.getOrderId())));
		row.put("tradeId", formatNumericIdCell(nz(t.getTradeId())));
		row.put("mobile", mobilePlain);
		row.put("user_name", userName);
		row.put("totalFee", centsToYuan(totalFeeRaw));
		row.put("payFee", payFeeOut);
		row.put("payPoint", payPointOut);
		row.put("discountFee", centsToYuan(discountFeeRaw));
		row.put("transactionId", formatNumericIdCell(nz(t.getTransactionId())));
		row.put("payDate", payDate);
		row.put("body", nz(t.getBody()));
		row.put("detail", nz(t.getDetail()));
		row.put("store_name", storeName);
		row.put("shop_name", shopName);
		row.put("feeType", labelFeeType(t.getFeeType()));
		row.put("tradeState", labelTradeState(t.getTradeState()));
		row.put("payType", labelPayType(payTypeRaw, payChannelRaw));
		row.put("timeStart", formatTradeTimeField(t.getTimeStart()));
		row.put("timeExpire", formatTradeTimeField(t.getTimeExpire()));
		row.put("tradeSourceType", labelTradeSourceType(t.getTradeSourceType()));
		boolean refunded = t.getTradeId() != null && refundedTradeIds.contains(t.getTradeId());
		row.put("if_refund", refunded ? "是" : "否");

		if (datapassBlock != 0) {
			row.put("mobile", DataMasking.maskMobileIfBlocked(row.get("mobile"), datapassBlock));
			row.put("user_name", DataMasking.maskTruenameIfBlocked(row.get("user_name"), datapassBlock));
		}
		return row;
	}

	private static LinkedHashMap<String, String> buildTitle() {
		LinkedHashMap<String, String> t = new LinkedHashMap<>();
		t.put("orderId", "订单号");
		t.put("tradeId", "交易单号");
		t.put("mobile", "会员手机号");
		t.put("user_name", "会员名称");
		t.put("totalFee", "订单总金额");
		t.put("payFee", "订单实付金额");
		t.put("payPoint", "订单实付积分");
		t.put("discountFee", "订单优惠金额");
		t.put("transactionId", "支付流水号");
		t.put("payDate", "支付时间");
		t.put("body", "交易描述");
		t.put("detail", "交易详情");
		t.put("store_name", "店铺名称");
		t.put("shop_name", "门店名称");
		t.put("feeType", "支付货币类型");
		t.put("tradeState", "交易状态");
		t.put("payType", "支付方式");
		t.put("timeStart", "交易开始时间");
		t.put("timeExpire", "交易结束时间");
		t.put("tradeSourceType", "交易单来源类型");
		t.put("if_refund", "是否退款");
		return t;
	}

	private Map<Long, Map<String, String>> loadContacts(List<Trade> records) {
		List<Long> userIds =
				records.stream()
						.map(t -> parseLongObject(t.getUserId()))
						.filter(id -> id != null && id > 0L)
						.distinct()
						.toList();
		if (userIds.isEmpty()) {
			return Map.of();
		}
		int lim = Math.max(PAGE_SIZE, userIds.size());
		return membersContactByUserIdsLookupService.loadDecryptedContactsByUserIds(userIds, lim);
	}

	private Map<Long, String> loadShopNames(long companyId, List<Trade> records) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Trade t : records) {
			Long sid = parseLongObject(t.getShopId());
			if (sid != null && sid > 0L) {
				ids.add(sid);
			}
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		List<WxShops> rows =
				wxShopsMapper.selectList(
						new LambdaQueryWrapper<WxShops>()
								.eq(WxShops::getCompanyId, companyId)
								.in(WxShops::getWxShopId, ids));
		return rows.stream()
				.filter(r -> r.getWxShopId() != null)
				.collect(
						Collectors.toMap(
								WxShops::getWxShopId,
								r -> r.getStoreName() == null ? "" : r.getStoreName(),
								(a, b) -> a));
	}

	private Map<Long, String> loadDistributorNames(long companyId, List<Trade> records) {
		Set<Long> idSet = new LinkedHashSet<>();
		for (Trade t : records) {
			Long did = parseLongObject(t.getDistributorId());
			if (did != null && did > 0L) {
				idSet.add(did);
			}
		}
		if (idSet.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = new ArrayList<>(idSet);
		List<TradeListDistributorRow> rows =
				tradeListDistributorRowMapper.selectList(
						new LambdaQueryWrapper<TradeListDistributorRow>()
								.eq(TradeListDistributorRow::getCompanyId, companyId)
								.in(TradeListDistributorRow::getDistributorId, ids));
		return rows.stream()
				.collect(
						Collectors.toMap(
								TradeListDistributorRow::getDistributorId,
								r -> r.getName() == null ? "" : r.getName(),
								(a, b) -> a));
	}

	private Set<String> loadRefundedTradeIds(long companyId, List<Trade> records) {
		List<String> tradeIds =
				records.stream().map(Trade::getTradeId).filter(StringUtils::hasText).map(String::trim).distinct().toList();
		if (tradeIds.isEmpty()) {
			return Set.of();
		}
		List<AftersalesRefund> refunds =
				aftersalesRefundMapper.selectList(
						new LambdaQueryWrapper<AftersalesRefund>()
								.eq(AftersalesRefund::getCompanyId, companyId)
								.in(AftersalesRefund::getTradeId, tradeIds)
								.eq(AftersalesRefund::getRefundStatus, "SUCCESS"));
		return refunds.stream()
				.map(AftersalesRefund::getTradeId)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());
	}

	private static String labelTradeState(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "--";
		}
		String k = raw.trim().toUpperCase(Locale.ROOT);
		return TRADE_STATE_LABELS.getOrDefault(k, raw.trim());
	}

	private static String labelTradeSourceType(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "--";
		}
		String k = raw.trim();
		return TRADE_SOURCE_LABELS.getOrDefault(k, k);
	}

	private static String labelPayType(String payTypeRaw, String payChannelRaw) {
		if ("localPay".equalsIgnoreCase(payTypeRaw) && "offline_pay".equalsIgnoreCase(payChannelRaw)) {
			return "线下转账";
		}
		if (!StringUtils.hasText(payTypeRaw)) {
			return "--";
		}
		String k = payTypeRaw.trim();
		String lc = k.toLowerCase(Locale.ROOT);
		if (PAY_TYPE_LABELS.containsKey(lc)) {
			return PAY_TYPE_LABELS.get(lc);
		}
		if (PAY_TYPE_LABELS.containsKey(k)) {
			return PAY_TYPE_LABELS.get(k);
		}
		return k;
	}

	private static String labelFeeType(String ft) {
		if (!StringUtils.hasText(ft)) {
			return "--";
		}
		String t = ft.trim();
		if ("CNY".equalsIgnoreCase(t)) {
			return "人民币";
		}
		return t;
	}

	private String formatTradeTimeField(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "--";
		}
		String s = raw.trim();
		if (isNullishToken(s)) {
			return "--";
		}
		String fromEpoch = payDateFromNumericEpochString(s);
		if (StringUtils.hasText(fromEpoch)) {
			return fromEpoch;
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(s, PAY_DATE_IN);
			return ldt.format(PAY_DATE_OUT);
		} catch (DateTimeParseException e) {
			try {
				LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
				return ldt.format(PAY_DATE_OUT);
			} catch (DateTimeParseException e2) {
				return s;
			}
		}
	}

	private String computePayDate(String timeExpire) {
		if (!StringUtils.hasText(timeExpire)) {
			return "";
		}
		String s = timeExpire.trim();
		if (isNullishToken(s)) {
			return "";
		}
		String fromEpoch = payDateFromNumericEpochString(s);
		if (StringUtils.hasText(fromEpoch)) {
			return fromEpoch;
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(s, PAY_DATE_IN);
			return ldt.format(PAY_DATE_OUT);
		} catch (DateTimeParseException e) {
			try {
				LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
				return ldt.format(PAY_DATE_OUT);
			} catch (DateTimeParseException e2) {
				return "";
			}
		}
	}

	private static boolean isNullishToken(String s) {
		return "null".equalsIgnoreCase(s) || "undefined".equalsIgnoreCase(s);
	}

	private String payDateFromNumericEpochString(String s) {
		if (s.isEmpty() || !isAsciiDigitsOnly(s)) {
			return "";
		}
		try {
			long n = Long.parseLong(s);
			Instant instant;
			if (s.length() >= 13) {
				instant = Instant.ofEpochMilli(n);
			} else {
				instant = Instant.ofEpochSecond(n);
			}
			return LocalDateTime.ofInstant(instant, SHANGHAI).format(PAY_DATE_OUT);
		} catch (DateTimeException | NumberFormatException | ArithmeticException e) {
			return "";
		}
	}

	private static boolean isAsciiDigitsOnly(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	private static String centsToYuan(int fen) {
		return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static String nzOrDash(String s) {
		return StringUtils.hasText(s) ? s : "--";
	}

	private static String formatNumericIdCell(String idStr) {
		if (!StringUtils.hasText(idStr)) {
			return "";
		}
		if (NUMERIC_ID.matcher(idStr.trim()).matches()) {
			return "\t" + idStr.trim();
		}
		return idStr.trim();
	}

	private static int parseDatapassBlockFromFilter(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0 ? 1 : 0;
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty() || "0".equals(t) || "false".equalsIgnoreCase(t)) {
			return 0;
		}
		return 1;
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
}
