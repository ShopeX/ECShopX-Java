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
import cn.shopex.ecshopx.goods.domain.SearchHotword;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 热词展示状态（ECX-11390 F-003 状态机）。
 *
 * <p>状态由展示时间与当前时间实时计算，不落库、不提供人工修改入口：
 * <ul>
 *   <li>{@link #NOT_STARTED}：{@code now < start_time}，前台不展示</li>
 *   <li>{@link #ACTIVE}：{@code start_time <= now <= end_time}，前台展示</li>
 *   <li>{@link #ENDED}：{@code now > end_time}，前台不展示</li>
 * </ul>
 * 开始时间按「达到即生效」，结束时间按「超过后失效」，两端点均含。
 */
public final class SearchHotwordStatus {

	public static final int NOT_STARTED = 0;

	public static final int ACTIVE = 1;

	public static final int ENDED = 2;

	private SearchHotwordStatus() {}

	/**
	 * 计算展示状态；展示时间缺失时按「已结束」处理（不展示），避免历史脏数据被静默视为有效。
	 */
	public static int resolve(Integer startTime, Integer endTime, long nowEpochSecond) {
		if (startTime == null || endTime == null) {
			return ENDED;
		}
		if (nowEpochSecond < startTime.longValue()) {
			return NOT_STARTED;
		}
		if (nowEpochSecond > endTime.longValue()) {
			return ENDED;
		}
		return ACTIVE;
	}

	public static String name(int status) {
		return switch (status) {
			case NOT_STARTED -> "未开始";
			case ACTIVE -> "生效中";
			default -> "已结束";
		};
	}

	/** 前台是否应返回该状态的热词。 */
	public static boolean visibleInFront(int status) {
		return status == ACTIVE;
	}

	/** 列表筛选：空返回 {@code null}；非法值 422。 */
	public static Integer parseFilter(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			int status = Integer.parseInt(raw.trim());
			if (status != NOT_STARTED && status != ACTIVE && status != ENDED) {
				throw invalidFilter();
			}
			return Integer.valueOf(status);
		} catch (NumberFormatException e) {
			throw invalidFilter();
		}
	}

	/** 按实时状态对应的时间条件写入列表查询（状态不落库）。 */
	public static void applyListFilter(
			LambdaQueryWrapper<SearchHotword> wrapper, Integer status, long nowEpochSecond) {
		if (status == null) {
			return;
		}
		int now = (int) nowEpochSecond;
		switch (status.intValue()) {
			case NOT_STARTED -> wrapper.gt(SearchHotword::getStartTime, now);
			case ACTIVE -> wrapper
					.le(SearchHotword::getStartTime, now)
					.ge(SearchHotword::getEndTime, now);
			case ENDED -> wrapper.lt(SearchHotword::getEndTime, now);
			default -> throw invalidFilter();
		}
	}

	private static BadRequestException invalidFilter() {
		return new BadRequestException(
				"status 取值无效，仅支持 0/1/2",
				Map.of("status", List.of("validation.in")),
				422);
	}
}
