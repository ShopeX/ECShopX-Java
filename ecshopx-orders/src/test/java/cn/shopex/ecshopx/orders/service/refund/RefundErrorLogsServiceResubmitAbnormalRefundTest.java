package cn.shopex.ecshopx.orders.service.refund;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.RefundErrorLogs;
import cn.shopex.ecshopx.orders.mapper.RefundErrorLogsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundErrorLogsServiceResubmitAbnormalRefundTest {

	@Mock
	private RefundErrorLogsMapper refundErrorLogsMapper;

	@Mock
	private AftersalesRefundDoRefundService aftersalesRefundDoRefundService;

	@Mock
	private AftersalesRefundMapper aftersalesRefundMapper;

	@BeforeAll
	static void initTables() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), RefundErrorLogs.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
	}

	@Test
	void resubmitRefund_abnormalStatus_rejectsBeforeDoRefund() {
		RefundErrorLogs log = new RefundErrorLogs();
		log.setId(7L);
		log.setDataJson("{\"refund_bn\":9001,\"company_id\":1}");
		when(refundErrorLogsMapper.selectById(7L)).thenReturn(log);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setCompanyId(1L);
		refund.setRefundBn(9001L);
		refund.setRefundStatus(AftersalesRefundService.REFUND_STATUS_DUP_PAY);
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(refund);

		RefundErrorLogsService service =
				new RefundErrorLogsService(
						refundErrorLogsMapper,
						aftersalesRefundDoRefundService,
						new ObjectMapper(),
						aftersalesRefundMapper);

		assertThatThrownBy(() -> service.resubmitRefund("7"))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("支付完结异常退款单不支持错误日志重提");

		verify(aftersalesRefundDoRefundService, never())
				.doRefund(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyBoolean());
	}
}