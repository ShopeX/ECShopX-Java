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
import com.aliyun.dysmsapi20170525.models.UpdateSmsTemplateRequest;
import com.aliyun.dysmsapi20170525.models.UpdateSmsTemplateResponse;
import com.aliyun.dysmsapi20170525.models.UpdateSmsTemplateResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AliyunsmsUpdateSmsTemplateClient {

	private static final Logger log = LoggerFactory.getLogger(AliyunsmsUpdateSmsTemplateClient.class);

	private final AccessKeyMapper accessKeyMapper;
	private final AliyunsmsSmsTemplateContentConverter smsTemplateContentConverter;

	public AliyunsmsUpdateSmsTemplateClient(
			AccessKeyMapper accessKeyMapper, AliyunsmsSmsTemplateContentConverter smsTemplateContentConverter) {
		this.accessKeyMapper = accessKeyMapper;
		this.smsTemplateContentConverter = smsTemplateContentConverter;
	}

	public void updateSmsTemplate(
			long companyId,
			int templateType,
			String templateName,
			String remark,
			String templateContent,
			int sceneId,
			String relatedSignName,
			String templateCode) {
		AccessKey akRow =
				accessKeyMapper.selectOne(new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId));
		if (akRow == null
				|| akRow.getAccesskeyId() == null
				|| akRow.getAccesskeyId().isBlank()
				|| akRow.getAccesskeySecret() == null
				|| akRow.getAccesskeySecret().isBlank()) {
			throw new ResourceException("请先配置AccessKey");
		}
		SmsTemplateConversionResult conv =
				smsTemplateContentConverter.convert(sceneId, templateContent, "修改阿里云短信模板失败");
		Config config = new Config();
		config.accessKeyId = akRow.getAccesskeyId();
		config.accessKeySecret = akRow.getAccesskeySecret();
		config.endpoint = "dysmsapi.aliyuncs.com";
		config.regionId = "cn-hangzhou";
		try {
			Client client = new Client(config);
			UpdateSmsTemplateRequest req = new UpdateSmsTemplateRequest();
			req.setTemplateType(templateType);
			req.setTemplateName(templateName);
			req.setRemark(remark);
			req.setTemplateCode(templateCode);
			req.setRelatedSignName(relatedSignName);
			req.setTemplateContent(conv.templateContent());
			req.setTemplateRule(conv.templateRuleJson());
			if (log.isDebugEnabled()) {
				log.debug(
						"UpdateSmsTemplate companyId={} templateType={} templateName={} sceneId={} templateCode={} rule={}",
						companyId,
						templateType,
						templateName,
						sceneId,
						templateCode,
						conv.templateRuleJson());
			}
			UpdateSmsTemplateResponse resp = client.updateSmsTemplate(req);
			UpdateSmsTemplateResponseBody body = resp != null ? resp.getBody() : null;
			String code = body != null ? body.getCode() : null;
			if (log.isDebugEnabled()) {
				log.debug(
						"UpdateSmsTemplate response code={} message={} templateCode={}",
						code,
						body != null ? body.getMessage() : null,
						body != null ? body.getTemplateCode() : null);
			}
			if (body == null || !"OK".equals(code)) {
				String msg =
						body != null && body.getMessage() != null && !body.getMessage().isEmpty()
								? body.getMessage()
								: "修改阿里云短信模板失败";
				log.warn("UpdateSmsTemplate not OK: {}", msg);
				throw new ResourceException(msg);
			}
			String tc = body.getTemplateCode();
			if (tc == null || tc.isEmpty()) {
				throw new ResourceException("修改阿里云短信模板失败");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("UpdateSmsTemplate SDK error: {}", e.getMessage());
			String fallback = "修改阿里云短信模板失败";
			String errMsg = e.getMessage();
			String msg = (errMsg != null && !errMsg.isBlank()) ? errMsg : fallback;
			throw new ResourceException(msg);
		}
	}
}
