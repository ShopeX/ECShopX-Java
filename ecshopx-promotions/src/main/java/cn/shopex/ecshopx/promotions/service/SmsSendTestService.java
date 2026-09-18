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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.promotions.domain.SmsIdiograph;
import cn.shopex.ecshopx.promotions.domain.SmsTemplate;
import cn.shopex.ecshopx.promotions.mapper.SmsIdiographMapper;
import cn.shopex.ecshopx.promotions.mapper.SmsTemplateMapper;
import cn.shopex.ecshopx.promotions.service.sms.SmsDefaultTemplateRegistry;
import cn.shopex.ecshopx.promotions.service.sms.SmsDefaultTemplateRow;
import cn.shopex.ecshopx.promotions.service.sms.SmsOemShuyunFlags;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismCoreHttpClient;
import cn.shopex.ecshopx.thirdparty.service.prism.ShopexPrismSmsSignClient;
import cn.shopex.ecshopx.thirdparty.service.shuyun.ShuyunSignedGatewayClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmsSendTestService {

	private static final Logger log = LoggerFactory.getLogger(SmsSendTestService.class);

	private static final Map<String, String> DEFAULT_REPLACE_PARAMS =
			Map.ofEntries(
					Map.entry("telephone", "联系电话"),
					Map.entry("rights_name", "权益名称"),
					Map.entry("num", "数量"),
					Map.entry("available_times", "可用次数"),
					Map.entry("brand_name", "品牌名称"),
					Map.entry("date", "日期"),
					Map.entry("shop_name", "门店名称"),
					Map.entry("shop_address", "门店地址"),
					Map.entry("item_name", "商品名称"),
					Map.entry("pay_money", "支付金额"),
					Map.entry("pay_time", "支付时间"),
					Map.entry("recharge_money", "充值金额"),
					Map.entry("recharge_date", "充值时间"),
					Map.entry("deposit_money", "账户余额"),
					Map.entry("end_time", "结束时间"),
					Map.entry("activity_name", "活动名称"),
					Map.entry("activity_start_time", "活动开始时间"),
					Map.entry("activity_place", "活动地点"),
					Map.entry("activity_address", "活动详细地址"),
					Map.entry("activity_refuse_reason", "拒绝原因"),
					Map.entry("review_result", "审核结果"),
					Map.entry("password", "随机密码"),
					Map.entry("phone", "手机号"),
					Map.entry("code", "验证码"),
					Map.entry("order_id", "订单号"),
					Map.entry("pickup_code", "提货码"),
					Map.entry("step", "步骤"),
					Map.entry("dealer", "经销商名称"),
					Map.entry("mer_name", "商户名称"));

	private final SmsIdiographMapper smsIdiographMapper;
	private final SmsTemplateMapper smsTemplateMapper;
	private final SmsDefaultTemplateRegistry defaultTemplateRegistry;
	private final CompanysMapper companysMapper;
	private final ObjectMapper objectMapper;
	private final ShuyunSignedGatewayClient shuyunSignedGatewayClient;
	private final Environment environment;
	private final PrismCoreHttpClient prismCoreHttpClient;
	private final StringRedisTemplate prismRedisTemplate;

	public SmsSendTestService(
			SmsIdiographMapper smsIdiographMapper,
			SmsTemplateMapper smsTemplateMapper,
			SmsDefaultTemplateRegistry defaultTemplateRegistry,
			CompanysMapper companysMapper,
			ObjectMapper objectMapper,
			ShuyunSignedGatewayClient shuyunSignedGatewayClient,
			Environment environment,
			PrismCoreHttpClient prismCoreHttpClient,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate) {
		this.smsIdiographMapper = smsIdiographMapper;
		this.smsTemplateMapper = smsTemplateMapper;
		this.defaultTemplateRegistry = defaultTemplateRegistry;
		this.companysMapper = companysMapper;
		this.objectMapper = objectMapper;
		this.shuyunSignedGatewayClient = shuyunSignedGatewayClient;
		this.environment = environment;
		this.prismCoreHttpClient = prismCoreHttpClient;
		this.prismRedisTemplate = prismRedisTemplate;
	}

	public void sendSmsTest(long companyId, String tmplName, String mobile, String content) {
		postNoticeSmsInternal(
				companyId, tmplName, mobile, content, buildTestPlaceholderData(tmplName), true);
	}

	public void sendTemplatedNoticeSms(long companyId, String mobile, String tmplName, Map<String, String> dataVars) {
		SmsTemplate dbRow =
				smsTemplateMapper.selectOne(
						new LambdaQueryWrapper<SmsTemplate>()
								.eq(SmsTemplate::getCompanyId, companyId)
								.eq(SmsTemplate::getTmplName, tmplName)
								.last("LIMIT 1"));
		String templateBody;
		if (dbRow != null && StringUtils.hasText(dbRow.getContent())) {
			templateBody = dbRow.getContent().trim();
		} else {
			Optional<SmsDefaultTemplateRow> defOpt = defaultTemplateRegistry.getByName(tmplName);
			if (defOpt.isEmpty() || !StringUtils.hasText(defOpt.get().content())) {
				throw new ResourceException("未知短信模板");
			}
			templateBody = defOpt.get().content().trim();
		}
		postNoticeSmsInternal(companyId, tmplName, mobile, templateBody, dataVars, false);
	}

	/**
	 * Sends operator-authored marketing text to one mobile; failures are logged only (async fan-out parity).
	 */
	public void sendMarketingFanOutPlainBodyOrLog(long companyId, String mobile, String plainBodyNoSign) {
		try {
			postMarketingFanOutPlainBodyInternal(companyId, mobile, plainBodyNoSign == null ? "" : plainBodyNoSign);
		} catch (Exception e) {
			log.warn(
					"member batch marketing sms failed companyId={} mobileTail={} err={}",
					companyId,
					mobile == null ? "" : maskMobileTail(mobile),
					e.getMessage());
		}
	}

	public void dispatchAdminMemberMassSmsFanOutOrLog(
			long companyId, List<String> mobilesOrdered, String smsContentPlainOrNull) {
		try {
			if (mobilesOrdered == null || mobilesOrdered.isEmpty()) {
				return;
			}
			List<String> phones =
					mobilesOrdered.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
			if (phones.isEmpty()) {
				return;
			}
			boolean oem = SmsOemShuyunFlags.isOemShuyun(this.environment);
			SmsIdiograph row =
					smsIdiographMapper.selectOne(
							new LambdaQueryWrapper<SmsIdiograph>()
									.eq(SmsIdiograph::getCompanyId, companyId)
									.last("LIMIT 1"));
			String signText = (row == null || row.getIdiograph() == null) ? "" : row.getIdiograph().trim();
			if (!StringUtils.hasText(signText)) {
				throw new ResourceException("签名未设置,不能发送短信", 400);
			}
			String plain = smsContentPlainOrNull == null ? "" : smsContentPlainOrNull;
			String fanOutSuffix = " 拒收请回复R";
			String sign = "【" + signText + "】";
			if (oem) {
				String finalContent = sign + plain + fanOutSuffix;
				Map<String, Object> bodyMap = new LinkedHashMap<>();
				bodyMap.put("smsType", "MARKETING");
				bodyMap.put("phones", phones);
				bodyMap.put("content", finalContent);
				bodyMap.put("remark", "会员群发短信");
				Companys c = companysMapper.selectById(companyId);
				String shopId =
						(c != null && StringUtils.hasText(c.getPassportUid())) ? c.getPassportUid().trim() : null;
				if (shopId != null) {
					bodyMap.put("shopId", shopId);
				}
				if (!shuyunSignedGatewayClient.isConfigured()) {
					log.debug("短信网关未配置，跳过会员群发: companyId={}", companyId);
					return;
				}
				String json;
				try {
					json = objectMapper.writeValueAsString(bodyMap);
				} catch (JsonProcessingException e) {
					throw new ForbiddenException("短信发送失败");
				}
				JsonNode resp = shuyunSignedGatewayClient.postSignedJson("/captcha/1.0/sms/send", json, null);
				if (resp == null) {
					throw new ForbiddenException("短信网关无有效响应");
				}
				int code = resp.path("code").asInt(-1);
				if (code != 0) {
					String msg = resp.path("message").asText("");
					throw new ForbiddenException(StringUtils.hasText(msg) ? msg : "短信发送失败");
				}
			} else {
				Companys c = companysMapper.selectById(companyId);
				String passportUid =
						(c != null && StringUtils.hasText(c.getPassportUid())) ? c.getPassportUid().trim() : null;
				if (!StringUtils.hasText(passportUid)) {
					throw new ForbiddenException("登录有误,请重新登录");
				}
				ShopexPrismSmsSignClient client =
						new ShopexPrismSmsSignClient(
								prismCoreHttpClient, prismRedisTemplate, objectMapper, companyId, passportUid);
				for (String one : phones) {
					String bodyCore = plain + fanOutSuffix;
					String finalOne = bodyCore + sign;
					Map<String, Object> singleContentMap = new LinkedHashMap<>();
					singleContentMap.put("phones", one);
					singleContentMap.put("content", finalOne);
					client.sendFanOutMarketingContents(List.of(singleContentMap), "fan-out");
				}
			}
		} catch (Exception e) {
			log.warn(
					"admin member mass sms fan-out failed companyId={} count={} err={}",
					companyId,
					mobilesOrdered == null ? 0 : mobilesOrdered.size(),
					e.getMessage());
		}
	}

	private static String maskMobileTail(String mobile) {
		String t = mobile.trim();
		if (t.length() <= 4) {
			return "****";
		}
		return "****" + t.substring(t.length() - 4);
	}

	private void postMarketingFanOutPlainBodyInternal(long companyId, String mobile, String plainBodyNoSign) {
		SmsIdiograph row =
				smsIdiographMapper.selectOne(
						new LambdaQueryWrapper<SmsIdiograph>()
								.eq(SmsIdiograph::getCompanyId, companyId)
								.last("LIMIT 1"));
		String signText = (row == null || row.getIdiograph() == null) ? "" : row.getIdiograph().trim();
		if (!StringUtils.hasText(signText)) {
			throw new ResourceException("签名未设置,不能发送短信", 400);
		}

		String sign = "【" + signText + "】";
		List<String> phones =
				Arrays.stream(mobile.split(","))
						.map(String::trim)
						.filter(s -> !s.isEmpty())
						.toList();
		if (phones.isEmpty()) {
			throw new BadRequestException("手机号格式不正确,不能发送短信");
		}

		String bodyAfterCompile = plainBodyNoSign + " 拒收请回复R";
		String finalContent = sign + bodyAfterCompile;
		String remark = "商城后台会员群发";

		Companys c = companysMapper.selectById(companyId);
		String shopId =
				(c != null && StringUtils.hasText(c.getPassportUid())) ? c.getPassportUid().trim() : null;

		Map<String, Object> bodyMap = new LinkedHashMap<>();
		bodyMap.put("smsType", "MARKETING");
		bodyMap.put("phones", phones);
		bodyMap.put("content", finalContent);
		bodyMap.put("remark", remark);
		if (shopId != null) {
			bodyMap.put("shopId", shopId);
		}

		log.info("sendSms member batch fan-out companyId={} contentLen={}", companyId, finalContent.length());

		if (!shuyunSignedGatewayClient.isConfigured()) {
			log.debug("短信网关未配置，跳过会员群发: companyId={}", companyId);
			return;
		}

		String json;
		try {
			json = objectMapper.writeValueAsString(bodyMap);
		} catch (JsonProcessingException e) {
			throw new ForbiddenException("短信发送失败");
		}

		JsonNode resp = shuyunSignedGatewayClient.postSignedJson("/captcha/1.0/sms/send", json, null);
		if (resp == null) {
			throw new ForbiddenException("短信网关无有效响应");
		}
		int code = resp.path("code").asInt(-1);
		if (code != 0) {
			String msg = resp.path("message").asText("");
			throw new ForbiddenException(StringUtils.hasText(msg) ? msg : "短信发送失败");
		}
	}

	private void postNoticeSmsInternal(
			long companyId,
			String tmplName,
			String mobile,
			String templateBodyContent,
			Map<String, String> dataVars,
			boolean throwWhenGatewayDisabled) {
		SmsIdiograph row =
				smsIdiographMapper.selectOne(
						new LambdaQueryWrapper<SmsIdiograph>()
								.eq(SmsIdiograph::getCompanyId, companyId)
								.last("LIMIT 1"));
		String signText = (row == null || row.getIdiograph() == null) ? "" : row.getIdiograph().trim();
		if (!StringUtils.hasText(signText)) {
			throw new ResourceException("签名未设置,不能发送短信", 400);
		}

		String sign = "【" + signText + "】";
		List<String> phones =
				Arrays.stream(mobile.split(","))
						.map(String::trim)
						.filter(s -> !s.isEmpty())
						.toList();
		if (phones.isEmpty()) {
			throw new BadRequestException("手机号格式不正确,不能发送短信");
		}

		SmsTemplate db =
				smsTemplateMapper.selectOne(
						new LambdaQueryWrapper<SmsTemplate>()
								.eq(SmsTemplate::getCompanyId, companyId)
								.eq(SmsTemplate::getTmplName, tmplName)
								.last("LIMIT 1"));

		String smsType;
		String remark;
		if (db != null) {
			smsType = db.getSmsType() == null ? "" : db.getSmsType();
			remark = extractRemarkFromDbSendTimeDesc(db.getSendTimeDesc());
		} else {
			Optional<SmsDefaultTemplateRow> defOpt = defaultTemplateRegistry.getByName(tmplName);
			if (defOpt.isEmpty()) {
				throw new ResourceException("未知短信模板");
			}
			SmsDefaultTemplateRow def = defOpt.get();
			smsType = def.smsType() == null ? "" : def.smsType();
			Map<String, Object> desc = def.sendTimeDesc();
			remark =
					desc == null
							? ""
							: (desc.get("tmpl_title") == null
									? ""
									: String.valueOf(desc.get("tmpl_title")));
		}

		String sendType = smsType;
		Map<String, String> safeData = dataVars == null ? Collections.emptyMap() : dataVars;
		String bodyAfterCompile =
				templateCompilers(templateBodyContent, safeData, DEFAULT_REPLACE_PARAMS);
		if ("fan-out".equals(sendType)) {
			bodyAfterCompile = bodyAfterCompile + " 拒收请回复R";
		}
		boolean oem = SmsOemShuyunFlags.isOemShuyun(this.environment);
		String finalContent = oem ? sign + bodyAfterCompile : bodyAfterCompile + sign;

		String gatewaySmsType;
		if ("notice".equals(sendType)) {
			gatewaySmsType = "NOTICE";
		} else if ("fan-out".equals(sendType)) {
			gatewaySmsType = "MARKETING";
		} else {
			gatewaySmsType = "NOTICE";
		}

		Companys c = companysMapper.selectById(companyId);
		String shopId =
				(c != null && StringUtils.hasText(c.getPassportUid())) ? c.getPassportUid().trim() : null;

		Map<String, Object> bodyMap = new LinkedHashMap<>();
		bodyMap.put("smsType", gatewaySmsType);
		bodyMap.put("phones", phones);
		bodyMap.put("content", finalContent);
		bodyMap.put("remark", remark);
		if (shopId != null) {
			bodyMap.put("shopId", shopId);
		}

		log.info("sendSms contents====>{}", finalContent);

		if (!shuyunSignedGatewayClient.isConfigured()) {
			if (throwWhenGatewayDisabled) {
				throw new ForbiddenException("短信网关无有效响应");
			}
			log.debug("短信网关未配置，跳过发送: tmplName={}", tmplName);
			return;
		}

		String json;
		try {
			json = objectMapper.writeValueAsString(bodyMap);
		} catch (JsonProcessingException e) {
			throw new ForbiddenException("短信发送失败");
		}

		JsonNode resp = shuyunSignedGatewayClient.postSignedJson("/captcha/1.0/sms/send", json, null);
		if (resp == null) {
			throw new ForbiddenException("短信网关无有效响应");
		}
		int code = resp.path("code").asInt(-1);
		if (code != 0) {
			String msg = resp.path("message").asText("");
			throw new ForbiddenException(StringUtils.hasText(msg) ? msg : "短信发送失败");
		}
	}

	private static String templateCompilers(
			String content, Map<String, String> data, Map<String, String> replaceParams) {
		if (replaceParams == null
				|| replaceParams.isEmpty()
				|| data == null
				|| data.isEmpty()) {
			return content;
		}
		String out = content;
		for (Map.Entry<String, String> e : replaceParams.entrySet()) {
			String dataKey = e.getKey();
			String zhLabel = e.getValue();
			Pattern p = Pattern.compile("\\{\\{" + Pattern.quote(zhLabel) + "\\}\\}");
			String replacement = String.valueOf(data.getOrDefault(dataKey, ""));
			out = p.matcher(out).replaceAll(Matcher.quoteReplacement(replacement));
		}
		return out;
	}

	private String extractRemarkFromDbSendTimeDesc(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		try {
			JsonNode tree = objectMapper.readTree(raw);
			return tree.path("tmpl_title").asText("");
		} catch (Exception e) {
			return "";
		}
	}

	private static Map<String, String> buildTestPlaceholderData(String tmplName) {
		if ("trade_pay_success".equals(tmplName)) {
			Map<String, String> m = new LinkedHashMap<>();
			m.put(
					"pay_time",
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
							.withZone(ZoneId.systemDefault())
							.format(Instant.now()));
			m.put("pay_money", "99999.99");
			return m;
		}
		if ("registration_result_notice".equals(tmplName)) {
			Map<String, String> m = new LinkedHashMap<>();
			m.put("activity_name", "报名活动的名称");
			m.put("review_result", "报名通过，允许参与");
			return m;
		}
		return Collections.emptyMap();
	}
}
