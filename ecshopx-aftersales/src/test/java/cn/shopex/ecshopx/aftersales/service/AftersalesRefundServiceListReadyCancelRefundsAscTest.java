package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.AftersalesRefundJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundAbnormalChannelPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AftersalesRefundServiceListReadyCancelRefundsAscTest {

	@Mock
	private AftersalesRefundMapper aftersalesRefundMapper;

	@Mock
	private AftersalesRefundJobDispatchPublisher aftersalesRefundJobDispatchPublisher;

	@Mock
	private AftersalesRefundAbnormalChannelPort aftersalesRefundAbnormalChannelPort;

	@Mock
	private ObjectProvider<OrderNormalOrderHeaderReadPort> orderNormalOrderHeaderReadPort;

	private AftersalesRefundService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
	}

	@BeforeEach
	void init() {
		service =
				new AftersalesRefundService(
						aftersalesRefundMapper,
						aftersalesRefundJobDispatchPublisher,
						aftersalesRefundAbnormalChannelPort,
						orderNormalOrderHeaderReadPort);
	}

	@Test
	@DisplayName("§4.4 售前自动同意仅拾取 refund_status=READY")
	@SuppressWarnings("unchecked")
	void listReadyCancelRefundsAsc_filtersReadyOnly() {
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		service.listReadyCancelRefundsAsc(1L, 100L, 0L);

		ArgumentCaptor<LambdaQueryWrapper<AftersalesRefund>> cap =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(aftersalesRefundMapper).selectList(cap.capture());
		LambdaQueryWrapper<AftersalesRefund> w = cap.getValue();
		assertThat(w.getSqlSegment()).contains("refund_status");
		assertThat(w.getParamNameValuePairs().values()).contains("READY");
		assertThat(w.getParamNameValuePairs().values())
				.doesNotContain(
						AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL,
						AftersalesRefundService.REFUND_STATUS_DUP_PAY);
	}
}