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

package cn.shopex.ecshopx.bootstrap.distribution;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.adapay.service.AdapayDistributorMemberInfoService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.OperatorDealerDetailReadService;
import cn.shopex.ecshopx.distribution.service.DistributorAftersalesAddressReadService;
import cn.shopex.ecshopx.distribution.service.DistributorInfoQueryHandler;
import cn.shopex.ecshopx.distribution.service.DistributorInfoResolveService;
import cn.shopex.ecshopx.distribution.service.LocalDeliveryDistributorInfoService;
import cn.shopex.ecshopx.distribution.service.OrderValidityPlatformSettingReadService;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.service.MerchantQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorInfoQueryHandlerImpl implements DistributorInfoQueryHandler {

	private final DistributorInfoResolveService distributorInfoResolveService;
	private final LocalDeliveryDistributorInfoService localDeliveryDistributorInfoService;
	private final AdapayDistributorMemberInfoService adapayDistributorMemberInfoService;
	private final OperatorDealerDetailReadService operatorDealerDetailReadService;
	private final MerchantQueryService merchantQueryService;
	private final DistributorAftersalesAddressReadService distributorAftersalesAddressReadService;
	private final OrderValidityPlatformSettingReadService orderValidityPlatformSettingReadService;
	private final LangueProperties langueProperties;

	@Value("${common.qqmap-key:}")
	private String qqmapKey;

	public DistributorInfoQueryHandlerImpl(
			DistributorInfoResolveService distributorInfoResolveService,
			LocalDeliveryDistributorInfoService localDeliveryDistributorInfoService,
			AdapayDistributorMemberInfoService adapayDistributorMemberInfoService,
			OperatorDealerDetailReadService operatorDealerDetailReadService,
			MerchantQueryService merchantQueryService,
			DistributorAftersalesAddressReadService distributorAftersalesAddressReadService,
			OrderValidityPlatformSettingReadService orderValidityPlatformSettingReadService,
				LangueProperties langueProperties) {
		this.distributorInfoResolveService = distributorInfoResolveService;
		this.localDeliveryDistributorInfoService = localDeliveryDistributorInfoService;
		this.adapayDistributorMemberInfoService = adapayDistributorMemberInfoService;
		this.operatorDealerDetailReadService = operatorDealerDetailReadService;
		this.merchantQueryService = merchantQueryService;
		this.distributorAftersalesAddressReadService = distributorAftersalesAddressReadService;
		this.orderValidityPlatformSettingReadService = orderValidityPlatformSettingReadService;
		this.langueProperties = langueProperties;
	}

	@Override
	public Map<String, Object> handle(
			HttpServletRequest request, Map<String, Object> operatorJwt, Map<String, Object> mergedInput) {
		long companyId = longOf(operatorJwt.get("company_id"));
		String requestLang = RequestLangTag.current(langueProperties);
		long requestedDistributorId = longOf(mergedInput.get("distributor_id"));
		Map<String, Object> result =
				distributorInfoResolveService
						.resolveStoreDetail(companyId, requestedDistributorId, requestLang)
						.orElseThrow(() -> new ResourceException("请选择店铺"));
		Map<String, Object> row = new LinkedHashMap<>(result);

		row.put("business_list", localDeliveryDistributorInfoService.businessList());
		row.put("is_local_delivery", localDeliveryDistributorInfoService.isLocalDeliveryOpen(companyId));

		Object ra = row.get("regionauth_id");
		if (isUnsetRegionauthId(ra)) {
			row.put("regionauth_id", "");
		}

		Map<String, Object> adapayMemberInfo =
				adapayDistributorMemberInfoService.buildMemberInfoSection(
						companyId, requestedDistributorId, "distributor");
		row.put("is_openAccount", resolveLegacyOpenAccountFlag(adapayMemberInfo));
		row.put("adapayMemberInfo", adapayMemberInfo);

		row.put("qqmapimg", buildQqMapImgUrl(row.get("lat"), row.get("lng")));

		applyDealerSection(row, companyId);

		int datapassToken = readDatapassToken(request);
		row.put("datapass_block", datapassToken);
		boolean datapassBlock = datapassToken != 0;
		if (datapassBlock) {
			applyDatapassMask(row, adapayMemberInfo);
		}
		row.put("datapass_block", datapassBlock ? 1 : 0);

		row.put("merchant_name", "");
		long merchantId = longOf(row.get("merchant_id"));
		if (merchantId > 0) {
			Merchant m = merchantQueryService.getInfo(companyId, merchantId, false);
			if (m != null && m.getMerchantName() != null) {
				row.put("merchant_name", m.getMerchantName());
			}
		}

		long effectiveDistributorId = longOf(row.get("distributor_id"));
		Object offlineAftersalesAddress =
				distributorAftersalesAddressReadService.readOfflineAftersalesAddress(effectiveDistributorId);
		row.put(
				"offline_aftersales_address",
				offlineAftersalesAddress != null ? offlineAftersalesAddress : List.of());

		Map<String, Object> platformSetting =
				orderValidityPlatformSettingReadService.readOrderValidityPlatformSetting(companyId);
		row.put("platform_setting", platformSetting);
		Object irf = platformSetting.get("is_refund_freight");
		if (irf == null || intValue(irf) == 0) {
			row.put("is_refund_freight", 0);
		}

		return row;
	}

	private void applyDealerSection(Map<String, Object> row, long companyId) {
		Object dealerRaw = row.get("dealer_id");
		long dealerId = longOf(dealerRaw);
		if (dealerId > 0) {
			Map<String, Object> staff = operatorDealerDetailReadService.buildDealerSection(dealerId, companyId);
			if (!staff.isEmpty()) {
				Map<String, Object> dealer = new LinkedHashMap<>();
				dealer.put("operator_id", staff.get("operator_id"));
				dealer.put("username", staff.get("username"));
				row.put("dealer", dealer);
				row.put("is_rel_dealer", Boolean.TRUE);
				return;
			}
		}
		row.put("is_rel_dealer", Boolean.FALSE);
	}

	private void applyDatapassMask(Map<String, Object> row, Map<String, Object> adapayMemberInfo) {
		Object mob = row.get("mobile");
		if (mob != null) {
			row.put("mobile", DataMasking.maskMobile(mob.toString()));
		}
		Object ct = row.get("contact");
		if (ct != null) {
			row.put("contact", DataMasking.maskTruename(ct.toString()));
		}
		Map<String, Object> target = adapayMemberInfo;
		putMask(target, "user_name", DataMasking.maskTruename(str(target.get("user_name"))));
		putMask(target, "tel_no", DataMasking.maskMobile(str(target.get("tel_no"))));
		putMask(target, "cert_id", DataMasking.maskIdcard(str(target.get("cert_id"))));
		putMask(target, "bank_card_name", DataMasking.maskTruename(str(target.get("bank_card_name"))));
		putMask(target, "bank_tel_no", DataMasking.maskMobile(str(target.get("bank_tel_no"))));
		putMask(target, "bank_card_id", DataMasking.maskIdcard(str(target.get("bank_card_id"))));
		putMask(target, "bank_cert_id", DataMasking.maskIdcard(str(target.get("bank_cert_id"))));
		if ("corp".equals(String.valueOf(target.get("member_type")))) {
			putMask(target, "legal_person", DataMasking.maskTruename(str(target.get("legal_person"))));
			putMask(target, "legal_cert_id", DataMasking.maskIdcard(str(target.get("legal_cert_id"))));
			putMask(target, "card_no", DataMasking.maskIdcard(str(target.get("card_no"))));
			putMask(target, "legal_mp", DataMasking.maskMobile(str(target.get("legal_mp"))));
			putMask(target, "tel_no", DataMasking.maskMobile(str(target.get("tel_no"))));
		}
	}

	private static void putMask(Map<String, Object> m, String k, String v) {
		if (v != null) {
			m.put(k, v);
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private String buildQqMapImgUrl(Object latObj, Object lngObj) {
		String lat = latObj != null && StringUtils.hasText(latObj.toString()) ? latObj.toString().trim() : "39.908739";
		String lng = lngObj != null && StringUtils.hasText(lngObj.toString()) ? lngObj.toString().trim() : "116.397513";
		String latlng = lat + "," + lng;
		String key = qqmapKey != null ? qqmapKey : "";
		return "http://apis.map.qq.com/ws/staticmap/v2/?"
				+ "key="
				+ key
				+ "&size=500x249"
				+ "&zoom=16"
				+ "&center="
				+ latlng
				+ "&markers=color:blue|label:A|"
				+ latlng;
	}

	private static int readDatapassToken(HttpServletRequest request) {
		if (DatapassBlockResolver.isBlocked(request)) {
			return 1;
		}
		return 0;
	}

	private static long longOf(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intValue(Object v) {
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = v != null ? v.toString().trim() : "";
		if ("true".equalsIgnoreCase(s)) {
			return 1;
		}
		if (s.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Missing or null {@code audit_state} matches legacy coercion to numeric zero before comparing to
	 * success code {@code C}; booleans and numbers follow the same loose comparison rules as the prior API.
	 */
	private static boolean resolveLegacyOpenAccountFlag(Map<String, Object> adapayMemberInfo) {
		if (!adapayMemberInfo.containsKey("audit_state")) {
			return true;
		}
		Object auditStateRaw = adapayMemberInfo.get("audit_state");
		if (auditStateRaw == null) {
			return true;
		}
		if (auditStateRaw instanceof Boolean b) {
			return !b;
		}
		if (auditStateRaw instanceof Number n) {
			double d = n.doubleValue();
			return !Double.isNaN(d) && d == 0.0;
		}
		return "C".equals(auditStateRaw.toString());
	}

	/**
	 * 旧系统对未设置的 {@code regionauth_id} 返回 {@code ""}；Java 侧将 null、空白字符串、
	 * numeric zero ({@code 0}/{@code 0L}, {@link BigDecimal#ZERO}, etc.), and {@code "0"} as empty.
	 */
	private static boolean isUnsetRegionauthId(Object ra) {
		if (ra == null) {
			return true;
		}
		if (ra instanceof BigDecimal bd) {
			return bd.signum() == 0;
		}
		if (ra instanceof Number n) {
			if (n instanceof Double d) {
				return d.doubleValue() == 0.0;
			}
			if (n instanceof Float f) {
				return f.floatValue() == 0.0f;
			}
			return n.longValue() == 0L;
		}
		String s = ra.toString().trim();
		return !StringUtils.hasText(s) || "0".equals(s);
	}

}
