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

import cn.shopex.ecshopx.aliyunsms.domain.AccessKey;
import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.AccessKeyMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsMassTaskSendClient;
import cn.shopex.ecshopx.common.aliyunsms.MassTaskSendSmsResult;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsBatchRecordJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.dto.AliyunsmsRunTaskMemberRow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

	private static final Logger log = LoggerFactory.getLogger(TaskService.class);

	private final TaskMapper taskMapper;
	private final RecordMapper recordMapper;
	private final AccessKeyMapper accessKeyMapper;
	private final TemplateMapper templateMapper;
	private final SignMapper signMapper;
	private final SceneMapper sceneMapper;
	private final MembersMapper membersMapper;
	private final AliyunsmsMassTaskSendClient aliyunsmsMassTaskSendClient;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final AliyunsmsAddSmsBatchRecordJobDispatchPublisher addSmsBatchRecordJobDispatchPublisher;

	public TaskService(
			TaskMapper taskMapper,
			RecordMapper recordMapper,
			AccessKeyMapper accessKeyMapper,
			TemplateMapper templateMapper,
			SignMapper signMapper,
			SceneMapper sceneMapper,
			MembersMapper membersMapper,
			AliyunsmsMassTaskSendClient aliyunsmsMassTaskSendClient,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			AliyunsmsAddSmsBatchRecordJobDispatchPublisher addSmsBatchRecordJobDispatchPublisher) {
		this.taskMapper = taskMapper;
		this.recordMapper = recordMapper;
		this.accessKeyMapper = accessKeyMapper;
		this.templateMapper = templateMapper;
		this.signMapper = signMapper;
		this.sceneMapper = sceneMapper;
		this.membersMapper = membersMapper;
		this.aliyunsmsMassTaskSendClient = aliyunsmsMassTaskSendClient;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.addSmsBatchRecordJobDispatchPublisher = addSmsBatchRecordJobDispatchPublisher;
	}

	/**
	 * 按「发送成功/全失败终态」回写群发短信任务行；与 PHP 一致每轮只处理第 1 页、每页 100 条、无 orderBy。返回本方法内
	 * {@code aliyunsms_task} 的 UPDATE 次数。
	 */
	public int scheduleUpdateStatus() {
		IPage<Task> page =
				taskMapper.selectPage(
						new Page<>(1, 100, false),
						new QueryWrapper<Task>().eq("status", "1").eq("is_send", 1));
		List<Task> tasks = page.getRecords();
		if (tasks == null || tasks.isEmpty()) {
			return 0;
		}
		int updated = 0;
		for (Task task : tasks) {
			Long id = task.getId();
			if (id == null) {
				continue;
			}
			int taskIdInt = id.intValue();

			long succNum =
					recordMapper.selectCount(
							new QueryWrapper<Record>().eq("task_id", taskIdInt).eq("status", "3"));
			if (succNum > 0) {
				taskMapper.update(
						null,
						new UpdateWrapper<Task>().eq("id", id).set("status", "2"));
				updated++;
				continue;
			}

			long failedNum =
					recordMapper.selectCount(
							new QueryWrapper<Record>().eq("task_id", taskIdInt).eq("status", "2"));

			long totalNum = task.getTotalNum() == null ? 0L : task.getTotalNum().longValue();
			if (failedNum == totalNum) {
				int failedInt = failedNum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) failedNum;
				taskMapper.update(
						null,
						new UpdateWrapper<Task>()
								.eq("id", id)
								.set("status", "3")
								.set("failed_num", failedInt));
				updated++;
			}
		}
		return updated;
	}

	/**
	 * 群发任务执行（PHP {@code TaskService::runTask}）；返回本轮成功将 {@code is_send} 置为 1 的任务行数。
	 */
	public int scheduleRunTask() {
		int nowSec = (int) Instant.now().getEpochSecond();
		IPage<Task> page =
				taskMapper.selectPage(
						new Page<>(1, 100),
						new QueryWrapper<Task>()
								.eq("status", "1")
								.eq("is_send", 0)
								.le("send_at", nowSec));
		List<Task> tasks = page.getRecords();
		if (tasks == null || tasks.isEmpty()) {
			return 0;
		}
		int successCount = 0;
		for (Task task : tasks) {
			try {
				processOneRunTask(task, nowSec);
				successCount++;
			} catch (Exception e) {
				log.error("执行群发短信任务: =>{}", e.getMessage(), e);
			}
		}
		return successCount;
	}

	private void processOneRunTask(Task task, int nowSec) {
		Long taskId = task.getId();
		Long companyId = task.getCompanyId();
		if (taskId == null || companyId == null) {
			throw new ResourceException("任务数据不完整");
		}

		AccessKey ak =
				accessKeyMapper.selectOne(
						new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId));
		if (ak == null
				|| ak.getAccesskeyId() == null
				|| ak.getAccesskeyId().isBlank()
				|| ak.getAccesskeySecret() == null
				|| ak.getAccesskeySecret().isBlank()) {
			throw new ResourceException("请先配置AccessKey");
		}

		List<Long> userIds = parseUserIdsCsv(task.getUserId());
		List<AliyunsmsRunTaskMemberRow> memberRows =
				userIds.isEmpty() ? List.of() : membersMapper.selectMembersForAliyunsmsRunTask(companyId, userIds);
		List<String> plainMobiles = new ArrayList<>();
		if (memberRows != null) {
			for (AliyunsmsRunTaskMemberRow row : memberRows) {
				if (row == null || row.getMobileEnc() == null || row.getMobileEnc().isBlank()) {
					continue;
				}
				String plain = sensitiveFieldEncryptor.decrypt(row.getMobileEnc());
				if (plain != null && !plain.isBlank()) {
					plainMobiles.add(plain.trim());
				}
			}
		}

		Integer templateIdInt = task.getTemplateId();
		if (templateIdInt == null) {
			throw new ResourceException("模板无效,不能发送");
		}
		Template template = templateMapper.selectById(templateIdInt.longValue());
		if (template == null || !"1".equals(template.getStatus())) {
			throw new ResourceException("模板无效,不能发送");
		}
		String templateCode = template.getTemplateCode();
		if (templateCode == null || templateCode.isBlank()) {
			throw new ResourceException("模板无效,不能发送");
		}

		Integer signIdInt = task.getSignId();
		if (signIdInt == null) {
			throw new ResourceException("签名无效,不能发送短");
		}
		Sign sign =
				signMapper.selectOne(
						Wrappers.<Sign>lambdaQuery()
								.eq(Sign::getId, signIdInt.longValue())
								.eq(Sign::getStatus, "1")
								.last("LIMIT 1"));
		if (sign == null || sign.getSignName() == null || sign.getSignName().isBlank()) {
			throw new ResourceException("签名无效,不能发送短");
		}

		String phoneNumbers = String.join(",", plainMobiles);
		MassTaskSendSmsResult sendResult =
				aliyunsmsMassTaskSendClient.sendMassSms(
						ak.getAccesskeyId(),
						ak.getAccesskeySecret(),
						phoneNumbers,
						sign.getSignName(),
						templateCode,
						null);
		if (!sendResult.ok()) {
			String apiMsg = sendResult.message() != null ? sendResult.message() : "短信发送失败";
			throw new ResourceException(apiMsg);
		}
		String bizId = sendResult.bizId();

		Scene scene =
				sceneMapper.selectOne(
						Wrappers.<Scene>lambdaQuery()
								.eq(Scene::getCompanyId, companyId)
								.eq(Scene::getTemplateType, "2")
								.last("LIMIT 1"));
		if (scene == null || scene.getId() == null || scene.getId() > Integer.MAX_VALUE) {
			throw new ResourceException("推广场景未配置");
		}
		int sceneId = scene.getId().intValue();

		String templateContent = template.getTemplateContent() != null ? template.getTemplateContent() : "";
		String smsDisplay = "【" + sign.getSignName() + "】" + templateContent;
		String templateType =
				template.getTemplateType() != null ? template.getTemplateType() : "0";
		int taskIdInt = taskId.intValue();

		addSmsBatchRecordJobDispatchPublisher.publish(
				companyId.longValue(),
				taskIdInt,
				new ArrayList<>(plainMobiles),
				sceneId,
				templateCode,
				templateType,
				smsDisplay,
				1,
				bizId);

		taskMapper.update(
				null, new UpdateWrapper<Task>().eq("id", taskId).set("is_send", 1).set("updated", nowSec));
	}

	private static List<Long> parseUserIdsCsv(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String part : raw.split(",")) {
			String p = part.trim();
			if (p.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(p));
			} catch (NumberFormatException ignored) {
				// skip invalid segment, same as empty contribution to IN list
			}
		}
		return out;
	}
}
