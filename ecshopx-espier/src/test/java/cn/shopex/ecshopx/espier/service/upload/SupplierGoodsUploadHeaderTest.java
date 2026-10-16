package cn.shopex.ecshopx.espier.service.upload;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class SupplierGoodsUploadHeaderTest {

	@Test
	void supplierGoodsTemplate_excludesSupplierGoodsBnColumn() {
		UploadHeaderTitle title = EspierUploadHeaderCatalog.SUPPLIER_GOODS();
		assertFalse(title.all().containsKey("供应商货号"));
		assertFalse(title.all().containsValue("supplier_goods_bn"));
		assertFalse(title.headerInfo().containsKey("供应商货号"));
	}
}
