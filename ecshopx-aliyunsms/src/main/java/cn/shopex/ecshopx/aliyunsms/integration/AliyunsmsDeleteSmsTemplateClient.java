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

import cn.shopex.ecshopx.aliyunsms.domain.AccessKey;
import cn.shopex.ecshopx.aliyunsms.mapper.AccessKeyMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.DeleteSmsTemplateRequest;
import com.aliyun.dysmsapi20170525.models.DeleteSmsTemplateResponse;
import com.aliyun.dysmsapi20170525.models.DeleteSmsTemplateResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AliyunsmsDeleteSmsTemplateClient {

	private static final Logger log = LoggerFactory.getLogger(AliyunsmsDeleteSmsTemplateClient.class);

	private final AccessKeyMapper accessKeyMapper;

	public AliyunsmsDeleteSmsTemplateClient(AccessKeyMapper accessKeyMapper) {
		this.accessKeyMapper = accessKeyMapper;
	}

	public void deleteSmsTemplate(long companyId, String templateCode) {
		AccessKey akRow =
				accessKeyMapper.selectOne(new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId));
		if (akRow == null
				|| akRow.getAccesskeyId() == null
				|| akRow.getAccesskeyId().isBlank()
				|| akRow.getAccesskeySecret() == null
				|| akRow.getAccesskeySecret().isBlank()) {
			throw new ResourceException("请先配置AccessKey");
		}
		Config config = new Config();
		config.accessKeyId = akRow.getAccesskeyId();
		config.accessKeySecret = akRow.getAccesskeySecret();
		config.endpoint = "dysmsapi.aliyuncs.com";
		config.regionId = "cn-hangzhou";
		try {
			Client client = new Client(config);
			DeleteSmsTemplateRequest req = new DeleteSmsTemplateRequest();
			req.setTemplateCode(templateCode);
			if (log.isDebugEnabled()) {
				log.debug("DeleteSmsTemplate companyId={} templateCode={}", companyId, templateCode);
			}
			DeleteSmsTemplateResponse resp = client.deleteSmsTemplate(req);
			DeleteSmsTemplateResponseBody body = resp != null ? resp.getBody() : null;
			String code = body != null ? body.getCode() : null;
			if (log.isDebugEnabled()) {
				log.debug(
						"DeleteSmsTemplate response code={} message={}",
						code,
						body != null ? body.getMessage() : null);
			}
			if (body == null || !"OK".equals(code)) {
				String msg =
						body != null && body.getMessage() != null && !body.getMessage().isEmpty()
								? body.getMessage()
								: "删除阿里云短信模板失败";
				log.warn("DeleteSmsTemplate not OK: {}", msg);
				throw new ResourceException(msg);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("DeleteSmsTemplate SDK error: {}", e.getMessage());
			String fallback = "删除阿里云短信模板失败";
			String errMsg = e.getMessage();
			String msg = (errMsg != null && !errMsg.isBlank()) ? errMsg : fallback;
			throw new ResourceException(msg);
		}
	}
}
