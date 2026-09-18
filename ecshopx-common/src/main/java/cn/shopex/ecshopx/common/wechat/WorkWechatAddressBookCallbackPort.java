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

package cn.shopex.ecshopx.common.wechat;

import java.util.Map;

/**
 * 企业微信通讯录回调：解密后的事件由 salesperson 模块处理（实现类位于 ecshopx-salesperson，由 Spring 扫描注册）。
 */
public interface WorkWechatAddressBookCallbackPort {

	void handleAddressBookEvent(Long companyId, Map<String, Object> eventMap);
}
