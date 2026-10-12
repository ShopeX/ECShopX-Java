package cn.shopex.ecshopx.espier.service.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NormalOrdersHeaderOptionalColumnsTest {

	@Test
	void oldThreeColumnsStillMap() {
		UploadHeaderTitle title = EspierUploadHeaderCatalog.NORMAL_ORDERS();
		Map<Integer, String> column =
				EspierUploadHeaderHandle.buildColumnMap(List.of("订单号", "快递单号", "快递公司"), title);
		assertEquals("order_id", column.get(0));
		assertEquals("delivery_code", column.get(1));
		assertEquals("delivery_corp_name", column.get(2));
		assertFalse(title.isNeed().containsKey("子订单号"));
	}

	@Test
	void optionalColumnsMapWhenPresent() {
		UploadHeaderTitle title = EspierUploadHeaderCatalog.NORMAL_ORDERS();
		Map<Integer, String> column =
				EspierUploadHeaderHandle.buildColumnMap(
						List.of("订单号", "快递单号", "快递公司", "子订单号", "商品编码", "发货数量"), title);
		assertEquals("order_item_id", column.get(3));
		assertEquals("item_bn", column.get(4));
		assertEquals("delivery_num", column.get(5));
		Map<String, Object> row =
				EspierUploadHeaderHandle.preRowHandle(column, List.of("100", "YD1", "顺丰", "55", "BN1", "2"));
		assertEquals("55", row.get("order_item_id"));
		assertEquals("BN1", row.get("item_bn"));
		assertEquals("2", row.get("delivery_num"));
	}

	@Test
	void missingOptionalColumnsAreAbsentFromRow() {
		UploadHeaderTitle title = EspierUploadHeaderCatalog.NORMAL_ORDERS();
		Map<Integer, String> column =
				EspierUploadHeaderHandle.buildColumnMap(List.of("订单号", "快递单号", "快递公司"), title);
		Map<String, Object> row = EspierUploadHeaderHandle.preRowHandle(column, List.of("100", "YD1", "顺丰"));
		assertNull(row.get("order_item_id"));
		assertNull(row.get("item_bn"));
		assertNull(row.get("delivery_num"));
	}
}
