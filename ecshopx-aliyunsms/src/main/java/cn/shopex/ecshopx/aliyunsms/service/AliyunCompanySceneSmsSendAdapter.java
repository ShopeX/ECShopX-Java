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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.companys.service.operator.sms.CompanySceneSmsSendPort;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class AliyunCompanySceneSmsSendAdapter implements CompanySceneSmsSendPort {

	private static final Logger log = LoggerFactory.getLogger(AliyunCompanySceneSmsSendAdapter.class);

	private static final String SCENE_TITLE_DEALER_RESET = "dealer_account_reset_pwd";

	private final AliyunsmsSettingStatusService aliyunsmsSettingStatusService;
	private final AccessKeyMapper accessKeyMapper;
	private final SceneMapper sceneMapper;
	private final SceneItemMapper sceneItemMapper;
	private final TemplateMapper templateMapper;
	private final SignMapper signMapper;
	private final RecordMapper recordMapper;
	private final ObjectMapper objectMapper;

	public AliyunCompanySceneSmsSendAdapter(
			AliyunsmsSettingStatusService aliyunsmsSettingStatusService,
			AccessKeyMapper accessKeyMapper,
			SceneMapper sceneMapper,
			SceneItemMapper sceneItemMapper,
			TemplateMapper templateMapper,
			SignMapper signMapper,
			RecordMapper recordMapper,
			ObjectMapper objectMapper) {
		this.aliyunsmsSettingStatusService = aliyunsmsSettingStatusService;
		this.accessKeyMapper = accessKeyMapper;
		this.sceneMapper = sceneMapper;
		this.sceneItemMapper = sceneItemMapper;
		this.templateMapper = templateMapper;
		this.signMapper = signMapper;
		this.recordMapper = recordMapper;
		this.objectMapper = objectMapper;
	}

	@Override
	public void sendDealerAccountResetPwd(
			long companyId, String mobile, String dealerDisplayName, String plainPassword) {
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("dealer", dealerDisplayName != null ? dealerDisplayName : "");
		payload.put("password", plainPassword != null ? plainPassword : "");
		sendBySceneTitle(companyId, mobile, SCENE_TITLE_DEALER_RESET, payload);
	}

	@Override
	public void sendSceneTemplatedSms(long companyId, String mobilePlain, String sceneTitle, Map<String, String> variables) {
		Map<String, String> data = variables != null ? variables : Map.of();
		sendBySceneTitle(companyId, mobilePlain, sceneTitle, data);
	}

	private void sendBySceneTitle(long companyId, String mobile, String sceneTitle, Map<String, String> payload) {
		if (!aliyunsmsSettingStatusService.getStatus(companyId)) {
			throw new ResourceException("短信通道未启用");
		}

		AccessKey ak =
				accessKeyMapper.selectOne(new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId));
		if (ak == null
				|| ak.getAccesskeyId() == null
				|| ak.getAccesskeyId().isBlank()
				|| ak.getAccesskeySecret() == null
				|| ak.getAccesskeySecret().isBlank()) {
			throw new ResourceException("短信通道 AccessKey 未配置");
		}

		Scene scene = sceneMapper.selectOne(
				Wrappers.<Scene>lambdaQuery()
						.eq(Scene::getCompanyId, companyId)
						.eq(Scene::getSceneTitle, sceneTitle)
						.last("LIMIT 1"));
		if (scene == null) {
			throw new ResourceException("短信场景未配置: " + sceneTitle);
		}

		Long sceneId = scene.getId();
		if (sceneId == null || sceneId > Integer.MAX_VALUE) {
			log.warn("Scene SMS failed: invalid scene id companyId={} sceneId={}", companyId, sceneId);
			throw new ResourceException("短信场景配置无效");
		}

		SceneItem item = sceneItemMapper.selectOne(
				Wrappers.<SceneItem>lambdaQuery()
						.eq(SceneItem::getSceneId, sceneId.intValue())
						.eq(SceneItem::getStatus, 1)
						.last("LIMIT 1"));
		if (item == null || item.getTemplateId() == null || item.getSignId() == null) {
			throw new ResourceException("短信场景未启用或无可用模板项");
		}

		Template template = templateMapper.selectById(Long.valueOf(item.getTemplateId()));
		if (template == null || template.getTemplateCode() == null || !"1".equals(template.getStatus())) {
			throw new ResourceException("短信模板未审核通过或未配置");
		}

		Sign sign = signMapper.selectOne(
				Wrappers.<Sign>lambdaQuery()
						.eq(Sign::getId, item.getSignId().longValue())
						.eq(Sign::getStatus, "1")
						.last("LIMIT 1"));
		if (sign == null || sign.getSignName() == null || sign.getSignName().isBlank()) {
			throw new ResourceException("短信签名未审核通过或未配置");
		}

		String templateContent = template.getTemplateContent() != null ? template.getTemplateContent() : "";
		Map<String, String> filtered =
				AliyunsmsTemplateVariableSupport.filterTemplateParam(
						templateContent, payload, scene.getVariables(), objectMapper);

		String templateParamJson;
		try {
			templateParamJson = filtered.isEmpty() ? null : objectMapper.writeValueAsString(filtered);
		} catch (Exception e) {
			log.warn("Scene SMS failed: cannot serialize template params companyId={}", companyId, e);
			throw new ResourceException("短信模板参数序列化失败");
		}

		SendSmsResponseBody body;
		try {
			body = invokeSendSms(ak, mobile, sign.getSignName(), template.getTemplateCode(), templateParamJson);
		} catch (Exception e) {
			log.warn(
					"Scene SMS failed: Aliyun API error companyId={} mobile={}",
					companyId,
					DataMasking.maskUname(mobile),
					e);
			throw new ResourceException(e.getMessage() != null ? e.getMessage() : "短信发送接口调用失败");
		}

		if (body == null || !"OK".equals(body.getCode())) {
			String apiMsg = body != null ? body.getMessage() : null;
			throw new ResourceException(apiMsg != null ? apiMsg : "短信发送失败");
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
}
