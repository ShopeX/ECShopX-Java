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
 * 调用阿里云 GetSmsSign 后的可写回字段快照；signStatus 为空表示云端未返回审核状态，对应跳过写库。
 */
public record GetSmsSignResult(String signStatus, String rejectInfo) {

	public static GetSmsSignResult noSignStatus() {
		return new GetSmsSignResult(null, null);
	}
}
