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

package cn.shopex.ecshopx.thirdparty.service.dm;

import cn.shopex.ecshopx.common.kaquan.port.DmCardTemplateMessageNotifyKaquanPort;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DmMessageNotifyWebhookService {

	private static final Logger log = LoggerFactory.getLogger(DmMessageNotifyWebhookService.class);

	/** Topics that should reach standard-card creation (aligned with subscribe job mapping names). */
	private static final Set<String> CARD_TEMPLATE_SYNC_TOPICS =
			Set.of("sync_card_template_create", "sync_card_template_modify", "sync_card_template_delete");

	private final DmCardTemplateMessageNotifyKaquanPort kaquanPort;

	public DmMessageNotifyWebhookService(DmCardTemplateMessageNotifyKaquanPort kaquanPort) {
		this.kaquanPort = kaquanPort;
	}

	public Map<String, Object> handle(long companyId, Map<String, Object> mergedInput, HttpServletRequest request) {
		verifySignaturePlaceholder(mergedInput, request);
		Map<String, Object> forwarded = copyWithNormalizedCardTemplateTopic(mergedInput);
		String topic = resolveTopic(forwarded);
		if (!isCardTemplateSyncTopic(topic)) {
			log.debug("dm messageNotify ignored topic={}", topic);
			Map<String, Object> noop = new LinkedHashMap<>();
			noop.put("status", Boolean.TRUE);
			noop.put("noop", Boolean.TRUE);
			return noop;
		}
		String authorizerAppid = resolveAuthorizerAppid(mergedInput, request);
		Map<String, Object> created = kaquanPort.handle(companyId, forwarded, authorizerAppid);
		Map<String, Object> out = new LinkedHashMap<>(created);
		out.put("status", Boolean.TRUE);
		return out;
	}

	private static void verifySignaturePlaceholder(Map<String, Object> mergedInput, HttpServletRequest request) {
		// Signing secret wiring is environment-specific; accept all until configuration is available.
		if (log.isTraceEnabled()) {
			log.trace(
					"dm messageNotify signature check skipped (headers={}, keys={})",
					request.getHeaderNames(),
					mergedInput != null ? mergedInput.keySet() : null);
		}
	}

	private static String resolveAuthorizerAppid(Map<String, Object> merged, HttpServletRequest request) {
		Object fromBody = merged != null ? merged.get("authorizer_appid") : null;
		if (fromBody != null && StringUtils.hasText(fromBody.toString())) {
			return fromBody.toString().trim();
		}
		String header = request.getHeader("Authorizer-Appid");
		if (StringUtils.hasText(header)) {
			return header.trim();
		}
		return "";
	}

	private static String resolveTopic(Map<String, Object> merged) {
		if (merged == null) {
			return "";
		}
		Object t = firstNonNull(merged.get("topic"), merged.get("Topic"), merged.get("msg_type"), merged.get("MsgType"));
		return t != null ? t.toString().trim() : "";
	}

	/**
	 * DM may send either a normalized topic string or a split {@code topic=card} + {@code event=cardTemplateModify}
	 * envelope. Normalize before whitelist checks so modify payloads are not dropped as unknown topics.
	 */
	private static Map<String, Object> copyWithNormalizedCardTemplateTopic(Map<String, Object> mergedInput) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
		if (mergedInput != null) {
			copy.putAll(mergedInput);
		}
		String normalized = normalizeCardTemplateSyncTopic(resolveTopic(copy), resolveDmEvent(copy));
		if (normalized != null) {
			copy.put("topic", normalized);
		}
		return copy;
	}

	private static String resolveDmEvent(Map<String, Object> merged) {
		if (merged == null) {
			return "";
		}
		Object e = firstOf(
				merged.get("event"),
				merged.get("Event"),
				merged.get("msg_event"),
				merged.get("MsgEvent"),
				merged.get("msg_Event"));
		return e != null ? e.toString().trim() : "";
	}

	private static Object firstOf(Object a, Object b, Object c, Object d, Object e) {
		if (a != null) {
			return a;
		}
		if (b != null) {
			return b;
		}
		if (c != null) {
			return c;
		}
		if (d != null) {
			return d;
		}
		return e;
	}

	/**
	 * @return canonical lowercase topic for card-template sync, or {@code null} if no normalization applies
	 */
	private static String normalizeCardTemplateSyncTopic(String topicRaw, String eventRaw) {
		if (!StringUtils.hasText(topicRaw)) {
			return null;
		}
		String t = topicRaw.trim();
		if ("sync_card_template_delete".equalsIgnoreCase(t)) {
			return "sync_card_template_delete";
		}
		if ("sync_card_template_create".equalsIgnoreCase(t)) {
			return "sync_card_template_create";
		}
		if ("sync_card_template_modify".equalsIgnoreCase(t)) {
			return "sync_card_template_modify";
		}
		if ("card".equalsIgnoreCase(t) && StringUtils.hasText(eventRaw)) {
			String e = eventRaw.trim();
			if ("cardtemplatedelete".equalsIgnoreCase(e)) {
				return "sync_card_template_delete";
			}
			if ("cardtemplatemodify".equalsIgnoreCase(e)) {
				return "sync_card_template_modify";
			}
		}
		return null;
	}

	private static Object firstNonNull(Object a, Object b, Object c, Object d) {
		if (a != null) {
			return a;
		}
		if (b != null) {
			return b;
		}
		if (c != null) {
			return c;
		}
		return d;
	}

	private static boolean isCardTemplateSyncTopic(String raw) {
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		return CARD_TEMPLATE_SYNC_TOPICS.contains(raw.trim().toLowerCase(Locale.ROOT));
	}
}
