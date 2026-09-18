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

package cn.shopex.ecshopx.espier.service.upload.handlers;

import cn.shopex.ecshopx.common.espier.upload.EspierImportRowSink;
import cn.shopex.ecshopx.common.espier.upload.EspierMarketingStyleItemsLookupPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.service.upload.AbstractEspierTableUploadHandler;
import cn.shopex.ecshopx.espier.service.upload.AbstractEspierUploadFileHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierGoodsMarketingStyleSyncSupport;
import cn.shopex.ecshopx.espier.service.upload.EspierGoodsMarketingStyleSyncSupport.PriceMode;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadHeaderCatalog;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadRowContext;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PurchaseGoodsUploadEspierHandler extends AbstractEspierTableUploadHandler {

	private final Map<String, EspierImportRowSink> rowSinks;
	private final EspierMarketingStyleItemsLookupPort itemsLookup;

	public PurchaseGoodsUploadEspierHandler(
			Map<String, EspierImportRowSink> rowSinks, EspierMarketingStyleItemsLookupPort itemsLookup) {
		super(EspierUploadHeaderCatalog.PURCHASE_GOODS());
		this.rowSinks = rowSinks;
		this.itemsLookup = itemsLookup;
	}

	@Override
	public String supportedFileType() {
		return "purchase_goods";
	}

	@Override
	public void check(MultipartFile file) {
		assertBaseUploadConstraints(file);
		String ext = AbstractEspierUploadFileHandler.extensionOf(file);
		if (!"xlsx".equalsIgnoreCase(ext)) {
			throw new BadRequestException("活动商品信息上传只支持Excel文件格式(xlsx)");
		}
	}

	@Override
	public Optional<Map<String, Object>> trySyncProcess(
			long companyId, long operatorId, long distributorId, long supplierId, MultipartFile file) {
		try {
			return Optional.of(EspierGoodsMarketingStyleSyncSupport.parseSync(
					file, getHeaderTitle(companyId), PriceMode.PURCHASE, companyId, distributorId, itemsLookup));
		} catch (IOException e) {
			throw new BadRequestException("头部标题或Excel解析错误");
		}
	}

	@Override
	protected void handleBusinessRow(EspierUploadRowContext ctx, Map<String, Object> row) {
		EspierImportRowSink sink = rowSinks.get("purchase_goods");
		if (sink == null) {
			throw new IllegalStateException("missing EspierImportRowSink for purchase_goods");
		}
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
