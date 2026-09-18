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

package cn.shopex.ecshopx.common.companys.wxapp;

import java.util.Map;

/**
 * H5 导购企业微信小程序登录编排（由 companys 控制器调用，实现在 salesperson 模块）。
 */
public interface H5WxappWorkWechatLoginPort {

	Map<String, Object> workwechatlogin(long companyId, String appname, String code);
}
