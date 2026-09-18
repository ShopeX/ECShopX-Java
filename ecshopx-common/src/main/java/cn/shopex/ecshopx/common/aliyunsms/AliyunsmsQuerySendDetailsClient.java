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
 * 按公司凭据查询阿里云短信发送详情；生产实现见 ecshopx-aliyunsms 模块，test-cron 下可由 Noop 覆盖。
 *
 * <p>调用方需保证 {@code mobile} 为<strong>明文</strong>（由 {@code SensitiveFieldEncryptor.decrypt} 解密后传入）。
 */
public interface AliyunsmsQuerySendDetailsClient {

	/**
	 * @param companyId 公司 ID
	 * @param mobile 手机号明文（非密文）
	 * @param bizId 发送回执 ID（对应 aliyunsms_record.biz_id）
	 * @param sendDate 发送日期，格式 yyyyMMdd（Asia/Shanghai）
	 */
	QuerySendDetailsResult querySendDetails(long companyId, String mobile, String bizId, String sendDate);
}
