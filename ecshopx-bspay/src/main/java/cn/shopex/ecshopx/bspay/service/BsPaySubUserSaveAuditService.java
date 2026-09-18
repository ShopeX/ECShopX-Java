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

import cn.shopex.ecshopx.bspay.domain.EntryApply;
import cn.shopex.ecshopx.bspay.domain.UserCard;
import cn.shopex.ecshopx.bspay.domain.UserEnt;
import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import cn.shopex.ecshopx.bspay.service.integration.BsPaySubUserV2UserSdkGateway;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huifu.bspay.sdk.opps.core.exception.BasePayException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BsPaySubUserSaveAuditService {

	private static final Logger log = LoggerFactory.getLogger(BsPaySubUserSaveAuditService.class);

	private static final String AUDIT_FAIL = "B";
	private static final String AUDIT_CARD_FAIL = "D";
	private static final String AUDIT_SUCCESS = "E";
	private static final String CARD_SUCCESS = "B";
	private static final String CARD_FAIL = "C";
	private static final String BUSINESS_SUCC = "S";
	private static final String HUIFU_RESP_OK = "00000000";

	private final EntryApplyMapper entryApplyMapper;
	private final UserEntMapper userEntMapper;
	private final UserIndvMapper userIndvMapper;
	private final UserCardMapper userCardMapper;
	private final DistributorMapper distributorMapper;
	private final BsPayPaymentSettingService bsPayPaymentSettingService;
	private final BsPaySubUserV2UserSdkGateway bspaySubUserV2UserSdkGateway;
	private final ObjectMapper objectMapper;

	public BsPaySubUserSaveAuditService(
			EntryApplyMapper entryApplyMapper,
			UserEntMapper userEntMapper,
			UserIndvMapper userIndvMapper,
			UserCardMapper userCardMapper,
			DistributorMapper distributorMapper,
			BsPayPaymentSettingService bsPayPaymentSettingService,
			BsPaySubUserV2UserSdkGateway bspaySubUserV2UserSdkGateway,
			ObjectMapper objectMapper) {
		this.entryApplyMapper = entryApplyMapper;
		this.userEntMapper = userEntMapper;
		this.userIndvMapper = userIndvMapper;
		this.userCardMapper = userCardMapper;
		this.distributorMapper = distributorMapper;
		this.bsPayPaymentSettingService = bsPayPaymentSettingService;
		this.bspaySubUserV2UserSdkGateway = bspaySubUserV2UserSdkGateway;
		this.objectMapper = objectMapper;
	}

	public void saveAudit(long companyId, Map<String, Object> body) {
		requireStatusPresent(body);
		long applyId = resolveApplyId(body);
		String statusParam = str(body.get("status")).trim();

		Map<String, Object> split = parseSplitLedgerJson(body.get("split_ledger_info"));
		validateSplitLedger(split);

		EntryApply apply = loadApplyOrThrow(companyId, applyId);
		long userSubjectPk = parseApplyUserId(apply.getUserId());

		String userType = apply.getUserType() == null ? "" : apply.getUserType().trim();
		UserEnt userEnt = null;
		UserIndv userIndv = null;
		if ("ent".equalsIgnoreCase(userType)) {
			userEnt = userEntMapper.selectById(userSubjectPk);
			if (userEnt == null || !Objects.equals(userEnt.getCompanyId(), companyId)) {
				throw new ResourceException("未找到用户进件信息");
			}
		} else if ("indv".equalsIgnoreCase(userType)) {
			userIndv = userIndvMapper.selectById(userSubjectPk);
			if (userIndv == null || !Objects.equals(userIndv.getCompanyId(), companyId)) {
				throw new ResourceException("未找到用户进件信息");
			}
		} else {
			throw new ResourceException("用户类型错误！");
		}

		UserCard card = loadUserCardRelaxed(companyId, userSubjectPk);
		Map<String, Object> userCardMap = buildUserCardSnakeMap(card);

		if ("APPROVED".equals(statusParam)) {
			if ("distributor".equalsIgnoreCase(str(body.get("operator_type")).trim())) {
				updateDistributorSplitLedger(companyId, body);
			}

			String reqSeqId1 = generateReqSeqId();
			Map<String, Object> subjectPayload =
					userEnt != null
							? buildEntSubjectPayload(userEnt, reqSeqId1)
							: buildIndvSubjectPayload(userIndv, reqSeqId1);

			boolean isModify =
					userEnt != null
							? userEnt.getIsUpdate() != null && userEnt.getIsUpdate() == 1
							: userIndv.getIsUpdate() != null && userIndv.getIsUpdate() == 1;

			BasicdataCallResult basic = callBasicdata(companyId, userEnt != null, isModify, subjectPayload);
			applySubjectUpdateAfterBasicdata(companyId, userSubjectPk, userEnt != null, basic);

			String huifuFromBasic = basic.huifuId();
			if (StringUtils.hasText(huifuFromBasic)) {
				userCardMap.put("huifu_id", huifuFromBasic);
				Map<String, Object> setting = bsPayPaymentSettingService.requireSettingMap(companyId);
				userCardMap.put("upper_huifu_id", str(setting.get("sys_id")));

				boolean useBusiModify = CARD_SUCCESS.equals(str(userCardMap.get("audit_state")));
				String reqSeqId2 = generateReqSeqId();
				userCardMap.put("req_seq_id", reqSeqId2);

				BusiCallResult busi = callBusi(companyId, useBusiModify, userCardMap);

				LambdaUpdateWrapper<UserCard> cardUw = new LambdaUpdateWrapper<>();
				cardUw.eq(UserCard::getCompanyId, companyId).eq(UserCard::getUserId, userSubjectPk);
				cardUw.set(UserCard::getReqSeqId, reqSeqId2);
				cardUw.set(UserCard::getHuifuId, huifuFromBasic);

				Map<String, Object> applySide = new LinkedHashMap<>();
				if (busi.error()) {
					cardUw.set(UserCard::getAuditDesc, busi.cardAuditDesc());
					cardUw.set(UserCard::getAuditState, CARD_FAIL);
				} else {
					Map<String, Object> data = busi.data();
					String respCode = str(data.get("resp_code"));
					if (HUIFU_RESP_OK.equals(respCode)) {
						cardUw.set(UserCard::getApplyNo, str(data.get("token_no")));
						String respBusinessRaw = str(data.get("resp_business"));
						applyBusiBusinessList(respBusinessRaw, cardUw, applySide);
					} else {
						cardUw.set(UserCard::getAuditDesc, str(data.get("resp_desc")));
						cardUw.set(UserCard::getAuditState, CARD_FAIL);
						applySide.put("audit_desc", str(data.get("resp_desc")));
					}
				}
				log.info(
						"saveAudit busi result====> {}",
						busi.rawForLog() == null ? "" : busi.rawForLog());
				log.info(
						"saveAudit update_filter====> company_id={}, user_id={}, card_row_update",
						companyId,
						userSubjectPk);

				userCardMapper.update(null, cardUw);

				if (!applySide.isEmpty()) {
					log.info(
							"saveAudit apply_update_filter====> company_id={}, id={}, apply_update_data====> {}",
							companyId,
							userSubjectPk,
							applySide);
					applySubjectPatch(companyId, userSubjectPk, userEnt != null, applySide);
				}
			}
		} else {
			LambdaUpdateWrapper<UserEnt> entUw = new LambdaUpdateWrapper<>();
			entUw.eq(UserEnt::getCompanyId, companyId).eq(UserEnt::getId, userSubjectPk);
			entUw.set(UserEnt::getAuditState, AUDIT_FAIL);
			entUw.set(UserEnt::getAuditDesc, str(body.get("comments")));
			LambdaUpdateWrapper<UserIndv> indvUw = new LambdaUpdateWrapper<>();
			indvUw.eq(UserIndv::getCompanyId, companyId).eq(UserIndv::getId, userSubjectPk);
			indvUw.set(UserIndv::getAuditState, AUDIT_FAIL);
			indvUw.set(UserIndv::getAuditDesc, str(body.get("comments")));
			if (userEnt != null) {
				userEntMapper.update(null, entUw);
			} else {
				userIndvMapper.update(null, indvUw);
			}
		}

		updateEntryApplyStatus(companyId, applyId, statusParam, str(body.get("comments")));
	}

	private void applyBusiBusinessList(
			String respBusinessRaw,
			LambdaUpdateWrapper<UserCard> cardUw,
			Map<String, Object> applySide) {
		List<Map<String, Object>> list = parseRespBusinessList(respBusinessRaw);
		for (Map<String, Object> b : list) {
			if (!"1".equals(str(b.get("type")))) {
				continue;
			}
			if (BUSINESS_SUCC.equals(str(b.get("code")))) {
				cardUw.set(UserCard::getAuditState, CARD_SUCCESS);
				applySide.put("audit_state", AUDIT_SUCCESS);
			} else {
				String msg = str(b.get("msg"));
				cardUw.set(UserCard::getAuditDesc, msg);
				cardUw.set(UserCard::getAuditState, CARD_FAIL);
				applySide.put("audit_desc", msg);
			}
		}
	}

	private List<Map<String, Object>> parseRespBusinessList(String respBusinessRaw) {
		if (!StringUtils.hasText(respBusinessRaw)) {
			return List.of();
		}
		try {
			List<Object> raw = objectMapper.readValue(respBusinessRaw, new TypeReference<List<Object>>() {});
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : raw) {
				if (o instanceof Map<?, ?> m) {
					@SuppressWarnings("unchecked")
					Map<String, Object> cast = (Map<String, Object>) m;
					out.add(cast);
				}
			}
			return out;
		} catch (JsonProcessingException e) {
			return List.of();
		}
	}

	private record BasicdataCallResult(
			String auditState, String auditDesc, String huifuId, Integer isUpdate, String reqSeqId) {}

	private BasicdataCallResult callBasicdata(
			long companyId, boolean ent, boolean modify, Map<String, Object> subjectPayload) {
		String reqSeqId = str(subjectPayload.get("req_seq_id"));
		try {
			Map<String, Object> resp =
					ent
							? (modify
									? bspaySubUserV2UserSdkGateway.basicdataEntModify(companyId, subjectPayload)
									: bspaySubUserV2UserSdkGateway.basicdataEnt(companyId, subjectPayload))
							: (modify
									? bspaySubUserV2UserSdkGateway.basicdataIndvModify(companyId, subjectPayload)
									: bspaySubUserV2UserSdkGateway.basicdataIndv(companyId, subjectPayload));
			Map<String, Object> layer = extractBsPayDataPayload(resp);
			if (layer == null) {
				String msg = firstNonBlank(str(resp.get("msg")), "请求失败");
				return new BasicdataCallResult(AUDIT_FAIL, msg, "", null, reqSeqId);
			}
			String code = str(layer.get("resp_code"));
			if (HUIFU_RESP_OK.equals(code)) {
				String huifuId = str(layer.get("huifu_id"));
				return new BasicdataCallResult(AUDIT_CARD_FAIL, "", huifuId, 1, reqSeqId);
			}
			return new BasicdataCallResult(AUDIT_FAIL, str(layer.get("resp_desc")), "", null, reqSeqId);
		} catch (BasePayException e) {
			String msg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : "请求失败";
			return new BasicdataCallResult(AUDIT_FAIL, msg, "", null, reqSeqId);
		} catch (IllegalAccessException e) {
			return new BasicdataCallResult(AUDIT_FAIL, "请求失败", "", null, reqSeqId);
		}
	}

	private void applySubjectUpdateAfterBasicdata(
			long companyId, long userSubjectPk, boolean ent, BasicdataCallResult r) {
		LambdaUpdateWrapper<UserEnt> entUw = new LambdaUpdateWrapper<>();
		entUw.eq(UserEnt::getCompanyId, companyId).eq(UserEnt::getId, userSubjectPk);
		entUw.set(UserEnt::getReqSeqId, r.reqSeqId());
		entUw.set(UserEnt::getAuditState, r.auditState());
		if (StringUtils.hasText(r.auditDesc())) {
			entUw.set(UserEnt::getAuditDesc, r.auditDesc());
		}
		if (StringUtils.hasText(r.huifuId())) {
			entUw.set(UserEnt::getHuifuId, r.huifuId());
		}
		if (r.isUpdate() != null) {
			entUw.set(UserEnt::getIsUpdate, r.isUpdate());
		}
		LambdaUpdateWrapper<UserIndv> indvUw = new LambdaUpdateWrapper<>();
		indvUw.eq(UserIndv::getCompanyId, companyId).eq(UserIndv::getId, userSubjectPk);
		indvUw.set(UserIndv::getReqSeqId, r.reqSeqId());
		indvUw.set(UserIndv::getAuditState, r.auditState());
		if (StringUtils.hasText(r.auditDesc())) {
			indvUw.set(UserIndv::getAuditDesc, r.auditDesc());
		}
		if (StringUtils.hasText(r.huifuId())) {
			indvUw.set(UserIndv::getHuifuId, r.huifuId());
		}
		if (r.isUpdate() != null) {
			indvUw.set(UserIndv::getIsUpdate, r.isUpdate());
		}
		if (ent) {
			userEntMapper.update(null, entUw);
		} else {
			userIndvMapper.update(null, indvUw);
		}
	}

	private void applySubjectPatch(long companyId, long userSubjectPk, boolean ent, Map<String, Object> patch) {
		LambdaUpdateWrapper<UserEnt> entUw = new LambdaUpdateWrapper<>();
		entUw.eq(UserEnt::getCompanyId, companyId).eq(UserEnt::getId, userSubjectPk);
		LambdaUpdateWrapper<UserIndv> indvUw = new LambdaUpdateWrapper<>();
		indvUw.eq(UserIndv::getCompanyId, companyId).eq(UserIndv::getId, userSubjectPk);
		if (patch.containsKey("audit_state")) {
			entUw.set(UserEnt::getAuditState, str(patch.get("audit_state")));
			indvUw.set(UserIndv::getAuditState, str(patch.get("audit_state")));
		}
		if (patch.containsKey("audit_desc")) {
			entUw.set(UserEnt::getAuditDesc, str(patch.get("audit_desc")));
			indvUw.set(UserIndv::getAuditDesc, str(patch.get("audit_desc")));
		}
		if (ent) {
			userEntMapper.update(null, entUw);
		} else {
			userIndvMapper.update(null, indvUw);
		}
	}

	private record BusiCallResult(boolean error, String cardAuditDesc, Map<String, Object> data, String rawForLog) {
		static BusiCallResult err(String desc) {
			return new BusiCallResult(true, desc, Map.of(), null);
		}

		static BusiCallResult ok(Map<String, Object> data, String rawForLog) {
			return new BusiCallResult(false, "", data, rawForLog);
		}
	}

	private BusiCallResult callBusi(long companyId, boolean modify, Map<String, Object> userCardMap) {
		try {
			Map<String, Object> resp =
					modify
							? bspaySubUserV2UserSdkGateway.busiModify(companyId, userCardMap)
							: bspaySubUserV2UserSdkGateway.busiOpen(companyId, userCardMap);
			String rawLog;
			try {
				rawLog = objectMapper.writeValueAsString(resp);
			} catch (JsonProcessingException e) {
				rawLog = String.valueOf(resp);
			}
			Map<String, Object> layer = extractBsPayDataPayload(resp);
			if (layer == null) {
				String desc = extractBusiErrorDesc(resp);
				return BusiCallResult.err(desc);
			}
			return BusiCallResult.ok(layer, rawLog);
		} catch (BasePayException e) {
			String desc = extractBusiExceptionDesc(e);
			return BusiCallResult.err(desc);
		} catch (IllegalAccessException e) {
			return BusiCallResult.err("请求失败");
		}
	}

	private static String extractBusiExceptionDesc(BasePayException e) {
		String code = e.getCode();
		if (StringUtils.hasText(code)) {
			return code + ": " + (e.getMessage() == null ? "" : e.getMessage());
		}
		return StringUtils.hasText(e.getMessage()) ? e.getMessage() : "请求失败";
	}

	@SuppressWarnings("unchecked")
	private static String extractBusiErrorDesc(Map<String, Object> resp) {
		Object data = resp.get("data");
		if (data instanceof Map<?, ?> m1) {
			Object inner = m1.get("data");
			if (inner instanceof Map<?, ?> m2) {
				String d = str(m2.get("resp_desc"));
				if (StringUtils.hasText(d)) {
					return d;
				}
			}
			String d = str(m1.get("resp_desc"));
			if (StringUtils.hasText(d)) {
				return d;
			}
		}
		return firstNonBlank(str(resp.get("msg")), "请求失败");
	}

	private void updateDistributorSplitLedger(long companyId, Map<String, Object> body) {
		if (!body.containsKey("save_id")) {
			throw new BadRequestException("参数错误");
		}
		Object rawSaveId = body.get("save_id");
		LambdaUpdateWrapper<Distributor> uw = new LambdaUpdateWrapper<>();
		uw.eq(Distributor::getCompanyId, companyId);
		if (rawSaveId == null) {
			uw.isNull(Distributor::getDistributorId);
		} else {
			uw.eq(Distributor::getDistributorId, parseSaveIdScalar(rawSaveId));
		}
		uw.set(Distributor::getBspaySplitLedgerInfo, splitLedgerToDbString(body.get("split_ledger_info")));
		distributorMapper.update(null, uw);
	}

	private String splitLedgerToDbString(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof Map<?, ?> m) {
			try {
				return objectMapper.writeValueAsString(m);
			} catch (JsonProcessingException e) {
				return "";
			}
		}
		return str(raw);
	}

	private static long parseSaveIdScalar(Object raw) {
		if (raw == null) {
			throw new BadRequestException("参数错误");
		}
		if (raw instanceof Boolean || raw instanceof Map<?, ?> || raw instanceof Collection<?>) {
			throw new BadRequestException("参数错误");
		}
		if (raw.getClass().isArray()) {
			throw new BadRequestException("参数错误");
		}
		if (raw instanceof Double d) {
			if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.rint(d)) {
				throw new BadRequestException("参数错误");
			}
			if (d > Long.MAX_VALUE || d < Long.MIN_VALUE) {
				throw new BadRequestException("参数错误");
			}
			return d.longValue();
		}
		if (raw instanceof Float f) {
			double d = f.doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.rint(d)) {
				throw new BadRequestException("参数错误");
			}
			if (d > Long.MAX_VALUE || d < Long.MIN_VALUE) {
				throw new BadRequestException("参数错误");
			}
			return (long) d;
		}
		if (raw instanceof BigDecimal bd) {
			if (bd.scale() > 0) {
				throw new BadRequestException("参数错误");
			}
			try {
				return bd.longValueExact();
			} catch (ArithmeticException e) {
				throw new BadRequestException("参数错误");
			}
		}
		if (raw instanceof BigInteger bi) {
			try {
				return bi.longValueExact();
			} catch (ArithmeticException e) {
				throw new BadRequestException("参数错误");
			}
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("参数错误");
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数错误");
			}
		}
		throw new BadRequestException("参数错误");
	}

	private static void requireStatusPresent(Map<String, Object> body) {
		if (!body.containsKey("status")) {
			throw new BadRequestException("参数错误");
		}
		if (!StringUtils.hasText(str(body.get("status")).trim())) {
			throw new BadRequestException("参数错误");
		}
	}

	private static long resolveApplyId(Map<String, Object> body) {
		if (!body.containsKey("id")) {
			throw new ResourceException("未找到用户进件信息");
		}
		Object raw = body.get("id");
		if (raw == null) {
			throw new ResourceException("未找到用户进件信息");
		}
		String t = str(raw).trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException("未找到用户进件信息");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("未找到用户进件信息");
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseSplitLedgerJson(Object raw) {
		if (raw == null) {
			throw new BadRequestException("参数错误");
		}
		if (raw instanceof Map<?, ?> m) {
			return new LinkedHashMap<>((Map<String, Object>) m);
		}
		if (!(raw instanceof String s)) {
			throw new BadRequestException("参数错误");
		}
		String t = s.trim();
		if (!StringUtils.hasText(t)) {
			throw new BadRequestException("参数错误");
		}
		try {
			Object parsed = objectMapper.readValue(t, Object.class);
			if (!(parsed instanceof Map<?, ?>)) {
				throw new BadRequestException("参数错误");
			}
			return new LinkedHashMap<>((Map<String, Object>) parsed);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数错误");
		}
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof String s) {
			if (s.isEmpty() || "0".equals(s)) {
				return false;
			}
			return true;
		}
		if (v instanceof Number n) {
			if (n instanceof BigDecimal bd) {
				return bd.compareTo(BigDecimal.ZERO) != 0;
			}
			if (n instanceof Double d) {
				return d != 0.0 && !Double.isNaN(d);
			}
			if (n instanceof Float f) {
				return f != 0.0f && !Float.isNaN(f);
			}
			return n.longValue() != 0L;
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		return true;
	}

	private static BigDecimal bd(Object v) {
		if (v == null) {
			return BigDecimal.ZERO;
		}
		if (v instanceof BigDecimal b) {
			return b;
		}
		if (v instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		String t = str(v).trim();
		if (!StringUtils.hasText(t)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(t);
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private void validateSplitLedger(Map<String, Object> split) {
		BigDecimal hq = bd(split.get("headquarters_proportion"));
		if (truthy(split.get("dealer_proportion"))) {
			if (hq.add(bd(split.get("dealer_proportion"))).compareTo(new BigDecimal("100")) > 0) {
				throw new ResourceException("分账占比合必须小于等于100%");
			}
		} else if (truthy(split.get("merchant_proportion"))) {
			if (hq.add(bd(split.get("merchant_proportion"))).compareTo(new BigDecimal("100")) > 0) {
				throw new ResourceException("分账占比合必须小于等于100%");
			}
		} else {
			if (hq.compareTo(new BigDecimal("100")) > 0) {
				throw new ResourceException("分账占比必须小于等于100%");
			}
		}
	}

	private EntryApply loadApplyOrThrow(long companyId, long applyId) {
		EntryApply apply =
				entryApplyMapper.selectOne(
						new LambdaQueryWrapper<EntryApply>()
								.eq(EntryApply::getCompanyId, companyId)
								.eq(EntryApply::getId, applyId)
								.orderByDesc(EntryApply::getCreated)
								.last("LIMIT 1"));
		if (apply == null) {
			throw new ResourceException("未找到用户进件信息");
		}
		return apply;
	}

	private static long parseApplyUserId(String raw) {
		String t = raw == null ? "" : raw.trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException("未找到用户进件信息");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("未找到用户进件信息");
		}
	}

	private UserCard loadUserCardRelaxed(long companyId, long userSubjectId) {
		return userCardMapper.selectOne(
				new LambdaQueryWrapper<UserCard>()
						.eq(UserCard::getCompanyId, companyId)
						.eq(UserCard::getUserId, userSubjectId)
						.last("LIMIT 1"));
	}

	private void updateEntryApplyStatus(long companyId, long applyId, String status, String comments) {
		int now = (int) Instant.now().getEpochSecond();
		LambdaUpdateWrapper<EntryApply> uw = new LambdaUpdateWrapper<>();
		uw.eq(EntryApply::getCompanyId, companyId).eq(EntryApply::getId, applyId);
		uw.set(EntryApply::getStatus, status);
		uw.set(EntryApply::getComments, comments);
		uw.set(EntryApply::getUpdated, now);
		int rows = entryApplyMapper.update(null, uw);
		if (rows != 1) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	private static Map<String, Object> buildUserCardSnakeMap(UserCard card) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (card == null) {
			return m;
		}
		m.put("card_type", nz(card.getCardType()));
		m.put("card_name", nz(card.getCardName()));
		m.put("card_no", nz(card.getCardNo()));
		m.put("prov_id", nz(card.getProvId()));
		m.put("area_id", nz(card.getAreaId()));
		m.put("bank_code", nz(card.getBankCode()));
		m.put("branch_name", nz(card.getBranchName()));
		m.put("cert_no", nz(card.getCertNo()));
		m.put(
				"cert_validity_type",
				card.getCertValidityType() == null ? "" : Integer.toString(card.getCertValidityType()));
		m.put("cert_begin_date", nz(card.getCertBeginDate()));
		m.put("cert_end_date", nz(card.getCertEndDate()));
		m.put("mp", nz(card.getMp()));
		m.put("audit_state", nz(card.getAuditState()));
		m.put("huifu_id", nz(card.getHuifuId()));
		return m;
	}

	private static Map<String, Object> buildEntSubjectPayload(UserEnt e, String reqSeqId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("req_seq_id", reqSeqId);
		m.put("huifu_id", nz(e.getHuifuId()));
		m.put("reg_name", nz(e.getRegName()));
		m.put("license_code", nz(e.getLicenseCode()));
		m.put(
				"license_validity_type",
				e.getLicenseValidityType() == null ? "" : Integer.toString(e.getLicenseValidityType()));
		m.put("license_begin_date", nz(e.getLicenseBeginDate()));
		m.put("license_end_date", nz(e.getLicenseEndDate()));
		m.put("reg_prov_id", nz(e.getRegProvId()));
		m.put("reg_area_id", nz(e.getRegAreaId()));
		m.put("reg_district_id", nz(e.getRegDistrictId()));
		m.put("reg_detail", nz(e.getRegDetail()));
		m.put("legal_name", nz(e.getLegalName()));
		m.put("legal_cert_no", nz(e.getLegalCertNo()));
		m.put(
				"legal_cert_validity_type",
				e.getLegalCertValidityType() == null ? "" : Integer.toString(e.getLegalCertValidityType()));
		m.put("legal_cert_begin_date", nz(e.getLegalCertBeginDate()));
		m.put("legal_cert_end_date", nz(e.getLegalCertEndDate()));
		m.put("contact_name", nz(e.getContactName()));
		m.put("contact_mobile", nz(e.getContactMobile()));
		return m;
	}

	private static Map<String, Object> buildIndvSubjectPayload(UserIndv i, String reqSeqId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("req_seq_id", reqSeqId);
		m.put("huifu_id", nz(i.getHuifuId()));
		m.put("name", nz(i.getName()));
		m.put("cert_no", nz(i.getCertNo()));
		m.put(
				"cert_validity_type",
				i.getCertValidityType() == null ? "" : Integer.toString(i.getCertValidityType()));
		m.put("cert_begin_date", nz(i.getCertBeginDate()));
		m.put("cert_end_date", nz(i.getCertEndDate()));
		m.put("mobile_no", nz(i.getMobileNo()));
		return m;
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static String generateReqSeqId() {
		String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now());
		int n = ThreadLocalRandom.current().nextInt(1_000_000);
		return ts + String.format("%06d", n);
	}

	private static String str(Object o) {
		return o == null ? "" : Objects.toString(o, "");
	}

	private static String firstNonBlank(String a, String b) {
		return StringUtils.hasText(a) ? a : b;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> extractBsPayDataPayload(Map<String, Object> resp) {
		if (resp == null) {
			return null;
		}
		Object layer1 = resp.get("data");
		if (!(layer1 instanceof Map<?, ?> m1)) {
			return null;
		}
		Object inner = m1.get("data");
		if (inner instanceof Map<?, ?> m2 && m2.containsKey("resp_code")) {
			return (Map<String, Object>) m2;
		}
		if (m1.containsKey("resp_code")) {
			return (Map<String, Object>) m1;
		}
		return null;
	}
}
