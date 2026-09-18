package cn.shopex.ecshopx.promotions.service;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.event.MarketingActivityCommittedEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketingActivityPostCommitJobsServiceShelvesDispatchTest {

	@Mock
	private SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher;

	@InjectMocks
	private MarketingActivityPostCommitJobsService service;

	@Test
	void enqueueSeckillEndSalespersonItemsShelves_normalType_delegatesPublishWithSeckillActivityType() {
		long companyId = 9L;
		long seckillId = 42L;
		service.enqueueSeckillEndSalespersonItemsShelves(companyId, seckillId, "normal");
		verify(salespersonItemsShelvesJobDispatchPublisher).publish(companyId, seckillId, "seckill");
	}

	@Test
	void enqueueGroupSalespersonItemsShelves_delegatesPublishWithGroupActivityType() {
		long companyId = 9L;
		long groupsActivityId = 42L;
		service.enqueueGroupSalespersonItemsShelves(companyId, groupsActivityId);
		verify(salespersonItemsShelvesJobDispatchPublisher).publish(companyId, groupsActivityId, "group");
	}

	@Test
	void enqueueSeckillEndSalespersonItemsShelves_nonNormalType_delegatesPublishWithLimitedTimeSale() {
		long companyId = 9L;
		long seckillId = 42L;
		service.enqueueSeckillEndSalespersonItemsShelves(companyId, seckillId, "other");
		verify(salespersonItemsShelvesJobDispatchPublisher).publish(companyId, seckillId, "limited_time_sale");
	}

	@Test
	void run_whenDispatchSalespersonItemsShelves_delegatesPlaceholderWithMarketingPayload() {
		long companyId = 11L;
		long marketingId = 22L;
		String marketingType = "full_minus";
		var event =
				new MarketingActivityCommittedEvent(
						companyId,
						marketingId,
						marketingType,
						0,
						0,
						false,
						null,
						List.of(),
						Map.<Long, BigDecimal>of(),
						true);
		service.run(event);
		verify(salespersonItemsShelvesJobDispatchPublisher).publish(companyId, marketingId, marketingType);
	}
}
