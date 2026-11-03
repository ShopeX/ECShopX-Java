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

package cn.shopex.ecshopx.orders.service.pos;

import cn.shopex.ecshopx.common.auth.DualJwtSupport;
import cn.shopex.ecshopx.common.auth.PosOrderDocAccessPort;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** G-P2/P3/B4：POS 写单据 —— 本店隔离；店员仅经手（含 id=0 拒）。 */
@Service
public class PosOrderDocAccessGuard implements PosOrderDocAccessPort {

	private final NormalOrdersMapper normalOrdersMapper;

	public PosOrderDocAccessGuard(NormalOrdersMapper normalOrdersMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
	}

	@Override
	public void assertWriteAccess(HttpServletRequest request, long companyId, long orderId) {
		if (!DualJwtSupport.isPosSession(request)) {
			return;
		}
		Map<String, Object> claims = DualJwtSupport.claims(request);
		NormalOrders order = load(companyId, orderId);
		assertStoreAndClerkWrite(claims, order.getDistributorId(), order.getPosStoreAccountId());
	}

	/** 纯规则（单测用）：已解析的订单门店与经手字段。 */
	static void assertStoreAndClerkWrite(
			Map<String, Object> claims, Long orderDistributorId, Long orderPosStoreAccountId) {
		Long selfShop = DualJwtSupport.longClaim(claims, "distributor_id");
		Long selfId = DualJwtSupport.longClaim(claims, "pos_store_account_id");
		if (selfId == null) {
			selfId = DualJwtSupport.longClaim(claims, "id");
		}
		String role = claims == null || claims.get("role") == null ? "" : String.valueOf(claims.get("role"));
		long orderShop = orderDistributorId == null ? 0L : orderDistributorId;
		long orderPos = orderPosStoreAccountId == null ? 0L : orderPosStoreAccountId;
		if (selfShop == null || selfShop <= 0L || !Objects.equals(selfShop, orderShop)) {
			throw new ForbiddenException("无权操作其他门店单据");
		}
		if ("store_manager".equals(role) || "supervisor".equals(role)) {
			return;
		}
		// clerk（及其他）：仅自己经手；0=无经手不可操作
		if (orderPos <= 0L || selfId == null || selfId <= 0L || !Objects.equals(selfId, orderPos)) {
			throw new ForbiddenException("无权操作该单据");
		}
	}

	private NormalOrders load(long companyId, long orderId) {
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrders::getCompanyId, companyId).eq(NormalOrders::getOrderId, orderId);
		NormalOrders order = normalOrdersMapper.selectOne(w);
		if (order == null) {
			throw new ResourceException("订单不存在");
		}
		return order;
	}
}
