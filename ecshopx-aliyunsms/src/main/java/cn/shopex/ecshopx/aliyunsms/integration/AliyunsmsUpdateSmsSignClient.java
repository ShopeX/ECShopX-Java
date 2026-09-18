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
import com.aliyun.dysmsapi20170525.models.UpdateSmsSignRequest;
import com.aliyun.dysmsapi20170525.models.UpdateSmsSignResponse;
import com.aliyun.dysmsapi20170525.models.UpdateSmsSignResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AliyunsmsUpdateSmsSignClient {

	private static final Logger log = LoggerFactory.getLogger(AliyunsmsUpdateSmsSignClient.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final AccessKeyMapper accessKeyMapper;

	public AliyunsmsUpdateSmsSignClient(AccessKeyMapper accessKeyMapper) {
		this.accessKeyMapper = accessKeyMapper;
	}

	public void updateSmsSign(
			long companyId,
			String signName,
			int signSource,
			String remark,
			boolean thirdParty,
			String qualificationId) {
		updateSmsSign(companyId, signName, signSource, remark, thirdParty, qualificationId, Map.of());
	}

	public void updateSmsSign(
			long companyId,
			String signName,
			int signSource,
			String remark,
			boolean thirdParty,
			String qualificationId,
			Map<String, Object> fileHints) {
		AccessKey akRow =
				accessKeyMapper.selectOne(new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId));
		if (akRow == null
				|| akRow.getAccesskeyId() == null
				|| akRow.getAccesskeyId().isBlank()
				|| akRow.getAccesskeySecret() == null
				|| akRow.getAccesskeySecret().isBlank()) {
			throw new ResourceException("请先配置AccessKey");
		}
		String qidTrimmed = qualificationId == null ? "" : qualificationId.trim();
		long qualificationAsLong = 0L;
		if (!qidTrimmed.isEmpty()) {
			try {
				qualificationAsLong = Long.parseLong(qidTrimmed);
			} catch (NumberFormatException ignored) {
				qualificationAsLong = 0L;
			}
		}
		Config config = new Config();
		config.accessKeyId = akRow.getAccesskeyId();
		config.accessKeySecret = akRow.getAccesskeySecret();
		config.endpoint = "dysmsapi.aliyuncs.com";
		config.regionId = "cn-hangzhou";
		try {
			Client client = new Client(config);
			UpdateSmsSignRequest req = new UpdateSmsSignRequest();
			req.setSignName(signName);
			req.setSignSource(signSource);
			req.setRemark(remark);
			req.setThirdParty(thirdParty);
			req.setQualificationId(qualificationAsLong);
			applyFileHints(req, fileHints);
			if (log.isDebugEnabled()) {
				log.debug(
						"UpdateSmsSign companyId={} signName={} signSource={} thirdParty={} qualificationId={}",
						companyId,
						signName,
						signSource,
						thirdParty,
						qualificationAsLong);
			}
			UpdateSmsSignResponse resp = client.updateSmsSign(req);
			UpdateSmsSignResponseBody body = resp != null ? resp.getBody() : null;
			String code = body != null ? body.getCode() : null;
			if (log.isDebugEnabled()) {
				log.debug("UpdateSmsSign response code={} message={}", code, body != null ? body.getMessage() : null);
			}
			if (body == null || !"OK".equals(code)) {
				String msg =
						body != null && body.getMessage() != null && !body.getMessage().isEmpty()
								? body.getMessage()
								: "修改阿里云短信签名失败";
				log.warn("UpdateSmsSign not OK: {}", msg);
				throw new ResourceException(msg);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("UpdateSmsSign SDK error: {}", e.getMessage());
			String fallback = "修改阿里云短信签名失败";
			String errMsg = e.getMessage();
			String msg = (errMsg != null && !errMsg.isBlank()) ? errMsg : fallback;
			throw new ResourceException(msg);
		}
	}

	private static void applyFileHints(UpdateSmsSignRequest req, Map<String, Object> fileHints) {
		if (fileHints == null || fileHints.isEmpty()) {
			return;
		}
		List<String> moreData = new ArrayList<>();
		appendSerializedFileMap(moreData, fileHints.get("sign_file"));
		appendSerializedFileMap(moreData, fileHints.get("delegate_file"));
		if (!moreData.isEmpty()) {
			req.setMoreData(moreData);
		}
	}

	private static void appendSerializedFileMap(List<String> moreData, Object entry) {
		if (!(entry instanceof Map<?, ?> raw) || raw.isEmpty()) {
			return;
		}
		try {
			moreData.add(OBJECT_MAPPER.writeValueAsString(raw));
		} catch (JsonProcessingException e) {
			throw new ResourceException("序列化签名附件失败");
		}
	}
}
