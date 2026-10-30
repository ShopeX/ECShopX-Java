package cn.shopex.ecshopx.members.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.goods.DistributorItemTotalStorePort;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.members.mapper.SubscribeNoticeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class GoodsArrivalSubscribeMatchServiceTest {

	@Mock
	private SubscribeNoticeMapper subscribeNoticeMapper;

	@Mock
	private DistributorItemTotalStorePort distributorItemTotalStorePort;

	@Mock
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	@InjectMocks
	private GoodsArrivalSubscribeMatchService service;

	@BeforeEach
	void initMybatisLambdaCache() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), SubscribeNotice.class);
	}

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

	@Test
	@DisplayName("markSuccess clears arrivalNoticeSub redis field for shop subscribe")
	void markSuccessClearsRedisForShopSubscribe() {
		SubscribeNotice row = row(11L, 88);
		row.setCompanyId(1L);
		row.setUserId(9L);
		row.setRelId(10L);
		row.setSource("wechat");
		when(subscribeNoticeMapper.selectList(any())).thenReturn(List.of(row));
		when(sharedStringRedisTemplate.opsForHash()).thenReturn(hashOperations);

		service.markSuccess(1L, 10L, 88);

		verify(subscribeNoticeMapper).update(eq(null), any());
		verify(hashOperations).delete("arrivalNoticeSub:1:10:88", "9");
	}

	@Test
	@DisplayName("markSuccess clears headquarters redis key without distributor suffix")
	void markSuccessClearsRedisForHeadquartersSubscribe() {
		SubscribeNotice row = row(12L, 0);
		row.setCompanyId(1L);
		row.setUserId(7L);
		row.setRelId(10L);
		row.setSource("wechat");
		when(subscribeNoticeMapper.selectList(any())).thenReturn(List.of(row));
		when(distributorItemTotalStorePort.filterDistributorIdsUsingTotalStore(
						anyLong(), anyLong(), any()))
				.thenReturn(Set.of());
		when(sharedStringRedisTemplate.opsForHash()).thenReturn(hashOperations);

		service.markSuccess(1L, 10L, 0);

		ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
		verify(hashOperations).delete(keyCap.capture(), eq("7"));
		assertThat(keyCap.getValue()).isEqualTo("arrivalNoticeSub:1:10");
	}

	private static SubscribeNotice row(long subId, int distributorId) {
		SubscribeNotice n = new SubscribeNotice();
		n.setSubId(subId);
		n.setDistributorId(distributorId);
		n.setOpenId("oid");
		return n;
	}
}
