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
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingBooleanParsing;
import cn.shopex.ecshopx.payment.service.dto.PaymentSettingCommand;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChinaumsPaymentSettingWriter {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final String storageLocalRoot;

	public ChinaumsPaymentSettingWriter(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${ecshopx.storage.local.root:storage/app/public}") String storageLocalRoot) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.storageLocalRoot = storageLocalRoot;
	}

	public void write(PaymentSettingCommand cmd) {
		Map<String, Object> scalar = cmd.scalarFields();

		String subKey = "";
		if ("dealer".equals(cmd.operatorType())) {
			subKey = "dealer_" + cmd.operatorId();
		} else if (cmd.distributorId() > 0) {
			subKey = "distributor_" + cmd.distributorId();
		}

		boolean extraScope = !"dealer".equals(cmd.operatorType()) && cmd.distributorId() == 0;

		String redisKey = PaymentSettingRedisKeys.chinaumsPaymentSettingKey(cmd.companyId(), subKey);
		String rawExisting = companysRedisTemplate.opsForValue().get(redisKey);
		Map<String, Object> redisData =
				rawExisting != null
						? PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawExisting)
						: new LinkedHashMap<>();

		Object midObj = scalar.get("mid");
		String mid = midObj != null ? String.valueOf(midObj).trim() : "";
		if (!StringUtils.hasText(mid) && redisData.get("mid") != null) {
			mid = String.valueOf(redisData.get("mid")).trim();
		}
		if (!StringUtils.hasText(mid)) {
			throw new BadRequestException("商户号不能为空");
		}

		if (extraScope && PaymentSettingBooleanParsing.looseTrueString(scalar.get("is_open"))) {
			companysRedisTemplate
					.opsForValue()
					.set(PaymentSettingRedisKeys.paymentTypeOpenConfigKey(cmd.companyId()), "chinaumspay");
		}

		Map<String, Object> data = new LinkedHashMap<>(redisData);
		data.put("mid", scalar.get("mid"));
		data.put("tid", scalar.get("tid"));
		data.put("enterpriseid", scalar.get("enterpriseid"));

		if (extraScope) {
			Object rateRaw = scalar.get("rate");
			data.put("rate", rateRaw != null ? String.valueOf(rateRaw).trim() : "0");

			if (cmd.files().containsKey("rsa_private")) {
				MultipartFile f = cmd.files().get("rsa_private");
				writeBinaryUnderChinaums(mid, "rsa_private.pfx", f);
			}
			if (cmd.files().containsKey("rsa_public")) {
				MultipartFile f = cmd.files().get("rsa_public");
				writeBinaryUnderChinaums(mid, "rsa_public.cer", f);
			}
			data.put("password", scalar.get("password"));
			data.put("bank_name", scalar.get("bank_name"));
			data.put("bank_code", scalar.get("bank_code"));
			data.put("bank_account", scalar.get("bank_account"));
			data.put("is_open", PaymentSettingBooleanParsing.looseTrueString(scalar.get("is_open")));
		}

		try {
			companysRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(data));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}
	}

	private void writeBinaryUnderChinaums(String mid, String filename, MultipartFile file) {
		try {
			Path root = Path.of(storageLocalRoot).toAbsolutePath().normalize();
			Path base = root.resolve("chinaumsPayment").normalize();
			Path target = base.resolve(mid).resolve(filename).normalize();
			if (!target.startsWith(base)) {
				throw new BadRequestException("参数类型错误");
			}
			Files.createDirectories(target.getParent());
			Files.write(target, file.getBytes());
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("参数类型错误");
		}
	}
}
