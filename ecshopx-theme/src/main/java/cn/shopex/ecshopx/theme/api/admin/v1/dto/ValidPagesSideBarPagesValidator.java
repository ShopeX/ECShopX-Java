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

package cn.shopex.ecshopx.theme.api.admin.v1.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

public class ValidPagesSideBarPagesValidator implements ConstraintValidator<ValidPagesSideBarPages, JsonNode> {

	@Override
	public boolean isValid(JsonNode pages, ConstraintValidatorContext ctx) {
		if (pages == null || pages.isNull()) {
			violate(ctx, "{theme.pages_sidebar.pages_required}");
			return false;
		}
		if (pages.isArray()) {
			if (pages.size() == 0) {
				violate(ctx, "{theme.pages_sidebar.pages_required}");
				return false;
			}
			List<String> preview = new ArrayList<>();
			for (JsonNode el : pages) {
				if (el == null || el.isNull()) {
					continue;
				}
				if (el.isTextual()) {
					String t = el.asText() == null ? "" : el.asText().trim();
					if (StringUtils.hasText(t)) {
						preview.add(t);
					}
					continue;
				}
				if (el.isIntegralNumber()) {
					preview.add(String.valueOf(el.longValue()).trim());
					continue;
				}
				violate(ctx, "{theme.pages_sidebar.pages_invalid_element}");
				return false;
			}
			if (preview.isEmpty()) {
				violate(ctx, "{theme.pages_sidebar.pages_required}");
				return false;
			}
			return true;
		}
		if (pages.isTextual()) {
			String text = pages.asText() == null ? "" : pages.asText().trim();
			boolean any = false;
			for (String part : text.split(",")) {
				if (StringUtils.hasText(part.trim())) {
					any = true;
					break;
				}
			}
			if (!any) {
				violate(ctx, "{theme.pages_sidebar.pages_required}");
				return false;
			}
			return true;
		}
		violate(ctx, "{theme.pages_sidebar.pages_invalid_type}");
		return false;
	}

	private static void violate(ConstraintValidatorContext ctx, String template) {
		ctx.disableDefaultConstraintViolation();
		ctx.buildConstraintViolationWithTemplate(template).addPropertyNode("pages").addConstraintViolation();
	}
}
