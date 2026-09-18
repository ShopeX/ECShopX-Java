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

package cn.shopex.ecshopx.common.operatorcart;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 运营购物车列表与结算前数据编排（由商品模块实现，companys 仅依赖本接口）。
 */
public interface OperatorCartDataListFacade {

	Map<String, Object> getCartDataList(long companyId, long operatorId, long distributorId, long targetUserId,
			HttpServletRequest request, boolean isSubmit);
}
