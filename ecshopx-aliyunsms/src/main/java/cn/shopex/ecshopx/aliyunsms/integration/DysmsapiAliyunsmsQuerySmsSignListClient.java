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
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySmsSignListClient;
import cn.shopex.ecshopx.common.aliyunsms.QuerySmsSignListItem;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.QuerySmsSignListRequest;
import com.aliyun.dysmsapi20170525.models.QuerySmsSignListResponse;
import com.aliyun.dysmsapi20170525.models.QuerySmsSignListResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DysmsapiAliyunsmsQuerySmsSignListClient implements AliyunsmsQuerySmsSignListClient {

	private static final Logger log = LoggerFactory.getLogger(DysmsapiAliyunsmsQuerySmsSignListClient.class);

	private final AccessKeyMapper accessKeyMapper;

	public DysmsapiAliyunsmsQuerySmsSignListClient(AccessKeyMapper accessKeyMapper) {
		this.accessKeyMapper = accessKeyMapper;
	}

	@Override
	public List<QuerySmsSignListItem> querySmsSignList(long companyId, int pageIndex, int pageSize) {
		AccessKey accessKey = accessKeyMapper.selectOne(
				new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId));
		if (accessKey == null
				|| accessKey.getAccesskeyId() == null
				|| accessKey.getAccesskeyId().isBlank()
				|| accessKey.getAccesskeySecret() == null
				|| accessKey.getAccesskeySecret().isBlank()) {
			throw new ResourceException("请先配置AccessKey");
		}
		Config config = new Config();
		config.accessKeyId = accessKey.getAccesskeyId();
		config.accessKeySecret = accessKey.getAccesskeySecret();
		config.endpoint = "dysmsapi.aliyuncs.com";
		config.regionId = "cn-hangzhou";
		try {
			Client client = new Client(config);
			QuerySmsSignListRequest request = new QuerySmsSignListRequest();
			request.setPageIndex(pageIndex);
			request.setPageSize(pageSize);
			QuerySmsSignListResponse response = client.querySmsSignList(request);
			QuerySmsSignListResponseBody body = response != null ? response.getBody() : null;
			if (body == null || !"OK".equals(body.getCode())) {
				String message = body != null && body.getMessage() != null && !body.getMessage().isBlank()
						? body.getMessage()
						: "查询阿里云短信签名列表失败";
				throw new ResourceException(message);
			}
			List<QuerySmsSignListItem> result = new ArrayList<>();
			List<QuerySmsSignListResponseBody.QuerySmsSignListResponseBodySmsSignList> signs = body.getSmsSignList();
			if (signs != null) {
				for (QuerySmsSignListResponseBody.QuerySmsSignListResponseBodySmsSignList sign : signs) {
					if (sign != null && sign.getSignName() != null && !sign.getSignName().isBlank()) {
						result.add(new QuerySmsSignListItem(sign.getSignName()));
					}
				}
			}
			return result;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("QuerySmsSignList SDK error: {}", e.getMessage());
			throw new ResourceException(e.getMessage() != null && !e.getMessage().isBlank()
					? e.getMessage()
					: "查询阿里云短信签名列表失败");
		}
	}
}
