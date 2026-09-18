package cn.shopex.ecshopx.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderInvoiceRedFromAftersalesPortImplTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderInvoice.class);
	}

	@Mock
	private OrderInvoiceMapper orderInvoiceMapper;

	@InjectMocks
	private OrderInvoiceRedFromAftersalesPortImpl port;

	@Test
	void redInvoice_emptyPayload_noDbTouch() {
		port.redInvoice(Map.of());
		verify(orderInvoiceMapper, never()).update(any(), any());
	}

	@Test
	void redInvoice_missingOrderId_noDbTouch() {
		Map<String, Object> job = new LinkedHashMap<>();
		job.put("company_id", 1L);
		port.redInvoice(job);
		verify(orderInvoiceMapper, never()).update(any(), any());
	}

	@SuppressWarnings("unchecked")
	@Test
	void redInvoice_setsWasteAndOptionalRedContent() {
		when(orderInvoiceMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
		Map<String, Object> job = new LinkedHashMap<>();
		job.put("company_id", 10L);
		job.put("order_id", "O99");
		job.put("aftersales_bn", "AS-BN-1");
		port.redInvoice(job);
		ArgumentCaptor<LambdaUpdateWrapper<OrderInvoice>> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(orderInvoiceMapper, times(1)).update(isNull(), cap.capture());
		LambdaUpdateWrapper<OrderInvoice> uw = cap.getValue();
		assertThat(uw.getSqlSegment()).contains("invoice_status").contains("company_id").contains("order_id");
		assertThat(uw.getSqlSet()).contains("invoice_status").contains("red_content");
	}
}
