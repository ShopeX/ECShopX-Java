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

import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.domain.UserUpdateLog;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
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
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserIndvUpdateService {

	private static final String AUDIT_WAIT = "A";
	private static final String AUDIT_FAIL = "B";
	private static final String HUIFU_RESP_OK = "00000000";

	private final UserEntCreateService userEntCreateService;
	private final UserIndvCreateService userIndvCreateService;
	private final UserIndvMapper userIndvMapper;
	private final BsPaySubUserV2UserSdkGateway bspaySubUserV2UserSdkGateway;
	private final UserUpdateLogMapper userUpdateLogMapper;
	private final ObjectMapper objectMapper;

	public UserIndvUpdateService(
			UserEntCreateService userEntCreateService,
			UserIndvCreateService userIndvCreateService,
			UserIndvMapper userIndvMapper,
			BsPaySubUserV2UserSdkGateway bspaySubUserV2UserSdkGateway,
			UserUpdateLogMapper userUpdateLogMapper,
			ObjectMapper objectMapper) {
		this.userEntCreateService = userEntCreateService;
		this.userIndvCreateService = userIndvCreateService;
		this.userIndvMapper = userIndvMapper;
		this.bspaySubUserV2UserSdkGateway = bspaySubUserV2UserSdkGateway;
		this.userUpdateLogMapper = userUpdateLogMapper;
		this.objectMapper = objectMapper;
	}

	public void update(long companyId, Map<String, Object> jwtMap, Map<String, Object> body) {
		Map<String, Object> params = new LinkedHashMap<>(body);
		params.put("company_id", companyId);
		params.put("cert_no", UserIndvCreateService.trimBspayParamString(params.get("cert_no")));
		params.put("card_no", UserIndvCreateService.trimBspayParamString(params.get("card_no")));

		userEntCreateService.validateEntUpdateBody(params);
		userIndvCreateService.checkIndvCertAndRegions(params);

		final String payloadJson;
		try {
			payloadJson = objectMapper.writeValueAsString(new LinkedHashMap<>(params));
		} catch (JsonProcessingException e) {
			throw new ResourceException("日志序列化失败");
		}

		long indvId = ((Number) params.get("id")).longValue();

		LambdaQueryWrapper<UserIndv> qw = Wrappers.lambdaQuery();
		qw.eq(UserIndv::getId, indvId).eq(UserIndv::getCompanyId, companyId);
		UserIndv row = userIndvMapper.selectOne(qw);
		if (row == null) {
			throw new ResourceException("开户信息不存在");
		}

		String reqSeqId = generateReqSeqId();

		LambdaUpdateWrapper<UserIndv> uw = Wrappers.lambdaUpdate();
		uw.eq(UserIndv::getId, indvId).eq(UserIndv::getCompanyId, companyId);
		uw.set(UserIndv::getIsUpdate, 1);
		uw.set(UserIndv::getReqSeqId, reqSeqId);
		uw.set(UserIndv::getUpdated, (int) (System.currentTimeMillis() / 1000));
		int n = userIndvMapper.update(null, uw);
		if (n != 1) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> sdk = new LinkedHashMap<>();
		sdk.put("req_seq_id", reqSeqId);
		sdk.put("name", nullToEmpty(row.getName()));
		sdk.put("cert_no", nullToEmpty(row.getCertNo()));
		sdk.put(
				"cert_validity_type",
				row.getCertValidityType() == null ? "" : String.valueOf(row.getCertValidityType()));
		sdk.put("cert_begin_date", nullToEmpty(row.getCertBeginDate()));
		sdk.put("cert_end_date", nullToEmpty(row.getCertEndDate()));
		sdk.put("mobile_no", nullToEmpty(row.getMobileNo()));

		Map<String, Object> resp;
		try {
			resp = bspaySubUserV2UserSdkGateway.basicdataIndv(companyId, sdk);
		} catch (BasePayException e) {
			String msg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : "请求失败";
			insertUpdateLog(companyId, row, indvId, payloadJson, AUDIT_FAIL, msg, nullToEmpty(row.getHuifuId()));
			throw new ResourceException("数据更新失败: " + msg);
		} catch (IllegalAccessException e) {
			insertUpdateLog(
					companyId, row, indvId, payloadJson, AUDIT_FAIL, "请求失败", nullToEmpty(row.getHuifuId()));
			throw new ResourceException("数据更新失败: 请求失败");
		}

		Map<String, Object> layer = extractBsPayDataPayload(resp);
		if (layer == null) {
			String msg =
					firstNonBlank(UserIndvCreateService.trimBspayParamString(resp.get("msg")), "请求失败");
			insertUpdateLog(companyId, row, indvId, payloadJson, AUDIT_FAIL, msg, nullToEmpty(row.getHuifuId()));
			throw new ResourceException("数据更新失败: " + msg);
		}
		if (!HUIFU_RESP_OK.equals(UserIndvCreateService.trimBspayParamString(layer.get("resp_code")))) {
			String msg = UserIndvCreateService.trimBspayParamString(layer.get("resp_desc"));
			insertUpdateLog(companyId, row, indvId, payloadJson, AUDIT_FAIL, msg, nullToEmpty(row.getHuifuId()));
			throw new ResourceException("数据更新失败: " + msg);
		}

		String huifuIdNew = UserIndvCreateService.trimBspayParamString(layer.get("huifu_id"));
		String huifuForLog =
				StringUtils.hasText(huifuIdNew) ? huifuIdNew : nullToEmpty(row.getHuifuId());
		insertUpdateLog(companyId, row, indvId, payloadJson, AUDIT_WAIT, "", huifuForLog);
	}

	private void insertUpdateLog(
			long companyId,
			UserIndv row,
			long indvId,
			String payloadJson,
			String auditState,
			String auditDesc,
			String huifuIdForLog) {
		UserUpdateLog log = new UserUpdateLog();
		log.setCompanyId(companyId);
		log.setSysId(nullToEmpty(row.getSysId()));
		log.setHuifuId(nullToEmpty(huifuIdForLog));
		log.setUserType("indv");
		log.setUserId(String.valueOf(indvId));
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
