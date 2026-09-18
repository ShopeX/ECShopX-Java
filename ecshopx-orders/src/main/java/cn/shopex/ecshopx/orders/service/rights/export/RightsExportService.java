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

package cn.shopex.ecshopx.orders.service.rights.export;

import cn.shopex.ecshopx.common.dispatch.OrderListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class RightsExportService {

	private final RightsMapper rightsMapper;
	private final RightsExportFilterAssembler rightsExportFilterAssembler;
	private final OrderListExportFileJobDispatchPublisher orderListExportFileJobDispatchPublisher;

	public RightsExportService(
			RightsMapper rightsMapper,
			RightsExportFilterAssembler rightsExportFilterAssembler,
			OrderListExportFileJobDispatchPublisher orderListExportFileJobDispatchPublisher) {
		this.rightsMapper = rightsMapper;
		this.rightsExportFilterAssembler = rightsExportFilterAssembler;
		this.orderListExportFileJobDispatchPublisher = orderListExportFileJobDispatchPublisher;
	}

	public RightsExportResult exportRightData(
			long companyId,
			long operatorId,
			HttpServletRequest request,
			String mobileParam,
			String userIdParam,
			String validParam,
			String dateBegin,
			String dateEnd,
			String rightsFrom,
			String orderId,
			String shopId,
			String datapassBlockHeader) {
		RightsExportFilterAssembler.Assembly assembly =
				rightsExportFilterAssembler.assemble(
						companyId,
						request,
						mobileParam,
						userIdParam,
						validParam,
						dateBegin,
						dateEnd,
						rightsFrom,
						orderId,
						shopId);
		if (assembly.emptyShopNoMembers()) {
			return new RightsExportEmptyShop();
		}
		LinkedHashMap<String, Object> filter = assembly.filter();
		long count = rightsMapper.selectCount(RightsExportQuerySupport.toCountWrapper(companyId, filter));
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		LinkedHashMap<String, Object> filterForJob = new LinkedHashMap<>(filter);
		filterForJob.put("datapass_block", datapassBlockHeader == null ? "" : datapassBlockHeader);
		orderListExportFileJobDispatchPublisher.enqueueRightsExport(companyId, operatorId, filterForJob);
		return new RightsExportQueued();
	}
}
