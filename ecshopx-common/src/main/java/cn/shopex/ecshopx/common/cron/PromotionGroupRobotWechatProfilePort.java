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

package cn.shopex.ecshopx.common.cron;

import java.util.List;

/**
 * 拼团机器人参团时获取随机微信用户头像、昵称，对应业务侧对 {@code WechatUserService::getRandUserInfo} 的抽象。
 */
public interface PromotionGroupRobotWechatProfilePort {

	/**
	 * 获取至多 {@code limit} 条用户资料，不足时可少于 {@code limit} 条，由调用方对缺省行补默认昵称。
	 */
	List<GroupRobotWechatInfo> getRandUserInfo(int limit);
}
