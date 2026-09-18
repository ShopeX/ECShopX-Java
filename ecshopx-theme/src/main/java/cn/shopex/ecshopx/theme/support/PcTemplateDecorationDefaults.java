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

package cn.shopex.ecshopx.theme.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Default PC decoration DSL payloads aligned with shop {@code sp-web-decoration} fallbacks, so empty DB
 * environments can open the editor the same way as PHP demo tenants that already have saved rows.
 */
@Component
public class PcTemplateDecorationDefaults {

	private final ObjectMapper objectMapper;

	public PcTemplateDecorationDefaults(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String defaultHeaderConfigJson() {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("pageType", "header");
		root.put("pageId", "header");
		ObjectNode sections = root.putObject("sections");
		ObjectNode header = sections.putObject("header");
		header.put("id", "header");
		header.put("type", "header");
		header.put("title", "头部");
		header.put("disabled", false);
		ObjectNode settings = header.putObject("settings");
		settings.put("color_mode", "light");
		settings.put("full_width", true);
		settings.put("sticky_header_type", "none");
		settings.put("padding_top", "xxs");
		settings.put("padding_bottom", "xxs");
		settings.put("menu_color_style", "pure");
		settings.put("menu_type_desktop", "dropdown");
		settings.put("show_line_separator", true);
		settings.put("color_scheme", "scheme-1");
		settings.put("layout", "middle");
		settings.put("margin_bottom", 0);
		settings.put("logo_position", "center");
		settings.put("mobile_logo_position", "center");
		settings.put("menu_color_mode", "dark");
		settings.put("menu_color_scheme", "scheme-1");
		settings.put("enable_country_selector", true);
		settings.put("enable_language_selector", true);
		settings.put("enable_customer_avatar", true);
		header.putObject("blocks");
		header.putArray("blockOrder");
		header.putArray("block_order");
		root.putArray("order").add("header");
		return write(root);
	}

	public String defaultFooterConfigJson() {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("pageType", "footer");
		root.put("pageId", "footer");
		ObjectNode sections = root.putObject("sections");
		ObjectNode footer = sections.putObject("footer");
		footer.put("id", "footer");
		footer.put("type", "footer");
		footer.put("title", "页脚");
		footer.put("disabled", false);
		ObjectNode settings = footer.putObject("settings");
		settings.put("content_alignment", "center");
		settings.put("title", "");
		settings.put("copyright", "");
		settings.put("newsletter_heading", "");
		settings.put("color_mode", "dark");
		settings.put("margin_top", 48);
		settings.put("full_width", false);
		settings.put("padding_top", "m");
		settings.put("padding_bottom", "xs");
		settings.put("color_scheme", "scheme-3");
		settings.put("show_social", true);
		settings.put("payment_enable", true);
		settings.put("newsletter_enable", false);
		settings.put("enable_country_selector", true);
		settings.put("enable_language_selector", true);
		settings.put("enable_brand_information", true);
		settings.put("show_policy", true);
		footer.putObject("blocks");
		footer.putArray("blockOrder");
		footer.putArray("block_order");
		root.putArray("order").add("footer");
		return write(root);
	}

	/**
	 * Page content row {@code params}: single DSL block (same shape as {@code saveTemplateContent} element).
	 */
	public String defaultPageConfigJson(String pageType, long themePcTemplateId) {
		String normalized =
				pageType == null || pageType.isBlank() || "index".equals(pageType) || "home".equals(pageType)
						? "home"
						: pageType.trim();
		ObjectNode root = objectMapper.createObjectNode();
		root.put("type", "ECX_SP_WEB_DECORATION_DSL_V1");
		root.put("pageType", normalized);
		root.put("pageId", String.valueOf(themePcTemplateId));
		root.putObject("sections");
		root.putArray("order");
		ObjectNode meta = root.putObject("meta");
		meta.put("version", 1);
		meta.put("scene", "1001");
		return write(root);
	}

	private String write(ObjectNode node) {
		try {
			return objectMapper.writeValueAsString(node);
		} catch (Exception ex) {
			throw new IllegalStateException("build default decoration dsl failed", ex);
		}
	}
}
