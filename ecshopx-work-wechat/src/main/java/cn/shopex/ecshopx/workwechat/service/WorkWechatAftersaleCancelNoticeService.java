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
import cn.shopex.ecshopx.workwechat.dispatch.SendAfterSaleCancelNoticeWorker;
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
import com.fasterxml.jackson.databind.JsonNode;
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
public class WorkWechatAftersaleCancelNoticeService implements SendAfterSaleCancelNoticeWorker {

	static final String TEMPLATE_ID = "afterSaleCancel";

	private static final String SQL_AFTERSALES =
			"SELECT order_id, distributor_id, shop_id, salesman_id, reason, refund_fee FROM aftersales "
					+ "WHERE company_id = ? AND aftersales_bn = ? LIMIT 1";

	private static final String SQL_DISTRIBUTOR_OPERATORS =
			"SELECT TRIM(r.work_userid) AS work_userid, r.operator_id, o.distributor_ids, IFNULL(o.is_disable, 0) AS is_disable "
					+ "FROM distributor_work_wechat_rel r INNER JOIN operators o ON o.operator_id = r.operator_id AND "
					+ "o.company_id = r.company_id WHERE r.company_id = ? AND TRIM(IFNULL(r.work_userid, '')) <> ''";

	private final WorkWechatMessageTemplateMapper workWechatMessageTemplateMapper;
	private final WorkWechatMessageManagerTemplateMapper workWechatMessageManagerTemplateMapper;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final WorkWechatMessageMapper workWechatMessageMapper;
	private final WorkWechatAccessTokenProvider workWechatAccessTokenProvider;
	private final WorkWechatCorpMessageSendPort workWechatCorpMessageSendPort;
	private final WorkWechatConfigService workWechatConfigService;
	private final ObjectMapper objectMapper;
	private final JdbcTemplate jdbcTemplate;

	public WorkWechatAftersaleCancelNoticeService(
			WorkWechatMessageTemplateMapper workWechatMessageTemplateMapper,
			WorkWechatMessageManagerTemplateMapper workWechatMessageManagerTemplateMapper,
			WorkWechatRelMapper workWechatRelMapper,
			WorkWechatMessageMapper workWechatMessageMapper,
			WorkWechatAccessTokenProvider workWechatAccessTokenProvider,
			WorkWechatCorpMessageSendPort workWechatCorpMessageSendPort,
			WorkWechatConfigService workWechatConfigService,
			ObjectMapper objectMapper,
			JdbcTemplate jdbcTemplate) {
		this.workWechatMessageTemplateMapper = workWechatMessageTemplateMapper;
		this.workWechatMessageManagerTemplateMapper = workWechatMessageManagerTemplateMapper;
		this.workWechatRelMapper = workWechatRelMapper;
		this.workWechatMessageMapper = workWechatMessageMapper;
		this.workWechatAccessTokenProvider = workWechatAccessTokenProvider;
		this.workWechatCorpMessageSendPort = workWechatCorpMessageSendPort;
		this.workWechatConfigService = workWechatConfigService;
		this.objectMapper = objectMapper;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean execute(long companyId, long aftersalesBn) {
		return afterSaleCancel(companyId, aftersalesBn);
	}

	public boolean afterSaleCancel(long companyId, long aftersalesBn) {
		if (companyId <= 0L || aftersalesBn <= 0L) {
			return false;
		}

		WorkWechatMessageTemplate shopTpl =
				workWechatMessageTemplateMapper.selectOne(
						new LambdaQueryWrapper<WorkWechatMessageTemplate>()
								.eq(WorkWechatMessageTemplate::getCompanyId, companyId)
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

		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(SQL_AFTERSALES, companyId, aftersalesBn);
		if (rows == null || rows.isEmpty()) {
			return false;
		}
		Map<String, Object> aftersalesRow = rows.get(0);

		long distributorId = longCell(aftersalesRow.get("distributor_id"));
		long salesmanId = longCell(aftersalesRow.get("salesman_id"));

		Set<Recipient> recipients = new LinkedHashSet<>();

		if (salesmanId > 0L) {
			List<WorkWechatRel> relRows =
					workWechatRelMapper.selectList(
							new LambdaQueryWrapper<WorkWechatRel>()
									.eq(WorkWechatRel::getCompanyId, companyId)
									.eq(WorkWechatRel::getSalespersonId, salesmanId));
			if (relRows != null) {
				for (WorkWechatRel rel : relRows) {
					String wu = rel.getWorkUserid() == null ? "" : rel.getWorkUserid().trim();
					if (StringUtils.hasText(wu)) {
						recipients.add(new Recipient(wu, 0L));
					}
				}
			}
		}

		List<Map<String, Object>> opRows = jdbcTemplate.queryForList(SQL_DISTRIBUTOR_OPERATORS, companyId);
		if (opRows != null) {
			for (Map<String, Object> row : opRows) {
				int disabled = intCell(row.get("is_disable"));
				if (disabled != 0) {
					continue;
				}
				String distributorIdsJson = stringify(row.get("distributor_ids"));
				if (!operatorManagesDistributor(distributorIdsJson, distributorId)) {
					continue;
				}
				String wu = stringify(row.get("work_userid")).trim();
				if (!StringUtils.hasText(wu)) {
					continue;
				}
				long operatorId = longCell(row.get("operator_id"));
				recipients.add(new Recipient(wu, operatorId));
			}
		}

		if (recipients.isEmpty()) {
			return false;
		}

		Optional<String> tokenOpt = workWechatAccessTokenProvider.getAccessToken(companyId);
		if (tokenOpt.isEmpty()) {
			return false;
		}
		Map<String, Object> cfg = workWechatConfigService.loadParsedWorkWechatConfig(companyId);
		int agentId = resolveAgentId(cfg);
		if (agentId <= 0) {
			return false;
		}

		Map<String, Object> placeholderCtx = new LinkedHashMap<>(aftersalesRow);
		placeholderCtx.put("company_id", companyId);
		String title = expandPlaceholders(shopTpl.getTitle(), placeholderCtx, aftersalesBn);
		String description = expandPlaceholders(shopTpl.getDescription(), placeholderCtx, aftersalesBn);
		String composedContent = title + "\n" + description;

		int ts = (int) (System.currentTimeMillis() / 1000L);
		boolean anySent = false;
		for (Recipient rc : recipients) {
			String json = buildTextCardPayload(agentId, rc.workUserid(), title, description);
			if (!StringUtils.hasText(json)) {
				continue;
			}
			workWechatCorpMessageSendPort.postMessageSend(tokenOpt.get(), json);
			insertNoticeRecord(companyId, distributorId, rc.operatorId(), composedContent, ts);
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
					"work wechat aftersale cancel notice json failed touser={} title.len={}",
					touser,
					title == null ? 0 : title.length(),
					e);
			return "";
		}
	}

	private boolean operatorManagesDistributor(String distributorIdsJson, long distributorId) {
		if (distributorId <= 0L || !StringUtils.hasText(distributorIdsJson)) {
			return false;
		}
		String raw = distributorIdsJson.trim();
		if ("[]".equals(raw)) {
			return false;
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root == null || !root.isArray()) {
				return false;
			}
			for (JsonNode n : root) {
				if (n.isNumber() && n.longValue() == distributorId) {
					return true;
				}
				if (n.isObject() && n.path("distributor_id").asLong(0L) == distributorId) {
					return true;
				}
				if (n.isTextual()) {
					try {
						if (Long.parseLong(n.asText().trim()) == distributorId) {
							return true;
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}
		} catch (Exception e) {
			log.debug("parse distributor_ids failed raw.len={}", raw.length(), e);
		}
		return false;
	}

	private static String expandPlaceholders(String template, Map<String, Object> ctx, long aftersalesBn) {
		String s = template == null ? "" : template;
		for (Map.Entry<String, Object> e : ctx.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			String replacement = val == null ? "" : String.valueOf(val);
			s = s.replace("{" + key + "}", replacement);
		}
		s = s.replace("{aftersales_bn}", String.valueOf(aftersalesBn));
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

	private static long longCell(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intCell(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringify(Object o) {
		return o == null ? "" : String.valueOf(o);
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
