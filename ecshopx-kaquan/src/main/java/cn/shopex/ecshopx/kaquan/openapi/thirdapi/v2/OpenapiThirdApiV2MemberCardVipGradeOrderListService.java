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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberCardVipGradeOrderListService {

	private static final String ORDER_STATUS_DONE = "DONE";

	private final VipGradeOrderMapper vipGradeOrderMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberCardVipGradeOrderListService(
			VipGradeOrderMapper vipGradeOrderMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.vipGradeOrderMapper = vipGradeOrderMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiList(long companyId, int page, int pageSize) {
		LambdaQueryWrapper<VipGradeOrder> baseFilter = new LambdaQueryWrapper<VipGradeOrder>()
				.eq(VipGradeOrder::getCompanyId, (int) companyId)
				.eq(VipGradeOrder::getOrderStatus, ORDER_STATUS_DONE);

		long totalCount = vipGradeOrderMapper.selectCount(baseFilter);

		LambdaQueryWrapper<VipGradeOrder> listWrapper = new LambdaQueryWrapper<VipGradeOrder>()
				.eq(VipGradeOrder::getCompanyId, (int) companyId)
				.eq(VipGradeOrder::getOrderStatus, ORDER_STATUS_DONE)
				.select(
						VipGradeOrder::getOrderId,
						VipGradeOrder::getPrice,
						VipGradeOrder::getUserId,
						VipGradeOrder::getMobile,
						VipGradeOrder::getVipGradeId,
						VipGradeOrder::getLvType,
						VipGradeOrder::getTitle,
						VipGradeOrder::getCardType,
						VipGradeOrder::getDiscount,
						VipGradeOrder::getCreated,
						VipGradeOrder::getUpdated)
				.orderByDesc(VipGradeOrder::getOrderId);

		Page<VipGradeOrder> pageRequest = new Page<>(page, pageSize, false);
		vipGradeOrderMapper.selectPage(pageRequest, listWrapper);
		List<VipGradeOrder> entities = pageRequest.getRecords();

		List<Map<String, Object>> list;
		if (entities.isEmpty()) {
			list = List.of();
		} else {
			list = entities.stream()
					.map(e -> OpenapiMemberCardVipGradeOrderOpenApiFormatSupport.formatOpenApiVipGradeOrderRow(
							e, resolveMobilePlain(e.getMobile()), objectMapper))
					.toList();
		}
		return formatListStruct(totalCount, list, page, pageSize);
	}

	private String resolveMobilePlain(String stored) {
		if (!StringUtils.hasText(stored)) {
			return stored;
		}
		String decrypted = sensitiveFieldEncryptor.decrypt(stored);
		if (decrypted != null && !decrypted.isBlank()) {
			return decrypted;
		}
		return stored;
	}

	private static Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		result.put("list", list != null ? list : List.of());
		return result;
	}

	private static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}
}
