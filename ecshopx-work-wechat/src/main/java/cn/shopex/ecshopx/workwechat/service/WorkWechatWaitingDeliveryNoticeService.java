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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.thirdparty.service.workwechat.WorkWechatAccessTokenProvider;
import cn.shopex.ecshopx.thirdparty.service.workwechat.WorkWechatCorpMessageSendPort;
import cn.shopex.ecshopx.workwechat.dispatch.SendWaitingDeliveryNoticeWorker;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatMessage;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatMessageManagerTemplate;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatMessageTemplate;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatMessageManagerTemplateMapper;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatMessageMapper;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatMessageTemplateMapper;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkWechatWaitingDeliveryNoticeService implements SendWaitingDeliveryNoticeWorker {

	static final String TEMPLATE_ID = "waitingDeliveryNotice";

	private static final String SQL_NORMAL_ORDER =
			"SELECT order_id, distributor_id, salesman_id, shop_id, title FROM orders_normal_orders "
					+ "WHERE company_id = ? AND order_id = ? LIMIT 1";

	private static final String SQL_SALESPERSON_IDS_FOR_DISTRIBUTOR_SHOP =
			"SELECT DISTINCT salesperson_id FROM shop_rel_salesperson "
					+ "WHERE company_id = ? AND store_type = 'distributor' AND shop_id = ? "
					+ "AND IFNULL(salesperson_id, 0) > 0";

	private final WorkWechatMessageTemplateMapper workWechatMessageTemplateMapper;
	private final WorkWechatMessageManagerTemplateMapper workWechatMessageManagerTemplateMapper;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final WorkWechatMessageMapper workWechatMessageMapper;
	private final WorkWechatAccessTokenProvider workWechatAccessTokenProvider;
	private final WorkWechatCorpMessageSendPort workWechatCorpMessageSendPort;
	private final WorkWechatConfigService workWechatConfigService;
	private final JdbcTemplate jdbcTemplate;

	public WorkWechatWaitingDeliveryNoticeService(
			WorkWechatMessageTemplateMapper workWechatMessageTemplateMapper,
			WorkWechatMessageManagerTemplateMapper workWechatMessageManagerTemplateMapper,
			WorkWechatRelMapper workWechatRelMapper,
			WorkWechatMessageMapper workWechatMessageMapper,
			WorkWechatAccessTokenProvider workWechatAccessTokenProvider,
			WorkWechatCorpMessageSendPort workWechatCorpMessageSendPort,
			WorkWechatConfigService workWechatConfigService,
			JdbcTemplate jdbcTemplate) {
		this.workWechatMessageTemplateMapper = workWechatMessageTemplateMapper;
		this.workWechatMessageManagerTemplateMapper = workWechatMessageManagerTemplateMapper;
		this.workWechatRelMapper = workWechatRelMapper;
		this.workWechatMessageMapper = workWechatMessageMapper;
		this.workWechatAccessTokenProvider = workWechatAccessTokenProvider;
		this.workWechatCorpMessageSendPort = workWechatCorpMessageSendPort;
		this.workWechatConfigService = workWechatConfigService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean execute(String companyId, String orderId, String distributorId) {
		long companyIdLong = parsePositiveLong(companyId);
		long orderIdLong = parsePositiveLong(orderId);
		long distributorIdLong = parsePositiveLong(distributorId);
		if (companyIdLong <= 0L || orderIdLong <= 0L || distributorIdLong <= 0L) {
			return false;
		}

		WorkWechatMessageTemplate shopTpl =
				workWechatMessageTemplateMapper.selectOne(
						new LambdaQueryWrapper<WorkWechatMessageTemplate>()
								.eq(WorkWechatMessageTemplate::getCompanyId, companyIdLong)
								.eq(WorkWechatMessageTemplate::getTemplateId, TEMPLATE_ID)
								.last("LIMIT 1"));
		if (shopTpl == null || Boolean.TRUE.equals(shopTpl.getDisabled())) {
			return false;
		}

		WorkWechatMessageManagerTemplate mgrTpl =
				workWechatMessageManagerTemplateMapper.selectOne(
						new LambdaQueryWrapper<WorkWechatMessageManagerTemplate>()
								.eq(WorkWechatMessageManagerTemplate::getTemplateId, TEMPLATE_ID)
								.last("LIMIT 1"));
		if (mgrTpl != null && Boolean.TRUE.equals(mgrTpl.getDisabled())) {
			return false;
		}

		List<Map<String, Object>> orderRows =
				jdbcTemplate.queryForList(SQL_NORMAL_ORDER, companyIdLong, orderIdLong);
		if (orderRows == null || orderRows.isEmpty()) {
			return false;
		}
		Map<String, Object> orderRow = orderRows.get(0);

		List<Long> salespersonIds =
				jdbcTemplate.query(
						SQL_SALESPERSON_IDS_FOR_DISTRIBUTOR_SHOP,
						(rs, i) -> rs.getLong("salesperson_id"),
						companyIdLong,
						distributorIdLong);
		if (salespersonIds == null) {
			salespersonIds = List.of();
		}

		Set<Recipient> recipients = new LinkedHashSet<>();
		for (Long spId : new LinkedHashSet<>(salespersonIds)) {
			if (spId == null || spId <= 0L) {
				continue;
			}
			List<WorkWechatRel> relRows =
					workWechatRelMapper.selectList(
							new LambdaQueryWrapper<WorkWechatRel>()
									.eq(WorkWechatRel::getCompanyId, companyIdLong)
									.eq(WorkWechatRel::getSalespersonId, spId));
			if (relRows != null) {
				for (WorkWechatRel rel : relRows) {
					String wu = rel.getWorkUserid() == null ? "" : rel.getWorkUserid().trim();
					if (StringUtils.hasText(wu)) {
						recipients.add(new Recipient(wu, 0L));
					}
				}
			}
		}

		if (recipients.isEmpty()) {
			return false;
		}

		Optional<String> tokenOpt = workWechatAccessTokenProvider.getAccessToken(companyIdLong);
		if (tokenOpt.isEmpty()) {
			return false;
		}
		Map<String, Object> cfg = workWechatConfigService.loadParsedWorkWechatConfig(companyIdLong);
		int agentId = resolveAgentId(cfg);
		if (agentId <= 0) {
			return false;
		}

		Map<String, Object> placeholderCtx = new LinkedHashMap<>(orderRow);
		placeholderCtx.put("company_id", companyIdLong);
		placeholderCtx.put("order_id", orderIdLong);
		String title = expandPlaceholders(shopTpl.getTitle(), placeholderCtx);
		String description = expandPlaceholders(shopTpl.getDescription(), placeholderCtx);
		String composedContent = title + "\n" + description;

		int ts = (int) (System.currentTimeMillis() / 1000L);
		boolean anySent = false;
		for (Recipient rc : recipients) {
			String json = buildTextCardPayload(agentId, rc.workUserid(), title, description);
			if (!StringUtils.hasText(json)) {
				continue;
			}
			workWechatCorpMessageSendPort.postMessageSend(tokenOpt.get(), json);
			insertNoticeRecord(companyIdLong, distributorIdLong, rc.operatorId(), composedContent, ts);
			anySent = true;
		}
		return anySent;
	}

	private void insertNoticeRecord(
			long companyId, long distributorId, long operatorId, String content, int ts) {
		WorkWechatMessage msg = new WorkWechatMessage();
		msg.setCompanyId(companyId);
		msg.setDistributorId(distributorId);
		msg.setOperatorId(operatorId);
		msg.setMsgType(1);
		msg.setContent(content);
		msg.setAddTime(ts);
		msg.setUpTime(ts);
		msg.setIsRead(0);
		workWechatMessageMapper.insert(msg);
	}

	private static String buildTextCardJson(int agentId, String touser, String title, String description)
			throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		Map<String, Object> textcard = new LinkedHashMap<>();
		textcard.put("title", title);
		textcard.put("description", description);
		textcard.put("url", "");
		textcard.put("btntxt", "详情");

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("touser", touser);
		root.put("msgtype", "textcard");
		root.put("agentid", agentId);
		root.put("textcard", textcard);
		return om.writeValueAsString(root);
	}

	private String buildTextCardPayload(int agentId, String touser, String title, String description) {
		try {
			return buildTextCardJson(agentId, touser, title, description);
		} catch (JsonProcessingException e) {
			log.debug(
					"work wechat waiting-delivery notice json failed touser={} title.len={}",
					touser,
					title == null ? 0 : title.length(),
					e);
			return "";
		}
	}

	private static String expandPlaceholders(String template, Map<String, Object> ctx) {
		String s = template == null ? "" : template;
		for (Map.Entry<String, Object> e : ctx.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			String replacement = val == null ? "" : String.valueOf(val);
			s = s.replace("{" + key + "}", replacement);
		}
		return s;
	}

	private static int resolveAgentId(Map<String, Object> config) {
		Object agentsObj = config == null ? null : config.get("agents");
		if (!(agentsObj instanceof Map<?, ?> agents)) {
			return 0;
		}
		Object appObj = agents.get("app");
		if (!(appObj instanceof Map<?, ?> app)) {
			return 0;
		}
		Object agentRaw = app.get("agent_id");
		if (agentRaw instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(agentRaw == null ? "" : agentRaw).trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long parsePositiveLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private record Recipient(String workUserid, long operatorId) {

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Recipient r && Objects.equals(workUserid, r.workUserid);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(workUserid);
		}
	}
}
