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

package cn.shopex.ecshopx.payment.service.settings;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.hfpay.HfPayPaymentSettingApplyPort;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingBooleanParsing;
import cn.shopex.ecshopx.payment.service.dto.PaymentSettingCommand;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 汇付支付配置写入：合并请求与 Redis 旧值后委托 {@link HfPayPaymentSettingApplyPort} 落库与证书同步。 */
@Service
public class HfPayPaymentSettingWriter {

	private final HfPayPaymentSettingApplyPort hfPayPaymentSettingApplyPort;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public HfPayPaymentSettingWriter(
			HfPayPaymentSettingApplyPort hfPayPaymentSettingApplyPort,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.hfPayPaymentSettingApplyPort = hfPayPaymentSettingApplyPort;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void write(PaymentSettingCommand cmd) {
		Map<String, Object> scalar = cmd.scalarFields();

		if (PaymentSettingBooleanParsing.looseTrueString(scalar.get("is_open"))) {
			companysRedisTemplate
					.opsForValue()
					.set(PaymentSettingRedisKeys.paymentTypeOpenConfigKey(cmd.companyId()), "hfpay");
		}

		String redisKey = PaymentSettingRedisKeys.hfPaymentSettingKey(cmd.companyId());
		String rawExisting = companysRedisTemplate.opsForValue().get(redisKey);
		Map<String, Object> existing =
				rawExisting != null ? PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawExisting) : Map.of();

		Map<String, Object> data = new LinkedHashMap<>(existing);
		data.put("mer_cust_id", scalar.get("mer_cust_id"));
		data.put("acct_id", scalar.get("acct_id"));
		data.put("pfx_password", scalar.get("pfx_password"));
		data.put("is_open", scalar.get("is_open"));

		if (cmd.files().containsKey("pfx_file")) {
			MultipartFile f = cmd.files().get("pfx_file");
			try {
				data.put("pfx_file", Base64.getEncoder().encodeToString(f.getBytes()));
			} catch (Exception e) {
				throw new BadRequestException("参数类型错误");
			}
		}
		if (cmd.files().containsKey("ca_pfx_file")) {
			MultipartFile f = cmd.files().get("ca_pfx_file");
			try {
				data.put("ca_pfx_file", new String(f.getBytes(), StandardCharsets.UTF_8));
			} catch (Exception e) {
				throw new BadRequestException("参数类型错误");
			}
		}
		if (cmd.files().containsKey("oca31_pfx_file")) {
			MultipartFile f = cmd.files().get("oca31_pfx_file");
			try {
				data.put("oca31_pfx_file", new String(f.getBytes(), StandardCharsets.UTF_8));
			} catch (Exception e) {
				throw new BadRequestException("参数类型错误");
			}
		}

		Object mer = data.get("mer_cust_id");
		if (mer == null || !StringUtils.hasText(String.valueOf(mer))) {
			throw new BadRequestException("参数类型错误");
		}

		hfPayPaymentSettingApplyPort.applySetPaymentSetting(cmd.companyId(), data);
	}
}
