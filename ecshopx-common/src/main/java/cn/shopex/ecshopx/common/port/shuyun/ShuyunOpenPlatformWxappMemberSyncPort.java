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
 * 数云开放平台 wxapp 会员同步（register + enhance 卡号 + bind.push）。
 * members 模块注入；默认 NoOp。
 */
public interface ShuyunOpenPlatformWxappMemberSyncPort {

	/**
	 * OPEN 启用时执行 wxapp 线上同步。
	 *
	 * @param distributorIdHint 优先门店；≤0 时用 members.reg_distributor / offline_reg_distributor
	 * @param failHard true=失败抛异常（新注册路径）；false=失败仅日志（老会员补同步）
	 * @return true 已执行且成功；false 跳过（未启用/缺参）
	 */
	boolean syncWxappOnlineIfEnabled(
			long companyId,
			long userId,
			String mobile,
			String unionId,
			String openId,
			long distributorIdHint,
			boolean failHard);
}
