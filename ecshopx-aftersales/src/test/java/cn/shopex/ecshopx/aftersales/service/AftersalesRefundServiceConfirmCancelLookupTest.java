package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.AftersalesRefundJobDispatchPublisher;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AftersalesRefundServiceConfirmCancelLookupTest {

	@Mock
	private AftersalesRefundMapper aftersalesRefundMapper;

	@Mock
	private AftersalesRefundJobDispatchPublisher aftersalesRefundJobDispatchPublisher;

	private AftersalesRefundService service;

	@BeforeEach
	void init() {
		service = new AftersalesRefundService(aftersalesRefundMapper, aftersalesRefundJobDispatchPublisher);
	}

	@Test
	@DisplayName("未传 refund_bn：只查 READY，按 create_time DESC 取最新待审单")
	void findSingleForConfirmCancel_withoutRefundBn_queriesLatestReadyOnly() {
		AftersalesRefund pending = new AftersalesRefund();
		pending.setRefundBn(9002L);
		pending.setRefundStatus("READY");
		pending.setCreateTime(100);
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(pending);

		AftersalesRefund found =
				service.findSingleForConfirmCancel(
						1L,
						5352863000075097L,
						0L,
						null,
						List.of("READY", "AUDIT_SUCCESS", "SUCCESS"));

		assertThat(found).isSameAs(pending);
	}

	@Test
	@DisplayName("未传 refund_bn 且无 READY：返回 null")
	void findSingleForConfirmCancel_withoutRefundBn_noReady_returnsNull() {
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(null);

		AftersalesRefund found =
				service.findSingleForConfirmCancel(
						1L, 100L, 0L, null, List.of("READY", "AUDIT_SUCCESS"));

		assertThat(found).isNull();
	}

	@Test
	@DisplayName("传 refund_bn：按 refund_bn 精确匹配")
	void findSingleForConfirmCancel_withRefundBn_queriesByRefundBn() {
		AftersalesRefund matched = new AftersalesRefund();
		matched.setRefundBn(9001L);
		matched.setRefundStatus("READY");
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(matched);

		AftersalesRefund found =
				service.findSingleForConfirmCancel(1L, 100L, 0L, "9001", List.of("READY"));

		assertThat(found).isSameAs(matched);
	}
}
