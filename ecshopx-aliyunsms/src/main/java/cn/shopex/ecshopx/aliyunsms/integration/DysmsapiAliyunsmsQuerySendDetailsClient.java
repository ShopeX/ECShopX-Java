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
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySendDetailsClient;
import cn.shopex.ecshopx.common.aliyunsms.QuerySendDetailsResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.QuerySendDetailsRequest;
import com.aliyun.dysmsapi20170525.models.QuerySendDetailsResponse;
import com.aliyun.dysmsapi20170525.models.QuerySendDetailsResponseBody;
import com.aliyun.dysmsapi20170525.models.QuerySendDetailsResponseBody.QuerySendDetailsResponseBodySmsSendDetailDTOs;
import com.aliyun.dysmsapi20170525.models.QuerySendDetailsResponseBody.QuerySendDetailsResponseBodySmsSendDetailDTOsSmsSendDetailDTO;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DysmsapiAliyunsmsQuerySendDetailsClient implements AliyunsmsQuerySendDetailsClient {

	private static final Logger log = LoggerFactory.getLogger(DysmsapiAliyunsmsQuerySendDetailsClient.class);

	private final AccessKeyMapper accessKeyMapper;

	public DysmsapiAliyunsmsQuerySendDetailsClient(AccessKeyMapper accessKeyMapper) {
		this.accessKeyMapper = accessKeyMapper;
	}

	@Override
	public QuerySendDetailsResult querySendDetails(long companyId, String mobile, String bizId, String sendDate) {
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
			QuerySendDetailsRequest req = new QuerySendDetailsRequest();
			req.setPhoneNumber(mobile);
			req.setBizId(bizId);
			req.setSendDate(sendDate);
			req.setPageSize(1L);
			req.setCurrentPage(1L);
			if (log.isDebugEnabled()) {
				log.debug(
						"QuerySendDetails companyId={} bizId={} sendDate={}",
						companyId,
						bizId,
						sendDate);
			}
			QuerySendDetailsResponse resp = client.querySendDetails(req);
			QuerySendDetailsResponseBody body = resp != null ? resp.getBody() : null;
			String code = body != null ? body.getCode() : null;
			if (log.isDebugEnabled()) {
				log.debug(
						"QuerySendDetails response code={} message={}",
						code,
						body != null ? body.getMessage() : null);
			}
			if (body == null || !"OK".equals(code)) {
				String msg =
						body != null && body.getMessage() != null && !body.getMessage().isEmpty()
								? body.getMessage()
								: "查询阿里云短信发送详情失败";
				log.warn("QuerySendDetails not OK: {}", msg);
				throw new ResourceException(msg);
			}
			QuerySendDetailsResponseBodySmsSendDetailDTOs dtos = body.getSmsSendDetailDTOs();
			if (dtos == null) {
				return QuerySendDetailsResult.noDetail();
			}
			List<QuerySendDetailsResponseBodySmsSendDetailDTOsSmsSendDetailDTO> list = dtos.getSmsSendDetailDTO();
			if (list == null || list.isEmpty()) {
				return QuerySendDetailsResult.noDetail();
			}
			QuerySendDetailsResponseBodySmsSendDetailDTOsSmsSendDetailDTO detail = list.get(0);
			if (detail == null) {
				return QuerySendDetailsResult.noDetail();
			}
			Long sendStatusLong = detail.getSendStatus();
			if (sendStatusLong == null) {
				return QuerySendDetailsResult.noDetail();
			}
			return new QuerySendDetailsResult(Long.toString(sendStatusLong), detail.getContent());
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("QuerySendDetails SDK error: {}", e.getMessage());
			String fallback = "查询阿里云短信发送详情失败";
			String errMsg = e.getMessage();
			String msg = (errMsg != null && !errMsg.isBlank()) ? errMsg : fallback;
			throw new ResourceException(msg);
		}
	}
}
