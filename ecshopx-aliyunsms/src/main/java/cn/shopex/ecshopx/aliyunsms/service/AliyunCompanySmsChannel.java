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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.aliyunsms.domain.AccessKey;
import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.domain.SceneItem;
import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.AccessKeyMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneItemMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class AliyunCompanySmsChannel implements CompanySmsChannel {

	private static final Logger log = LoggerFactory.getLogger(AliyunCompanySmsChannel.class);

	private final AccessKeyMapper accessKeyMapper;
	private final SceneMapper sceneMapper;
	private final SceneItemMapper sceneItemMapper;
	private final TemplateMapper templateMapper;
	private final SignMapper signMapper;
	private final RecordMapper recordMapper;
	private final ObjectMapper objectMapper;

	AliyunCompanySmsChannel(
			AccessKeyMapper accessKeyMapper,
			SceneMapper sceneMapper,
			SceneItemMapper sceneItemMapper,
			TemplateMapper templateMapper,
			SignMapper signMapper,
			RecordMapper recordMapper,
			ObjectMapper objectMapper) {
		this.accessKeyMapper = accessKeyMapper;
		this.sceneMapper = sceneMapper;
		this.sceneItemMapper = sceneItemMapper;
		this.templateMapper = templateMapper;
		this.signMapper = signMapper;
		this.recordMapper = recordMapper;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean send(long companyId, String mobile, String sceneTitle, Map<String, String> data, String forgetSmsExactBody) {
		AccessKey ak =
				accessKeyMapper.selectOne(new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId));
		if (ak == null
				|| ak.getAccesskeyId() == null
				|| ak.getAccesskeyId().isBlank()
				|| ak.getAccesskeySecret() == null
				|| ak.getAccesskeySecret().isBlank()) {
			throw new ResourceException("发送阿里云短信失败");
		}

		Scene scene = sceneMapper.selectOne(
				Wrappers.<Scene>lambdaQuery()
						.eq(Scene::getCompanyId, companyId)
						.eq(Scene::getSceneTitle, sceneTitle)
						.last("LIMIT 1"));
		if (scene == null) {
			throw new BadRequestException("短信场景无效,不能发送");
		}

		Long sceneId = scene.getId();
		if (sceneId == null || sceneId > Integer.MAX_VALUE) {
			throw new BadRequestException("短信场景无效,不能发送");
		}

		SceneItem item = sceneItemMapper.selectOne(
				Wrappers.<SceneItem>lambdaQuery()
						.eq(SceneItem::getSceneId, sceneId.intValue())
						.eq(SceneItem::getStatus, 1)
						.last("LIMIT 1"));
		if (item == null || item.getTemplateId() == null || item.getSignId() == null) {
			throw new BadRequestException("模板未启用,不能发送");
		}

		Template template = templateMapper.selectById(Long.valueOf(item.getTemplateId()));
		if (template == null || template.getTemplateCode() == null || template.getTemplateCode().isBlank()) {
			throw new BadRequestException("模板无效,不能发送");
		}

		Sign sign = signMapper.selectOne(
				Wrappers.<Sign>lambdaQuery()
						.eq(Sign::getId, item.getSignId().longValue())
						.eq(Sign::getStatus, "1")
						.last("LIMIT 1"));
		if (sign == null || sign.getSignName() == null || sign.getSignName().isBlank()) {
			throw new BadRequestException("签名无效,不能发送");
		}

		String templateContent = template.getTemplateContent() != null ? template.getTemplateContent() : "";
		Map<String, String> filtered =
				AliyunsmsTemplateVariableSupport.filterTemplateParam(
						templateContent, data, scene.getVariables(), objectMapper);
		String code = data == null ? "" : data.getOrDefault("code", "");
		if (forgetSmsExactBody != null && !forgetSmsExactBody.isBlank()) {
			String expected = fixedOperatorForgetSmsSentence(code);
			if (!expected.equals(forgetSmsExactBody)) {
				log.warn("Company SMS forget flow: body mismatch companyId={}", companyId);
				return false;
			}
			String compiled =
					AliyunsmsTemplateVariableSupport.compileTemplateDisplay(
							templateContent, filtered, scene.getVariables(), objectMapper);
			if (!expected.equals(compiled)) {
				log.warn(
						"Company SMS forget flow: template render must match fixed sentence companyId={}",
						companyId);
				return false;
			}
		}
		String templateParamJson;
		try {
			templateParamJson = filtered.isEmpty() ? null : objectMapper.writeValueAsString(filtered);
		} catch (Exception e) {
			throw new BadRequestException("模板无效,不能发送");
		}

		SendSmsResponseBody body;
		try {
			body = invokeSendSms(ak, mobile, sign.getSignName(), template.getTemplateCode(), templateParamJson);
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "发送阿里云短信失败");
		}

		if (body == null || !"OK".equals(body.getCode())) {
			String message = body != null && body.getMessage() != null && !body.getMessage().isBlank()
					? body.getMessage()
					: "发送阿里云短信失败";
			throw new ResourceException(message);
		}

		String smsDisplay =
				"【"
						+ sign.getSignName()
						+ "】"
						+ AliyunsmsTemplateVariableSupport.compileTemplateDisplay(
								templateContent, filtered, scene.getVariables(), objectMapper);
		int now = (int) Instant.now().getEpochSecond();
		Record rec = new Record();
		rec.setCompanyId(companyId);
		rec.setMobile(mobile);
		rec.setSceneId(sceneId.intValue());
		rec.setTaskId(0);
		rec.setTemplateCode(template.getTemplateCode());
		rec.setSmsContent(smsDisplay);
		rec.setTemplateType(scene.getTemplateType() != null ? scene.getTemplateType() : "0");
		rec.setStatus("1");
		rec.setBizId(body.getBizId());
		rec.setCreated(now);
		rec.setUpdated(now);
		recordMapper.insert(rec);
		return true;
	}

	private SendSmsResponseBody invokeSendSms(
			AccessKey ak, String mobile, String signName, String templateCode, String templateParamJson)
			throws Exception {
		Config config = new Config();
		config.accessKeyId = ak.getAccesskeyId();
		config.accessKeySecret = ak.getAccesskeySecret();
		config.endpoint = "dysmsapi.aliyuncs.com";
		config.regionId = "cn-hangzhou";
		Client client = new Client(config);
		SendSmsRequest req = new SendSmsRequest();
		req.setPhoneNumbers(mobile);
		req.setSignName(signName);
		req.setTemplateCode(templateCode);
		req.setTemplateParam(templateParamJson);
		SendSmsResponse resp = client.sendSms(req);
		return resp != null ? resp.getBody() : null;
	}

	private static String fixedOperatorForgetSmsSentence(String verificationCode) {
		return "您的验证码是" + verificationCode + "，有效期为30分钟";
	}
}
