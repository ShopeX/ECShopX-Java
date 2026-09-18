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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardUpdateValidationService {

	private static final String REQUIRED = "validation.required";

	public void validateNewGiftCardId(Map<String, Object> params) {
		Object raw = params.get("card_id");
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			throw new BadRequestException(KaquanDiscountCardMessages.UPDATE_CARD_NO_ID,
					Map.of("card_id", List.of(REQUIRED)));
		}
	}

	public void validateStandardPatchFields(Map<String, Object> params) {
		boolean colorOk = StringUtils.hasText(DiscountCardParamNormalize.stringVal(params.get("color")));
		boolean descOk = StringUtils.hasText(DiscountCardParamNormalize.stringVal(params.get("description")));
		Object cardIdRaw = params.get("card_id");
		boolean cardIdOk = cardIdRaw != null && StringUtils.hasText(String.valueOf(cardIdRaw).trim());

		Map<String, List<String>> errors = new LinkedHashMap<>();
		if (!colorOk) {
			errors.put("color", List.of(REQUIRED));
		}
		if (!descOk) {
			errors.put("description", List.of(REQUIRED));
		}
		if (!cardIdOk) {
			errors.put("card_id", List.of(REQUIRED));
		}
		if (errors.isEmpty()) {
			return;
		}
		if (errors.size() == 1 && errors.containsKey("card_id")) {
			throw new BadRequestException(KaquanDiscountCardMessages.UPDATE_CARD_NO_ID, errors);
		}
		throw new BadRequestException(KaquanDiscountCardMessages.UPDATE_CARD_ERROR, errors);
	}
}
