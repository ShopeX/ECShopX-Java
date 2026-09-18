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

import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTaskModifyService {

	private final TemplateMapper templateMapper;
	private final SignMapper signMapper;
	private final TaskMapper taskMapper;

	public AliyunsmsTaskModifyService(
			TemplateMapper templateMapper, SignMapper signMapper, TaskMapper taskMapper) {
		this.templateMapper = templateMapper;
		this.signMapper = signMapper;
		this.taskMapper = taskMapper;
	}

	public void modifyTask(
			long companyId,
			long taskId,
			String taskName,
			int signId,
			int templateId,
			long sendAtSeconds) {
		Task existing =
				taskMapper.selectOne(
						Wrappers.<Task>lambdaQuery()
								.eq(Task::getId, taskId)
								.eq(Task::getCompanyId, companyId)
								.eq(Task::getStatus, "4"));
		if (existing == null) {
			throw new ResourceException("已撤销的任务才能编辑");
		}

		if (sendAtSeconds != 0 && sendAtSeconds < Instant.now().getEpochSecond()) {
			throw new ResourceException("定时发送时间不能小于当前时间");
		}

		Template template =
				templateMapper.selectOne(
						Wrappers.<Template>lambdaQuery()
								.eq(Template::getId, (long) templateId)
								.eq(Template::getCompanyId, companyId)
								.eq(Template::getStatus, "1")
								.eq(Template::getTemplateType, "2"));
		if (template == null) {
			throw new ResourceException("模板无效");
		}

		Sign sign =
				signMapper.selectOne(
						Wrappers.<Sign>lambdaQuery()
								.eq(Sign::getId, (long) signId)
								.eq(Sign::getCompanyId, companyId)
								.eq(Sign::getStatus, "1"));
		if (sign == null) {
			throw new ResourceException("签名无效");
		}

		int now = (int) Instant.now().getEpochSecond();
		Task taskPatch = new Task();
		taskPatch.setTaskName(taskName);
		taskPatch.setSignId(signId);
		taskPatch.setTemplateId(templateId);
		taskPatch.setSendAt((int) sendAtSeconds);
		taskPatch.setStatus("1");
		taskPatch.setUpdated(now);

		int rows =
				taskMapper.update(
						taskPatch,
						Wrappers.<Task>lambdaUpdate()
								.eq(Task::getId, taskId)
								.eq(Task::getCompanyId, companyId));
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据", 422);
		}
	}
}
