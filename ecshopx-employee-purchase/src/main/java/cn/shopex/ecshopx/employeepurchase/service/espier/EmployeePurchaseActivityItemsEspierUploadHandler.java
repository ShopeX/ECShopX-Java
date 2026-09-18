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

package cn.shopex.ecshopx.employeepurchase.service.espier;

import cn.shopex.ecshopx.common.espier.upload.EspierImportRowSink;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.espier.service.upload.AbstractEspierUploadFileHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadHeaderCatalog;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadRowContext;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 对齐 PHP {@code ActivityItemsUploadService}：xlsx 校验 + 按活动 if_share_store 动态表头。
 */
@Component
public class EmployeePurchaseActivityItemsEspierUploadHandler extends AbstractEspierUploadFileHandler
		implements EspierUploadTableHandler {

	private final ActivitiesMapper activitiesMapper;
	private final EspierImportRowSink sink;

	public EmployeePurchaseActivityItemsEspierUploadHandler(
			ActivitiesMapper activitiesMapper, EmployeePurchaseActivityItemsEspierImportRowSink sink) {
		this.activitiesMapper = activitiesMapper;
		this.sink = sink;
	}

	@Override
	public String supportedFileType() {
		return "employee_purchase_activity_items";
	}

	@Override
	public void check(MultipartFile file) {
		assertBaseUploadConstraints(file);
		if (!XLSX.equalsIgnoreCase(extensionOf(file))) {
			throw new BadRequestException("内购活动商品只支持上传Excel文件格式");
		}
	}

	@Override
	public UploadHeaderTitle getHeaderTitle(long companyId) {
		return EspierUploadHeaderCatalog.EMPLOYEE_PURCHASE_ACTIVITY_ITEMS(false);
	}

	@Override
	public UploadHeaderTitle getHeaderTitle(long companyId, long relationId) {
		Activities activity = requireActivity(companyId, relationId);
		boolean ifShareStore = Boolean.TRUE.equals(activity.getIfShareStore());
		return EspierUploadHeaderCatalog.EMPLOYEE_PURCHASE_ACTIVITY_ITEMS(ifShareStore);
	}

	@Override
	public void handleRow(EspierUploadRowContext ctx, Map<String, Object> row) {
		sink.acceptRow(
				ctx.companyId(),
				ctx.operatorId(),
				ctx.distributorId(),
				ctx.supplierId(),
				ctx.merchantId(),
				row,
				ctx.operatorType());
	}

	private Activities requireActivity(long companyId, long relationId) {
		if (relationId <= 0) {
			throw new BadRequestException("关联id不能为空");
		}
		Activities activity =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, relationId)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new BadRequestException("内购活动不存在");
		}
		return activity;
	}
}
