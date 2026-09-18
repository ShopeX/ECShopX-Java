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

import cn.shopex.ecshopx.aliyunsms.domain.SceneItem;
import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneItemMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDeleteSmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsSignDeleteService {

	private final SignMapper signMapper;
	private final SceneItemMapper sceneItemMapper;
	private final TaskMapper taskMapper;
	private final AliyunsmsDeleteSmsSignJobDispatchPublisher deleteSmsSignJobDispatchPublisher;

	public AliyunsmsSignDeleteService(
			SignMapper signMapper,
			SceneItemMapper sceneItemMapper,
			TaskMapper taskMapper,
			AliyunsmsDeleteSmsSignJobDispatchPublisher deleteSmsSignJobDispatchPublisher) {
		this.signMapper = signMapper;
		this.sceneItemMapper = sceneItemMapper;
		this.taskMapper = taskMapper;
		this.deleteSmsSignJobDispatchPublisher = deleteSmsSignJobDispatchPublisher;
	}

	public void deleteSign(long companyId, long signId) {
		Sign sign =
				signMapper.selectOne(
						Wrappers.<Sign>lambdaQuery()
								.eq(Sign::getCompanyId, companyId)
								.eq(Sign::getId, signId));
		if (sign == null) {
			return;
		}
		if ("0".equals(sign.getStatus())) {
			throw new ResourceException("不支持删除正在审核中的签名");
		}
		long sceneRefCount =
				sceneItemMapper.selectCount(
						Wrappers.<SceneItem>lambdaQuery()
								.eq(SceneItem::getCompanyId, companyId)
								.apply("sign_id = {0}", signId));
		if (sceneRefCount > 0) {
			throw new ResourceException("不能删除已关联短信场景的模板");
		}
		long waitingTaskCount =
				taskMapper.selectCount(
						Wrappers.<Task>lambdaQuery()
								.eq(Task::getCompanyId, companyId)
								.apply("sign_id = {0}", signId)
								.eq(Task::getStatus, "1"));
		if (waitingTaskCount > 0) {
			throw new ResourceException("不能删除关联群发任务的模板");
		}
		String signNameForCloud = sign.getSignName();
		signMapper.deleteById(sign.getId());
		deleteSmsSignJobDispatchPublisher.publish(companyId, signNameForCloud);
	}
}
