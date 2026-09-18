package cn.shopex.ecshopx.employeepurchase.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmployeePurchaseActivityItemsCsvExportServiceFlattenTest {

	@Test
	void flatten_multiSpec_expandsSpecItemsAndFormatsMoney() {
		Map<String, Object> goods = new LinkedHashMap<>();
		goods.put("item_name", "外套");
		goods.put("goods_bn", "G1");
		goods.put("item_bn", "S-HEAD");
		goods.put("activity_price", 1999);
		goods.put("activity_store", 3);
		goods.put("limit_num", 1);
		goods.put("limit_fee", 5000);
		goods.put("shelf_status", 1);
		goods.put("sort", 9);

		Map<String, Object> spec1 = new LinkedHashMap<>();
		spec1.put("item_id", 11L);
		spec1.put("item_name", "外套-红");
		spec1.put("goods_bn", "G1");
		spec1.put("item_bn", "S-RED");
		spec1.put("activity_price", 1999);
		spec1.put("activity_store", 2);
		spec1.put("limit_num", 1);
		spec1.put("limit_fee", 5000);
		spec1.put("shelf_status", 1);
		spec1.put("sort", 9);

		Map<String, Object> spec2 = new LinkedHashMap<>();
		spec2.put("item_id", 12L);
		spec2.put("item_name", "外套-蓝");
		spec2.put("goods_bn", "G1");
		spec2.put("item_bn", "S-BLUE");
		spec2.put("activity_price", 2099);
		spec2.put("activity_store", 1);
		spec2.put("limit_num", 0);
		spec2.put("limit_fee", 0);
		spec2.put("shelf_status", 0);
		spec2.put("sort", 8);
		goods.put("spec_items", List.of(spec1, spec2));

		List<Map<String, String>> rows =
				EmployeePurchaseActivityItemsCsvExportService.flattenGoodsRows(goods, List.of(12L));
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).get("goods_bn")).isEqualTo("G1");
		assertThat(rows.get(0).get("item_bn")).isEqualTo("S-BLUE");
		assertThat(rows.get(0).get("activity_price")).isEqualTo("20.99");
		assertThat(rows.get(0).get("limit_fee")).isEqualTo("0.00");
		assertThat(rows.get(0).get("shelf_status")).isEqualTo("下架");
	}

	@Test
	void flatten_singleSku_usesGoodsItself() {
		Map<String, Object> goods = new LinkedHashMap<>();
		goods.put("item_id", 1L);
		goods.put("item_name", "单规格");
		goods.put("goods_bn", "G2");
		goods.put("item_bn", "SKU2");
		goods.put("activity_price", 100);
		goods.put("activity_store", 0);
		goods.put("limit_num", 0);
		goods.put("limit_fee", 0);
		goods.put("shelf_status", null);
		goods.put("sort", 0);

		List<Map<String, String>> rows =
				EmployeePurchaseActivityItemsCsvExportService.flattenGoodsRows(goods, null);
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).get("goods_bn")).isEqualTo("G2");
		assertThat(rows.get(0).get("item_bn")).isEqualTo("SKU2");
		assertThat(rows.get(0).get("activity_price")).isEqualTo("1.00");
		assertThat(rows.get(0).get("shelf_status")).isEqualTo("上架");
	}
}
