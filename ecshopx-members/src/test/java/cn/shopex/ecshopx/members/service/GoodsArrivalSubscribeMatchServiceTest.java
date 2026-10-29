package cn.shopex.ecshopx.members.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.goods.DistributorItemTotalStorePort;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.members.mapper.SubscribeNoticeMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoodsArrivalSubscribeMatchServiceTest {

	@Mock
	private SubscribeNoticeMapper subscribeNoticeMapper;

	@Mock
	private DistributorItemTotalStorePort distributorItemTotalStorePort;

	@InjectMocks
	private GoodsArrivalSubscribeMatchService service;

	@Test
	@DisplayName("shop stock event keeps exact distributor_id match")
	void shopEventExactMatch() {
		SubscribeNotice row = row(1L, 88);
		when(subscribeNoticeMapper.selectList(any())).thenReturn(List.of(row));

		List<SubscribeNotice> pending = service.listPending(1L, 10L, 88, "wechat");

		assertThat(pending).containsExactly(row);
		verify(distributorItemTotalStorePort, org.mockito.Mockito.never())
				.filterDistributorIdsUsingTotalStore(any(Long.class), any(Long.class), any());
	}

	@Test
	@DisplayName("headquarters event includes shop rows with is_total_store=true")
	void headquartersEventIncludesTotalStoreShops() {
		SubscribeNotice hq = row(1L, 0);
		SubscribeNotice totalStoreShop = row(2L, 88);
		SubscribeNotice localStoreShop = row(3L, 99);
		when(subscribeNoticeMapper.selectList(any()))
				.thenReturn(List.of(hq, totalStoreShop, localStoreShop));
		when(distributorItemTotalStorePort.filterDistributorIdsUsingTotalStore(
						eq(1L), eq(10L), any()))
				.thenReturn(Set.of(88));

		List<SubscribeNotice> pending = service.listPending(1L, 10L, 0, "wechat");

		assertThat(pending).containsExactly(hq, totalStoreShop);
	}

	private static SubscribeNotice row(long subId, int distributorId) {
		SubscribeNotice n = new SubscribeNotice();
		n.setSubId(subId);
		n.setDistributorId(distributorId);
		n.setOpenId("oid");
		return n;
	}
}
