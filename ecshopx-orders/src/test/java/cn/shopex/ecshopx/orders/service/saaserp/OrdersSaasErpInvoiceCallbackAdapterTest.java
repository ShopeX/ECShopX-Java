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

package cn.shopex.ecshopx.orders.service.saaserp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceItem;
import cn.shopex.ecshopx.orders.domain.OrderInvoices;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceItemMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceLogMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoicesMapper;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceStatusChangeHandler;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OrdersSaasErpInvoiceCallbackAdapterTest {

	private static final long companyId = 10L;
	private static final long invoicePk = 501L;
	private static final long orderId = 9001L;
	private static final String applyBn = "FP-APPLY-1";

	@Mock
	OrderInvoiceMapper orderInvoiceMapper;

	@Mock
	OrderInvoiceItemMapper orderInvoiceItemMapper;

	@Mock
	OrderInvoicesMapper orderInvoicesMapper;

	@Mock
	OrderInvoiceLogMapper orderInvoiceLogMapper;

	@Mock
	NormalOrdersMapper normalOrdersMapper;

	@Mock
	NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	OrderInvoiceStatusChangeHandler orderInvoiceStatusChangeHandler;

	@Mock
	PlatformTransactionManager platformTransactionManager;

	@Mock
	TransactionStatus transactionStatus;

	OrdersSaasErpInvoiceCallbackAdapter adapter;

	@BeforeAll
	static void initMybatisPlusMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
		TableInfoHelper.initTableInfo(assistant, OrderInvoice.class);
		TableInfoHelper.initTableInfo(assistant, OrderInvoiceItem.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrdersItems.class);
	}

	@BeforeEach
	void setUp() {
		lenient().when(platformTransactionManager.getTransaction(any())).thenReturn(transactionStatus);
		TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
		adapter =
				new OrdersSaasErpInvoiceCallbackAdapter(
						orderInvoiceMapper,
						orderInvoiceItemMapper,
						orderInvoicesMapper,
						orderInvoiceLogMapper,
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderInvoiceStatusChangeHandler,
						new ObjectMapper(),
						transactionTemplate);
	}

	@Test
	void memoTwo_updatesSuccessStatusAndInvoiceFileUrl() throws Exception {
		OrderInvoice row = baseInvoice("inProgress");
		when(orderInvoiceMapper.selectOne(any())).thenReturn(row);
		when(orderInvoiceMapper.update(isNull(), any())).thenReturn(1);
		OrderInvoice refreshed = baseInvoice("inProgress");
		refreshed.setInvoiceStatus("success");
		refreshed.setInvoiceFileUrl("https://pdf.example/blue.pdf");
		when(orderInvoiceMapper.selectById(invoicePk)).thenReturn(refreshed);

		Map<String, String> params = callbackParams("2");
		params.put("pdf_info", "https://pdf.example/blue.pdf");

		Map<String, Object> result = adapter.handle(params);

		assertThat(result.get("success")).isEqualTo(true);
		assertThat(result.get("message")).isEqualTo("开票状态更新成功");
		assertThat(result.get("data")).isNotNull();

		ArgumentCaptor<LambdaUpdateWrapper<OrderInvoice>> captor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(orderInvoiceMapper).update(isNull(), captor.capture());
		String sqlSet = captor.getValue().getSqlSet();
		assertThat(sqlSet).contains("invoice_status");
		assertThat(sqlSet).contains("invoice_file_url");

		Map<String, Object> dataMap =
				new ObjectMapper().readValue(String.valueOf(result.get("data")), Map.class);
		assertThat(dataMap.get("invoice_status")).isEqualTo("success");
		assertThat(dataMap.get("invoice_file_url")).isEqualTo("https://pdf.example/blue.pdf");

		verify(orderInvoicesMapper).insert(org.mockito.ArgumentMatchers.<OrderInvoices>any());
		verify(orderInvoiceStatusChangeHandler)
				.handle(invoicePk, "inProgress", "success");
	}

	@Test
	void memoThree_redFlush_setsItemIsInvoiceThreeAndOrderPartialWhenAnotherItemInvoiced() {
		OrderInvoice row = baseInvoice("inProgress");
		when(orderInvoiceMapper.selectOne(any())).thenReturn(row);
		when(orderInvoiceMapper.update(isNull(), any())).thenReturn(1);
		when(orderInvoiceMapper.selectById(invoicePk)).thenReturn(row);

		OrderInvoiceItem lineOne = new OrderInvoiceItem();
		lineOne.setOid("101");
		lineOne.setOrderId(String.valueOf(orderId));
		lineOne.setCompanyId(companyId);
		OrderInvoiceItem lineTwo = new OrderInvoiceItem();
		lineTwo.setOid("102");
		lineTwo.setOrderId(String.valueOf(orderId));
		lineTwo.setCompanyId(companyId);
		when(orderInvoiceItemMapper.selectList(any())).thenReturn(List.of(lineOne, lineTwo));

		NormalOrdersItems stillInvoiced = new NormalOrdersItems();
		stillInvoiced.setIsInvoice(1);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(stillInvoiced));
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);

		Map<String, Object> result = adapter.handle(callbackParams("3"));

		assertThat(result.get("success")).isEqualTo(true);
		verify(normalOrdersItemsMapper).update(isNull(), any());
		ArgumentCaptor<LambdaUpdateWrapper<NormalOrders>> orderCaptor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(normalOrdersMapper).update(isNull(), orderCaptor.capture());
		assertThat(orderCaptor.getValue().getParamNameValuePairs().values()).contains("PARTAIL");
	}

	@Test
	void foundInvoicePath_repositoryFailure_returnsFailureMessage() {
		OrderInvoice row = baseInvoice("inProgress");
		when(orderInvoiceMapper.selectOne(any())).thenReturn(row);
		doThrow(new RuntimeException("db down")).when(orderInvoiceMapper).update(isNull(), any());

		Map<String, Object> result = adapter.handle(callbackParams("2"));

		assertThat(result.get("success")).isEqualTo(false);
		assertThat(String.valueOf(result.get("message"))).startsWith("开票状态更新失败: ");
		assertThat(result).doesNotContainKey("data");
		verify(platformTransactionManager).rollback(transactionStatus);
	}

	@Test
	void missingInvoiceRow_setsOrderDoneAndItemsInvoiced() {
		when(orderInvoiceMapper.selectOne(any())).thenReturn(null);
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(isNull(), any())).thenReturn(2);

		Map<String, String> params = new LinkedHashMap<>();
		params.put("invoice_no", applyBn);
		params.put("order_bn", String.valueOf(orderId));

		Map<String, Object> result = adapter.handle(params);

		assertThat(result.get("success")).isEqualTo(true);
		assertThat(result.get("message")).isEqualTo("开票状态更新成功");
		assertThat(result).doesNotContainKey("data");

		ArgumentCaptor<LambdaUpdateWrapper<NormalOrders>> orderCaptor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(normalOrdersMapper).update(isNull(), orderCaptor.capture());
		assertThat(orderCaptor.getValue().getParamNameValuePairs().values()).contains("DONE");
		verify(normalOrdersItemsMapper).update(isNull(), any());
	}

	private static OrderInvoice baseInvoice(String status) {
		OrderInvoice inv = new OrderInvoice();
		inv.setId(invoicePk);
		inv.setCompanyId(companyId);
		inv.setInvoiceApplyBn(applyBn);
		inv.setInvoiceStatus(status);
		inv.setUserId(7L);
		return inv;
	}

	private static Map<String, String> callbackParams(String memo) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("invoice_no", applyBn);
		params.put("invoice_memo", memo);
		params.put("order_bn", String.valueOf(orderId));
		params.put("blue_invoice_code", "CODE-1");
		params.put("blue_invoice_no", "NO-1");
		params.put("invoice_code", "CODE-1");
		params.put("invoice_time", "2026-10-28 12:00:00");
		return params;
	}
}
