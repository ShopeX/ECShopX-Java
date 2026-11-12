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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.service.upload.AbstractEspierUploadFileHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadRowContext;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTemplateExtension;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 对齐 PHP {@code NormalGoodsUploadService}：动态会员价表头、示例行 + xlsx 校验。
 */
@Component
public class NormalGoodsEspierUploadHandler extends AbstractEspierUploadFileHandler
		implements EspierUploadTableHandler, EspierUploadTemplateExtension {

	private static final String FILE_TYPE = "normal_goods";

	private final NormalGoodsUploadHeaderTitleService headerTitleService;
	private final NormalGoodsEspierImportRowSink sink;

	public NormalGoodsEspierUploadHandler(
			NormalGoodsUploadHeaderTitleService headerTitleService, NormalGoodsEspierImportRowSink sink) {
		this.headerTitleService = headerTitleService;
		this.sink = sink;
	}

	@Override
	public String supportedFileType() {
		return FILE_TYPE;
	}

	@Override
	public void check(MultipartFile file) {
		assertBaseUploadConstraints(file);
		if (!"xlsx".equalsIgnoreCase(extensionOf(file))) {
			throw new BadRequestException("实体商品信息上传只支持Excel文件格式(xlsx)");
		}
	}

	@Override
	public UploadHeaderTitle getHeaderTitle(long companyId) {
		return headerTitleService.forCompany(companyId);
	}

	@Override
	public Optional<List<List<Object>>> templateDemoRows(long companyId, String operatorType) {
		UploadHeaderTitle title = headerTitleService.forCompany(companyId);
		return Optional.of(NormalGoodsUploadTemplateDemoRows.forHeader(title));
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
}
