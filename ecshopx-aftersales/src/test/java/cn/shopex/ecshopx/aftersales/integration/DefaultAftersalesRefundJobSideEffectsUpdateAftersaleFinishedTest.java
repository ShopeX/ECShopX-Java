package cn.shopex.ecshopx.aftersales.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundOnlineRefundPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.TradeByIdReadPort;
import cn.shopex.ecshopx.common.port.promotions.BargainOrderActivityStatusPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Aligns {@link DefaultAftersalesRefundJobSideEffects#updateAftersaleFinished} with PHP
 * {@code RefundJob::__updateAfterSaleFinish}: aftersales / aftersales_detail → status 2, progress 4.
 */
class DefaultAftersalesRefundJobSideEffectsUpdateAftersaleFinishedTest {

	private AftersalesMapper aftersalesMapper;
	private AftersalesDetailMapper aftersalesDetailMapper;
	private DefaultAftersalesRefundJobSideEffects sideEffects;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
	}

	@BeforeEach
	void setUp() {
		aftersalesMapper = mock(AftersalesMapper.class);
		aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		sideEffects =
				new DefaultAftersalesRefundJobSideEffects(
						mock(AftersalesRefundOnlineRefundPort.class),
						mock(TradeRefundFinishEventDispatchPublisher.class),
						aftersalesMapper,
						aftersalesDetailMapper,
						mock(OrderNormalOrderHeaderReadPort.class),
						mock(AftersalesRefundMapper.class),
						mock(TradeByIdReadPort.class),
						mock(BargainOrderActivityStatusPort.class));
		when(aftersalesMapper.update(isNull(), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(isNull(), any())).thenReturn(1);
	}

	@Test
	@DisplayName("售后退款完成：主表与明细置 aftersales_status=2、progress=4")
	void updateAftersaleFinished_writesStatus2Progress4OnMainAndDetail() {
		AftersalesRefund refund = new AftersalesRefund();
		refund.setCompanyId(1L);
		refund.setAftersalesBn(202608255427266L);
		refund.setRefundBn(2202608258337278667L);

		sideEffects.updateAftersaleFinished(refund);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<Aftersales>> mainCap =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(aftersalesMapper).update(isNull(), mainCap.capture());
		assertThat(mainCap.getValue().getSqlSet())
				.contains("aftersales_status")
				.contains("progress");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<AftersalesDetail>> detailCap =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(aftersalesDetailMapper).update(isNull(), detailCap.capture());
		assertThat(detailCap.getValue().getSqlSet())
				.contains("aftersales_status")
				.contains("progress");
	}

	@Test
	@DisplayName("无 aftersales_bn（售前退款）不写售后表")
	void updateAftersaleFinished_skipsWhenNoAftersalesBn() {
		AftersalesRefund refund = new AftersalesRefund();
		refund.setCompanyId(1L);
		refund.setAftersalesBn(0L);
		refund.setRefundBn(99L);

		sideEffects.updateAftersaleFinished(refund);

		verify(aftersalesMapper, never()).update(any(), any());
		verify(aftersalesDetailMapper, never()).update(any(), any());
	}
}
