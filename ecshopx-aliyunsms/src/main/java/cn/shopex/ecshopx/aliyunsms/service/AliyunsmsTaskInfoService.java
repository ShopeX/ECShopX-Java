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

import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTaskInfoService {

	private final TaskMapper taskMapper;

	public AliyunsmsTaskInfoService(TaskMapper taskMapper) {
		this.taskMapper = taskMapper;
	}

	public Map<String, Object> getInfo(long companyId, Long id) {
		if (id == null) {
			return new LinkedHashMap<>();
		}
		Task row =
				taskMapper.selectOne(
						Wrappers.<Task>lambdaQuery()
								.eq(Task::getId, id)
								.eq(Task::getCompanyId, companyId));
		if (row == null) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("company_id", row.getCompanyId());
		out.put("task_name", row.getTaskName());
		out.put("user_id", row.getUserId());
		out.put("sign_id", row.getSignId());
		out.put("template_id", row.getTemplateId());
		out.put("template_name", row.getTemplateName());
		out.put("total_num", row.getTotalNum());
		out.put("failed_num", row.getFailedNum());
		out.put("status", row.getStatus());
		out.put("send_at", row.getSendAt());
		out.put("is_send", row.getIsSend());
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		return out;
	}
}
