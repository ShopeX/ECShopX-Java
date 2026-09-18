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
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTaskAddService {

	private static final int MAX_NUM = 1000;

	private final TemplateMapper templateMapper;
	private final SignMapper signMapper;
	private final MembersMapper membersMapper;
	private final TaskMapper taskMapper;

	public AliyunsmsTaskAddService(
			TemplateMapper templateMapper,
			SignMapper signMapper,
			MembersMapper membersMapper,
			TaskMapper taskMapper) {
		this.templateMapper = templateMapper;
		this.signMapper = signMapper;
		this.membersMapper = membersMapper;
		this.taskMapper = taskMapper;
	}

	public void addTask(
			long companyId,
			String taskName,
			int signId,
			int templateId,
			long sendAtSeconds,
			List<Long> userIdsOrNull) {
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
		String templateName = template.getTemplateName();
		Sign sign =
				signMapper.selectOne(
						Wrappers.<Sign>lambdaQuery()
								.eq(Sign::getId, (long) signId)
								.eq(Sign::getCompanyId, companyId)
								.eq(Sign::getStatus, "1"));
		if (sign == null) {
			throw new ResourceException("签名无效");
		}
		List<Long> userIds;
		if (userIdsOrNull != null && !userIdsOrNull.isEmpty()) {
			userIds = userIdsOrNull;
		} else {
			userIds =
					membersMapper
							.selectList(Wrappers.<Members>lambdaQuery().eq(Members::getCompanyId, companyId))
							.stream()
							.map(Members::getUserId)
							.collect(Collectors.toCollection(ArrayList::new));
		}
		int totalCount = userIds.size();
		List<List<Long>> chunks = chunkList(userIds, MAX_NUM);
		double totalNum = totalCount;
		int i = 0;
		while (totalNum / (double) MAX_NUM > 0) {
			totalNum -= MAX_NUM;
			int batchTotal = totalNum >= 0 ? MAX_NUM : (int) totalNum + MAX_NUM;
			List<Long> chunk = chunks.get(i++);
			String userIdCsv = chunk.stream().map(String::valueOf).collect(Collectors.joining(","));
			int now = (int) Instant.now().getEpochSecond();
			Task row = new Task();
			row.setCompanyId(companyId);
			row.setTaskName(taskName);
			row.setSignId(signId);
			row.setTemplateId(templateId);
			row.setTemplateName(templateName);
			row.setUserId(userIdCsv);
			row.setSendAt((int) sendAtSeconds);
			row.setStatus("1");
			row.setTotalNum(batchTotal);
			row.setCreated(now);
			row.setUpdated(now);
			taskMapper.insert(row);
		}
	}

	private static List<List<Long>> chunkList(List<Long> ids, int size) {
		List<List<Long>> out = new ArrayList<>();
		for (int from = 0; from < ids.size(); from += size) {
			int to = Math.min(from + size, ids.size());
			out.add(new ArrayList<>(ids.subList(from, to)));
		}
		return out;
	}
}
