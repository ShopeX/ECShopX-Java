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
import java.util.Optional;

/**
 * 小程序模板基础信息所需的跨模块读取（模板集行、门店标题），由 {@code ecshopx-bootstrap} 实现，
 * 避免 {@code ecshopx-wechat} 与 {@code ecshopx-theme}/{@code ecshopx-companys} 形成 Maven 环依赖。
 */
public interface WxappPagestemplateBaseinfoSupportFacade {

	Optional<Map<String, Object>> pagesTemplateSetRow(long companyId, String requestLang, long regionauthId);

	String wxShopsBrandNameForTitle(long companyId, String countryCode);
}
