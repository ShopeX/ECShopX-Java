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
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySmsTemplateListClient;
import cn.shopex.ecshopx.common.aliyunsms.QuerySmsTemplateListItem;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.QuerySmsTemplateListRequest;
import com.aliyun.dysmsapi20170525.models.QuerySmsTemplateListResponse;
import com.aliyun.dysmsapi20170525.models.QuerySmsTemplateListResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DysmsapiAliyunsmsQuerySmsTemplateListClient implements AliyunsmsQuerySmsTemplateListClient {

	private static final Logger log = LoggerFactory.getLogger(DysmsapiAliyunsmsQuerySmsTemplateListClient.class);

	private final AccessKeyMapper accessKeyMapper;

	public DysmsapiAliyunsmsQuerySmsTemplateListClient(AccessKeyMapper accessKeyMapper) {
		this.accessKeyMapper = accessKeyMapper;
	}

	@Override
	public List<QuerySmsTemplateListItem> querySmsTemplateList(long companyId, int pageIndex, int pageSize) {
		AccessKey akRow = accessKeyMapper.selectOne(new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId));
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
			QuerySmsTemplateListRequest req = new QuerySmsTemplateListRequest();
			req.setPageIndex(pageIndex);
			req.setPageSize(pageSize);
			if (log.isDebugEnabled()) {
				log.debug("QuerySmsTemplateList companyId={} pageIndex={} pageSize={}", companyId, pageIndex, pageSize);
			}
			QuerySmsTemplateListResponse resp = client.querySmsTemplateList(req);
			QuerySmsTemplateListResponseBody body = resp != null ? resp.getBody() : null;
			String code = body != null ? body.getCode() : null;
			if (body == null || !"OK".equals(code)) {
				String msg = body != null && body.getMessage() != null && !body.getMessage().isEmpty() ? body.getMessage() : "查询阿里云短信模板列表失败";
				log.warn("QuerySmsTemplateList not OK: {}", msg);
				throw new ResourceException(msg);
			}
			List<QuerySmsTemplateListItem> result = new ArrayList<>();
			List<QuerySmsTemplateListResponseBody.QuerySmsTemplateListResponseBodySmsTemplateList> list = body.getSmsTemplateList();
			if (list != null) {
				for (QuerySmsTemplateListResponseBody.QuerySmsTemplateListResponseBodySmsTemplateList item : list) {
					if (item != null && item.getTemplateCode() != null && !item.getTemplateCode().isBlank()) {
						result.add(new QuerySmsTemplateListItem(item.getTemplateCode()));
					}
				}
			}
			return result;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("QuerySmsTemplateList SDK error: {}", e.getMessage());
			throw new ResourceException(e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "查询阿里云短信模板列表失败");
		}
	}
}
