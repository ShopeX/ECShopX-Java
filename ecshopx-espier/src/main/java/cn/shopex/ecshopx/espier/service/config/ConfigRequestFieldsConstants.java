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

package cn.shopex.ecshopx.espier.service.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置请求字段：模块类型与字段类型常量（与业务约定一致）。
 */
public final class ConfigRequestFieldsConstants {

	public static final String SETTING_SWITCH_FIRST_AUTH_FORCE_VALIDATION = "switch_first_auth_force_validation";

	public static final int MODULE_TYPE_MEMBER_INFO = 1;
	public static final int MODULE_TYPE_CHIEF_INFO = 2;

	public static final Map<Integer, String> MODULE_TYPE_MAP;

	public static final int FIELD_TYPE_TEXT = 1;
	public static final int FIELD_TYPE_NUMBER = 2;
	public static final int FIELD_TYPE_DATE = 3;
	public static final int FIELD_TYPE_RADIO = 4;
	public static final int FIELD_TYPE_CHECKBOX = 5;
	public static final int FIELD_TYPE_MOBILE = 6;
	public static final int FIELD_TYPE_IMAGE = 7;

	public static final Map<Integer, String> FIELD_TYPE_MAP;

	public static final Map<Integer, String> FIELD_TYPE_ELEMENT_MAP;

	public static final int SWITCH_COLUMN_IS_OPEN = 1;
	public static final int SWITCH_COLUMN_IS_REQUIRED = 2;
	public static final int SWITCH_COLUMN_IS_EDIT = 3;
	public static final int SWITCH_COLUMN_IS_PRESET = 4;

	public static final Map<Integer, String> SWITCH_COLUMN_MAP;

	public static final Map<String, Integer> REQUEST_FIELD_SETTING_DEFAULTS;

	static {
		Map<Integer, String> module = new LinkedHashMap<>();
		module.put(MODULE_TYPE_MEMBER_INFO, "会员个人信息");
		module.put(MODULE_TYPE_CHIEF_INFO, "社区团购团长信息");
		MODULE_TYPE_MAP = Collections.unmodifiableMap(module);

		Map<Integer, String> field = new LinkedHashMap<>();
		field.put(FIELD_TYPE_TEXT, "文本");
		field.put(FIELD_TYPE_NUMBER, "数字");
		field.put(FIELD_TYPE_DATE, "日期");
		field.put(FIELD_TYPE_RADIO, "单选项");
		field.put(FIELD_TYPE_CHECKBOX, "多选项");
		field.put(FIELD_TYPE_MOBILE, "手机号");
		field.put(FIELD_TYPE_IMAGE, "图片");
		FIELD_TYPE_MAP = Collections.unmodifiableMap(field);

		Map<Integer, String> element = new LinkedHashMap<>();
		element.put(FIELD_TYPE_TEXT, "input");
		element.put(FIELD_TYPE_NUMBER, "numeric");
		element.put(FIELD_TYPE_DATE, "date");
		element.put(FIELD_TYPE_RADIO, "select");
		element.put(FIELD_TYPE_CHECKBOX, "checkbox");
		element.put(FIELD_TYPE_MOBILE, "mobile");
		element.put(FIELD_TYPE_IMAGE, "image");
		FIELD_TYPE_ELEMENT_MAP = Collections.unmodifiableMap(element);

		Map<Integer, String> switchCol = new LinkedHashMap<>();
		switchCol.put(SWITCH_COLUMN_IS_OPEN, "is_open");
		switchCol.put(SWITCH_COLUMN_IS_REQUIRED, "is_required");
		switchCol.put(SWITCH_COLUMN_IS_EDIT, "is_edit");
		switchCol.put(SWITCH_COLUMN_IS_PRESET, "is_preset");
		SWITCH_COLUMN_MAP = Collections.unmodifiableMap(switchCol);

		Map<String, Integer> requestFieldSettingDefaults = new LinkedHashMap<>();
		requestFieldSettingDefaults.put(SETTING_SWITCH_FIRST_AUTH_FORCE_VALIDATION, 0);
		REQUEST_FIELD_SETTING_DEFAULTS = Collections.unmodifiableMap(requestFieldSettingDefaults);
	}

	private ConfigRequestFieldsConstants() {
	}
}
