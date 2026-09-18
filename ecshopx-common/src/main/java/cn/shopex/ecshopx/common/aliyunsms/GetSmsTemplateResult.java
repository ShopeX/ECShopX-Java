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

package cn.shopex.ecshopx.common.aliyunsms;

/**
 * 调用阿里云 GetSmsTemplate 后的可写回字段快照；templateStatus 为空表示云端未返回审核状态，对应跳过写库。
 */
public record GetSmsTemplateResult(
		String templateStatus,
		String rejectInfo,
		String templateName,
		String remark,
		String templateContent,
		String relatedSignName,
		String templateType) {

	public GetSmsTemplateResult(String templateStatus, String rejectInfo) {
		this(templateStatus, rejectInfo, null, null, null, null, null);
	}

	public static GetSmsTemplateResult noTemplateStatus() {
		return new GetSmsTemplateResult(null, null, null, null, null, null, null);
	}
}
