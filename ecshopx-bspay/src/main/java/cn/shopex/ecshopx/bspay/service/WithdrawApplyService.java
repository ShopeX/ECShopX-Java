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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.DivFee;
import cn.shopex.ecshopx.bspay.domain.EntryApply;
import cn.shopex.ecshopx.bspay.domain.UserEnt;
import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.domain.UserCard;
import cn.shopex.ecshopx.bspay.domain.WithdrawApply;
import cn.shopex.ecshopx.bspay.mapper.DivFeeMapper;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import cn.shopex.ecshopx.bspay.mapper.WithdrawApplyMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WithdrawApplyService {

	private static final int SUCCESS_STATUS = 4;
	private static final List<Integer> PENDING_STATUSES = List.of(0, 1, 3);

	private final WithdrawApplyMapper withdrawApplyMapper;
	private final DivFeeMapper divFeeMapper;
	private final EntryApplyMapper entryApplyMapper;
	private final UserEntMapper userEntMapper;
	private final UserIndvMapper userIndvMapper;
	private final BsPayPaymentSettingService bsPayPaymentSettingService;
	private final WithdrawApplyExportQueryService withdrawApplyExportQueryService;
	private final WithdrawApplyAdminFilterBuilder withdrawApplyAdminFilterBuilder;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final MerchantMapper merchantMapper;
	private final UserCardMapper userCardMapper;
	private final WithdrawHuifuWithdrawTransactionalService withdrawHuifuWithdrawTransactionalService;
	private final PlatformTransactionManager platformTransactionManager;
	private final TransactionTemplate markFailedRequiresNew;

	public WithdrawApplyService(
			WithdrawApplyMapper withdrawApplyMapper,
			DivFeeMapper divFeeMapper,
			EntryApplyMapper entryApplyMapper,
			UserEntMapper userEntMapper,
			UserIndvMapper userIndvMapper,
			BsPayPaymentSettingService bsPayPaymentSettingService,
			WithdrawApplyExportQueryService withdrawApplyExportQueryService,
			WithdrawApplyAdminFilterBuilder withdrawApplyAdminFilterBuilder,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			MerchantMapper merchantMapper,
			UserCardMapper userCardMapper,
			WithdrawHuifuWithdrawTransactionalService withdrawHuifuWithdrawTransactionalService,
			PlatformTransactionManager platformTransactionManager) {
		this.withdrawApplyMapper = withdrawApplyMapper;
		this.divFeeMapper = divFeeMapper;
		this.entryApplyMapper = entryApplyMapper;
		this.userEntMapper = userEntMapper;
		this.userIndvMapper = userIndvMapper;
		this.bsPayPaymentSettingService = bsPayPaymentSettingService;
		this.withdrawApplyExportQueryService = withdrawApplyExportQueryService;
		this.withdrawApplyAdminFilterBuilder = withdrawApplyAdminFilterBuilder;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.merchantMapper = merchantMapper;
		this.userCardMapper = userCardMapper;
		this.withdrawHuifuWithdrawTransactionalService = withdrawHuifuWithdrawTransactionalService;
		this.platformTransactionManager = platformTransactionManager;
		this.markFailedRequiresNew = new TransactionTemplate(platformTransactionManager);
		this.markFailedRequiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Transactional(rollbackFor = Exception.class)
	public WithdrawApply applyWithdraw(WithdrawApplyContext ctx) {
		validateWithdrawApplyBodyPhaseA(ctx);
		String huifuId = resolveHuifuIdForWithdraw(ctx);
		validateWithdrawApplyBodyPhaseB(ctx);
		int amountFen = parseAmountYuanToFenInt(ctx);
		long effectiveFen = computeEffectiveAvailableFen(ctx);
		assertAmountFenWithinAvailable(amountFen, effectiveFen);
		WithdrawApply entity = buildAndInsertWithdrawApply(ctx, huifuId, amountFen);
		log.info(
				"提现申请创建成功 apply_id:{},amount:{},operator_type:{},operator_id:{},operator:{}",
				entity.getId(),
				entity.getAmount(),
				ctx.operatorType(),
				ctx.operatorId(),
				ctx.operatorMobile());
		return entity;
	}

	private static void validateWithdrawApplyBodyPhaseA(WithdrawApplyContext ctx) {
		BigDecimal amountYuan = ctx.amountYuan();
		if (amountYuan == null) {
			throw new BadRequestException("提现金额必填且必须大于0.01元");
		}
		if (amountYuan.signum() <= 0) {
			throw new BadRequestException("提现金额必填且必须大于0.01元");
		}
		if (amountYuan.compareTo(BigDecimal.valueOf(0.01)) < 0) {
			throw new BadRequestException("提现金额必填且必须大于0.01元");
		}

		String withdrawType = ctx.withdrawType();
		String trimmedType = withdrawType == null ? "" : withdrawType.trim();
		if (!StringUtils.hasText(withdrawType) || !"T1".equals(trimmedType)) {
			throw new BadRequestException("提现类型必填且只能为T1");
		}

		if (!StringUtils.hasText(ctx.invoiceUrl())) {
			throw new BadRequestException("发票文件URL必填且必须是有效的URL地址");
		}
		String trimmedUrl = ctx.invoiceUrl().trim();
		if (!isValidHttpOrHttpsUrl(trimmedUrl)) {
			throw new BadRequestException("发票文件URL必填且必须是有效的URL地址");
		}
	}

	private static boolean isValidHttpOrHttpsUrl(String invoiceUrl) {
		try {
			URI u = new URI(invoiceUrl);
			String scheme = u.getScheme();
			if (scheme == null) {
				return false;
			}
			if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
				return false;
			}
			String host = u.getHost();
			return host != null && !host.isEmpty();
		} catch (URISyntaxException | IllegalArgumentException e) {
			return false;
		}
	}

	private String resolveHuifuIdForWithdraw(WithdrawApplyContext ctx) {
		long queryOpId = resolveQueryOperatorIdForHuifu(ctx);
		String operatorType = ctx.operatorType() == null ? "" : ctx.operatorType();

		if ("admin".equals(operatorType) || "staff".equals(operatorType)) {
			String huifuId = bsPayPaymentSettingService
					.findOptionalSysId(ctx.companyId())
					.map(String::trim)
					.filter(StringUtils::hasText)
					.orElse(null);
			if (huifuId == null) {
				throw new ResourceException("未找到对应的汇付用户ID");
			}
			return huifuId;
		}

		int operatorIdForEntry = toIntOperatorIdForEntry(queryOpId);
		EntryApply entry = selectLatestApprovedEntryApply(ctx, operatorIdForEntry);
		if (entry == null) {
			throw new ResourceException("未找到对应的汇付用户ID");
		}
		String userType = entry.getUserType() == null ? "" : entry.getUserType().trim();
		String uidRaw = entry.getUserId();
		if (!StringUtils.hasText(uidRaw)) {
			throw new BadRequestException("进件用户编号格式无效");
		}
		long userPk;
		try {
			userPk = Long.parseLong(uidRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("进件用户编号格式无效");
		}

		if ("ent".equals(userType)) {
			UserEnt user = userEntMapper.selectById(userPk);
			if (user == null || !StringUtils.hasText(user.getHuifuId())) {
				throw new ResourceException("未找到对应的汇付用户ID");
			}
			return user.getHuifuId().trim();
		}
		if ("indv".equals(userType)) {
			UserIndv user = userIndvMapper.selectById(userPk);
			if (user == null || !StringUtils.hasText(user.getHuifuId())) {
				throw new ResourceException("未找到对应的汇付用户ID");
			}
			return user.getHuifuId().trim();
		}
		throw new ResourceException("获取用户进件信息失败");
	}

	private static long resolveQueryOperatorIdForHuifu(WithdrawApplyContext ctx) {
		if ("distributor".equals(ctx.operatorType())) {
			return ctx.distributorId();
		}
		if ("merchant".equals(ctx.operatorType())) {
			return ctx.merchantId();
		}
		return ctx.operatorId();
	}

	private static int toIntOperatorIdForEntry(long queryOpId) {
		if (queryOpId > Integer.MAX_VALUE || queryOpId < Integer.MIN_VALUE) {
			throw new ResourceException("未找到对应的汇付用户ID");
		}
		return (int) queryOpId;
	}

	private EntryApply selectLatestApprovedEntryApply(WithdrawApplyContext ctx, int queryOpId) {
		String operatorType = ctx.operatorType() == null ? "" : ctx.operatorType();
		List<EntryApply> rows = entryApplyMapper.selectList(
				Wrappers.<EntryApply>lambdaQuery()
						.eq(EntryApply::getCompanyId, ctx.companyId())
						.eq(EntryApply::getOperatorType, operatorType)
						.eq(EntryApply::getOperatorId, queryOpId)
						.eq(EntryApply::getStatus, "APPROVED")
						.orderByDesc(EntryApply::getId)
						.last("LIMIT 1"));
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		return rows.get(0);
	}

	private static void validateWithdrawApplyBodyPhaseB(WithdrawApplyContext ctx) {
		if (ctx.amountYuan().signum() <= 0) {
			throw new BadRequestException("申请金额必须大于0");
		}
		String wt = ctx.withdrawType();
		if (!StringUtils.hasText(wt) || !"T1".equals(wt.trim())) {
			throw new BadRequestException("提现类型只能是T1");
		}
		if (!StringUtils.hasText(ctx.invoiceUrl())) {
			throw new BadRequestException("请上传发票文件");
		}
	}

	private static int parseAmountYuanToFenInt(WithdrawApplyContext ctx) {
		BigDecimal fenBd =
				ctx.amountYuan().movePointRight(2).setScale(0, RoundingMode.DOWN);
		if (fenBd.signum() < 0) {
			throw new ResourceException("可提现余额不足");
		}
		if (fenBd.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
			throw new ResourceException("可提现余额不足");
		}
		try {
			return fenBd.intValueExact();
		} catch (ArithmeticException e) {
			throw new ResourceException("可提现余额不足");
		}
	}

	private long computeEffectiveAvailableFen(WithdrawApplyContext ctx) {
		long divSum = sumDivFeeFen(ctx);
		long succSum = sumWithdrawApplyAmountFen(ctx, SUCCESS_STATUS, null);
		long pendSum = sumWithdrawApplyAmountFen(ctx, null, PENDING_STATUSES);
		long available = divSum - succSum;
		long pending = pendSum;
		return Math.max(0L, available - pending);
	}

	/**
	 * Available balance is {@code max(0, SUM(div_fee) - SUM(amount) where status = success)}; pending is not
	 * subtracted from available (aligns with balance display API).
	 */
	public Map<String, Long> getUserBalance(
			long companyId, String operatorType, long distributorId, long merchantId) {
		log.info(
				"getUserBalance companyId={}, operatorType={}, distributorId={}, merchantId={}",
				companyId,
				operatorType,
				distributorId,
				merchantId);
		String normalizedOpType = operatorType == null ? "" : operatorType;
		BalanceQueryScope scope = new BalanceQueryScope(companyId, normalizedOpType, merchantId, distributorId);
		long totalDivFee = sumDivFeeFen(scope);
		long totalWithdrawnSuccess = sumWithdrawApplyAmountFen(scope, SUCCESS_STATUS, null);
		long rawAvailable = totalDivFee - totalWithdrawnSuccess;
		long availableBalance = Math.max(0L, rawAvailable);
		long pendingBalance = sumWithdrawApplyAmountFen(scope, null, PENDING_STATUSES);
		return Map.of("available_balance", availableBalance, "pending_balance", pendingBalance);
	}

	public Map<String, Object> lists(
			long companyId,
			long jwtOperatorId,
			Map<String, Object> jwtMap,
			HttpServletRequest request,
			int pageOneBased,
			int pageSize) {
		LinkedHashMap<String, Object> filter =
				withdrawApplyAdminFilterBuilder.buildForLists(companyId, jwtOperatorId, jwtMap, request);
		long total = withdrawApplyExportQueryService.countWithdrawApplies(filter);
		if (total == 0L) {
			return Map.of("list", Collections.emptyList(), "total_count", Long.valueOf(0L));
		}

		List<WithdrawApply> rows =
				withdrawApplyExportQueryService.pageWithdrawAppliesForExport(filter, pageOneBased, pageSize);

		String jwtOperatorType =
				Optional.ofNullable(jwtMap.get("operator_type")).map(Object::toString).orElse("").trim();
		String jwtOtLower = jwtOperatorType.toLowerCase(Locale.ROOT);
		boolean jwtIsAdminOrStaff = "admin".equals(jwtOtLower) || "staff".equals(jwtOtLower);

		LinkedHashSet<Long> positiveDistIds = new LinkedHashSet<>();
		LinkedHashSet<Long> positiveMerchantIds = new LinkedHashSet<>();
		for (WithdrawApply wa : rows) {
			Long did = wa.getDistributorId();
			if (did != null && did > 0L) {
				positiveDistIds.add(did);
			}
			Long mid = wa.getMerchantId();
			if (mid != null && mid > 0L) {
				positiveMerchantIds.add(mid);
			}
		}
		boolean anyZeroDist =
				rows.stream()
						.anyMatch(
								wa -> {
									Long d = wa.getDistributorId();
									return d == null || d == 0L;
								});

		Map<Long, String> distNameById = new LinkedHashMap<>();
		String selfDistributorName = "";
		if (anyZeroDist) {
			Map<String, Object> selfRow = distributionDistributorSelfReadMapper.selectSelfStoreRow(companyId);
			if (selfRow != null && !selfRow.isEmpty()) {
				Object n = selfRow.get("name");
				selfDistributorName = n == null ? "" : n.toString().trim();
			}
			if (!StringUtils.hasText(selfDistributorName)) {
				selfDistributorName = "平台自营";
			}
		}
		if (!positiveDistIds.isEmpty()) {
			List<Map<String, Object>> easyRows =
					distributorRepositoryGetInfoSimpleService.listEasylistsByDistributorIds(
							companyId, new ArrayList<>(positiveDistIds));
			for (Map<String, Object> row : easyRows) {
				long id = parseRowLong(row.get("distributor_id"));
				Object nameObj = row.get("name");
				distNameById.put(id, nameObj == null ? "" : nameObj.toString().trim());
			}
		}

		Map<Long, String> merchantNameById = new LinkedHashMap<>();
		if (!positiveMerchantIds.isEmpty()) {
			List<Merchant> merchants =
					merchantMapper.selectList(
							new LambdaQueryWrapper<Merchant>()
									.select(Merchant::getId, Merchant::getMerchantName)
									.in(Merchant::getId, positiveMerchantIds));
			if (merchants != null) {
				for (Merchant merchant : merchants) {
					merchantNameById.put(merchant.getId(), emptyTrimmed(merchant.getMerchantName()));
				}
			}
		}

		List<Map<String, Object>> listPayload = new ArrayList<>(rows.size());
		for (WithdrawApply wa : rows) {
			Long did = wa.getDistributorId();
			long didNorm = did == null ? 0L : did.longValue();
			String distributorNameRaw = didNorm > 0L ? distNameById.getOrDefault(didNorm, "") : selfDistributorName;
			if (didNorm == 0L && !jwtIsAdminOrStaff) {
				distributorNameRaw = "";
			}
			Long mid = wa.getMerchantId();
			String merchantNameRaw =
					mid == null || mid <= 0L ? "" : merchantNameById.getOrDefault(mid, "");

			LinkedHashMap<String, Object> line = new LinkedHashMap<>();
			line.put("id", wa.getId());
			line.put("company_id", wa.getCompanyId());
			line.put("merchant_id", wa.getMerchantId());
			line.put("distributor_id", wa.getDistributorId());
			line.put("operator_type", wa.getOperatorType());
			line.put("operator_id", wa.getOperatorId());
			line.put("operator", wa.getOperator());
			line.put("huifu_id", wa.getHuifuId());
			line.put("amount", wa.getAmount());
			line.put("withdraw_type", wa.getWithdrawType());
			line.put("invoice_file", wa.getInvoiceFile());
			line.put("status", wa.getStatus());
			line.put("audit_time", wa.getAuditTime());
			line.put("auditor", wa.getAuditor());
			line.put("auditor_operator_id", wa.getAuditorOperatorId());
			line.put("audit_remark", wa.getAuditRemark());
			line.put("hf_seq_id", wa.getHfSeqId());
			line.put("req_seq_id", wa.getReqSeqId());
			line.put("request_time", wa.getRequestTime());
			line.put("failure_reason", wa.getFailureReason());
			line.put("created", wa.getCreated());
			line.put("updated", wa.getUpdated());
			line.put("distributor_name", distributorNameRaw);
			line.put("merchant_name", merchantNameRaw);
			listPayload.add(line);
		}

		return Map.of("list", listPayload, "total_count", Long.valueOf(total));
	}

	private static long parseRowLong(Object idObj) {
		if (idObj instanceof Number n) {
			return n.longValue();
		}
		if (idObj == null) {
			return 0L;
		}
		return Long.parseLong(idObj.toString().trim());
	}

	private static String emptyTrimmed(String s) {
		return s == null ? "" : s.trim();
	}

	private long sumDivFeeFen(WithdrawApplyContext ctx) {
		return sumDivFeeFen(BalanceQueryScope.from(ctx));
	}

	private long sumDivFeeFen(BalanceQueryScope scope) {
		QueryWrapper<DivFee> div = new QueryWrapper<>();
		div.select("IFNULL(SUM(div_fee),0) AS total");
		div.eq("company_id", String.valueOf(scope.companyId()));
		div.eq("operator_type", scope.operatorType());
		if ("merchant".equals(scope.operatorType()) && scope.merchantId() > 0) {
			div.eq("merchant_id", String.valueOf(scope.merchantId()));
		}
		if ("distributor".equals(scope.operatorType()) && scope.distributorId() > 0) {
			div.eq("distributor_id", String.valueOf(scope.distributorId()));
		}
		List<Map<String, Object>> divRows = divFeeMapper.selectMaps(div);
		return readTotalLong(divRows);
	}

	private long sumWithdrawApplyAmountFen(
			WithdrawApplyContext ctx, Integer singleStatus, List<Integer> inStatuses) {
		return sumWithdrawApplyAmountFen(BalanceQueryScope.from(ctx), singleStatus, inStatuses);
	}

	private long sumWithdrawApplyAmountFen(
			BalanceQueryScope scope, Integer singleStatus, List<Integer> inStatuses) {
		QueryWrapper<WithdrawApply> w = new QueryWrapper<>();
		w.select("IFNULL(SUM(amount),0) AS total");
		w.eq("company_id", String.valueOf(scope.companyId()));
		w.eq("operator_type", scope.operatorType());
		if ("merchant".equals(scope.operatorType()) && scope.merchantId() > 0) {
			w.eq("merchant_id", String.valueOf(scope.merchantId()));
		}
		if ("distributor".equals(scope.operatorType()) && scope.distributorId() > 0) {
			w.eq("distributor_id", String.valueOf(scope.distributorId()));
		}
		if (singleStatus != null) {
			w.eq("status", singleStatus);
		}
		if (inStatuses != null) {
			w.in("status", inStatuses);
		}
		List<Map<String, Object>> rows = withdrawApplyMapper.selectMaps(w);
		return readTotalLong(rows);
	}

	private static record BalanceQueryScope(long companyId, String operatorType, long merchantId, long distributorId) {
		private static BalanceQueryScope from(WithdrawApplyContext ctx) {
			String operatorType = ctx.operatorType() == null ? "" : ctx.operatorType();
			return new BalanceQueryScope(ctx.companyId(), operatorType, ctx.merchantId(), ctx.distributorId());
		}
	}

	private static long readTotalLong(List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return 0L;
		}
		Object total = rows.get(0).getOrDefault("total", 0L);
		if (total instanceof Number n) {
			return n.longValue();
		}
		if (total instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static void assertAmountFenWithinAvailable(int amountFen, long effectiveAvailableFen) {
		if ((long) amountFen > effectiveAvailableFen) {
			throw new ResourceException("可提现余额不足");
		}
	}

	private WithdrawApply buildAndInsertWithdrawApply(
			WithdrawApplyContext ctx, String huifuId, int amountFen) {
		String operatorType = ctx.operatorType() == null ? "" : ctx.operatorType();
		WithdrawApply entity = new WithdrawApply();
		entity.setCompanyId(ctx.companyId());
		entity.setMerchantId(ctx.merchantId());
		entity.setDistributorId(ctx.distributorId());
		entity.setOperatorType(operatorType);
		entity.setOperatorId(ctx.operatorId());
		entity.setOperator(ctx.operatorMobile() == null ? "" : ctx.operatorMobile());
		entity.setHuifuId(huifuId);
		entity.setAmount(amountFen);
		entity.setWithdrawType(ctx.withdrawType() == null ? "" : ctx.withdrawType().trim());
		entity.setInvoiceFile(ctx.invoiceUrl().trim());
		entity.setStatus(0);
		int now = (int) (System.currentTimeMillis() / 1000);
		entity.setCreated(now);
		entity.setUpdated(now);
		withdrawApplyMapper.insert(entity);
		return entity;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> auditWithdraw(
			long applyId,
			String action,
			String auditor,
			long auditorOperatorId,
			String remark,
			String operatorTypeRaw) {
		WithdrawApply apply = withdrawApplyMapper.selectById(applyId);
		if (apply == null) {
			throw new ResourceException("提现申请记录不存在");
		}

		String ot = operatorTypeRaw == null ? "" : operatorTypeRaw.trim();
		if (!"admin".equals(ot)) {
			throw new ForbiddenException("只有管理员可以审核提现申请");
		}

		Integer st = apply.getStatus();
		if (st == null || st != 0) {
			throw new ResourceException("该申请已审核，不能重复审核");
		}

		int auditTimeSec = (int) (System.currentTimeMillis() / 1000L);
		String auditRemark = remark == null ? "" : remark;

		String a = action == null ? "" : action.trim();
		int newStatus;
		if ("approve".equals(a)) {
			newStatus = 1;
		} else if ("reject".equals(a)) {
			newStatus = 2;
		} else {
			throw new BadRequestException("审核操作无效");
		}

		LambdaUpdateWrapper<WithdrawApply> uw = new LambdaUpdateWrapper<>();
		uw.eq(WithdrawApply::getId, applyId)
				.set(WithdrawApply::getAuditTime, auditTimeSec)
				.set(WithdrawApply::getAuditor, auditor == null ? "" : auditor)
				.set(WithdrawApply::getAuditorOperatorId, auditorOperatorId)
				.set(WithdrawApply::getAuditRemark, auditRemark)
				.set(WithdrawApply::getStatus, newStatus)
				.set(WithdrawApply::getUpdated, auditTimeSec);
		int rows = withdrawApplyMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		log.info(
				"提现申请审核完成 apply_id:{},action:{},auditor:{},auditor_operator_id:{},remark:{}",
				applyId,
				a,
				auditor,
				auditorOperatorId,
				auditRemark);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", apply.getId());
		out.put("amount", apply.getAmount());
		out.put("status", newStatus);
		out.put("audit_time", auditTimeSec);
		out.put("auditor", auditor == null ? "" : auditor);
		out.put("audit_remark", auditRemark);
		return out;
	}

	public void executeHuifuWithdraw(long applyId) {
		WithdrawApply apply = withdrawApplyMapper.selectById(applyId);
		if (apply == null) {
			throw new ResourceException("提现申请记录不存在");
		}

		Integer st = apply.getStatus();
		if (Objects.equals(st, 4) || Objects.equals(st, 3)) {
			log.info("提现申请已处理成功或正在处理，跳过重复执行 apply_id:{},status:{}", applyId, st);
			return;
		}
		if (!Objects.equals(st, 1) && !Objects.equals(st, 5)) {
			throw new ResourceException("提现申请状态不正确，无法执行，当前状态：" + labelForStatus(st));
		}

		String opType = apply.getOperatorType() == null ? "" : apply.getOperatorType().trim();
		String tokenNo;
		if ("admin".equals(opType)) {
			Map<String, Object> cfg = bsPayPaymentSettingService.requireSettingMap(apply.getCompanyId());
			tokenNo = Objects.toString(cfg.get("admin_token_no"), "").trim();
			if (!StringUtils.hasText(tokenNo)) {
				throw new ResourceException("未配置管理员提现卡序列号，请在支付配置中设置");
			}
		} else {
			UserCard card =
					userCardMapper.selectOne(
							Wrappers.<UserCard>lambdaQuery()
									.eq(UserCard::getCompanyId, apply.getCompanyId())
									.eq(UserCard::getHuifuId, apply.getHuifuId())
									.last("LIMIT 1"));
			if (card == null) {
				throw new ResourceException("未找到用户银行卡信息，请先完成银行卡绑定");
			}
			String applyNo = card.getApplyNo() == null ? "" : card.getApplyNo().trim();
			if (!StringUtils.hasText(applyNo)) {
				throw new ResourceException("取现卡序列号不存在，请先完成银行卡绑定");
			}
			tokenNo = applyNo;
		}

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", apply.getCompanyId());
		params.put("huifu_id", apply.getHuifuId());
		params.put("withdraw_type", apply.getWithdrawType());
		params.put("amount", apply.getAmount());
		params.put("token_no", tokenNo);

		try {
			withdrawHuifuWithdrawTransactionalService.runWithdrawEncashmentInTransaction(applyId, params, apply);
		} catch (Exception e) {
			String failureReason =
					e instanceof ResourceException re ? re.getMessage() : "汇付取现失败：" + e.getMessage();
			int failTs = (int) (System.currentTimeMillis() / 1000L);
			markFailedRequiresNew.executeWithoutResult(
					status -> {
						LambdaUpdateWrapper<WithdrawApply> uw = new LambdaUpdateWrapper<>();
						uw.eq(WithdrawApply::getId, applyId)
								.set(WithdrawApply::getStatus, 5)
								.set(WithdrawApply::getRequestTime, failTs)
								.set(WithdrawApply::getFailureReason, failureReason)
								.set(WithdrawApply::getUpdated, failTs);
						withdrawApplyMapper.update(null, uw);
					});
			log.error("bspay::doWithdraw::提现申请处理失败::apply_id:{}", applyId, e);
			throw new ResourceException(failureReason);
		}
	}

	private static String labelForStatus(Integer status) {
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

	public WithdrawApply getByReqSeqId(String reqSeqId) {
		if (!StringUtils.hasText(reqSeqId)) {
			return null;
		}
		return withdrawApplyMapper.selectOne(
				Wrappers.<WithdrawApply>lambdaQuery()
						.eq(WithdrawApply::getReqSeqId, reqSeqId.trim())
						.last("LIMIT 1"));
	}

	public void handleWithdrawNotify(Map<String, Object> notifyData, WithdrawApply withdrawApply) {
		if (notifyData == null) {
			throw new BadRequestException("回调数据无效");
		}
		if (withdrawApply == null) {
			return;
		}
		if (Objects.equals(withdrawApply.getStatus(), SUCCESS_STATUS)) {
			log.info(
					"重复通知 req_seq_id:{} id:{} status:{}",
					withdrawApply.getReqSeqId(),
					withdrawApply.getId(),
					withdrawApply.getStatus());
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		String trans = Objects.toString(notifyData.get("trans_status"), "");
		Integer newStatus;
		String failureReason = null;
		boolean updateFailureReason = false;
		switch (trans) {
			case "S" -> {
				newStatus = 4;
				failureReason = "";
				updateFailureReason = true;
			}
			case "F" -> {
				newStatus = 5;
				failureReason =
						String.format(
								"错误码：%s，错误描述：%s",
								Objects.toString(notifyData.get("sub_resp_code"), ""),
								Objects.toString(notifyData.get("sub_resp_desc"), ""));
				updateFailureReason = true;
			}
			case "P" -> newStatus = 3;
			default -> {
				log.info("状态异常 trans_status:{} apply_id:{}", trans, withdrawApply.getId());
				return;
			}
		}
		LambdaUpdateWrapper<WithdrawApply> uw = new LambdaUpdateWrapper<>();
		uw.eq(WithdrawApply::getId, withdrawApply.getId())
				.set(WithdrawApply::getStatus, newStatus)
				.set(WithdrawApply::getUpdated, now);
		if (updateFailureReason) {
			uw.set(WithdrawApply::getFailureReason, failureReason);
		}
		withdrawApplyMapper.update(null, uw);
		log.info(
				"提现回调状态已更新 req_seq_id:{} apply_id:{} hf_seq_id:{}",
				Objects.toString(notifyData.get("req_seq_id"), ""),
				withdrawApply.getId(),
				Objects.toString(notifyData.get("hf_seq_id"), ""));
	}
}
