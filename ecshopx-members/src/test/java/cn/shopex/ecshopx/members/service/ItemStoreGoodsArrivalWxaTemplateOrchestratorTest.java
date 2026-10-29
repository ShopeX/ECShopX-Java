package cn.shopex.ecshopx.members.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.goods.ItemCompanyIdResolver;
import cn.shopex.ecshopx.common.members.port.MembersSubscribeNoticeItemNamePort;
import cn.shopex.ecshopx.common.port.members.GoodsArrivalWxaSubscribeSendPort;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class ItemStoreGoodsArrivalWxaTemplateOrchestratorTest {

	@Mock
	private ItemCompanyIdResolver itemCompanyIdResolver;

	@Mock
	private GoodsArrivalSubscribeMatchService goodsArrivalSubscribeMatchService;

	@Mock
	private MembersSubscribeNoticeItemNamePort membersSubscribeNoticeItemNamePort;

	@Mock
	private GoodsArrivalWxaSubscribeSendPort goodsArrivalWxaSubscribeSendPort;

	@Mock
	private WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;

	@Mock
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private ItemStoreGoodsArrivalWxaTemplateOrchestrator orchestrator;

	@Test
	@DisplayName("store<=0 skips")
	void skipsWhenNoStore() {
		orchestrator.dispatchIfApplicable(10L, 0, 0L);
		verifyNoInteractions(goodsArrivalSubscribeMatchService, goodsArrivalWxaSubscribeSendPort);
	}

	@Test
	@DisplayName("sends goodsArrivalNotice for wechat subscribers")
	void sendsForPending() {
		when(itemCompanyIdResolver.findCompanyIdByItemId(10L)).thenReturn(Optional.of(1L));
		SubscribeNotice row = new SubscribeNotice();
		row.setOpenId("oid");
		row.setUserId(9L);
		row.setDistributorId(0);
		when(goodsArrivalSubscribeMatchService.listPending(1L, 10L, 0, "wechat"))
				.thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(1L, "yykweishop"))
				.thenReturn(Optional.of("wxapp"));
		when(membersSubscribeNoticeItemNamePort.resolveItemName(1L, 10L, "zh-CN")).thenReturn("商品名");
		when(sharedStringRedisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.get(any(), eq("9"))).thenReturn(null);

		orchestrator.dispatchIfApplicable(10L, 5, 0L);

		verify(goodsArrivalWxaSubscribeSendPort)
				.send(
						argThat(
								m -> "goodsArrivalNotice".equals(m.get("scenes_name"))
										&& "wxapp".equals(m.get("appid"))
										&& "oid".equals(m.get("openid"))
										&& "id=10".equals(m.get("page_query_str"))),
						eq(true));
	}

	@Test
	@DisplayName("headquarters stock event uses row distributor_id for redis appid key")
	void headquartersEventUsesRowDistributorRedisKey() {
		when(itemCompanyIdResolver.findCompanyIdByItemId(10L)).thenReturn(Optional.of(1L));
		SubscribeNotice row = new SubscribeNotice();
		row.setOpenId("oid");
		row.setUserId(9L);
		row.setDistributorId(88);
		when(goodsArrivalSubscribeMatchService.listPending(1L, 10L, 0, "wechat"))
				.thenReturn(List.of(row));
		when(weappAuthorizerAppidRepository.findAuthorizerAppid(1L, "yykweishop"))
				.thenReturn(Optional.of("wxapp"));
		when(membersSubscribeNoticeItemNamePort.resolveItemName(1L, 10L, "zh-CN")).thenReturn("商品名");
		when(sharedStringRedisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.get("arrivalNoticeSub:1:10:88", "9")).thenReturn(null);

		orchestrator.dispatchIfApplicable(10L, 5, 0L);

		verify(hashOperations).get("arrivalNoticeSub:1:10:88", "9");
		verify(goodsArrivalWxaSubscribeSendPort).send(any(), eq(true));
	}
}
