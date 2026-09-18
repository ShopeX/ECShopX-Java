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

package cn.shopex.ecshopx.theme.api.admin.v1.dto.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.math.BigDecimal;

public class CopySourcePagesTemplateIdDeserializer extends JsonDeserializer<Long> {

	@Override
	public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonToken t = p.currentToken();
		if (t == JsonToken.VALUE_NUMBER_INT) {
			return p.getLongValue();
		}
		if (t == JsonToken.VALUE_NUMBER_FLOAT) {
			BigDecimal bd = p.getDecimalValue();
			bd = bd.stripTrailingZeros();
			if (bd.scale() > 0) {
				return null;
			}
			try {
				return bd.longValueExact();
			} catch (ArithmeticException ex) {
				return null;
			}
		}
		if (t == JsonToken.VALUE_STRING) {
			String s = p.getValueAsString();
			if (s == null) {
				return null;
			}
			s = s.trim();
			if (s.isEmpty()) {
				return null;
			}
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		if (t == JsonToken.VALUE_NULL) {
			return null;
		}
		return null;
	}
}
