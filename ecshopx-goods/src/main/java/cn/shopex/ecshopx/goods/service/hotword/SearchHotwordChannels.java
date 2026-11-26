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

package cn.shopex.ecshopx.goods.service.hotword;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 热词展示渠道（ECX-11390 F-005/AC-006）。
 *
 * <p>一条热词只归属一个渠道（Admin 单选）：库列与接口字段均为 {@code channel}，
 * 值为 {@code mobile} 或 {@code pc}。前台查询按请求的 {@code channel} 精确匹配。
 */
public final class SearchHotwordChannels {

	public static final String MOBILE = "mobile";

	public static final String PC = "pc";

	private static final List<String> ALLOWED = List.of(MOBILE, PC);

	private SearchHotwordChannels() {}

	/** 落库串 → 接口出参；非法/空返回 {@code null}。历史逗号串取第一个合法值。 */
	public static String toResponseChannel(String stored) {
		if (stored == null || stored.isBlank()) {
			return null;
		}
		for (String part : stored.split(",")) {
			String v = part.trim().toLowerCase(Locale.ROOT);
			if (ALLOWED.contains(v)) {
				return v;
			}
		}
		return null;
	}

	/**
	 * 保存入参 → 落库串。接受单个字符串；兼容误传单元素数组。
	 * 多选数组或非法值抛 422（字段码 {@code channel}）。
	 */
	public static String normalizeSaveChannel(Object raw) {
		if (raw == null) {
			throw invalid();
		}
		String value;
		if (raw instanceof List<?> list) {
			if (list.size() != 1 || list.get(0) == null) {
				throw invalid();
			}
			value = list.get(0).toString();
		} else if (raw instanceof Object[] array) {
			if (array.length != 1 || array[0] == null) {
				throw invalid();
			}
			value = array[0].toString();
		} else {
			value = raw.toString();
			if (value.contains(",")) {
				throw invalid();
			}
		}
		String v = value.trim().toLowerCase(Locale.ROOT);
		if (!ALLOWED.contains(v)) {
			throw invalid();
		}
		return v;
	}

	/** 前台查询渠道：缺省 {@code mobile}，仅接受 mobile / pc。 */
	public static String normalizeQueryChannel(String channel) {
		if (channel == null || channel.isBlank()) {
			return MOBILE;
		}
		return requireAllowed(channel);
	}

	/** Admin 列表筛选：空返回 {@code null}；非法值 422。 */
	public static String optionalFilterChannel(String channel) {
		if (channel == null || channel.isBlank()) {
			return null;
		}
		return requireAllowed(channel);
	}

	private static String requireAllowed(String channel) {
		String v = channel.trim().toLowerCase(Locale.ROOT);
		if (!ALLOWED.contains(v)) {
			throw new BadRequestException(
					"channel 取值无效，仅支持 mobile 或 pc",
					Map.of("channel", List.of("validation.in")),
					422);
		}
		return v;
	}

	/** 落库值是否命中请求渠道（精确匹配；兼容历史 {@code mobile,pc} 逗号串）。 */
	public static boolean matchesChannel(String stored, String channel) {
		if (stored == null || stored.isBlank() || channel == null) {
			return false;
		}
		for (String part : stored.split(",")) {
			if (channel.equals(part.trim().toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	private static BadRequestException invalid() {
		return new BadRequestException(
				"channel 必填，仅支持 mobile 或 pc（单选）",
				Map.of("channel", List.of("validation.in")),
				422);
	}
}
