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

import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsSignClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsSignResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.aliyunsms.domain.AccessKey;
import cn.shopex.ecshopx.aliyunsms.mapper.AccessKeyMapper;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.GetSmsSignRequest;
import com.aliyun.dysmsapi20170525.models.GetSmsSignResponse;
import com.aliyun.dysmsapi20170525.models.GetSmsSignResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DysmsapiAliyunsmsGetSmsSignClient implements AliyunsmsGetSmsSignClient {

	private static final Logger log = LoggerFactory.getLogger(DysmsapiAliyunsmsGetSmsSignClient.class);

	private final AccessKeyMapper accessKeyMapper;

	public DysmsapiAliyunsmsGetSmsSignClient(AccessKeyMapper accessKeyMapper) {
		this.accessKeyMapper = accessKeyMapper;
	}

	@Override
	public GetSmsSignResult getSmsSign(long companyId, String signName) {
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
			GetSmsSignRequest req = new GetSmsSignRequest();
			req.setSignName(signName);
			if (log.isDebugEnabled()) {
				log.debug("GetSmsSign companyId={} signName={}", companyId, signName);
			}
			GetSmsSignResponse resp = client.getSmsSign(req);
			GetSmsSignResponseBody body = resp != null ? resp.getBody() : null;
			String code = body != null ? body.getCode() : null;
			if (log.isDebugEnabled()) {
				log.debug("GetSmsSign response code={} message={}", code, body != null ? body.getMessage() : null);
			}
			if (body == null || !"OK".equals(code)) {
				String msg =
						body != null && body.getMessage() != null && !body.getMessage().isEmpty()
								? body.getMessage()
								: "查询阿里云短信签名失败";
				log.warn("GetSmsSign not OK: {}", msg);
				throw new ResourceException(msg);
			}
			Long signStatusLong = body.getSignStatus();
			if (signStatusLong == null) {
				return GetSmsSignResult.noSignStatus();
			}
			GetSmsSignResponseBody.GetSmsSignResponseBodyAuditInfo audit = body.getAuditInfo();
			String reject = audit != null && audit.getRejectInfo() != null ? audit.getRejectInfo() : "";
			return new GetSmsSignResult(Long.toString(signStatusLong), reject);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("GetSmsSign SDK error: {}", e.getMessage());
			String fallback = "查询阿里云短信签名失败";
			String errMsg = e.getMessage();
			String msg = (errMsg != null && !errMsg.isBlank()) ? errMsg : fallback;
			throw new ResourceException(msg);
		}
	}
}
