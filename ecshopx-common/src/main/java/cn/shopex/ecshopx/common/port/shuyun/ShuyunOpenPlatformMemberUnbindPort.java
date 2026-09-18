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

package cn.shopex.ecshopx.common.port.shuyun;

/**
 * 数云开放平台会员 unbind（D7）。members 注入；默认 NoOp。
 */
public interface ShuyunOpenPlatformMemberUnbindPort {

	boolean isOpenPlatformMemberEnabled(long companyId);

	/**
	 * @param skip 为 true 时跳过（对齐 PHP skipShuyunOpenPlatformUnbind）
	 * @return true 已调用或跳过；失败抛 ResourceException
	 */
	boolean unbindIfNeeded(long companyId, long userId, boolean skip);
}
