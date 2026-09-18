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

package cn.shopex.ecshopx.espier.service.upload;

import cn.shopex.ecshopx.common.espier.upload.EspierImportRowSink;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EspierUploadHandlerBeans {

	@Bean
	public EspierUploadFileHandler espierUploadMemberInfoHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("member_info", EspierUploadHeaderCatalog.MEMBER_INFO(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadMemberUpdateHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("member_update", EspierUploadHeaderCatalog.MEMBER_UPDATE(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadMemberConsumeHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("member_consume", EspierUploadHeaderCatalog.MEMBER_CONSUME(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadSupplierGoodsHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("supplier_goods", EspierUploadHeaderCatalog.SUPPLIER_GOODS(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadNormalGoodsHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("normal_goods", EspierUploadHeaderCatalog.NORMAL_GOODS(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadNormalEpidemicGoodsHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating(
				"normal_epidemic_goods", EspierUploadHeaderCatalog.NORMAL_EPIDEMIC_GOODS(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadNormalGoodsStoreHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("normal_goods_store", EspierUploadHeaderCatalog.NORMAL_GOODS_STORE(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadNormalGoodsProfitHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("normal_goods_profit", EspierUploadHeaderCatalog.NORMAL_GOODS_PROFIT(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadNormalGoodsTagHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("normal_goods_tag", EspierUploadHeaderCatalog.NORMAL_GOODS_TAG(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadSelformRegistrationRecordHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating(
				"selform_registration_record", EspierUploadHeaderCatalog.SELFORM_REGISTRATION_RECORD(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadNormalOrdersHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("normal_orders", EspierUploadHeaderCatalog.NORMAL_ORDERS(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadNormalOrdersCancelHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("normal_orders_cancel", EspierUploadHeaderCatalog.NORMAL_ORDERS_CANCEL(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadNormalPointsmallGoodsHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating(
				"normal_pointsmall_goods", EspierUploadHeaderCatalog.NORMAL_POINTSMALL_GOODS(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadNormalPointsmallGoodsStoreHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating(
				"normal_pointsmall_goods_store",
				EspierUploadHeaderCatalog.NORMAL_POINTSMALL_GOODS_STORE(),
				null,
				sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadWhitelistCreateHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("whitelist_create", EspierUploadHeaderCatalog.WHITELIST_CREATE(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadUpdateDistributionItemHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating(
				"update_distribution_item", EspierUploadHeaderCatalog.UPDATE_DISTRIBUTION_ITEM(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadLimitSaleItemHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("limit_sale_item", EspierUploadHeaderCatalog.LIMIT_SALE_ITEM(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadAdapayTradedataHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("adapay_tradedata", EspierUploadHeaderCatalog.ADAPAY_TRADEDATA(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadCommunityChiefHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("community_chief", EspierUploadHeaderCatalog.COMMUNITY_CHIEF(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadEmployeePurchaseEmployeesHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating(
				"employee_purchase_employees",
				EspierUploadHeaderCatalog.EMPLOYEE_PURCHASE_EMPLOYEES(),
				null,
				sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadEmployeePurchaseActivityItemsSortHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating(
				"employee_purchase_activity_items_sort",
				EspierUploadHeaderCatalog.EMPLOYEE_PURCHASE_ACTIVITY_ITEMS_SORT(),
				null,
				sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadUploadDistributorWhiteHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating(
				"upload_distributor_white", EspierUploadHeaderCatalog.UPLOAD_DISTRIBUTOR_WHITE(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadDistributorInfoHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating("distributor_info", EspierUploadHeaderCatalog.DISTRIBUTOR_INFO(), null, sinks);
	}

	@Bean
	public EspierUploadFileHandler espierUploadUploadTbItemsHandler(
			@Qualifier("espierImportRowSinkByType") Map<String, EspierImportRowSink> sinks) {
		return delegating(
				"upload_tb_items",
				EspierUploadHeaderCatalog.UPLOAD_TB_ITEMS(),
				f -> {
					if (!"xlsx".equalsIgnoreCase(AbstractEspierUploadFileHandler.extensionOf(f))) {
						throw new BadRequestException("淘宝商品上传只支持Excel文件格式");
					}
				},
				sinks);
	}

	private static EspierUploadFileHandler delegating(
			String fileType,
			UploadHeaderTitle headerTitle,
			java.util.function.Consumer<org.springframework.web.multipart.MultipartFile> extraCheck,
			Map<String, EspierImportRowSink> sinks) {
		return new AbstractEspierTableUploadHandler(headerTitle, extraCheck) {
			@Override
			public String supportedFileType() {
				return fileType;
			}

			@Override
			protected void handleBusinessRow(EspierUploadRowContext ctx, java.util.Map<String, Object> row) {
				EspierImportRowSink sink = sinks.get(fileType);
				if (sink == null) {
					throw new IllegalStateException("missing EspierImportRowSink for file_type=" + fileType);
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
		};
	}
}
