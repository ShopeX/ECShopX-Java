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

package cn.shopex.ecshopx.promotions.service.export;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Admin 会员任务 CSV 列与明细 preamble（与 {@code java-docs/prd/会员任务.md} 及 Admin OpenAPI
 * {@code x-export-templates} 一致，改 md 时同步改此处与 yaml）。
 */
public final class MemberTaskExportCsvTemplates {

	public static final String EXPORT_TYPE_OVERVIEW = "export_member_task_overview";
	public static final String EXPORT_TYPE_COMPLETION = "export_member_task_completion";

	private MemberTaskExportCsvTemplates() {}

	public static LinkedHashMap<String, String> overviewDataColumnKeysToHeader() {
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("task_id", "任务ID");
		titles.put("name", "任务名称");
		titles.put("type_label", "任务类型");
		titles.put("task_time", "任务时间");
		titles.put("reward_text", "任务奖励");
		titles.put("completion_count", "任务完成次数");
		titles.put("completion_user_count", "任务完成人数");
		titles.put("attributed_spend", "通过任务累计消费");
		titles.put("attributed_join_count", "通过任务累计入会");
		return titles;
	}

	public static List<String> overviewDataColumnHeadersInOrder() {
		return List.copyOf(overviewDataColumnKeysToHeader().values());
	}

	public static LinkedHashMap<String, String> completionDataColumnKeysToHeader() {
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("nickname", "完成会员");
		titles.put("user_id", "会员编号");
		titles.put("completed_at", "完成时间");
		titles.put("grant_status", "奖励发放状态");
		titles.put("error_message", "发放失败原因");
		return titles;
	}

	public static List<String> completionDataColumnHeadersInOrder() {
		return List.copyOf(completionDataColumnKeysToHeader().values());
	}

	public static String completionTitlePreambleLine(
			String taskName, long taskId, String taskTimeRangeText) {
		String name = taskName == null ? "" : taskName.trim();
		String time = taskTimeRangeText == null ? "" : taskTimeRangeText.trim();
		return name + "_" + taskId + " " + time;
	}

	public static String completionSummaryPreambleLine(long completionCount, long completionUserCount) {
		return "任务完成总次数："
				+ completionCount
				+ "，任务完成总人数："
				+ completionUserCount;
	}

	public static List<String> completionPreambleLines(
			String taskName,
			long taskId,
			String taskTimeRangeText,
			long completionCount,
			long completionUserCount) {
		return List.of(
				completionTitlePreambleLine(taskName, taskId, taskTimeRangeText),
				completionSummaryPreambleLine(completionCount, completionUserCount));
	}
}
