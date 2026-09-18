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

package cn.shopex.ecshopx.wechat.api.open;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

final class WechatOpenPlatformCallbackXmlSupport {

	private WechatOpenPlatformCallbackXmlSupport() {
	}

	/**
	 * Store-audit fields mirror WeChat push tags; when absent (e.g. subscribe/unsubscribe), values are
	 * empty strings.
	 */
	record ParsedEventXml(
			String msgType,
			String event,
			String fromUserName,
			String auditId,
			String status,
			String reason,
			String isUpgrade,
			String poiid) {}

	/**
	 * Parses plaintext third-party / official-account push XML. Returns empty when the body is blank,
	 * malformed, or only carries {@code Encrypt} without decrypted fields (encrypted mode not handled here).
	 */
	static Optional<ParsedEventXml> tryParsePlaintextEventXml(String rawBody) {
		if (rawBody == null || rawBody.isBlank()) {
			return Optional.empty();
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setNamespaceAware(false);
			Document doc =
					factory.newDocumentBuilder()
							.parse(new ByteArrayInputStream(rawBody.getBytes(StandardCharsets.UTF_8)));
			NodeList encryptNodes = doc.getElementsByTagName("Encrypt");
			String msgType = elementText(doc, "MsgType");
			if (encryptNodes.getLength() > 0 && msgType.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(
					new ParsedEventXml(
							msgType,
							elementText(doc, "Event"),
							elementText(doc, "FromUserName"),
							firstNonEmptyText(doc, "audit_id", "AuditId"),
							firstNonEmptyText(doc, "status", "Status"),
							firstNonEmptyText(doc, "reason", "Reason"),
							firstNonEmptyText(doc, "is_upgrade", "IsUpgrade"),
							firstNonEmptyText(doc, "poiid", "PoiId", "poi_id")));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private static String firstNonEmptyText(Document doc, String... localNames) {
		for (String name : localNames) {
			String t = elementText(doc, name);
			if (!t.isEmpty()) {
				return t;
			}
		}
		return "";
	}

	private static String elementText(Document doc, String localName) {
		NodeList nl = doc.getElementsByTagName(localName);
		if (nl.getLength() == 0) {
			return "";
		}
		String t = nl.item(0).getTextContent();
		return t == null ? "" : t.trim();
	}
}
