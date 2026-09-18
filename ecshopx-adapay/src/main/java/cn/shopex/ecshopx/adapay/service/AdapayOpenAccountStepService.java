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
import cn.shopex.ecshopx.adapay.domain.AdapayMerchantEntry;
import cn.shopex.ecshopx.adapay.domain.AdapayMerchantResident;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantEntryMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantResidentMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdapayOpenAccountStepService {

	private static final String PLACEHOLDER_IMAGE_URL =
			"https://b-img-cdn.yuanyuanke.cn/image/21/2021/10/21/6522d21e446741584632bc04601feb6fBy5oX5syerwAs6FP1cyfOWJd90z5Mb3g";

	private final AdapayMerchantEntryInfoService adapayMerchantEntryInfoService;
	private final AdapayMerchantResidentInfoService adapayMerchantResidentInfoService;
	private final AdapaySubmitLicenseInfoService adapaySubmitLicenseInfoService;
	private final AdapayMerchantEntryMapper adapayMerchantEntryMapper;
	private final AdapayMerchantResidentMapper adapayMerchantResidentMapper;

	@Value("${ecshopx.common.demo-company-ids:}")
	private String demoCompanyIdsRaw;

	public AdapayOpenAccountStepService(
			AdapayMerchantEntryInfoService adapayMerchantEntryInfoService,
			AdapayMerchantResidentInfoService adapayMerchantResidentInfoService,
			AdapaySubmitLicenseInfoService adapaySubmitLicenseInfoService,
			AdapayMerchantEntryMapper adapayMerchantEntryMapper,
			AdapayMerchantResidentMapper adapayMerchantResidentMapper) {
		this.adapayMerchantEntryInfoService = adapayMerchantEntryInfoService;
		this.adapayMerchantResidentInfoService = adapayMerchantResidentInfoService;
		this.adapaySubmitLicenseInfoService = adapaySubmitLicenseInfoService;
		this.adapayMerchantEntryMapper = adapayMerchantEntryMapper;
		this.adapayMerchantResidentMapper = adapayMerchantResidentMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> openAccountStep(long companyId, HttpServletRequest request) {
		Object rawEntry = adapayMerchantEntryInfoService.merchantEntryInfo(companyId);
		Object rawResident = adapayMerchantResidentInfoService.merchantResidentInfo(companyId);
		Object rawSubmit = adapaySubmitLicenseInfoService.submitLicenseInfo(companyId);

		Map<String, Object> me = asInfoMap(normalizeInfoPayload(rawEntry));
		Map<String, Object> mr = asInfoMap(normalizeInfoPayload(rawResident));
		Map<String, Object> sl = asInfoMap(normalizeInfoPayload(rawSubmit));

		Map<String, Object> info = new LinkedHashMap<>();
		info.put("MerchantEntry", me);
		info.put("MerchantResident", mr);
		info.put("SubmitLicense", sl == null ? List.of() : sl);

		int step = computeStepWithTimeout(companyId, me, mr, sl);

		boolean demo = isDemoCompany(companyId);
		boolean datapassBlock = resolveDatapassBlock(request) || demo;

		if (datapassBlock && info.get("MerchantEntry") != null) {
			applyOpenAccountStepMasking(me, sl);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("step", Integer.valueOf(step));
		out.put("info", info);
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public int computeOpenStepForIsOpen(long companyId) {
		Object rawEntry = adapayMerchantEntryInfoService.merchantEntryInfo(companyId);
		Object rawResident = adapayMerchantResidentInfoService.merchantResidentInfo(companyId);
		Object rawSubmit = adapaySubmitLicenseInfoService.submitLicenseInfo(companyId);

		Map<String, Object> me = asInfoMap(normalizeInfoPayload(rawEntry));
		Map<String, Object> mr = asInfoMap(normalizeInfoPayload(rawResident));
		Map<String, Object> sl = asInfoMap(normalizeInfoPayload(rawSubmit));

		return computeStepWithTimeout(companyId, me, mr, sl);
	}

	private int computeStepWithTimeout(
			long companyId, Map<String, Object> me, Map<String, Object> mr, Map<String, Object> sl) {
		int step = 1;
		if (me != null && "succeeded".equals(String.valueOf(me.get("status")))) {
			step++;
			if (mr != null && "succeeded".equals(String.valueOf(mr.get("status")))) {
				step++;
				if (sl != null && "P".equals(String.valueOf(sl.get("audit_status")))) {
					step++;
				}
			}
		}

		if (step == 3) {
			if (mr == null) {
				return step;
			}
			int residentUt = parseResidentUpdateTimeSec(mr.get("update_time"));
			long expireTime = (long) residentUt + 60L * 60 * 24 * 5;
			long nowSec = Instant.now().getEpochSecond();
			String audit = "W";
			if (sl != null && sl.get("audit_status") != null) {
				audit = String.valueOf(sl.get("audit_status"));
			}
			if (expireTime < nowSec && !"I".equals(audit)) {
				step = 1;
				LambdaUpdateWrapper<AdapayMerchantEntry> w1 =
						new LambdaUpdateWrapper<AdapayMerchantEntry>()
								.eq(AdapayMerchantEntry::getCompanyId, companyId)
								.set(AdapayMerchantEntry::getStatus, "failed")
								.set(
										AdapayMerchantEntry::getErrorMsg,
										"入驻成功后5个工作日内没有提交证照");
				adapayMerchantEntryMapper.update(null, w1);

				LambdaUpdateWrapper<AdapayMerchantResident> w2 =
						new LambdaUpdateWrapper<AdapayMerchantResident>()
								.eq(AdapayMerchantResident::getCompanyId, companyId)
								.set(AdapayMerchantResident::getStatus, "failed");
				adapayMerchantResidentMapper.update(null, w2);
			}
		}

		return step;
	}

	private static int parseResidentUpdateTimeSec(Object ut) {
		if (ut instanceof Number n) {
			return n.intValue();
		}
		if (ut instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0;
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private void applyOpenAccountStepMasking(Map<String, Object> me, Map<String, Object> sl) {
		maskIfPresent(me, "cont_name", DataMasking::maskTruename);
		maskIfPresent(me, "cust_tel", DataMasking::maskMobile);
		maskIfPresent(me, "legal_name", DataMasking::maskTruename);
		maskIfPresent(me, "legal_mp", DataMasking::maskMobile);
		maskIfPresent(me, "cont_phone", DataMasking::maskMobile);
		maskIfPresent(me, "legal_idno", DataMasking::maskIdcard);
		maskIfPresent(me, "usr_phone", DataMasking::maskMobile);
		maskIfPresent(me, "card_id_mask", DataMasking::maskBankcard);
		maskIfPresent(me, "card_name", DataMasking::maskTruename);
		if (me.containsKey("cert_name")) {
			me.put("cert_name", DataMasking.maskTruename(String.valueOf(me.get("cert_name"))));
		}
		if (me.containsKey("cert_id")) {
			me.put("cert_id", DataMasking.maskIdcard(String.valueOf(me.get("cert_id"))));
		}

		if (sl != null) {
			for (String k :
					List.of(
							"legal_certId_front_url",
							"legal_cert_id_back_url",
							"cert_front_image_url",
							"cert_back_image_url",
							"account_opening_permit_url")) {
				if (sl.containsKey(k)) {
					sl.put(k, PLACEHOLDER_IMAGE_URL);
				}
			}
		}
	}

	@FunctionalInterface
	private interface StringMasker {
		String mask(String raw);
	}

	private static void maskIfPresent(Map<String, Object> me, String key, StringMasker masker) {
		if (!me.containsKey(key)) {
			return;
		}
		me.put(key, masker.mask(String.valueOf(me.get(key))));
	}

	private boolean isDemoCompany(long companyId) {
		if (demoCompanyIdsRaw == null || demoCompanyIdsRaw.isBlank()) {
			return false;
		}
		Set<Long> ids = new HashSet<>();
		for (String p : demoCompanyIdsRaw.split(",")) {
			if (p == null) {
				continue;
			}
			String t = p.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				ids.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// ignore invalid token
			}
		}
		return ids.contains(companyId);
	}

	private static boolean resolveDatapassBlock(HttpServletRequest request) {
		if (truthyDatapassToken(request.getAttribute("x-datapass-block"))) {
			return true;
		}
		return truthyDatapassToken(request.getParameter("x-datapass-block"));
	}

	private static boolean truthyDatapassToken(Object token) {
		if (token == null) {
			return false;
		}
		if (token instanceof Number n) {
			return n.intValue() != 0;
		}
		if (Boolean.TRUE.equals(token)) {
			return true;
		}
		String t = token.toString().trim();
		if (t.isEmpty() || "0".equals(t) || "false".equalsIgnoreCase(t)) {
			return false;
		}
		return true;
	}

	private static Object normalizeInfoPayload(Object o) {
		if (o instanceof List<?> list && list.isEmpty()) {
			return null;
		}
		return o;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asInfoMap(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		throw new IllegalStateException("unexpected info payload type: " + o.getClass().getName());
	}
}
