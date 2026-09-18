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

package cn.shopex.ecshopx.payment.service.front;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.payment.FrontDepositPayEnabledPort;
import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingBooleanParsing;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingInputResolver;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingReader;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontPaymentSettingReadService {

	private static final String PT_OFFLINE = "offline_pay";

	private final StringRedisTemplate companysRedisTemplate;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final PaymentSettingInputResolver paymentSettingInputResolver;
	private final PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort;
	private final DoumenIntlPaymentSettingReader doumenIntlPaymentSettingReader;
	private final FrontDepositPayEnabledPort frontDepositPayEnabledPort;

	@Value("${common.oem-shuyun:false}")
	private boolean oemShuyun;

	public FrontPaymentSettingReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper,
			PointMemberRuleReadService pointMemberRuleReadService,
			PaymentSettingInputResolver paymentSettingInputResolver,
			PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort,
			DoumenIntlPaymentSettingReader doumenIntlPaymentSettingReader,
			FrontDepositPayEnabledPort frontDepositPayEnabledPort) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.paymentSettingInputResolver = paymentSettingInputResolver;
		this.paymentSubjectDistributorIdPort = paymentSubjectDistributorIdPort;
		this.doumenIntlPaymentSettingReader = doumenIntlPaymentSettingReader;
		this.frontDepositPayEnabledPort = frontDepositPayEnabledPort;
	}

	public Object getPaymentSetting(long companyId, String payType, String countryCodeForLang) {
		String normalized = payType == null ? "" : payType;
		if (!PT_OFFLINE.equals(normalized)) {
			throw new BadRequestException("暂时不支持 " + normalized);
		}

		String raw = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.offlinePaySettingKey(companyId, countryCodeForLang));
		Map<String, Object> data = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		String nameOverride = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.offlinePayNameLangKey(companyId, countryCodeForLang));
		if (StringUtils.hasText(nameOverride)) {
			data.put("pay_name", nameOverride);
		}
		return emptyPaymentSettingBodyOrArray(data);
	}

	/**
	 * 前台支付方式列表（按 platform 分支与 Redis 配置）。
	 */
	public List<Map<String, Object>> getPaymentSettingList(
			HttpServletRequest request,
			long companyId,
			long distributorId,
			String platform,
			String orderType,
			String countryCodeNormalized) {
		String lang = normalizeMagicLang(countryCodeNormalized);
		boolean en = "en-CN".equals(lang);

		String rawAda = sharedStringRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.adapaySettingKey(companyId));
		Map<String, Object> ada = PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawAda);
		boolean adaOpen = PaymentSettingBooleanParsing.looseTrueString(ada.get("is_open"));
		LinkedHashSet<String> adaChannels = new LinkedHashSet<>();
		if (adaOpen) {
			fillAdaBsPayChannelTokens(adaChannels, ada.get("pay_channel"), objectMapper);
			addAdaFlagChannels(ada, adaChannels);
		}

		String rawBs = companysRedisTemplate.opsForValue().get(PaymentSettingRedisKeys.bspaySettingKey(companyId));
		Map<String, Object> bs = PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawBs);
		boolean bsOpen = PaymentSettingBooleanParsing.looseTrueString(bs.get("is_open"));
		LinkedHashSet<String> bsChannels = new LinkedHashSet<>();
		if (bsOpen) {
			fillAdaBsPayChannelTokens(bsChannels, bs.get("pay_channel"), objectMapper);
			addBsFlagChannels(bs, bsChannels);
		}

		List<Map<String, Object>> result = new ArrayList<>();

		if (paypalPlatformAllowed(platform)) {
			Map<String, Object> pp = loadPaypalSetting(companyId, distributorId);
			boolean hasPaypal =
					!pp.isEmpty() && PaymentSettingBooleanParsing.strictTrueString(pp.get("is_open"));
			if (hasPaypal) {
				Map<String, Object> paypalRow = new LinkedHashMap<>();
				paypalRow.put("pay_type_code", "paypal");
				paypalRow.put("pay_type_name", "PayPal");
				result.add(paypalRow);
			}
			if (doumenIntlPaymentSettingReader.isConfigured(companyId)) {
				Map<String, Object> doumenRow = new LinkedHashMap<>();
				doumenRow.put("pay_type_code", "doumen_intl");
				doumenRow.put("pay_type_name", "斗门国际");
				result.add(doumenRow);
			}
		}

		String rawOff = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.offlinePaySettingKey(companyId, lang));
		Map<String, Object> offlineMap = PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawOff);

		String p = platform == null ? "" : platform;
		switch (p) {
			case "alipaymini" -> appendAlipayMini(result, companyId, distributorId, lang, en);
			case "wxPlatform" -> appendWxPlatform(result, companyId, distributorId, adaOpen, adaChannels, bsOpen, bsChannels, en);
			case "h5" -> appendH5(result, companyId, distributorId, offlineMap, adaOpen, adaChannels, bsOpen, bsChannels, en, lang);
			case "app" -> appendApp(result, companyId, distributorId, offlineMap, adaOpen, adaChannels, bsOpen, bsChannels, en);
			case "pc" -> appendPc(result, companyId, distributorId, offlineMap, adaOpen, adaChannels, bsOpen, bsChannels, en);
			default -> appendDefaultBranch(
					request, result, companyId, distributorId, offlineMap, adaOpen, adaChannels, bsOpen, bsChannels, en, lang);
		}

		if (!"normal_employee_purchase".equals(orderType)
				&& frontDepositPayEnabledPort.isDepositPayEnabled(companyId)) {
			Map<String, Object> depositRow = new LinkedHashMap<>();
			depositRow.put("pay_type_code", "deposit");
			depositRow.put("pay_type_name", en ? "Balance Payment" : "余额支付");
			result.add(depositRow);
		}

		if (oemShuyun && pointMemberRuleReadService.getIsOpenPoint(companyId)) {
			Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId, lang);
			Map<String, Object> pointRow = new LinkedHashMap<>();
			pointRow.put("pay_type_code", "point");
			pointRow.put("pay_type_name", en ? "Points Payment" : "积分支付");
			if (pointPayFirst(rule)) {
				result.add(0, pointRow);
			} else {
				result.add(pointRow);
			}
		}

		return result;
	}

	private Map<String, Object> loadPaypalSetting(long companyId, long distributorId) {
		String raw1 = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.paypalRedisKey(companyId, distributorId));
		Map<String, Object> pp = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw1);
		if (pp.isEmpty() && distributorId > 0L) {
			String raw0 = companysRedisTemplate
					.opsForValue()
					.get(PaymentSettingRedisKeys.paypalRedisKey(companyId, 0L));
			pp = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw0);
		}
		return pp;
	}

	private void appendAlipayMini(
			List<Map<String, Object>> result, long companyId, long distributorId, String lang, boolean en) {
		long actualDistributorId =
				paymentSubjectDistributorIdPort.resolveActualDistributorId(companyId, distributorId);
		String rawAli = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.alipayRedisKey(companyId, actualDistributorId));
		Map<String, Object> ali = PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawAli);
		if (!ali.isEmpty() && PaymentSettingBooleanParsing.strictTrueString(ali.get("is_open"))) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "alipaymini");
			row.put("pay_type_name", en ? "Alipay" : "支付宝");
			result.add(row);
		}
	}

	private void appendWxPlatform(
			List<Map<String, Object>> result,
			long companyId,
			long distributorId,
			boolean adaOpen,
			LinkedHashSet<String> adaChannels,
			boolean bsOpen,
			LinkedHashSet<String> bsChannels,
			boolean en) {
		String wxName = en ? "WeChat Pay" : "微信支付";
		if (adaOrBs(adaOpen, adaChannels, bsOpen, bsChannels, "wx_pub")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", adaHas(adaOpen, adaChannels, "wx_pub") ? "adapay" : "bspay");
			row.put("pay_channel", "wx_pub");
			row.put("pay_type_name", wxName);
			result.add(row);
			return;
		}
		Map<String, Object> wx = readWxpayMap(companyId, distributorId);
		if (!wx.isEmpty() && "true".equals(String.valueOf(wx.get("is_open")).trim())) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "wxpayjs");
			row.put("pay_type_name", wxName);
			result.add(row);
		}
	}

	private void appendH5(
			List<Map<String, Object>> result,
			long companyId,
			long distributorId,
			Map<String, Object> offlineMap,
			boolean adaOpen,
			LinkedHashSet<String> adaChannels,
			boolean bsOpen,
			LinkedHashSet<String> bsChannels,
			boolean en,
			String lang) {
		String wxName = en ? "WeChat Pay" : "微信支付";
		String aliName = en ? "Alipay" : "支付宝";
		if (adaOrBs(adaOpen, adaChannels, bsOpen, bsChannels, "wx_lite")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", adaHas(adaOpen, adaChannels, "wx_lite") ? "adapay" : "bspay");
			row.put("pay_channel", "wx_lite");
			row.put("pay_type_name", wxName);
			result.add(row);
		} else {
			Map<String, Object> wxh = readWxpayMap(companyId, distributorId);
			if (!wxh.isEmpty() && "true".equals(String.valueOf(wxh.get("is_open")).trim())) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("pay_type_code", "wxpayh5");
				row.put("pay_type_name", wxName);
				result.add(row);
			}
		}
		if (adaOrBs(adaOpen, adaChannels, bsOpen, bsChannels, "alipay_wap")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", adaHas(adaOpen, adaChannels, "alipay_wap") ? "adapay" : "bspay");
			row.put("pay_channel", "alipay_wap");
			row.put("pay_type_name", aliName);
			result.add(row);
		} else {
			Map<String, Object> alih = readAlipayMap(companyId, distributorId);
			if (!alih.isEmpty() && "true".equals(String.valueOf(alih.get("is_open")).trim())) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("pay_type_code", "alipayh5");
				row.put("pay_type_name", aliName);
				result.add(row);
			}
		}
		maybeAppendOfflineH5AppPc(result, "h5", offlineMap, lang, companyId);
	}

	private void appendApp(
			List<Map<String, Object>> result,
			long companyId,
			long distributorId,
			Map<String, Object> offlineMap,
			boolean adaOpen,
			LinkedHashSet<String> adaChannels,
			boolean bsOpen,
			LinkedHashSet<String> bsChannels,
			boolean en) {
		String wxName = en ? "WeChat Pay" : "微信支付";
		String aliName = en ? "Alipay" : "支付宝";
		if (adaOrBs(adaOpen, adaChannels, bsOpen, bsChannels, "wx_lite")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", adaHas(adaOpen, adaChannels, "wx_lite") ? "adapay" : "bspay");
			row.put("pay_channel", "wx_lite");
			row.put("pay_type_name", wxName);
			result.add(row);
		} else {
			Map<String, Object> wxApp = readWxpayMap(companyId, distributorId);
			if (!wxApp.isEmpty() && "true".equals(String.valueOf(wxApp.get("is_open")).trim())) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("pay_type_code", "wxpayapp");
				row.put("pay_type_name", wxName);
				result.add(row);
			}
		}
		if (adaHas(adaOpen, adaChannels, "alipay")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "adapay");
			row.put("pay_channel", "alipay");
			row.put("pay_type_name", aliName);
			result.add(row);
		} else {
			Map<String, Object> aliApp = readAlipayMap(companyId, distributorId);
			if (!aliApp.isEmpty() && "true".equals(String.valueOf(aliApp.get("is_open")).trim())) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("pay_type_code", "alipayapp");
				row.put("pay_type_name", aliName);
				result.add(row);
			}
		}
		maybeAppendOfflineH5AppPc(result, "app", offlineMap, null, companyId);
	}

	private void appendPc(
			List<Map<String, Object>> result,
			long companyId,
			long distributorId,
			Map<String, Object> offlineMap,
			boolean adaOpen,
			LinkedHashSet<String> adaChannels,
			boolean bsOpen,
			LinkedHashSet<String> bsChannels,
			boolean en) {
		String wxName = en ? "WeChat Pay" : "微信支付";
		String aliName = en ? "Alipay" : "支付宝";
		if (adaHas(adaOpen, adaChannels, "wx_pub")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "adapay");
			row.put("pay_channel", "wx_qr");
			row.put("pay_type_name", wxName);
			result.add(row);
		} else if (bsHas(bsOpen, bsChannels, "wx_qr")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "bspay");
			row.put("pay_channel", "wx_qr");
			row.put("pay_type_name", wxName);
			result.add(row);
		} else {
			Map<String, Object> wxPc = readWxpayMap(companyId, distributorId);
			if (!wxPc.isEmpty() && "true".equals(String.valueOf(wxPc.get("is_open")).trim())) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("pay_type_code", "wxpaypc");
				row.put("pay_type_name", wxName);
				result.add(row);
			}
		}
		if (adaHas(adaOpen, adaChannels, "alipay_qr")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "adapay");
			row.put("pay_channel", "alipay_qr");
			row.put("pay_type_name", aliName);
			result.add(row);
		} else if (bsHas(bsOpen, bsChannels, "alipay_qr")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "bspay");
			row.put("pay_channel", "alipay_qr");
			row.put("pay_type_name", aliName);
			result.add(row);
		} else {
			Map<String, Object> aliPc = readAlipayMap(companyId, distributorId);
			if (!aliPc.isEmpty() && "true".equals(String.valueOf(aliPc.get("is_open")).trim())) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("pay_type_code", "alipay");
				row.put("pay_type_name", aliName);
				result.add(row);
			}
		}
		maybeAppendOfflineH5AppPc(result, "pc", offlineMap, null, companyId);
	}

	private void appendDefaultBranch(
			HttpServletRequest request,
			List<Map<String, Object>> result,
			long companyId,
			long distributorId,
			Map<String, Object> offlineMap,
			boolean adaOpen,
			LinkedHashSet<String> adaChannels,
			boolean bsOpen,
			LinkedHashSet<String> bsChannels,
			boolean en,
			String lang) {
		String wxName = en ? "WeChat Pay" : "微信支付";
		if (adaOpen && !adaChannels.isEmpty() && adaChannels.contains("wx_lite")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "adapay");
			row.put("pay_channel", "wx_lite");
			row.put("pay_type_name", wxName);
			result.add(row);
		} else if (bsOpen && !bsChannels.isEmpty() && bsChannels.contains("wx_lite")) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "bspay");
			row.put("pay_channel", "wx_lite");
			row.put("pay_type_name", wxName);
			result.add(row);
		} else {
			Map<String, Object> wxDef = readWxpayMap(companyId, distributorId);
			if (!wxDef.isEmpty() && "true".equals(String.valueOf(wxDef.get("is_open")).trim())) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("pay_type_code", "wxpay");
				row.put("pay_type_name", wxName);
				result.add(row);
			}
		}
		String subKey = paymentSettingInputResolver.resolveChinaumsSubKey(request, distributorId);
		String umsRaw = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.chinaumsPaymentSettingKey(companyId, subKey));
		Map<String, Object> ums = PaymentConfigJsonSupport.parseObjectMap(objectMapper, umsRaw);
		if (!ums.isEmpty() && PaymentSettingBooleanParsing.looseTrueString(ums.get("is_open"))) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pay_type_code", "chinaums");
			row.put("pay_type_name", en ? "WeChat Pay-UnionPay" : "微信支付-银联");
			result.add(row);
		}
		maybeAppendOfflineDefault(result, offlineMap);
	}

	private Map<String, Object> readWxpayMap(long companyId, long distributorId) {
		long actualDistributorId =
				paymentSubjectDistributorIdPort.resolveActualDistributorId(companyId, distributorId);
		String raw = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.wxpayRedisKey(companyId, actualDistributorId));
		return PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
	}

	private Map<String, Object> readAlipayMap(long companyId, long distributorId) {
		long actualDistributorId =
				paymentSubjectDistributorIdPort.resolveActualDistributorId(companyId, distributorId);
		String raw = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.alipayRedisKey(companyId, actualDistributorId));
		return PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
	}

	private void maybeAppendOfflineH5AppPc(
			List<Map<String, Object>> result,
			String platform,
			Map<String, Object> offlineMap,
			String lang,
			long companyId) {
		if (offlineMap.isEmpty() || !PaymentSettingBooleanParsing.looseTrueString(offlineMap.get("is_open"))) {
			return;
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("pay_type_code", "offline_pay");
		String payTypeName = String.valueOf(offlineMap.getOrDefault("pay_name", "")).trim();
		if ("h5".equals(platform) && lang != null) {
			String override = companysRedisTemplate
					.opsForValue()
					.get(PaymentSettingRedisKeys.offlinePayNameLangKey(companyId, lang));
			if (StringUtils.hasText(override != null ? override.trim() : "")) {
				payTypeName = override.trim();
			}
		}
		row.put("pay_type_name", payTypeName);
		result.add(row);
	}

	private void maybeAppendOfflineDefault(List<Map<String, Object>> result, Map<String, Object> offlineMap) {
		if (offlineMap.isEmpty() || !PaymentSettingBooleanParsing.strictTrueString(offlineMap.get("is_open"))) {
			return;
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("pay_type_code", "offline_pay");
		row.put("pay_type_name", String.valueOf(offlineMap.getOrDefault("pay_name", "")).trim());
		result.add(row);
	}

	private static boolean paypalPlatformAllowed(String platform) {
		String p = platform == null ? "" : platform;
		return "international".equals(p) || "h5".equals(p) || "pc".equals(p);
	}

	private static String normalizeMagicLang(String countryCode) {
		String s = countryCode == null ? "" : countryCode.trim();
		if (s.isEmpty() || "0".equals(s)) {
			return "zh-CN";
		}
		return s;
	}

	private static boolean adaHas(boolean adaOpen, LinkedHashSet<String> adaChannels, String token) {
		return adaOpen && adaChannels != null && adaChannels.contains(token);
	}

	private static boolean bsHas(boolean bsOpen, LinkedHashSet<String> bsChannels, String token) {
		return bsOpen && bsChannels != null && bsChannels.contains(token);
	}

	private static boolean adaOrBs(
			boolean adaOpen,
			LinkedHashSet<String> adaChannels,
			boolean bsOpen,
			LinkedHashSet<String> bsChannels,
			String token) {
		return adaHas(adaOpen, adaChannels, token) || bsHas(bsOpen, bsChannels, token);
	}

	private static void fillAdaBsPayChannelTokens(
			LinkedHashSet<String> target, Object payChannelField, ObjectMapper om) {
		if (payChannelField instanceof Collection<?> c) {
			for (Object e : c) {
				String t = String.valueOf(e).trim();
				if (StringUtils.hasText(t)) {
					target.add(t);
				}
			}
		} else if (payChannelField instanceof String s) {
			String trimmed = s.trim();
			if (!StringUtils.hasText(trimmed)) {
				return;
			}
			try {
				List<?> list = om.readValue(trimmed, List.class);
				if (list != null) {
					for (Object e : list) {
						String t = String.valueOf(e).trim();
						if (StringUtils.hasText(t)) {
							target.add(t);
						}
					}
				}
			} catch (Exception e) {
				for (String part : trimmed.split(",")) {
					String t = part.trim();
					if (StringUtils.hasText(t)) {
						target.add(t);
					}
				}
			}
		}
	}

	private static void addAdaFlagChannels(Map<String, Object> ada, LinkedHashSet<String> channels) {
		if (anyKeyOpen(ada, "wx_pub_online", "wx_pub_offline")) {
			channels.add("wx_pub");
		}
		if (anyKeyOpen(ada, "wx_lite_online", "wx_lite_offline")) {
			channels.add("wx_lite");
		}
		if (PaymentConfigJsonSupport.normalizeIsOpen(ada.get("wx_scan"))) {
			channels.add("wx_scan");
		}
		if (anyKeyOpen(ada, "alipay_qr_online", "alipay_qr_offline")) {
			channels.add("alipay_qr");
		}
		if (PaymentConfigJsonSupport.normalizeIsOpen(ada.get("alipay_scan"))) {
			channels.add("alipay_scan");
		}
		if (anyKeyOpen(ada, "alipay_lite_online", "alipay_lite_offline")) {
			channels.add("alipay_lite");
		}
		if (PaymentConfigJsonSupport.normalizeIsOpen(ada.get("alipay_call"))) {
			channels.add("alipay");
		}
	}

	private static void addBsFlagChannels(Map<String, Object> bs, LinkedHashSet<String> channels) {
		if (PaymentConfigJsonSupport.normalizeIsOpen(bs.get("wx_lite_online"))) {
			channels.add("wx_lite");
		}
		if (PaymentConfigJsonSupport.normalizeIsOpen(bs.get("wx_pub_online"))) {
			channels.add("wx_pub");
		}
		if (PaymentConfigJsonSupport.normalizeIsOpen(bs.get("wx_qr_online"))) {
			channels.add("wx_qr");
		}
		if (PaymentConfigJsonSupport.normalizeIsOpen(bs.get("alipay_call"))) {
			channels.add("alipay_wap");
		}
		if (PaymentConfigJsonSupport.normalizeIsOpen(bs.get("alipay_qr_online"))) {
			channels.add("alipay_qr");
		}
	}

	private static boolean anyKeyOpen(Map<String, Object> map, String k1, String k2) {
		return PaymentConfigJsonSupport.normalizeIsOpen(map.get(k1))
				|| PaymentConfigJsonSupport.normalizeIsOpen(map.get(k2));
	}

	private static boolean pointPayFirst(Map<String, Object> rule) {
		Object v = rule.get("point_pay_first");
		if (v instanceof Number n && n.longValue() != 0L) {
			return true;
		}
		if (v instanceof Boolean b && b) {
			return true;
		}
		String vs = String.valueOf(v).trim();
		return "true".equals(vs) || "1".equals(vs);
	}

	private static Object emptyPaymentSettingBodyOrArray(Map<String, Object> data) {
		if (data == null || data.isEmpty()) {
			return Collections.emptyList();
		}
		return data;
	}
}
