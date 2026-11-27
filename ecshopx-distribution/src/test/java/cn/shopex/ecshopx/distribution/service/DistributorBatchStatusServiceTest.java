package cn.shopex.ecshopx.distribution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorBatchStatusServiceTest {

	@Mock
	private DistributorWriteRepository distributorWriteRepository;

	@Mock
	private DistributorUpdateOrchestrator distributorUpdateOrchestrator;

	@InjectMocks
	private DistributorBatchStatusService service;

	@Test
	@DisplayName("空 ids 拒绝")
	void emptyIdsRejected() {
		assertThrows(
				BadRequestException.class,
				() -> service.batch(7L, Map.of(), List.of(), "false", null));
	}

	@Test
	@DisplayName("超过 100 拒绝")
	void overLimitRejected() {
		List<Long> ids = IntStream.rangeClosed(1, 101).mapToLong(i -> i).boxed().toList();
		assertThrows(
				BadRequestException.class,
				() -> service.batch(7L, Map.of(), ids, "false", null));
	}

	@Test
	@DisplayName("非法 is_valid 拒绝")
	void invalidIsValidRejected() {
		assertThrows(
				BadRequestException.class,
				() -> service.batch(7L, Map.of(), List.of(1L), "enabled", null));
	}

	@Test
	@DisplayName("部分成功：改状态 / 已撤店 skip / 不存在 failed / 自营 skip / 同态 skip")
	void batchPartialSuccess() {
		when(distributorWriteRepository.selectByCompanyAndIdIncludeDeleted(7L, 1L))
				.thenReturn(Optional.of(shop(1L, "true", 0)));
		when(distributorWriteRepository.selectByCompanyAndIdIncludeDeleted(7L, 2L))
				.thenReturn(Optional.of(shop(2L, "delete", 0)));
		when(distributorWriteRepository.selectByCompanyAndIdIncludeDeleted(7L, 3L))
				.thenReturn(Optional.empty());
		when(distributorWriteRepository.selectByCompanyAndIdIncludeDeleted(7L, 4L))
				.thenReturn(Optional.of(shop(4L, "true", 1)));
		when(distributorWriteRepository.selectByCompanyAndIdIncludeDeleted(7L, 5L))
				.thenReturn(Optional.of(shop(5L, "false", 0)));
		when(distributorUpdateOrchestrator.update(
						any(), any(), eq(1L), nullable(String.class), anySet()))
				.thenReturn(new LinkedHashMap<>());

		Map<String, Object> out =
				service.batch(7L, Map.of("company_id", 7L), List.of(1L, 2L, 3L, 4L, 5L), "false", null);

		assertEquals(List.of(1L), out.get("success"));
		assertEquals(List.of(2L, 4L, 5L), out.get("skipped"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> failed = (List<Map<String, Object>>) out.get("failed");
		assertEquals(1, failed.size());
		assertEquals(3L, failed.get(0).get("distributor_id"));
		verify(distributorUpdateOrchestrator, times(1))
				.update(any(), any(), eq(1L), isNull(), anySet());
		verify(distributorUpdateOrchestrator, never())
				.update(any(), any(), eq(2L), nullable(String.class), anySet());
	}

	@Test
	@DisplayName("全部 skip 时抛无可操作店铺")
	void allSkippedRejected() {
		when(distributorWriteRepository.selectByCompanyAndIdIncludeDeleted(7L, 2L))
				.thenReturn(Optional.of(shop(2L, "delete", 0)));
		assertThrows(
				BadRequestException.class,
				() -> service.batch(7L, Map.of(), List.of(2L), "closed", null));
	}

	private static Distributor shop(long id, String isValid, int distributorSelf) {
		Distributor d = new Distributor();
		d.setDistributorId(id);
		d.setCompanyId(7L);
		d.setIsValid(isValid);
		d.setDistributorSelf(distributorSelf);
		return d;
	}
}
