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

package cn.shopex.ecshopx.distribution.api.admin.v1.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RelDistributorIdDeserializer extends JsonDeserializer<List<Long>> {

	@Override
	public List<Long> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonToken t = p.currentToken();
		if (t == JsonToken.VALUE_NULL) {
			return null;
		}
		if (t == JsonToken.START_ARRAY) {
			ArrayList<Long> out = new ArrayList<>();
			while (p.nextToken() != JsonToken.END_ARRAY) {
				JsonToken et = p.currentToken();
				if (et == JsonToken.VALUE_NULL) {
					continue;
				}
				if (et == JsonToken.VALUE_NUMBER_INT || et == JsonToken.VALUE_NUMBER_FLOAT) {
					out.add(p.getLongValue());
					continue;
				}
				if (et == JsonToken.VALUE_STRING) {
					String s = p.getText();
					if (s == null) {
						continue;
					}
					s = s.trim();
					if (s.isEmpty()) {
						continue;
					}
					try {
						out.add(Long.parseLong(s));
					} catch (NumberFormatException ex) {
						throw InvalidFormatException.from(p, "not a valid long value", s, Long.class);
					}
					continue;
				}
				throw InvalidFormatException.from(p, "unsupported token in id array", p.getText(), Long.class);
			}
			return out;
		}
		if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
			return List.of(p.getLongValue());
		}
		if (t == JsonToken.VALUE_STRING) {
			String s = p.getText();
			if (s == null) {
				return null;
			}
			s = s.trim();
			try {
				return List.of(Long.parseLong(s));
			} catch (NumberFormatException ex) {
				throw InvalidFormatException.from(p, "not a valid long value", s, Long.class);
			}
		}
		throw InvalidFormatException.from(p, "id must be a number, string, or array", null, List.class);
	}
}
