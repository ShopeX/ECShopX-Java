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

import java.util.List;

public interface WxaSubscribeMessageTemplateAdminPort {

	/**
	 * @param authorizerAppid 即 wxapp_appid
	 * @param wxaLibraryTemplateId 即库表 wxa_template_id
	 * @return 微信返回的 priTmplId（非空）
	 */
	String addTemplate(String authorizerAppid, String wxaLibraryTemplateId, List<Integer> keywordIdList, String sceneDescription);

	/**
	 * 仅当返回 errmsg 等于 "ok" 时视为删除成功。
	 */
	boolean deleteTemplate(String authorizerAppid, String priTemplateId);
}
