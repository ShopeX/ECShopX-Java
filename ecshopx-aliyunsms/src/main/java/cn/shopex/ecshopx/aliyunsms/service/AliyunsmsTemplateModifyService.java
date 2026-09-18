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
import cn.shopex.ecshopx.common.dispatch.AliyunsmsModifySmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTemplateModifyService {

	private final AliyunsmsTemplateAddService aliyunsmsTemplateAddService;
	private final TemplateMapper templateMapper;
	private final AliyunsmsModifySmsTemplateJobDispatchPublisher modifySmsTemplateJobDispatchPublisher;

	public AliyunsmsTemplateModifyService(
			AliyunsmsTemplateAddService aliyunsmsTemplateAddService,
			TemplateMapper templateMapper,
			AliyunsmsModifySmsTemplateJobDispatchPublisher modifySmsTemplateJobDispatchPublisher) {
		this.aliyunsmsTemplateAddService = aliyunsmsTemplateAddService;
		this.templateMapper = templateMapper;
		this.modifySmsTemplateJobDispatchPublisher = modifySmsTemplateJobDispatchPublisher;
	}

	public void modifyTemplate(
			long companyId,
			long templateId,
			String templateName,
			int templateType,
			String remark,
			String templateContent,
			int sceneId,
			String relatedSignName) {
		aliyunsmsTemplateAddService.checkValidTemplateVariables(templateContent, sceneId);
		Template statusRow =
				templateMapper.selectOne(
						new LambdaQueryWrapper<Template>()
								.eq(Template::getId, templateId)
								.eq(Template::getCompanyId, companyId));
		if (statusRow == null) {
			throw new ResourceException("未查询到模板");
		}
		if ("0".equals(statusRow.getStatus())) {
			throw new ResourceException("审核中的模板不可修改");
		}
		String templateCode = statusRow.getTemplateCode();
		if (templateCode == null || templateCode.isBlank()) {
			throw new ResourceException("当前模板code未同步");
		}
		int now = (int) Instant.now().getEpochSecond();
		int rows =
				templateMapper.update(
						null,
						new LambdaUpdateWrapper<Template>()
								.eq(Template::getCompanyId, companyId)
								.eq(Template::getId, templateId)
								.set(Template::getTemplateName, templateName)
								.set(Template::getTemplateType, String.valueOf(templateType))
								.set(Template::getRemark, remark)
								.set(Template::getTemplateContent, templateContent)
								.set(Template::getSceneId, sceneId)
								.set(Template::getRelatedSignName, relatedSignName)
								.set(Template::getStatus, "0")
								.set(Template::getUpdated, now));
		if (rows != 1) {
			throw new ResourceException("未查询到更新数据");
		}
		modifySmsTemplateJobDispatchPublisher.publish(
				companyId,
				templateId,
				templateType,
				templateName,
				remark,
				templateContent,
				sceneId,
				relatedSignName,
				templateCode);
	}
}
