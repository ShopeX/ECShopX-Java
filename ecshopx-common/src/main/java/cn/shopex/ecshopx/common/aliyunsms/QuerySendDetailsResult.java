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
 * 调用阿里云 QuerySendDetails 后的可写回字段快照；sendStatus 为空表示云端未返回 SmsSendDetailDTO，对应跳过写库。
 *
 * <p>阿里云 SDK 中 {@code SendStatus} 为 {@code java.lang.Long}，在 Client 出口前必须 {@code Long.toString(...)} 转为 {@link String}，
 * 与 Entity 列 {@code status (String)} 对齐。{@code content} 可能为 null，service 层需<strong>原样写入</strong>（对应 SQL {@code SET sms_content = NULL}）。
 */
public record QuerySendDetailsResult(String sendStatus, String content) {

	public static QuerySendDetailsResult noDetail() {
		return new QuerySendDetailsResult(null, null);
	}
}
