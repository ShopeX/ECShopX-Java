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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.salesperson.domain.SalespersonTask;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.thirdparty.service.workwechat.WorkWechatAccessTokenProvider;
import cn.shopex.ecshopx.thirdparty.service.workwechat.WorkWechatCorpMessageSendPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SalespersonTaskWorkWechatNoticeService {

	private static final Logger log = LoggerFactory.getLogger(SalespersonTaskWorkWechatNoticeService.class);

	private static final String CONFIG_KEY_PREFIX = "workwechat:config:";

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter DESC_FMT = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");

	private static final DateTimeFormatter DONE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SalespersonTaskMapper salespersonTaskMapper;
	private final WorkWechatAccessTokenProvider workWechatAccessTokenProvider;
	private final WorkWechatCorpMessageSendPort workWechatCorpMessageSendPort;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public void sendTaskProgressNotice(long companyId, long taskId, long salespersonId, String username) {
		ShopSalesperson sp = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.last("LIMIT 1"));
		if (sp == null || !StringUtils.hasText(sp.getWorkUserid())) {
			return;
		}
		String appid = resolveAppidFromRedis(companyId);
		if (!StringUtils.hasText(appid)) {
			log.info("companyId:{}, 导购发货通知:企业微信未配置", companyId);
			return;
		}
		Optional<String> token = workWechatAccessTokenProvider.getAccessToken(companyId);
		if (token.isEmpty()) {
			log.info("companyId:{}, 导购任务通知:企业微信未配置或无法获取 access_token", companyId);
			return;
		}
		String taskName = resolveTaskName(companyId, taskId);
		String displayName = StringUtils.hasText(username) ? username : "微信用户";
		ZonedDateTime now = ZonedDateTime.now(CN);
		String description = now.format(DESC_FMT);
		String doneTime = now.format(DONE_FMT);

		Map<String, Object> miniprogramNotice = new LinkedHashMap<>();
		miniprogramNotice.put("appid", appid);
		miniprogramNotice.put("title", "物料转发任务通知");
		miniprogramNotice.put("description", description);
		miniprogramNotice.put("emphasis_first_item", false);
		List<Map<String, String>> contentItem = new ArrayList<>();
		contentItem.add(Map.of("key", "任务名称", "value", taskName));
		contentItem.add(Map.of("key", "任务进度", "value", "你完成了一个指标"));
		contentItem.add(Map.of("key", "客户名称", "value", displayName));
		contentItem.add(Map.of("key", "完成时间", "value", doneTime));
		miniprogramNotice.put("content_item", contentItem);

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("touser", sp.getWorkUserid().trim());
		root.put("msgtype", "miniprogram_notice");
		root.put("miniprogram_notice", miniprogramNotice);
		String json;
		try {
			json = objectMapper.writeValueAsString(root);
		} catch (JsonProcessingException e) {
			log.debug("task progress notice json build failed companyId={}", companyId, e);
			return;
		}
		workWechatCorpMessageSendPort.postMessageSend(token.get(), json);
	}

	private String resolveAppidFromRedis(long companyId) {
		if (companyId <= 0L) {
			return "";
		}
		String raw = stringRedisTemplate.opsForValue().get(CONFIG_KEY_PREFIX + sha1Hex(String.valueOf(companyId)));
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		try {
			String appid = objectMapper.readTree(raw).path("agents").path("app").path("appid").asText("").trim();
			return appid;
		} catch (Exception e) {
			log.debug("work wechat appid read failed companyId={}", companyId, e);
			return "";
		}
	}

	private String resolveTaskName(long companyId, long taskId) {
		SalespersonTask task = salespersonTaskMapper.selectOne(new LambdaQueryWrapper<SalespersonTask>()
				.eq(SalespersonTask::getCompanyId, companyId)
				.eq(SalespersonTask::getTaskId, taskId)
				.last("LIMIT 1"));
		if (task == null || !StringUtils.hasText(task.getTaskName())) {
			return "";
		}
		return task.getTaskName().trim();
	}

	private static String sha1Hex(String input) {
		try {
			var md = java.security.MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
