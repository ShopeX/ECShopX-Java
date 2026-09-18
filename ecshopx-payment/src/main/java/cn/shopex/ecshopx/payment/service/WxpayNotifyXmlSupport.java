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

package cn.shopex.ecshopx.payment.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class WxpayNotifyXmlSupport {

	private WxpayNotifyXmlSupport() {
	}

	static Optional<Map<String, String>> tryParseNotifyXml(String rawBody) {
		if (rawBody == null || rawBody.isBlank()) {
			return Optional.empty();
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setNamespaceAware(false);
			Document doc =
					factory.newDocumentBuilder()
							.parse(new ByteArrayInputStream(rawBody.getBytes(StandardCharsets.UTF_8)));
			NodeList children = doc.getDocumentElement().getChildNodes();
			Map<String, String> out = new LinkedHashMap<>();
			for (int i = 0; i < children.getLength(); i++) {
				Node n = children.item(i);
				if (n.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				String name = n.getNodeName();
				if (name == null || name.isBlank()) {
					continue;
				}
				String text = n.getTextContent();
				out.put(name, text == null ? "" : text.trim());
			}
			if (out.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(out);
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
