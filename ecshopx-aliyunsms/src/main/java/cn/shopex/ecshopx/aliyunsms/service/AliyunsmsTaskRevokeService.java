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
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTaskRevokeService {

	private final TaskMapper taskMapper;

	public AliyunsmsTaskRevokeService(TaskMapper taskMapper) {
		this.taskMapper = taskMapper;
	}

	public void revokeTask(long companyId, long taskId) {
		Task task =
				taskMapper.selectOne(
						Wrappers.<Task>lambdaQuery()
								.eq(Task::getId, taskId)
								.eq(Task::getCompanyId, companyId)
								.eq(Task::getStatus, "1"));
		if (task == null) {
			throw new ResourceException("当前任务不能撤销");
		}

		long now = Instant.now().getEpochSecond();
		long sendAtSec = task.getSendAt() == null ? 0L : task.getSendAt().longValue();
		if (sendAtSec < now + 300L) {
			throw new ResourceException("发送前5分钟不能撤销");
		}

		Task patch = new Task();
		patch.setStatus("4");
		patch.setUpdated((int) now);

		int rows =
				taskMapper.update(patch, Wrappers.<Task>lambdaUpdate().eq(Task::getId, taskId));
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据", 422);
		}
	}
}
