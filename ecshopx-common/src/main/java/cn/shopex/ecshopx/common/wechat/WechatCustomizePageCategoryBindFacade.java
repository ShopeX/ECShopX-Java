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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 管理端自定义分类页与销售分类绑定（由商品模块编排分类表与页面校验，避免 wechat 与 goods 的 Maven 环依赖）。
 */
public interface WechatCustomizePageCategoryBindFacade {

	Map<String, Object> bindCategoryId(long companyId, long jwtDistributorId, long customizePageId, long regionauthId, long categoryId);

	List<Map<String, Object>> listCategoriesForCustomizePageList(long companyId, long jwtDistributorId,
			Collection<Long> customizePageIds);
}
