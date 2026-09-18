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
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsSignClient;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySmsSignListClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsSignResult;
import cn.shopex.ecshopx.common.aliyunsms.QuerySmsSignListItem;
import cn.shopex.ecshopx.common.aliyunsms.SyncSmsSignResult;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsSignSyncService {

	private static final Logger log = LoggerFactory.getLogger(AliyunsmsSignSyncService.class);
	private static final int PAGE_SIZE = 50;

	private final SignMapper signMapper;
	private final SceneItemMapper sceneItemMapper;
	private final TaskMapper taskMapper;
	private final AliyunsmsQuerySmsSignListClient querySmsSignListClient;
	private final AliyunsmsGetSmsSignClient getSmsSignClient;

	public AliyunsmsSignSyncService(
			SignMapper signMapper,
			SceneItemMapper sceneItemMapper,
			TaskMapper taskMapper,
			AliyunsmsQuerySmsSignListClient querySmsSignListClient,
			AliyunsmsGetSmsSignClient getSmsSignClient) {
		this.signMapper = signMapper;
		this.sceneItemMapper = sceneItemMapper;
		this.taskMapper = taskMapper;
		this.querySmsSignListClient = querySmsSignListClient;
		this.getSmsSignClient = getSmsSignClient;
	}

	public SyncSmsSignResult sync(long companyId) {
		SyncSmsSignResult result = new SyncSmsSignResult();
		log.info("SyncSmsSigns started companyId={}", companyId);
		Set<String> cloudNames = fetchCloudNames(companyId);
		List<Sign> localSigns = signMapper.selectList(
				Wrappers.<Sign>lambdaQuery().eq(Sign::getCompanyId, companyId));
		log.info(
				"SyncSmsSigns snapshot companyId={} cloudSignCount={} localBeforeCount={}",
				companyId,
				cloudNames.size(),
				localSigns.size());
		Map<String, Sign> localByName = new LinkedHashMap<>();
		for (Sign sign : localSigns) {
			if (sign.getSignName() != null && !sign.getSignName().isBlank()) {
				localByName.put(sign.getSignName(), sign);
			}
		}

		for (String signName : cloudNames) {
			try {
				GetSmsSignResult cloud = getSmsSignClient.getSmsSign(companyId, signName);
				Sign existing = localByName.get(signName);
				if (existing == null) {
					Sign created = new Sign();
					created.setCompanyId(companyId);
					created.setSignName(trimToLength(signName, 20));
					created.setSignSource("0");
					created.setRemark("");
					created.setThirdParty(0);
					created.setQualificationId("");
					created.setStatus(mapStatus(cloud.signStatus()));
					created.setReason(trimToLength(cloud.rejectInfo(), 255));
					int now = (int) Instant.now().getEpochSecond();
					created.setCreated(now);
					created.setUpdated(now);
					signMapper.insert(created);
					result.incCreated();
				} else {
					existing.setStatus(mapStatus(cloud.signStatus()));
					existing.setReason(trimToLength(cloud.rejectInfo(), 255));
					existing.setUpdated((int) Instant.now().getEpochSecond());
					signMapper.updateById(existing);
					result.incUpdated();
				}
			} catch (RuntimeException e) {
				result.incFailed();
				result.addError(signName, e.getMessage());
				log.warn("SyncSmsSigns upsert failed: {} => {}", signName, e.getMessage());
			}
		}

		cleanupOrphans(companyId, cloudNames, localSigns, result);
		log.info(
				"SyncSmsSigns finished companyId={} created={} updated={} deleted={} skipped={} failed={}",
				companyId, result.created(), result.updated(), result.deleted(), result.skipped(), result.failed());
		return result;
	}

	private Set<String> fetchCloudNames(long companyId) {
		Set<String> names = new LinkedHashSet<>();
		int pageIndex = 1;
		while (true) {
			List<QuerySmsSignListItem> page = querySmsSignListClient.querySmsSignList(companyId, pageIndex, PAGE_SIZE);
			log.info(
					"SyncSmsSigns list page companyId={} pageIndex={} pageSize={} pageReturnedCount={}",
					companyId,
					pageIndex,
					PAGE_SIZE,
					page.size());
			for (QuerySmsSignListItem item : page) {
				if (item != null && item.signName() != null && !item.signName().isBlank()) {
					names.add(item.signName());
				}
			}
			if (page.size() < PAGE_SIZE) {
				log.info(
						"SyncSmsSigns list completed companyId={} totalUniqueCloudSigns={}",
						companyId,
						names.size());
				return names;
			}
			pageIndex++;
		}
	}

	private void cleanupOrphans(
			long companyId, Set<String> cloudNames, List<Sign> localSigns, SyncSmsSignResult result) {
		for (Sign sign : new ArrayList<>(localSigns)) {
			if (sign.getSignName() == null || sign.getSignName().isBlank() || cloudNames.contains(sign.getSignName())) {
				continue;
			}
			long sceneCount = sceneItemMapper.selectCount(
					Wrappers.<SceneItem>lambdaQuery()
							.eq(SceneItem::getCompanyId, companyId)
							.apply("sign_id = {0}", sign.getId()));
			if (sceneCount > 0) {
				result.incSkipped();
				result.addError(sign.getSignName(), "本地签名仍被短信场景引用，跳过删除");
				continue;
			}
			long taskCount = taskMapper.selectCount(
					Wrappers.<Task>lambdaQuery()
							.eq(Task::getCompanyId, companyId)
							.apply("sign_id = {0}", sign.getId())
							.eq(Task::getStatus, "1"));
			if (taskCount > 0) {
				result.incSkipped();
				result.addError(sign.getSignName(), "本地签名仍被进行中的群发任务引用，跳过删除");
				continue;
			}
			signMapper.deleteById(sign.getId());
			result.incDeleted();
		}
	}

	private static String mapStatus(String cloudStatus) {
		if (cloudStatus == null || cloudStatus.isBlank()) {
			return "0";
		}
		return "10".equals(cloudStatus) ? "2" : cloudStatus;
	}

	private static String trimToLength(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}
}
