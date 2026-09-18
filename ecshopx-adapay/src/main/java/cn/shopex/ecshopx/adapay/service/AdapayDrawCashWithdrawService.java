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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.adapay.domain.AdapayDrawCash;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMerchantEntry;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayDrawCashMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantEntryMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.adapay.service.integration.AdapayDrawCashAdaPayGateway;
import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayDrawCashWithdrawService {

	private static final Logger log = LoggerFactory.getLogger(AdapayDrawCashWithdrawService.class);

	private static final Set<String> CASH_TYPES = Set.of("D0", "D1", "T1");

	private static final ZoneId LIST_QUERY_ZONE = ZoneId.of("Asia/Shanghai");

	private static final Pattern LIST_PARAMS_DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

	private static final DateTimeFormatter LIST_PARAMS_DATE_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final DateTimeFormatter LIST_PARAMS_DATE_TIME_ALT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss");

	private final MemberOperatorContextService memberOperatorContextService;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapaySubMerchantDrawCashConfigService adapaySubMerchantDrawCashConfigService;
	private final OperatorsQueryService operatorsQueryService;
	private final DistributorMapper distributorMapper;
	private final AdapayDrawCashAdaPayGateway adapayDrawCashAdaPayGateway;
	private final AdapayAdaPayPaymentSettingReadService adapayAdaPayPaymentSettingReadService;
	private final AdapayMerchantEntryMapper adapayMerchantEntryMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapayDrawCashMapper adapayDrawCashMapper;
	private final AdapayOperationLogRecordPort adapayOperationLogRecordPort;
	private final ObjectMapper objectMapper;
	private final String adapayNotifyUrl;

	public AdapayDrawCashWithdrawService(
			MemberOperatorContextService memberOperatorContextService,
			AdapayMemberMapper adapayMemberMapper,
			AdapaySubMerchantDrawCashConfigService adapaySubMerchantDrawCashConfigService,
			OperatorsQueryService operatorsQueryService,
			DistributorMapper distributorMapper,
			AdapayDrawCashAdaPayGateway adapayDrawCashAdaPayGateway,
			AdapayAdaPayPaymentSettingReadService adapayAdaPayPaymentSettingReadService,
			AdapayMerchantEntryMapper adapayMerchantEntryMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapayDrawCashMapper adapayDrawCashMapper,
			AdapayOperationLogRecordPort adapayOperationLogRecordPort,
				ObjectMapper objectMapper,
			@Value("${adapay.notify-url:}") String adapayNotifyUrl) {
		this.memberOperatorContextService = memberOperatorContextService;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapaySubMerchantDrawCashConfigService = adapaySubMerchantDrawCashConfigService;
		this.operatorsQueryService = operatorsQueryService;
		this.distributorMapper = distributorMapper;
		this.adapayDrawCashAdaPayGateway = adapayDrawCashAdaPayGateway;
		this.adapayAdaPayPaymentSettingReadService = adapayAdaPayPaymentSettingReadService;
		this.adapayMerchantEntryMapper = adapayMerchantEntryMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapayDrawCashMapper = adapayDrawCashMapper;
		this.adapayOperationLogRecordPort = adapayOperationLogRecordPort;
		this.objectMapper = objectMapper;
		this.adapayNotifyUrl = adapayNotifyUrl == null ? "" : adapayNotifyUrl;
	}

	public void withdraw(
			long companyId,
			long jwtOperatorId,
			Long jwtDistributorId,
			String jwtOperatorType,
			String cashAmtRaw,
			String cashType) {
		if (cashAmtRaw == null || !StringUtils.hasText(cashAmtRaw.trim())) {
			throw new BadRequestException("请输入提现金额");
		}
		if (cashType == null || !StringUtils.hasText(cashType.trim())) {
			throw new BadRequestException("请选择提现类型");
		}
		String cashTypeTrim = cashType.trim();
		if (!CASH_TYPES.contains(cashTypeTrim)) {
			throw new BadRequestException("请选择提现类型");
		}
		BigDecimal cashAmt;
		try {
			cashAmt = new BigDecimal(cashAmtRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("请输入提现金额");
		}
		if (cashAmt.compareTo(new BigDecimal("0.01")) < 0) {
			throw new BadRequestException("提现金额不低于0.01元的数字");
		}

		String jwtType = jwtOperatorType == null ? "" : jwtOperatorType;
		Map<String, Object> operator =
				memberOperatorContextService.resolve(jwtOperatorId, jwtType, jwtDistributorId);
		Object opTypeObj = operator.get("operator_type");
		String opType = opTypeObj == null ? "" : String.valueOf(opTypeObj).trim();
		Object opIdObj = operator.get("operator_id");
		if (!(opIdObj instanceof Number)) {
			throw new ResourceException("没有账号信息");
		}
		long opId = ((Number) opIdObj).longValue();

		boolean main = "admin".equals(opType) || "staff".equals(opType);
		long memberPk = 0L;
		if (!main) {
			AdapayMember row =
					adapayMemberMapper.selectOne(
							new LambdaQueryWrapper<AdapayMember>()
									.eq(AdapayMember::getCompanyId, companyId)
									.eq(AdapayMember::getOperatorId, (int) opId)
									.eq(AdapayMember::getOperatorType, opType)
									.last("LIMIT 1"));
			if (row == null || row.getId() == null) {
				throw new ResourceException("开户信息不存在");
			}
			memberPk = row.getId();
		}

		BigDecimal cashLimitCents = adapaySubMerchantDrawCashConfigService.getDrawLimitCents(companyId);
		Map<String, BigDecimal> listMap =
				adapaySubMerchantDrawCashConfigService.getDrawLimitListCentsByMemberId(companyId);
		String listKey = Long.toString(memberPk);
		if (listMap.containsKey(listKey)) {
			BigDecimal override = listMap.get(listKey);
			if (override != null) {
				cashLimitCents = override;
			}
		}

		if ("dealer".equals(opType)) {
			Map<String, Object> dealerRow =
					operatorsQueryService.getInfo(
							Map.of("operator_id", opId, "company_id", companyId));
			if (dealerRow != null) {
				Object dis = dealerRow.get("is_disable");
				if (Boolean.TRUE.equals(dis)
						|| (dis instanceof Number n && n.intValue() == 1)
						|| "1".equals(String.valueOf(dis))) {
					cashLimitCents = BigDecimal.ZERO;
				}
			}
		} else if ("distributor".equals(opType)) {
			Distributor d =
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getDistributorId, opId)
									.last("LIMIT 1"));
			if (d == null || !"true".equalsIgnoreCase(String.valueOf(d.getIsValid()).trim())) {
				cashLimitCents = BigDecimal.ZERO;
			}
		} else {
			cashLimitCents = BigDecimal.ZERO;
		}

		BigDecimal avlYuan = queryAvlBalanceYuan(companyId, opType, opId);
		BigDecimal cashAmtYuan = cashAmt.setScale(2, RoundingMode.HALF_UP);
		BigDecimal freezeYuan =
				cashLimitCents.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		if (freezeYuan.add(cashAmtYuan).compareTo(avlYuan) > 0) {
			throw new ResourceException("余额不足");
		}

		String orderNo =
				"CS_"
						+ DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
								.withZone(ZoneId.systemDefault())
								.format(Instant.now())
						+ ThreadLocalRandom.current().nextInt(100000, 1000000);
		String appId = requireAppId(companyId);
		Map<String, Object> staffRow =
				operatorsQueryService.getInfo(
						Map.of("operator_id", jwtOperatorId, "company_id", companyId));
		String operatorName =
				firstNonBlank(
						staffRow == null ? null : staffRow.get("username"),
						staffRow == null ? null : staffRow.get("mobile"),
						"-");

		Map<String, Object> drawBody = new LinkedHashMap<>();
		drawBody.put("company_id", companyId);
		drawBody.put("order_no", orderNo);
		drawBody.put("app_id", appId);
		drawBody.put("cash_type", cashTypeTrim);
		drawBody.put("cash_amt", cashAmtYuan.toPlainString());
		drawBody.put("member_id", memberPk == 0L ? "0" : String.valueOf(memberPk));
		drawBody.put("notify_url", adapayNotifyUrl);
		drawBody.put("api_method", "DrawCash.create");
		drawBody.put("operator", operatorName);

		Map<String, Object> root = adapayDrawCashAdaPayGateway.drawCashCreate(drawBody);
		Object dataObj = root.get("data");
		if (!(dataObj instanceof Map<?, ?> dataMapRaw)) {
			log.error("DrawCash.create unexpected response: {}", root);
			throw new ResourceException("汇付接口错误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) dataMapRaw;
		if ("failed".equalsIgnoreCase(String.valueOf(dataMap.get("status")))) {
			log.error("DrawCash.create failed: {}", dataMap);
			throw new ResourceException(String.valueOf(dataMap.getOrDefault("error_msg", "汇付接口错误")));
		}

		String drawStatus = "pending";
		Object st = dataMap.get("status");
		if (st != null && StringUtils.hasText(String.valueOf(st))) {
			drawStatus = String.valueOf(st).trim();
		}

		String bankCardId;
		String bankCardName;
		if (memberPk == 0L) {
			AdapayMerchantEntry entry =
					adapayMerchantEntryMapper.selectOne(
							new LambdaQueryWrapper<AdapayMerchantEntry>()
									.eq(AdapayMerchantEntry::getCompanyId, companyId)
									.last("LIMIT 1"));
			bankCardId = entry == null || entry.getCardIdMask() == null ? "" : entry.getCardIdMask();
			bankCardName = entry == null || entry.getCardName() == null ? "" : entry.getCardName();
		} else {
			AdapaySettleAccount acc =
					adapaySettleAccountMapper.selectOne(
							new LambdaQueryWrapper<AdapaySettleAccount>()
									.eq(AdapaySettleAccount::getMemberId, memberPk)
									.eq(AdapaySettleAccount::getCompanyId, companyId)
									.last("LIMIT 1"));
			bankCardId = acc == null || acc.getCardId() == null ? "" : acc.getCardId();
			bankCardName = acc == null || acc.getCardName() == null ? "" : acc.getCardName();
		}

		String requestParamsJson;
		try {
			requestParamsJson = objectMapper.writeValueAsString(dataMap);
		} catch (JsonProcessingException e) {
			throw new ResourceException("汇付接口错误");
		}
		String cashIdStr = String.valueOf(dataMap.getOrDefault("id", ""));
		String cashAmtCents =
				cashAmt.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).toPlainString();
		int now = (int) (System.currentTimeMillis() / 1000L);

		AdapayDrawCash row = new AdapayDrawCash();
		row.setCompanyId(companyId);
		row.setOperatorId((int) opId);
		row.setOperatorType(opType);
		row.setOperator(operatorName);
		row.setAppId(appId);
		row.setOrderNo(orderNo);
		row.setCashId(cashIdStr);
		row.setBankCardId(bankCardId);
		row.setBankCardName(bankCardName);
		row.setCashType(cashTypeTrim);
		row.setCashAmt(cashAmtCents);
		row.setAdapayMemberId(memberPk == 0L ? "0" : String.valueOf(memberPk));
		row.setStatus(drawStatus);
		row.setRequestParams(requestParamsJson);
		row.setResponseParams("");
		row.setCreateTime(now);
		row.setUpdateTime(now);
		row.setRemark("");
		adapayDrawCashMapper.insert(row);

		recordWithdrawLog(companyId, jwtOperatorId, jwtDistributorId, jwtType, operator, opId);
	}

	public Map<String, Object> getList(
			long companyId,
			long jwtOperatorId,
			Long jwtDistributorId,
			String jwtOperatorType,
			String paramsRaw,
			boolean datapassBlocked) {
		String jwtType = jwtOperatorType == null ? "" : jwtOperatorType;
		Map<String, Object> operator =
				memberOperatorContextService.resolve(jwtOperatorId, jwtType, jwtDistributorId);
		Object opTypeObj = operator.get("operator_type");
		String opType = opTypeObj == null ? "" : String.valueOf(opTypeObj).trim();
		Object opIdObj = operator.get("operator_id");
		if (!(opIdObj instanceof Number)) {
			throw new ResourceException("没有账号信息");
		}
		long opId = ((Number) opIdObj).longValue();

		BigDecimal cashLimitCents = adapaySubMerchantDrawCashConfigService.getDrawLimitCents(companyId);
		boolean main = "admin".equals(opType) || "staff".equals(opType);
		AdapayMember memberRow = null;
		if (!main) {
			memberRow =
					adapayMemberMapper.selectOne(
							new LambdaQueryWrapper<AdapayMember>()
									.eq(AdapayMember::getCompanyId, companyId)
									.eq(AdapayMember::getOperatorId, (int) opId)
									.eq(AdapayMember::getOperatorType, opType)
									.last("LIMIT 1"));
			if (memberRow == null || memberRow.getId() == null) {
				throw new ResourceException("开户信息不存在");
			}
			long adaPayMemberId = memberRow.getId();
			Map<String, BigDecimal> listMap =
					adapaySubMerchantDrawCashConfigService.getDrawLimitListCentsByMemberId(companyId);
			String listKey = Long.toString(adaPayMemberId);
			if (listMap.containsKey(listKey)) {
				BigDecimal override = listMap.get(listKey);
				if (override != null) {
					cashLimitCents = override;
				}
			}
		}

		BigDecimal avlYuan = queryAvlBalanceYuan(companyId, opType, opId);
		BigDecimal cashBalanceCents =
				avlYuan.compareTo(BigDecimal.ZERO) > 0
						? avlYuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)
						: BigDecimal.ZERO;
		BigDecimal cashLimitEff = cashLimitCents.min(cashBalanceCents);
		BigDecimal validBalanceCents = cashBalanceCents.subtract(cashLimitEff);

		JsonNode params = readListParamsJsonTree(paramsRaw);
		int pageNo = readListIntParam(params, "page", 1, 1);
		int pageSize = readListIntParam(params, "page_size", 20, 1);
		String orderNo = listTextParam(params, "order_no");
		String beginTime = listTextParam(params, "begin_time");
		String endTime = listTextParam(params, "end_time");
		String filterStatus = listTextParam(params, "status");

		LambdaQueryWrapper<AdapayDrawCash> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AdapayDrawCash::getCompanyId, companyId);
		if (StringUtils.hasText(orderNo)) {
			wrapper.eq(AdapayDrawCash::getOrderNo, orderNo.trim());
		}
		if (StringUtils.hasText(filterStatus)) {
			wrapper.eq(AdapayDrawCash::getStatus, filterStatus.trim());
		}
		if (StringUtils.hasText(beginTime) && StringUtils.hasText(endTime)) {
			long[] bounds = resolveListCreateTimeEpochBounds(beginTime, endTime);
			if (bounds != null) {
				wrapper
						.ge(AdapayDrawCash::getCreateTime, (int) bounds[0])
						.le(AdapayDrawCash::getCreateTime, (int) bounds[1]);
			}
		}
		if (main) {
			wrapper.in(AdapayDrawCash::getOperatorType, "admin", "staff");
		} else if ("distributor".equals(opType)) {
			wrapper.eq(AdapayDrawCash::getAdapayMemberId, String.valueOf(memberRow.getId()));
		} else {
			wrapper
					.eq(AdapayDrawCash::getOperatorType, opType)
					.eq(AdapayDrawCash::getOperatorId, (int) opId);
		}
		wrapper.orderByDesc(AdapayDrawCash::getCreateTime);

		Page<AdapayDrawCash> page = new Page<>(pageNo, pageSize, false);
		long total = adapayDrawCashMapper.selectCount(wrapper);
		List<AdapayDrawCash> entityRows =
				total > 0 ? adapayDrawCashMapper.selectPage(page, wrapper).getRecords() : List.of();

		List<Map<String, Object>> list = new ArrayList<>();
		for (AdapayDrawCash entity : entityRows) {
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("id", entity.getId());
			line.put("create_time", entity.getCreateTime());
			line.put("order_no", entity.getOrderNo() == null ? "" : entity.getOrderNo());
			line.put("cash_type", entity.getCashType() == null ? "" : entity.getCashType());
			line.put("cash_amt", entity.getCashAmt() == null ? "" : entity.getCashAmt());
			String rowStatus = entity.getStatus() == null ? "" : entity.getStatus();
			line.put("status", rowStatus);
			line.put("remark", entity.getRemark() == null ? "" : entity.getRemark());
			line.put("bank_card", entity.getBankCardId() == null ? "" : entity.getBankCardId());
			line.put("user_name", entity.getBankCardName() == null ? "" : entity.getBankCardName());
			line.put(
					"adapay_member_id",
					entity.getAdapayMemberId() == null ? "" : entity.getAdapayMemberId());
			line.put(
					"operator_id",
					entity.getOperatorId() == null ? 0 : entity.getOperatorId().intValue());
			line.put("update_time", entity.getUpdateTime());

			if ("succeeded".equals(rowStatus) || "pending".equals(rowStatus)) {
				line.put("remark", "");
			}
			if (datapassBlocked) {
				line.put(
						"bank_card",
						DataMasking.maskBankcard(String.valueOf(line.get("bank_card"))));
			}
			list.add(line);
		}

		Map<String, Object> searchOptions =
				Map.of(
						"status",
						List.of(
								Map.of("label", "提现成功", "value", "succeeded"),
								Map.of("label", "提现失败", "value", "failed"),
								Map.of("label", "提现中", "value", "pending")));

		Map<String, Object> autoCfg =
				adapaySubMerchantDrawCashConfigService.getAutoCashConfig(companyId);
		Object autoRaw = autoCfg.get("auto_draw_cash");
		String autoDraw = autoRaw == null ? "N" : String.valueOf(autoRaw).trim();
		if (!StringUtils.hasText(autoDraw)) {
			autoDraw = "N";
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("cash_balance", validBalanceCents.longValue());
		out.put("cash_limit", cashLimitEff.longValue());
		out.put("commission_fee", 0);
		out.put("cash_types", List.of("D0", "D1", "T1"));
		out.put("search_options", searchOptions);
		out.put("auto_draw_cash", autoDraw);
		out.put("count", (int) total);
		out.put("list", list);
		return out;
	}

	private JsonNode readListParamsJsonTree(String paramsRaw) {
		if (paramsRaw == null || !StringUtils.hasText(paramsRaw.trim())) {
			return objectMapper.createObjectNode();
		}
		try {
			JsonNode n = objectMapper.readTree(paramsRaw.trim());
			if (n != null && n.isObject()) {
				return n;
			}
		} catch (JsonProcessingException ignored) {
		}
		return objectMapper.createObjectNode();
	}

	private static int readListIntParam(JsonNode root, String key, int defaultVal, int resetWhenBelow) {
		int v = readListIntLenient(root, key, defaultVal);
		if (v < resetWhenBelow) {
			return defaultVal;
		}
		return v;
	}

	private static int readListIntLenient(JsonNode root, String key, int defaultVal) {
		if (root == null || !root.has(key)) {
			return defaultVal;
		}
		JsonNode n = root.get(key);
		if (n == null || n.isNull()) {
			return defaultVal;
		}
		if (n.isIntegralNumber()) {
			return n.intValue();
		}
		if (n.isTextual()) {
			try {
				return Integer.parseInt(n.asText().trim());
			} catch (NumberFormatException e) {
				return defaultVal;
			}
		}
		return defaultVal;
	}

	private static String listTextParam(JsonNode root, String key) {
		if (root == null || !root.has(key)) {
			return "";
		}
		JsonNode n = root.get(key);
		if (n == null || n.isNull()) {
			return "";
		}
		if (n.isTextual()) {
			return n.asText();
		}
		return String.valueOf(n);
	}

	private long[] resolveListCreateTimeEpochBounds(String beginTime, String endTime) {
		String b = beginTime.trim();
		String e = endTime.trim();
		Long gte = listParseEpochBoundary(b, true);
		Long lte = listParseEpochBoundary(e, false);
		if (gte == null || lte == null) {
			return null;
		}
		return new long[] {gte, lte};
	}

	private Long listParseEpochBoundary(String raw, boolean begin) {
		try {
			String t = raw.trim();
			if (LIST_PARAMS_DATE_ONLY.matcher(t).matches()) {
				LocalDate d = LocalDate.parse(t);
				if (begin) {
					return d.atStartOfDay(LIST_QUERY_ZONE).toEpochSecond();
				}
				return ZonedDateTime.of(
								d, LocalTime.of(23, 59, 59, 999_000_000), LIST_QUERY_ZONE)
						.toEpochSecond();
			}
			LocalDateTime ldt = listParseFlexibleDateTime(t);
			if (ldt == null) {
				return null;
			}
			return ldt.atZone(LIST_QUERY_ZONE).toEpochSecond();
		} catch (Exception e) {
			return null;
		}
	}

	private static LocalDateTime listParseFlexibleDateTime(String t) {
		String s = t.trim();
		try {
			return LocalDateTime.parse(s, LIST_PARAMS_DATE_TIME);
		} catch (DateTimeParseException ignored) {
		}
		try {
			return LocalDateTime.parse(s, LIST_PARAMS_DATE_TIME_ALT);
		} catch (DateTimeParseException ignored) {
		}
		try {
			return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		} catch (DateTimeParseException ignored) {
		}
		return null;
	}

	private void recordWithdrawLog(
			long companyId,
			long jwtOperatorId,
			Long jwtDistributorId,
			String jwtOperatorType,
			Map<String, Object> operator,
			long opId) {
		Object opTypeObj = operator.get("operator_type");
		String opTypeNorm = opTypeObj == null ? "" : String.valueOf(opTypeObj).trim();
		String sourceType;
		if ("distributor".equalsIgnoreCase(opTypeNorm)) {
			sourceType = "distributor";
		} else if ("dealer".equalsIgnoreCase(opTypeNorm)) {
			sourceType = "dealer";
		} else {
			sourceType = "merchant";
		}

		String name;
		long relId;
		if ("distributor".equalsIgnoreCase(opTypeNorm)) {
			long rel = jwtDistributorId != null ? jwtDistributorId : opId;
			relId = rel;
			Distributor nameRow =
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getDistributorId, relId)
									.select(Distributor::getName)
									.last("LIMIT 1"));
			if (nameRow == null) {
				throw new ResourceException("操作者信息为空");
			}
			String n = nameRow.getName();
			name = (n == null || !StringUtils.hasText(n.trim())) ? "-" : n.trim();
			Map<String, Object> logParams =
					Map.of(
							"company_id",
							companyId,
							"name",
							name,
							"operator_type",
							jwtOperatorType == null ? "" : jwtOperatorType);
			adapayOperationLogRecordPort.logRecord(
					logParams, relId, "withdraw", sourceType, jwtOperatorId);
			return;
		}

		Map<String, Object> opInfo =
				operatorsQueryService.getInfo(Map.of("operator_id", jwtOperatorId, "company_id", companyId));
		name =
				firstNonBlank(
						opInfo == null ? null : opInfo.get("username"),
						opInfo == null ? null : opInfo.get("mobile"),
						"-");
		Object relObj = operator.get("operator_id");
		if (!(relObj instanceof Number)) {
			throw new ResourceException("操作者信息为空");
		}
		relId = ((Number) relObj).longValue();
		Map<String, Object> logParams =
				Map.of(
						"company_id",
						companyId,
						"name",
						name,
						"operator_type",
						jwtOperatorType == null ? "" : jwtOperatorType);
		adapayOperationLogRecordPort.logRecord(
				logParams, relId, "withdraw", sourceType, jwtOperatorId);
	}

	private BigDecimal queryAvlBalanceYuan(long companyId, String opType, long opId) {
		String appId = requireAppId(companyId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_id", companyId);
		body.put("notify_url", adapayNotifyUrl);
		body.put("api_method", "SettleAccount.balance");
		body.put("app_id", appId);
		boolean main = "admin".equals(opType) || "staff".equals(opType);
		if (main) {
			body.put("member_id", "0");
		} else {
			Map<String, String> ids = loadMemberInfoForBalance(companyId, opType, opId);
			body.put("member_id", ids.get("member_id"));
			body.put("settle_account_id", ids.get("settle_account_id"));
		}
		Map<String, Object> root = adapayDrawCashAdaPayGateway.settleAccountBalance(body);
		Object dataObj = root.get("data");
		if (!(dataObj instanceof Map<?, ?> dataMapRaw)) {
			throw new ResourceException("汇付接口错误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> m = (Map<String, Object>) dataMapRaw;
		Object st = m.get("status");
		if ("failed".equalsIgnoreCase(String.valueOf(st))) {
			throw new ResourceException(String.valueOf(m.getOrDefault("error_msg", "汇付接口错误")));
		}
		Object avl = m.get("avl_balance");
		if (avl == null) {
			throw new ResourceException("汇付接口错误");
		}
		return new BigDecimal(avl.toString()).setScale(2, RoundingMode.HALF_UP);
	}

	private Map<String, String> loadMemberInfoForBalance(long companyId, String opType, long opId) {
		AdapayMember adapayMember =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getOperatorId, (int) opId)
								.eq(AdapayMember::getOperatorType, opType)
								.last("LIMIT 1"));
		if (adapayMember == null) {
			throw new ResourceException("未入网,开户信息不全");
		}
		AdapaySettleAccount acc =
				adapaySettleAccountMapper.selectOne(
						new LambdaQueryWrapper<AdapaySettleAccount>()
								.eq(AdapaySettleAccount::getMemberId, adapayMember.getId())
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (acc == null || !StringUtils.hasText(acc.getSettleAccountId())) {
			throw new ResourceException("未入网,开户信息不全");
		}
		Map<String, String> out = new LinkedHashMap<>();
		out.put("member_id", String.valueOf(adapayMember.getId()));
		out.put("settle_account_id", acc.getSettleAccountId().trim());
		return out;
	}

	private String requireAppId(long companyId) {
		String appId = adapayAdaPayPaymentSettingReadService.loadAppId(companyId);
		if (!StringUtils.hasText(appId)) {
			throw new BadRequestException("adapay 支付信息未配置");
		}
		return appId.trim();
	}

	private static String firstNonBlank(Object a, Object b, String def) {
		if (a != null) {
			String sa = String.valueOf(a).trim();
			if (StringUtils.hasText(sa)) {
				return sa;
			}
		}
		if (b != null) {
			String sb = String.valueOf(b).trim();
			if (StringUtils.hasText(sb)) {
				return sb;
			}
		}
		return def;
	}

	/**
	 * 队列异步执行入口；与 PHP {@code DrawCashJob::handle} → {@code autoDrawCash} 行为对齐。
	 */
	public void runScheduledAutoDrawCash(long companyId, long memberId, String settleAccountId) {
		String settle = settleAccountId == null ? "" : settleAccountId;
		if (memberId > 0L) {
			AdapayMember m =
					adapayMemberMapper.selectOne(
							new LambdaQueryWrapper<AdapayMember>()
									.eq(AdapayMember::getId, memberId)
									.last("LIMIT 1"));
			if (m != null && "promoter".equalsIgnoreCase(String.valueOf(m.getOperatorType()).trim())) {
				log.info(
						"scheduled auto draw skipped (promoter): companyId={}, memberId={}",
						companyId,
						memberId);
				return;
			}
		}

		Map<String, Object> autoConfig = adapaySubMerchantDrawCashConfigService.getAutoCashConfig(companyId);
		BigDecimal freezeCents = resolveFreezeCentsFromDrawLimit(companyId, memberId);
		BigDecimal freezeYuan =
				freezeCents.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

		Object cashTypeObj = autoConfig.get("cash_type");
		String cashType = cashTypeObj == null || !StringUtils.hasText(String.valueOf(cashTypeObj).trim())
				? "D0"
				: String.valueOf(cashTypeObj).trim();

		BigDecimal minYuan = new BigDecimal("999");
		Object minRaw = autoConfig.get("min_cash");
		if (minRaw != null && StringUtils.hasText(String.valueOf(minRaw).trim())) {
			try {
				minYuan = new BigDecimal(String.valueOf(minRaw).trim());
			} catch (NumberFormatException ignored) {
			}
		}

		BigDecimal availableYuan =
				queryAvlBalanceYuanForScheduledJob(companyId, memberId, settle);
		BigDecimal cashYuan;
		if (memberId == 0L) {
			cashYuan = availableYuan;
		} else {
			cashYuan = availableYuan.subtract(freezeYuan);
		}

		if (cashYuan.compareTo(minYuan) < 0) {
			log.info(
					"scheduled auto draw skipped (below min): companyId={}, memberId={}, cashYuan={}, minYuan={}",
					companyId,
					memberId,
					cashYuan.toPlainString(),
					minYuan.toPlainString());
			return;
		}

		autoDrawCashCreateAndInsert(
				companyId, memberId, settle, cashType, cashYuan);
	}

	private BigDecimal resolveFreezeCentsFromDrawLimit(long companyId, long memberId) {
		Object limitObj = adapaySubMerchantDrawCashConfigService.getDrawLimit(companyId);
		BigDecimal freezeCents = new BigDecimal("999");
		if (limitObj instanceof Map<?, ?> lm && lm.get("draw_limit") != null) {
			try {
				freezeCents = new BigDecimal(String.valueOf(lm.get("draw_limit")).trim());
			} catch (NumberFormatException ignored) {
			}
		}
		if (memberId > 0L) {
			Map<String, BigDecimal> listMap =
					adapaySubMerchantDrawCashConfigService.getDrawLimitListCentsByMemberId(companyId);
			BigDecimal override = listMap.get(String.valueOf(memberId));
			if (override != null) {
				freezeCents = override;
			}
		} else {
			// 主户路径与 PHP 一致：经销商禁用等不在这个分支覆盖 freeze，保持与接口提现校验分离
		}
		return freezeCents;
	}

	private BigDecimal queryAvlBalanceYuanForScheduledJob(
			long companyId, long memberId, String settleAccountId) {
		String appId = requireAppId(companyId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_id", companyId);
		body.put("notify_url", adapayNotifyUrl);
		body.put("api_method", "SettleAccount.balance");
		body.put("app_id", appId);
		if (memberId == 0L) {
			body.put("member_id", "0");
		} else {
			body.put("member_id", String.valueOf(memberId));
			if (StringUtils.hasText(settleAccountId)) {
				body.put("settle_account_id", settleAccountId.trim());
			}
		}
		Map<String, Object> root = adapayDrawCashAdaPayGateway.settleAccountBalance(body);
		Object dataObj = root.get("data");
		if (!(dataObj instanceof Map<?, ?>)) {
			throw new ResourceException("汇付接口错误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> m = (Map<String, Object>) dataObj;
		Object st = m.get("status");
		if ("failed".equalsIgnoreCase(String.valueOf(st))) {
			throw new ResourceException(String.valueOf(m.getOrDefault("error_msg", "汇付接口错误")));
		}
		Object avl = m.get("avl_balance");
		if (avl == null) {
			throw new ResourceException("汇付接口错误");
		}
		return new BigDecimal(avl.toString()).setScale(2, RoundingMode.HALF_UP);
	}

	private void autoDrawCashCreateAndInsert(
			long companyId,
			long memberId,
			String settleAccountId,
			String cashTypeTrim,
			BigDecimal cashAmtYuan) {
		BigDecimal cashAmt = cashAmtYuan.setScale(2, RoundingMode.HALF_UP);
		String opType;
		int opId;
		String operatorName;
		if (memberId == 0L) {
			opType = "admin";
			opId = 0;
			operatorName = "自动提现";
		} else {
			AdapayMember mem =
					adapayMemberMapper.selectOne(
							new LambdaQueryWrapper<AdapayMember>()
									.eq(AdapayMember::getId, memberId)
									.last("LIMIT 1"));
			if (mem == null) {
				return;
			}
			String ot = mem.getOperatorType() == null ? "admin" : mem.getOperatorType();
			Object oid = mem.getOperatorId();
			opType = ot;
			opId = oid instanceof Number ? ((Number) oid).intValue() : 0;
			operatorName = "自动提现";
		}

		String orderNo =
				"CS_"
						+ DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
								.withZone(ZoneId.systemDefault())
								.format(Instant.now())
						+ ThreadLocalRandom.current().nextInt(100000, 1000000);
		String appId = requireAppId(companyId);

		Map<String, Object> drawBody = new LinkedHashMap<>();
		drawBody.put("company_id", companyId);
		drawBody.put("order_no", orderNo);
		drawBody.put("app_id", appId);
		drawBody.put("cash_type", cashTypeTrim);
		drawBody.put("cash_amt", cashAmt.toPlainString());
		drawBody.put("member_id", memberId == 0L ? "0" : String.valueOf(memberId));
		drawBody.put("notify_url", adapayNotifyUrl);
		drawBody.put("api_method", "DrawCash.create");
		drawBody.put("operator", operatorName);

		Map<String, Object> root;
		String drawStatus = "pending";
		String failRemark = "";
		try {
			root = adapayDrawCashAdaPayGateway.drawCashCreate(drawBody);
		} catch (Exception ex) {
			log.error("DrawCash.create auto: companyId={} memberId={} network error", companyId, memberId, ex);
			return;
		}
		Object dataObj = root.get("data");
		if (!(dataObj instanceof Map<?, ?>)) {
			log.error("DrawCash.create auto unexpected: {}", root);
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) dataObj;
		if ("failed".equalsIgnoreCase(String.valueOf(dataMap.get("status")))) {
			drawStatus = "failed";
			Object em = dataMap.get("error_msg");
			failRemark = em == null ? "" : String.valueOf(em);
			log.error("DrawCash.create auto failed: companyId={} err={}", companyId, failRemark);
		} else {
			Object st = dataMap.get("status");
			if (st != null && StringUtils.hasText(String.valueOf(st))) {
				drawStatus = String.valueOf(st).trim();
			}
		}

		String bankCardId;
		String bankCardName;
		if (memberId == 0L) {
			AdapayMerchantEntry entry =
					adapayMerchantEntryMapper.selectOne(
							new LambdaQueryWrapper<AdapayMerchantEntry>()
									.eq(AdapayMerchantEntry::getCompanyId, companyId)
									.last("LIMIT 1"));
			bankCardId = entry == null || entry.getCardIdMask() == null ? "" : entry.getCardIdMask();
			bankCardName = entry == null || entry.getCardName() == null ? "" : entry.getCardName();
		} else {
			AdapaySettleAccount acc;
			if (StringUtils.hasText(settleAccountId)) {
				acc =
						adapaySettleAccountMapper.selectOne(
								new LambdaQueryWrapper<AdapaySettleAccount>()
										.eq(AdapaySettleAccount::getMemberId, memberId)
										.eq(AdapaySettleAccount::getCompanyId, companyId)
										.eq(AdapaySettleAccount::getSettleAccountId, settleAccountId)
										.last("LIMIT 1"));
			} else {
				acc =
						adapaySettleAccountMapper.selectOne(
								new LambdaQueryWrapper<AdapaySettleAccount>()
										.eq(AdapaySettleAccount::getMemberId, memberId)
										.eq(AdapaySettleAccount::getCompanyId, companyId)
										.last("LIMIT 1"));
			}
			bankCardId = acc == null || acc.getCardId() == null ? "" : acc.getCardId();
			bankCardName = acc == null || acc.getCardName() == null ? "" : acc.getCardName();
		}

		String requestParamsJson;
		try {
			requestParamsJson = objectMapper.writeValueAsString(dataMap);
		} catch (JsonProcessingException e) {
			log.error("auto draw: serialize request params", e);
			return;
		}
		String cashIdStr = String.valueOf(dataMap.getOrDefault("id", ""));
		String cashAmtCents = cashAmt.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)
				.toPlainString();
		int now = (int) (System.currentTimeMillis() / 1000L);

		AdapayDrawCash row = new AdapayDrawCash();
		row.setCompanyId(companyId);
		row.setOperatorId(opId);
		row.setOperatorType(opType);
		row.setOperator(operatorName);
		row.setAppId(appId);
		row.setOrderNo(orderNo);
		row.setCashId(cashIdStr);
		row.setBankCardId(bankCardId);
		row.setBankCardName(bankCardName);
		row.setCashType(cashTypeTrim);
		row.setCashAmt(cashAmtCents);
		row.setAdapayMemberId(memberId == 0L ? "0" : String.valueOf(memberId));
		row.setStatus(drawStatus);
		row.setRequestParams(requestParamsJson);
		row.setResponseParams("");
		row.setCreateTime(now);
		row.setUpdateTime(now);
		row.setRemark(failRemark);
		if ("succeeded".equalsIgnoreCase(drawStatus) || "pending".equalsIgnoreCase(drawStatus)) {
			row.setRemark("");
		}
		adapayDrawCashMapper.insert(row);
	}
}
