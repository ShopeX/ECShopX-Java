package cn.shopex.ecshopx.goods.service.items;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.Items;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoodsItemsOtherProductMaterialsTest {

	@Test
	@DisplayName("toRow 回显其他产品资料对象数组")
	void toRowReturnsOtherProductMaterialsList() {
		Items it = new Items();
		it.setItemId(1L);
		it.setItemName("demo");
		it.setPics("[]");
		it.setOtherProductMaterials(
				"[{\"name\":\"说明书.pdf\",\"url\":\"https://cdn.example/a.pdf\",\"size\":12}]");

		Map<String, Object> row = GoodsItemsListRowMapper.toRow(it);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> materials = (List<Map<String, Object>>) row.get("other_product_materials");
		assertThat(materials).hasSize(1);
		assertThat(materials.get(0).get("name")).isEqualTo("说明书.pdf");
		assertThat(materials.get(0).get("url")).isEqualTo("https://cdn.example/a.pdf");
		assertThat(materials.get(0).get("size")).isEqualTo(12);
	}

	@Test
	@DisplayName("空资料解析为 []")
	void resolveEmptyMaterials() {
		assertThat(GoodsItemsListRowMapper.resolveOtherProductMaterialsForRow(null)).isEmpty();
		assertThat(GoodsItemsListRowMapper.resolveOtherProductMaterialsForRow("")).isEmpty();
		assertThat(GoodsItemsListRowMapper.resolveOtherProductMaterialsForRow("[]")).isEmpty();
	}

	@Test
	@DisplayName("校验：条数超限拒绝")
	void validateRejectsTooMany() {
		List<Map<String, Object>> list = new ArrayList<>();
		for (int i = 0; i < 21; i++) {
			list.add(Map.of("url", "https://cdn.example/" + i + ".pdf"));
		}
		assertThatThrownBy(() -> GoodsItemsCreateParamValidator.validateOtherProductMaterials(list))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("最多上传20个");
	}

	@Test
	@DisplayName("校验：缺少 url 拒绝")
	void validateRejectsMissingUrl() {
		List<Map<String, Object>> list = List.of(Map.of("name", "a.pdf"));
		assertThatThrownBy(() -> GoodsItemsCreateParamValidator.validateOtherProductMaterials(list))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("文件地址不能为空");
	}

	@Test
	@DisplayName("校验：合法数组通过；create 主校验含资料字段不报错")
	void validateAcceptsValidAndCreatePasses() {
		List<Map<String, Object>> materials = List.of(Map.of(
				"name", "说明书.pdf",
				"url", "https://cdn.example/a.pdf",
				"size", 100));
		GoodsItemsCreateParamValidator.validateOtherProductMaterials(materials);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("item_name", "商品A");
		body.put("pics", List.of("https://cdn.example/p.jpg"));
		body.put("sort", 1);
		body.put("other_product_materials", materials);
		GoodsItemsCreateParamValidator.validate(body);
	}

	@Test
	@DisplayName("校验：非数组拒绝")
	void validateRejectsNonArray() {
		assertThatThrownBy(() -> GoodsItemsCreateParamValidator.validateOtherProductMaterials("not-json"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("格式不正确");
	}
}
