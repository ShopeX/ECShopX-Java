package cn.shopex.ecshopx.goods.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoodsItemsCsvExportServicePlatformTitlesTest {

	@Test
	void buildTitleMapForPlatform_lastColumnIsSupplierName() {
		LinkedHashMap<String, String> titles = GoodsItemsCsvExportService.buildTitleMapForPlatform("item", List.of(), List.of());
		assertThat(titles).isNotEmpty();
		assertThat(titles.keySet().stream().reduce((first, second) -> second).orElse(""))
				.isEqualTo("supplier_name");
		assertThat(titles.get("supplier_name")).isEqualTo("所属供应商");
	}

	@Test
	void buildTitleMapForPlatform_supplierItemSource_noDuplicateSupplierColumn() {
		LinkedHashMap<String, String> titles =
				GoodsItemsCsvExportService.buildTitleMapForPlatform("supplier", List.of(), List.of());
		long supplierColumns =
				titles.entrySet().stream().filter(e -> "supplier_name".equals(e.getKey())).count();
		assertThat(supplierColumns).isEqualTo(1);
		assertThat(titles.get("supplier_name")).isEqualTo("所属供应商");
	}
}