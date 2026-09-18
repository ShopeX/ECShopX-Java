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

package cn.shopex.ecshopx.aliyunsms.integration;

import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsMassTaskSendClient;
import cn.shopex.ecshopx.common.aliyunsms.MassTaskSendSmsResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DysmsapiAliyunsmsMassTaskSendClient implements AliyunsmsMassTaskSendClient {

	private static final Logger log = LoggerFactory.getLogger(DysmsapiAliyunsmsMassTaskSendClient.class);

	@Override
	public MassTaskSendSmsResult sendMassSms(
			String accessKeyId,
			String accessKeySecret,
			String phoneNumbers,
			String signName,
			String templateCode,
			String templateParamJson) {
		Config config = new Config();
		config.accessKeyId = accessKeyId;
		config.accessKeySecret = accessKeySecret;
		config.endpoint = "dysmsapi.aliyuncs.com";
		config.regionId = "cn-hangzhou";
		try {
			Client client = new Client(config);
			SendSmsRequest req = new SendSmsRequest();
			req.setPhoneNumbers(phoneNumbers != null ? phoneNumbers : "");
			req.setSignName(signName);
			req.setTemplateCode(templateCode);
			req.setTemplateParam(templateParamJson);
			SendSmsResponse resp = client.sendSms(req);
			SendSmsResponseBody body = resp != null ? resp.getBody() : null;
			String code = body != null ? body.getCode() : null;
			String msg = body != null ? body.getMessage() : null;
			String bizId = body != null ? body.getBizId() : null;
			if (log.isDebugEnabled()) {
				log.debug("SendSms mass task code={} message={} bizId={}", code, msg, bizId);
			}
			return new MassTaskSendSmsResult(code, msg, bizId);
		} catch (Exception e) {
			log.warn("SendSms mass task SDK error: {}", e.getMessage());
			String fallback = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "短信发送接口调用失败";
			throw new ResourceException(fallback);
		}
	}
}
