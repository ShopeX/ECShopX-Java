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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordContentDatapassService {

	public Object applyMaskIfNeeded(Object contentTree, boolean datapassBlocked) {
		if (!datapassBlocked || contentTree == null) {
			return contentTree;
		}
		if (!(contentTree instanceof JsonNode root)) {
			return contentTree;
		}
		if (!root.isArray()) {
			return contentTree;
		}
		if (root.isEmpty()) {
			return contentTree;
		}
		ArrayNode copy = (ArrayNode) root.deepCopy();
		for (JsonNode card : copy) {
			if (!card.isObject()) {
				continue;
			}
			JsonNode formdata = card.get("formdata");
			if (formdata == null || !formdata.isArray()) {
				continue;
			}
			maskFormDataArray((ArrayNode) formdata);
		}
		return copy;
	}

	private static void maskFormDataArray(ArrayNode formdata) {
		for (int i = 0; i < formdata.size(); i++) {
			JsonNode line = formdata.get(i);
			if (!line.isObject()) {
				continue;
			}
			ObjectNode obj = (ObjectNode) line;
			String fieldName = "";
			JsonNode fn = obj.get("field_name");
			if (fn != null && !fn.isNull() && fn.isTextual()) {
				fieldName = fn.asText();
			}
			JsonNode ans = obj.get("answer");
			String formatted = formatAnswerCell(ans);
			String masked = maskFormAnswer(fieldName, formatted);
			obj.set("answer", TextNode.valueOf(masked));
		}
	}

	private static String formatAnswerCell(JsonNode answerNode) {
		if (answerNode == null || answerNode.isNull()) {
			return "无";
		}
		if (answerNode.isArray()) {
			List<String> parts = new ArrayList<>();
			for (JsonNode n : answerNode) {
				parts.add(textualAnswerPart(n));
			}
			return String.join(";", parts);
		}
		String s = textualAnswerPart(answerNode);
		return StringUtils.hasText(s) ? s : "无";
	}

	private static String textualAnswerPart(JsonNode n) {
		if (n == null || n.isNull()) {
			return "";
		}
		if (n.isTextual()) {
			return n.asText();
		}
		if (n.isNumber()) {
			return n.asText();
		}
		if (n.isBoolean()) {
			return n.asBoolean() ? "1" : "0";
		}
		return n.toString();
	}

	private static String maskFormAnswer(String fieldName, String answer) {
		if (answer == null) {
			return "";
		}
		return switch (fieldName) {
			case "username" -> DataMasking.maskTruename(answer);
			case "mobile" -> DataMasking.maskMobile(answer);
			case "birthday" -> DataMasking.maskBirthday(answer);
			case "bankcard" -> DataMasking.maskBankcard(answer);
			case "idcard" -> DataMasking.maskIdcard(answer);
			case "address" -> DataMasking.maskDetailedAddress(answer);
			default -> answer;
		};
	}
}
