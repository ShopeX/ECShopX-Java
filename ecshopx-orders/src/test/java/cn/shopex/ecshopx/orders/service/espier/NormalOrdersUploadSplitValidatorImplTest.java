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

package cn.shopex.ecshopx.orders.service.espier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrdersUploadSplitValidatorImplTest {

	private static final long COMPANY_ID = 1L;
	private static final long ORDER_ID = 9003L;
	private static final long SUPPLIER_ID = 18L;
	private static final long ITEM_ID_11 = 11L;
	private static final long ITEM_ID_12 = 12L;

	@Mock NormalOrdersMapper normalOrdersMapper;
	@Mock SupplierOrderMapper supplierOrderMapper;
	@Mock NormalOrdersItemsMapper normalOrdersItemsMapper;

	@InjectMocks NormalOrdersUploadSplitValidatorImpl validator;

	@Test
	void masterOrderMissing_bothRowsOrderNotFound() {
		List<Map<String, Object>> segment = twoSplitRows(ITEM_ID_11, "BN-1", "1", ITEM_ID_12, "BN-2", "1");

		List<String> result = validator.validate(COMPANY_ID, 0L, segment);

		assertThat(result).containsExactly("订单不存在", "订单不存在");
	}

	@Test
	void masterOrderNotPay_bothRowsUnpaid() {
		stubMasterOrder(order -> order.setOrderStatus("NOTPAY"));

		List<String> result = validator.validate(COMPANY_ID, 0L, twoSplitRows());

		assertThat(result).containsExactly("订单未支付，不能发货", "订单未支付，不能发货");
	}

	@Test
	void masterOrderCancel_bothRowsCancelled() {
		stubMasterOrder(order -> order.setOrderStatus("CANCEL"));

		List<String> result = validator.validate(COMPANY_ID, 0L, twoSplitRows());

		assertThat(result).containsExactly("订单已取消，不能发货", "订单已取消，不能发货");
	}

	@Test
	void masterOrderRefundPending_bothRowsRefundBlocked() {
		stubMasterOrder(order -> order.setCancelStatus("WAIT_PROCESS"));

		List<String> result = validator.validate(COMPANY_ID, 0L, twoSplitRows());

		assertThat(result).containsExactly("订单有退款待处理，不能发货", "订单有退款待处理，不能发货");
	}

	@Test
	void masterOrderAlreadyDelivered_bothRowsRepeatShipBlocked() {
		stubMasterOrder(order -> order.setDeliveryStatus("DONE"));

		List<String> result = validator.validate(COMPANY_ID, 0L, twoSplitRows());

		assertThat(result).containsExactly("订单已发货，不能重复发货", "订单已发货，不能重复发货");
	}

	@Test
	void masterOrderMerchantReceipt_bothRowsNeedSelfDeliveryStatus() {
		stubMasterOrder(order -> order.setReceiptType("merchant"));

		List<String> result = validator.validate(COMPANY_ID, 0L, twoSplitRows());

		assertThat(result).containsExactly("自配送订单发货必须选择配送状态", "自配送订单发货必须选择配送状态");
	}

	@Test
	void supplierOrderMissing_bothRowsOrderNotFound() {
		stubMasterOrder(order -> order.setOrderStatus("PAYED"));

		List<String> result = validator.validate(COMPANY_ID, SUPPLIER_ID, twoSplitRows());

		assertThat(result).containsExactly("订单不存在", "订单不存在");
	}

	@Test
	void supplierImportUsesSupplierOrderStatus_notMasterPaid() {
		stubMasterOrder(order -> order.setOrderStatus("PAYED"));
		SupplierOrder supplierOrder = new SupplierOrder();
		supplierOrder.setCompanyId(COMPANY_ID);
		supplierOrder.setOrderId(ORDER_ID);
		supplierOrder.setSupplierId((int) SUPPLIER_ID);
		supplierOrder.setOrderStatus("NOTPAY");
		when(supplierOrderMapper.selectOne(any())).thenReturn(supplierOrder);

		List<String> result = validator.validate(COMPANY_ID, SUPPLIER_ID, twoSplitRows());

		assertThat(result).containsExactly("订单未支付，不能发货", "订单未支付，不能发货");
	}

	@Test
	void lineItemNotOnOrder_firstRowOnly() {
		stubShippableMasterOrder();
		when(normalOrdersItemsMapper.selectById(ITEM_ID_11)).thenReturn(null);
		stubItem(item(ITEM_ID_12, "BN-2", 0));

		List<String> result =
				validator.validate(
						COMPANY_ID,
						0L,
						twoSplitRows(ITEM_ID_11, "BN-1", "1", ITEM_ID_12, "BN-2", "1"));

		assertThat(result).containsExactly("子订单号不属于该订单", "同一包裹存在错误行，本次未发货");
	}

	@Test
	void platformImportSupplierLineItem_supplierMustShip() {
		stubShippableMasterOrder();
		stubItem(item(ITEM_ID_11, "BN-1", SUPPLIER_ID));

		List<String> result =
				validator.validate(COMPANY_ID, 0L, segmentRow(ITEM_ID_11, "BN-1", "1"));

		assertThat(result).containsExactly("供应商商品请由供应商发货");
	}

	@Test
	void supplierImportWrongSupplierLineItem_incorrectGoods() {
		stubShippableMasterOrder();
		stubSupplierOrder();
		stubItem(item(ITEM_ID_11, "BN-1", 19));

		List<String> result =
				validator.validate(COMPANY_ID, SUPPLIER_ID, segmentRow(ITEM_ID_11, "BN-1", "1"));

		assertThat(result).containsExactly("发货商品不正确");
	}

	@Test
	void itemBnCaseMismatch_bnDoesNotMatch() {
		stubShippableMasterOrder();
		stubItem(item(ITEM_ID_11, "BN-1", 0));

		List<String> result =
				validator.validate(COMPANY_ID, 0L, segmentRow(ITEM_ID_11, "bn-1", "1"));

		assertThat(result).containsExactly("商品编码与子订单号不匹配");
	}

	@Test
	void duplicateSubOrderInSegment_bothRowsDuplicate() {
		stubShippableMasterOrder();
		stubItem(item(ITEM_ID_11, "BN-1", 0, 5, 1, 1));

		List<String> result =
				validator.validate(
						COMPANY_ID,
						0L,
						twoSplitRows(ITEM_ID_11, "BN-1", "1", ITEM_ID_11, "BN-1", "1"));

		assertThat(result).containsExactly("同一包裹中子订单号重复", "同一包裹中子订单号重复");
	}

	@Test
	void duplicateSubOrderWithBnMismatch_bnErrorAndDuplicate() {
		stubShippableMasterOrder();
		stubItem(item(ITEM_ID_11, "BN-1", 0, 5, 1, 1));

		List<String> result =
				validator.validate(
						COMPANY_ID,
						0L,
						twoSplitRows(ITEM_ID_11, "bn-1", "1", ITEM_ID_11, "BN-1", "1"));

		assertThat(result)
				.containsExactly("商品编码与子订单号不匹配", "同一包裹中子订单号重复");
	}

	@Test
	void lineItemFullyShipped_noRemain() {
		stubShippableMasterOrder();
		stubItem(item(ITEM_ID_11, "BN-1", 0, 5, 4, 1));

		List<String> result =
				validator.validate(COMPANY_ID, 0L, segmentRow(ITEM_ID_11, "BN-1", "1"));

		assertThat(result).containsExactly("发货商品已发货");
	}

	@Test
	void deliveryQtyExceedsRemain_overQuantity() {
		stubShippableMasterOrder();
		stubItem(item(ITEM_ID_11, "BN-1", 0, 5, 1, 1));

		List<String> result =
				validator.validate(COMPANY_ID, 0L, segmentRow(ITEM_ID_11, "BN-1", "4"));

		assertThat(result).containsExactly("发货数量超过剩余可发数量");
	}

	@Test
	void oneLineOverQty_otherLineSiblingFailure() {
		stubShippableMasterOrder();
		stubItem(item(ITEM_ID_11, "BN-1", 0, 5, 1, 1));
		stubItem(item(ITEM_ID_12, "BN-2", 0, 5, 1, 1));

		List<String> result =
				validator.validate(
						COMPANY_ID,
						0L,
						twoSplitRows(ITEM_ID_11, "BN-1", "4", ITEM_ID_12, "BN-2", "1"));

		assertThat(result)
				.containsExactly("发货数量超过剩余可发数量", "同一包裹存在错误行，本次未发货");
	}

	@Test
	void deliveryQtyEqualsRemain_passesRow() {
		stubShippableMasterOrder();
		stubItem(item(ITEM_ID_11, "BN-1", 0, 5, 1, 1));

		List<String> result =
				validator.validate(COMPANY_ID, 0L, segmentRow(ITEM_ID_11, "BN-1", "3"));

		assertThat(result).containsExactly((String) null);
	}

	@Test
	void splitRowOrderIdWithLeadingQuoteAndTab_resolvesToOrder9003() {
		stubShippableMasterOrder();
		stubItem(item(ITEM_ID_11, "BN-1", 0));

		Map<String, Object> row = splitRow(ITEM_ID_11, "BN-1", "1");
		row.put("order_id", "\"9003\t\"");

		List<String> result = validator.validate(COMPANY_ID, 0L, List.of(row));

		assertThat(result).doesNotContain("订单不存在");
		assertThat(result).containsExactly((String) null);
	}

	private void stubShippableMasterOrder() {
		stubMasterOrder(
				order -> {
					order.setOrderStatus("PAYED");
					order.setCancelStatus("NO_APPLY_CANCEL");
					order.setDeliveryStatus("PENDING");
					order.setReceiptType("logistics");
				});
	}

	private void stubMasterOrder(java.util.function.Consumer<NormalOrders> customizer) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(COMPANY_ID);
		order.setOrderId(ORDER_ID);
		order.setOrderStatus("PAYED");
		order.setCancelStatus("NO_APPLY_CANCEL");
		order.setDeliveryStatus("PENDING");
		order.setReceiptType("logistics");
		customizer.accept(order);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
	}

	private void stubSupplierOrder() {
		SupplierOrder supplierOrder = new SupplierOrder();
		supplierOrder.setCompanyId(COMPANY_ID);
		supplierOrder.setOrderId(ORDER_ID);
		supplierOrder.setSupplierId((int) SUPPLIER_ID);
		supplierOrder.setOrderStatus("PAYED");
		supplierOrder.setCancelStatus("NO_APPLY_CANCEL");
		supplierOrder.setDeliveryStatus("PENDING");
		supplierOrder.setReceiptType("logistics");
		when(supplierOrderMapper.selectOne(any())).thenReturn(supplierOrder);
	}

	private void stubItem(NormalOrdersItems item) {
		when(normalOrdersItemsMapper.selectById(item.getId())).thenReturn(item);
	}

	private static NormalOrdersItems item(long id, String itemBn, long supplierId) {
		return item(id, itemBn, supplierId, 5, 1, 1);
	}

	private static NormalOrdersItems item(
			long id,
			String itemBn,
			long supplierId,
			int num,
			int deliveryItemNum,
			int cancelItemNum) {
		NormalOrdersItems item = new NormalOrdersItems();
		item.setId(id);
		item.setCompanyId(COMPANY_ID);
		item.setOrderId(ORDER_ID);
		item.setItemBn(itemBn);
		item.setSupplierId((int) supplierId);
		item.setNum(num);
		item.setDeliveryItemNum(deliveryItemNum);
		item.setCancelItemNum(cancelItemNum);
		return item;
	}

	private static List<Map<String, Object>> twoSplitRows() {
		return twoSplitRows(ITEM_ID_11, "BN-1", "1", ITEM_ID_12, "BN-2", "1");
	}

	private static List<Map<String, Object>> twoSplitRows(
			long itemId1,
			String bn1,
			String qty1,
			long itemId2,
			String bn2,
			String qty2) {
		return List.of(
				splitRow(itemId1, bn1, qty1),
				splitRow(itemId2, bn2, qty2));
	}

	private static List<Map<String, Object>> segmentRow(long itemId, String bn, String qty) {
		return List.of(splitRow(itemId, bn, qty));
	}

	private static Map<String, Object> splitRow(long itemId, String bn, String qty) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("order_id", String.valueOf(ORDER_ID));
		row.put("delivery_code", "YD1");
		row.put("delivery_corp_name", "顺丰");
		row.put("order_item_id", String.valueOf(itemId));
		row.put("item_bn", bn);
		row.put("delivery_num", qty);
		return row;
	}
}
