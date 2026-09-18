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

package cn.shopex.ecshopx.shopmenuborder.service.dto;

import java.util.List;

/**
 * Border POST /api/v1/shopmenu 创建菜单入参（Controller 解析后传入 Service）。
 */
public record BorderShopMenuCreateInput(
		Long shopmenuId,
		Integer companyId,
		Long pid,
		Integer sort,
		Integer version,
		String aliasName,
		List<String> menuTypeNames,
		boolean hasName,
		String name,
		boolean hasUrl,
		String url,
		boolean hasApis,
		String apis,
		boolean hasIcon,
		String icon,
		boolean hasIsShow,
		Object isShowRaw,
		boolean hasIsMenu,
		Object isMenuRaw,
		boolean hasDisabled,
		Object disabledRaw,
		/** 请求语种（如 {@code zh-CN}），用于同步 outside 多语言表 */
		String requestLang) {}
