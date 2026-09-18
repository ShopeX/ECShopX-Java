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

package cn.shopex.ecshopx.common.promotions.port;

import java.util.List;

/**
 * 小程序订阅消息：按企业与模板业务类型查询已开启的模板 ID 列表（Front 只读）。
 */
public interface WxaNewTmplListPort {

	/**
	 * 返回 template_id 列列表（顺序与 DB 行一致；元素可为 null）。
	 *
	 * @param companyId  租户 ID（由 Controller 在通过假值判定后调用 requireCompanyId 得到）
	 * @param sourceType 订阅业务类型
	 * @param tempName   请求参数 temp_name，对应库 template_name
	 */
	List<String> listTemplateIds(long companyId, String sourceType, String tempName);
}
