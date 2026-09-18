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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardActionValidationService {

	public static final String DATE_TYPE_LONG = "DATE_TYPE_LONG";
	public static final String DATE_TYPE_SHORT = "DATE_TYPE_SHORT";

	public void validateCommon(Map<String, Object> input) {
		requireText(input, "card_type", KaquanDiscountCardMessages.CARD_TYPE_REQUIRED);
		requireText(input, "title", KaquanDiscountCardMessages.TITLE_REQUIRED);
		requireText(input, "color", KaquanDiscountCardMessages.COLOR_REQUIRED);
		requireText(input, "description", KaquanDiscountCardMessages.DESCRIPTION_REQUIRED);
		if (input.get("quantity") == null) {
			throw new ResourceException(KaquanDiscountCardMessages.QUANTITY_RANGE_ERROR);
		}
		int q = DiscountCardParamNormalize.parseIntFlexible(input.get("quantity"), Integer.MIN_VALUE);
		if (q < 1 || q > 2147483647) {
			throw new ResourceException(KaquanDiscountCardMessages.QUANTITY_RANGE_ERROR);
		}
		if (input.get("begin_time") == null || !StringUtils.hasText(String.valueOf(input.get("begin_time")).trim())) {
			throw new ResourceException(KaquanDiscountCardMessages.BEGIN_TIME_REQUIRED);
		}
		requireText(input, "date_type", KaquanDiscountCardMessages.DATE_TYPE_REQUIRED);
		if (!DiscountCardParamNormalize.isNumericOptional(input, "least_cost")) {
			throw new ResourceException(KaquanDiscountCardMessages.LEAST_COST_NUMERIC);
		}
		if (!DiscountCardParamNormalize.isNumericOptional(input, "reduce_cost")) {
			throw new ResourceException(KaquanDiscountCardMessages.REDUCE_COST_NUMERIC);
		}
		if (!DiscountCardParamNormalize.isNumericOptional(input, "discount")) {
			throw new ResourceException(KaquanDiscountCardMessages.DISCOUNT_NUMERIC);
		}
	}

	public void validateFixTermBeginTime(Map<String, Object> input) {
		if (!"DATE_TYPE_FIX_TERM".equals(DiscountCardParamNormalize.stringVal(input.get("date_type")))) {
			return;
		}
		if (!DiscountCardParamNormalize.isNumericString(input.get("begin_time"))) {
			throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_VALIDITY_ERROR);
		}
	}

	public void validateNewGiftDateAndFields(Map<String, Object> input) {
		String dateType = DiscountCardParamNormalize.stringVal(input.get("date_type"));
		if (DATE_TYPE_LONG.equals(dateType)) {
			if (input.get("send_begin_time") == null || !DiscountCardParamNormalize.isNumericString(input.get("send_begin_time"))) {
				throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_VALIDITY_ERROR);
			}
			if (input.get("begin_time") == null || !DiscountCardParamNormalize.isNumericString(input.get("begin_time"))) {
				throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_VALIDITY_ERROR);
			}
			if (input.get("days") == null || !DiscountCardParamNormalize.isNumericString(input.get("days"))) {
				throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_VALIDITY_ERROR);
			}
		} else if (DATE_TYPE_SHORT.equals(dateType)) {
			if (input.get("send_begin_time") == null || !DiscountCardParamNormalize.isNumericString(input.get("send_begin_time"))) {
				throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_VALIDITY_ERROR);
			}
			if (input.get("send_end_time") == null || !DiscountCardParamNormalize.isNumericString(input.get("send_end_time"))) {
				throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_VALIDITY_ERROR);
			}
			if (input.get("begin_time") == null || !DiscountCardParamNormalize.isNumericString(input.get("begin_time"))) {
				throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_VALIDITY_ERROR);
			}
			if (input.get("end_time") == null || !DiscountCardParamNormalize.isNumericString(input.get("end_time"))) {
				throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_VALIDITY_ERROR);
			}
		} else {
			throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_TYPE_ERROR);
		}
		if (input.get("lock_time") == null || !DiscountCardParamNormalize.isNumericString(input.get("lock_time"))) {
			throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_GENERAL_ERROR);
		}
		if (input.get("receive") == null || !StringUtils.hasText(String.valueOf(input.get("receive")).trim())) {
			throw new ResourceException(KaquanDiscountCardMessages.ADD_CARD_GENERAL_ERROR);
		}
	}

	private static void requireText(Map<String, Object> input, String key, String msg) {
		Object v = input.get(key);
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			throw new ResourceException(msg);
		}
	}
}
