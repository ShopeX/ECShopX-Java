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

package cn.shopex.ecshopx.adapay.service.admin;

import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.service.AdapayOpenAccountStepService;
import cn.shopex.ecshopx.adapay.service.AdapayPaymentSettingRedisReader;
import cn.shopex.ecshopx.common.payment.AdminPaymentSettingListPort;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminPaymentSettingListService implements AdminPaymentSettingListPort {

	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;
	private final AdapayOpenAccountStepService adapayOpenAccountStepService;
	private final AdapayMemberMapper adapayMemberMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final HfPayPaymentSettingService hfPayPaymentSettingService;

	public AdminPaymentSettingListService(
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			AdapayOpenAccountStepService adapayOpenAccountStepService,
			AdapayMemberMapper adapayMemberMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			HfPayPaymentSettingService hfPayPaymentSettingService) {
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.adapayOpenAccountStepService = adapayOpenAccountStepService;
		this.adapayMemberMapper = adapayMemberMapper;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.hfPayPaymentSettingService = hfPayPaymentSettingService;
	}

	@Override
	public List<Map<String, Object>> getPaymentSettingList(
			long companyId, long distributorId, HttpServletRequest request) {
		List<Map<String, Object>> result = new ArrayList<>();

		Map<String, Object> adapay = adapayPaymentSettingRedisReader.getPaymentSetting(companyId);
		Map<String, Object> stepPayload = adapayOpenAccountStepService.openAccountStep(companyId, request);
		Object stepObj = stepPayload.get("step");
		int step = (stepObj instanceof Number) ? ((Number) stepObj).intValue() : 0;
		boolean adapayOpen = adapayIsOpenTruthy(adapay.get("is_open"));
		if (!adapay.isEmpty() && adapayOpen && step == 4) {
			if (distributorId == 0) {
				result.add(adapayListItem());
			} else if (distributorId >= Integer.MIN_VALUE && distributorId <= Integer.MAX_VALUE) {
				LambdaQueryWrapper<AdapayMember> w = new LambdaQueryWrapper<AdapayMember>()
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getOperatorType, "distributor")
						.eq(AdapayMember::getOperatorId, (int) distributorId)
						.eq(AdapayMember::getAuditState, "E");
				if (adapayMemberMapper.selectCount(w) > 0) {
					result.add(adapayListItem());
				}
			}
		}

		if (result.isEmpty()) {
			String redisKey = PaymentSettingRedisKeys.wxpayRedisKey(companyId, distributorId);
			String rawWx = companysRedisTemplate.opsForValue().get(redisKey);
			Map<String, Object> wechat = PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawWx);
			if (!wechat.isEmpty() && wechatIsOpenLooseTrue(wechat.get("is_open"))) {
				Map<String, Object> wxItem = new LinkedHashMap<>();
				wxItem.put("pay_type_code", "wxpay");
				wxItem.put("pay_type_name", "微信支付");
				result.add(wxItem);
			}
		}

		Map<String, Object> hfpay = hfPayPaymentSettingService.loadForAdminPaymentList(companyId);
		if (!hfpay.isEmpty()
				&& "true".equals(Objects.toString(hfpay.get("is_open"), "").trim())) {
			Map<String, Object> hfItem = new LinkedHashMap<>();
			hfItem.put("pay_type_code", "hfpay");
			hfItem.put("pay_type_name", "微信支付");
			result.add(hfItem);
		}

		return result;
	}

	private static Map<String, Object> adapayListItem() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("pay_type_code", "adapay");
		m.put("pay_channel", "wx_lite");
		m.put("pay_type_name", "微信支付");
		return m;
	}

	private static boolean adapayIsOpenTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b.booleanValue();
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		if (v instanceof String s) {
			return adapayStringTruthy(s);
		}
		return adapayStringTruthy(String.valueOf(v));
	}

	private static boolean adapayStringTruthy(String s) {
		String t = s.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equalsIgnoreCase(t)) {
			return false;
		}
		return true;
	}

	private static boolean wechatIsOpenLooseTrue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b == Boolean.TRUE;
		}
		if (v instanceof Number) {
			return false;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.length() == 4 && t.equalsIgnoreCase("true");
		}
		return false;
	}
}
