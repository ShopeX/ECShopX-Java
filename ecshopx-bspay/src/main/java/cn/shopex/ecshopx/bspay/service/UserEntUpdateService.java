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

import cn.shopex.ecshopx.bspay.domain.UserEnt;
import cn.shopex.ecshopx.bspay.domain.UserUpdateLog;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.mapper.UserUpdateLogMapper;
import cn.shopex.ecshopx.bspay.service.integration.BsPaySubUserV2UserSdkGateway;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huifu.bspay.sdk.opps.core.exception.BasePayException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserEntUpdateService {

	private static final String AUDIT_WAIT = "A";
	private static final String AUDIT_FAIL = "B";
	private static final String HUIFU_RESP_OK = "00000000";

	private final UserEntCreateService userEntCreateService;
	private final UserEntMapper userEntMapper;
	private final BsPaySubUserV2UserSdkGateway bspaySubUserV2UserSdkGateway;
	private final UserUpdateLogMapper userUpdateLogMapper;
	private final ObjectMapper objectMapper;

	public UserEntUpdateService(
			UserEntCreateService userEntCreateService,
			UserEntMapper userEntMapper,
			BsPaySubUserV2UserSdkGateway bspaySubUserV2UserSdkGateway,
			UserUpdateLogMapper userUpdateLogMapper,
			ObjectMapper objectMapper) {
		this.userEntCreateService = userEntCreateService;
		this.userEntMapper = userEntMapper;
		this.bspaySubUserV2UserSdkGateway = bspaySubUserV2UserSdkGateway;
		this.userUpdateLogMapper = userUpdateLogMapper;
		this.objectMapper = objectMapper;
	}

	public void update(long companyId, Map<String, Object> jwtMap, Map<String, Object> body) {
		Map<String, Object> params = new LinkedHashMap<>(body);
		params.put("company_id", companyId);
		String legalCertNo = str(params.get("legal_cert_no"));
		String cardNo = str(params.get("card_no"));
		params.put("legal_cert_no", legalCertNo);
		params.put("card_no", cardNo);

		userEntCreateService.validateEntUpdateBody(params);
		userEntCreateService.normalizeEntBodyParamsForUpdate(params);

		final String payloadJson;
		try {
			payloadJson = objectMapper.writeValueAsString(new LinkedHashMap<>(params));
		} catch (JsonProcessingException e) {
			throw new ResourceException("日志序列化失败");
		}

		long entId = ((Number) params.get("id")).longValue();

		LambdaQueryWrapper<UserEnt> qw = Wrappers.lambdaQuery();
		qw.eq(UserEnt::getId, entId).eq(UserEnt::getCompanyId, companyId);
		UserEnt row = userEntMapper.selectOne(qw);
		if (row == null) {
			throw new ResourceException("开户信息不存在");
		}

		String reqSeqId = generateReqSeqId();

		LambdaUpdateWrapper<UserEnt> uw = Wrappers.lambdaUpdate();
		uw.eq(UserEnt::getId, entId).eq(UserEnt::getCompanyId, companyId);
		uw.set(UserEnt::getIsUpdate, 1);
		uw.set(UserEnt::getReqSeqId, reqSeqId);
		uw.set(UserEnt::getUpdated, (int) (System.currentTimeMillis() / 1000));
		int n = userEntMapper.update(null, uw);
		if (n != 1) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> sdk = buildBasicdataEntModifySdk(params, row, reqSeqId);

		Map<String, Object> resp;
		try {
			resp = bspaySubUserV2UserSdkGateway.basicdataEntModify(companyId, sdk);
		} catch (BasePayException e) {
			String msg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : "请求失败";
			insertUpdateLog(companyId, row, entId, payloadJson, AUDIT_FAIL, msg, nullToEmpty(row.getHuifuId()));
			throw new ResourceException("数据更新失败: " + msg);
		} catch (IllegalAccessException e) {
			insertUpdateLog(companyId, row, entId, payloadJson, AUDIT_FAIL, "请求失败", nullToEmpty(row.getHuifuId()));
			throw new ResourceException("数据更新失败: 请求失败");
		}

		Map<String, Object> layer = extractBsPayDataPayload(resp);
		if (layer == null) {
			String msg = firstNonBlank(str(resp.get("msg")), "请求失败");
			insertUpdateLog(companyId, row, entId, payloadJson, AUDIT_FAIL, msg, nullToEmpty(row.getHuifuId()));
			throw new ResourceException("数据更新失败: " + msg);
		}
		if (!HUIFU_RESP_OK.equals(str(layer.get("resp_code")))) {
			String msg = str(layer.get("resp_desc"));
			insertUpdateLog(companyId, row, entId, payloadJson, AUDIT_FAIL, msg, nullToEmpty(row.getHuifuId()));
			throw new ResourceException("数据更新失败: " + msg);
		}

		String huifuIdNew = str(layer.get("huifu_id"));
		String huifuForLog =
				StringUtils.hasText(huifuIdNew) ? huifuIdNew : nullToEmpty(row.getHuifuId());
		insertUpdateLog(companyId, row, entId, payloadJson, AUDIT_WAIT, "", huifuForLog);
	}

	private Map<String, Object> buildBasicdataEntModifySdk(
			Map<String, Object> params, UserEnt row, String reqSeqId) {
		Map<String, Object> sdk = new LinkedHashMap<>();
		sdk.put("req_seq_id", reqSeqId);
		sdk.put("huifu_id", nullToEmpty(row.getHuifuId()));
		sdk.put("reg_name", pickStrParamOrRow(params, "reg_name", row.getRegName()));
		sdk.put("license_code", pickStrParamOrRow(params, "license_code", row.getLicenseCode()));
		sdk.put(
				"license_validity_type",
				pickLicenseValidityTypeSdk(params, row));
		sdk.put(
				"license_begin_date",
				pickStrParamOrRow(params, "license_begin_date", row.getLicenseBeginDate()));
		sdk.put("license_end_date", pickStrParamOrRow(params, "license_end_date", row.getLicenseEndDate()));
		sdk.put("reg_prov_id", pickStrParamOrRow(params, "reg_prov_id", row.getRegProvId()));
		sdk.put("reg_area_id", pickStrParamOrRow(params, "reg_area_id", row.getRegAreaId()));
		sdk.put("reg_district_id", pickStrParamOrRow(params, "reg_district_id", row.getRegDistrictId()));
		sdk.put("reg_detail", pickStrParamOrRow(params, "reg_detail", row.getRegDetail()));
		sdk.put("legal_name", pickStrParamOrRow(params, "legal_name", row.getLegalName()));
		sdk.put("legal_cert_no", pickStrParamOrRow(params, "legal_cert_no", row.getLegalCertNo()));
		sdk.put(
				"legal_cert_validity_type",
				pickLegalCertValidityTypeSdk(params, row));
		sdk.put(
				"legal_cert_begin_date",
				pickStrParamOrRow(params, "legal_cert_begin_date", row.getLegalCertBeginDate()));
		sdk.put("contact_name", pickStrParamOrRow(params, "contact_name", row.getContactName()));
		sdk.put("contact_mobile", pickStrParamOrRow(params, "contact_mobile", row.getContactMobile()));
		return sdk;
	}

	private String pickStrParamOrRow(Map<String, Object> params, String key, String rowVal) {
		String p = str(params.get(key));
		if (StringUtils.hasText(p)) {
			return p;
		}
		return nullToEmpty(rowVal);
	}

	private String pickLicenseValidityTypeSdk(Map<String, Object> params, UserEnt row) {
		String p = str(params.get("license_validity_type"));
		if (StringUtils.hasText(p)) {
			return String.valueOf(userEntCreateService.persistLicenseValidityType(params.get("license_validity_type")));
		}
		return row.getLicenseValidityType() == null ? "" : String.valueOf(row.getLicenseValidityType());
	}

	private String pickLegalCertValidityTypeSdk(Map<String, Object> params, UserEnt row) {
		String p = str(params.get("legal_cert_validity_type"));
		if (StringUtils.hasText(p)) {
			return String.valueOf(
					userEntCreateService.persistLegalCertValidityType(params.get("legal_cert_validity_type")));
		}
		return row.getLegalCertValidityType() == null ? "" : String.valueOf(row.getLegalCertValidityType());
	}

	private void insertUpdateLog(
			long companyId,
			UserEnt row,
			long entId,
			String payloadJson,
			String auditState,
			String auditDesc,
			String huifuIdForLog) {
		UserUpdateLog log = new UserUpdateLog();
		log.setCompanyId(companyId);
		log.setSysId(nullToEmpty(row.getSysId()));
		log.setHuifuId(nullToEmpty(huifuIdForLog));
		log.setUserType("ent");
		log.setUserId(String.valueOf(entId));
		log.setData(payloadJson);
		log.setAuditState(auditState);
		log.setAuditDesc(auditDesc == null ? "" : auditDesc);
		int now = (int) (System.currentTimeMillis() / 1000);
		log.setCreated(now);
		log.setUpdated(now);
		int ins = userUpdateLogMapper.insert(log);
		if (ins <= 0) {
			throw new ResourceException("日志记录失败");
		}
	}

	private static String generateReqSeqId() {
		String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
		int rnd = ThreadLocalRandom.current().nextInt(1_000_000);
		return ts + String.format("%06d", rnd);
	}

	private static String str(Object o) {
		return o == null ? "" : Objects.toString(o, "").trim();
	}

	private static String firstNonBlank(String a, String b) {
		return StringUtils.hasText(a) ? a : b;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
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
