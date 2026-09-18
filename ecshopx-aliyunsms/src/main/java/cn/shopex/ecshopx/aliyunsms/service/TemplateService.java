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

import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsTemplateClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsTemplateResult;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {

	private final TemplateMapper templateMapper;
	private final AliyunsmsGetSmsTemplateClient aliyunsmsGetSmsTemplateClient;
	private final AliyunsmsQuerySmsTemplateJobDispatchPublisher querySmsTemplateJobDispatchPublisher;

	public TemplateService(
			TemplateMapper templateMapper,
			AliyunsmsGetSmsTemplateClient aliyunsmsGetSmsTemplateClient,
			AliyunsmsQuerySmsTemplateJobDispatchPublisher querySmsTemplateJobDispatchPublisher) {
		this.templateMapper = templateMapper;
		this.aliyunsmsGetSmsTemplateClient = aliyunsmsGetSmsTemplateClient;
		this.querySmsTemplateJobDispatchPublisher = querySmsTemplateJobDispatchPublisher;
	}

	/**
	 * 查询审核中模板列表，对每条有效模板码入队异步查询任务。返回成功入队的条数（跳过 {@code template_code}
	 * 为空或空白的行）。
	 */
	public int scheduleQueryTemplateAuditStatus() {
		List<Template> pending =
				templateMapper.selectList(
						new QueryWrapper<Template>()
								.select("company_id", "template_code")
								.eq("status", "0")
								.ne("template_code", ""));
		if (pending == null || pending.isEmpty()) {
			return 0;
		}
		int dispatched = 0;
		for (Template item : pending) {
			long companyId = item.getCompanyId() != null ? item.getCompanyId() : 0L;
			String templateCode = item.getTemplateCode();
			if (templateCode == null || templateCode.isEmpty()) {
				continue;
			}
			querySmsTemplateJobDispatchPublisher.publish(companyId, templateCode);
			dispatched++;
		}
		return dispatched;
	}

	/**
	 * 拉取云端模板审核状态并在条件满足时写回本地 {@code aliyunsms_template}（审核中状态过滤更新）。
	 */
	public void applyPendingTemplateAuditFromCloud(long companyId, String templateCode) {
		GetSmsTemplateResult cloud =
				aliyunsmsGetSmsTemplateClient.getSmsTemplate(companyId, templateCode);
		if (cloud.templateStatus() == null) {
			return;
		}
		Template current =
				templateMapper.selectOne(
						new QueryWrapper<Template>()
								.eq("company_id", companyId)
								.eq("template_code", templateCode)
								.eq("status", "0"));
		if (current == null) {
			throw new ResourceException("未查询到更新数据");
		}
		String reason = cloud.rejectInfo() != null ? cloud.rejectInfo() : "";
		int now = (int) Instant.now().getEpochSecond();
		int rows =
				templateMapper.update(
						null,
						new UpdateWrapper<Template>()
								.eq("company_id", companyId)
								.eq("template_code", templateCode)
								.eq("status", "0")
								.set("status", cloud.templateStatus())
								.set("reason", reason)
								.set("updated", now));
		if (rows != 1) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
