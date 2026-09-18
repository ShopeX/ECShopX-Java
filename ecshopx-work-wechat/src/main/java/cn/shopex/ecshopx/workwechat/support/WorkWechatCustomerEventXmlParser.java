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

package cn.shopex.ecshopx.workwechat.support;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class WorkWechatCustomerEventXmlParser {

	public Map<String, Object> toMap(String decryptedXml) {
		Map<String, Object> map = new LinkedHashMap<>();
		if (decryptedXml == null || decryptedXml.isEmpty()) {
			return map;
		}
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(decryptedXml)));
			Element root = doc.getDocumentElement();
			if (root == null) {
				return map;
			}
			NodeList children = root.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node n = children.item(i);
				if (n.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				String name = n.getNodeName();
				String text = textContent((Element) n);
				map.put(name, text);
			}
			return map;
		} catch (Exception e) {
			return map;
		}
	}

	private static String textContent(Element el) {
		NodeList kids = el.getChildNodes();
		if (kids.getLength() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < kids.getLength(); i++) {
			Node c = kids.item(i);
			if (c.getNodeType() == Node.TEXT_NODE || c.getNodeType() == Node.CDATA_SECTION_NODE) {
				sb.append(c.getNodeValue());
			}
		}
		return sb.toString();
	}
}
