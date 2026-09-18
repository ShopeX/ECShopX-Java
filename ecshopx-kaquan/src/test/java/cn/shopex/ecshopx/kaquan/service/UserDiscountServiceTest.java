package cn.shopex.ecshopx.kaquan.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.excard.ExcardInventoryPort;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class UserDiscountServiceTest {

	@Mock
	private UserDiscountMapper userDiscountMapper;

	@Mock
	private ExcardInventoryPort excardInventoryPort;

	@Mock
	private PlatformTransactionManager platformTransactionManager;

	private UserDiscountService userDiscountService;

	@BeforeEach
	void setUp() {
		lenient().when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
				.thenReturn(new SimpleTransactionStatus());
		userDiscountService = new UserDiscountService(userDiscountMapper, excardInventoryPort, platformTransactionManager);
	}

	@Test
	@DisplayName("analysis §3 步骤 1+8: 5 轮满 200 条仍继续直到第 5 轮结束")
	void outerStopsAtFiveRoundsWithFullBatches() {
		List<Long> twoHundred = LongStream.rangeClosed(1, 200).boxed().toList();
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(twoHundred, twoHundred, twoHundred, twoHundred, twoHundred);
		when(userDiscountMapper.selectById(anyLong())).thenAnswer(invocation -> {
			long id = invocation.getArgument(0);
			UserDiscount u = new UserDiscount();
			u.setId(id);
			u.setStatus(1);
			u.setCompanyId(1L);
			return u;
		});
		userDiscountService.scheduleCancelExCard();
		verify(userDiscountMapper, times(5)).selectExpiredLockedCardIds(anyInt(), eq(200));
	}

	@Test
	@DisplayName("analysis §3 步骤 2: 首查为空则结束")
	void emptyFirstBatch() {
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(Collections.emptyList());
		userDiscountService.scheduleCancelExCard();
		verify(excardInventoryPort, never()).minusItemStore(
				anyLong(), anyLong(), anyInt(), anyLong(), anyBoolean());
		verify(userDiscountMapper, never()).update(isNull(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 3+7: 批不足 200 不再进下一轮")
	void batchSmallerThanPageBreaksOuter() {
		List<Long> three = List.of(10L, 11L, 12L);
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(three);
		when(userDiscountMapper.selectById(anyLong())).thenAnswer(invocation -> {
			long id = invocation.getArgument(0);
			UserDiscount u = new UserDiscount();
			u.setId(id);
			u.setStatus(1);
			u.setCompanyId(1L);
			return u;
		});
		userDiscountService.scheduleCancelExCard();
		verify(userDiscountMapper, times(1)).selectExpiredLockedCardIds(anyInt(), eq(200));
	}

	@Test
	@DisplayName("analysis §3 步骤 2+7+8: 满 200 后下一轮，第二轮空则停")
	void fullPageThenEmptySecondQuery() {
		List<Long> twoHundred = LongStream.rangeClosed(1, 200).boxed().toList();
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(twoHundred, Collections.emptyList());
		when(userDiscountMapper.selectById(anyLong())).thenAnswer(invocation -> {
			long id = invocation.getArgument(0);
			UserDiscount u = new UserDiscount();
			u.setId(id);
			u.setStatus(1);
			u.setCompanyId(1L);
			return u;
		});
		userDiscountService.scheduleCancelExCard();
		verify(userDiscountMapper, times(2)).selectExpiredLockedCardIds(anyInt(), eq(200));
	}

	@Test
	@DisplayName("analysis §3 步骤 3: 同批 2 条均处理")
	void twoInBatchBothProcessed() {
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(List.of(1L, 2L));
		UserDiscount a = buildLocked(1L, 1L, "9", "4");
		UserDiscount b = buildLocked(2L, 1L, "9", "4");
		when(userDiscountMapper.selectById(1L)).thenReturn(a);
		when(userDiscountMapper.selectById(2L)).thenReturn(b);
		when(excardInventoryPort.resolveIsTotalStore(1L, 9L, 4L)).thenReturn(true);
		when(excardInventoryPort.minusItemStore(1L, 9L, -1, 4L, true)).thenReturn(true);
		userDiscountService.scheduleCancelExCard();
		verify(excardInventoryPort, times(2)).minusItemStore(1L, 9L, -1, 4L, true);
		verify(userDiscountMapper, times(2)).update(isNull(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 3-A: 非正 id 不查库")
	void skipsNonPositiveId() {
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(new ArrayList<>(List.of(0L, 5L)));
		UserDiscount u = buildLocked(5L, 1L, "1", "1");
		when(userDiscountMapper.selectById(5L)).thenReturn(u);
		when(excardInventoryPort.resolveIsTotalStore(1L, 1L, 1L)).thenReturn(true);
		when(excardInventoryPort.minusItemStore(1L, 1L, -1, 1L, true)).thenReturn(true);
		userDiscountService.scheduleCancelExCard();
		verify(userDiscountMapper, never()).selectById(0L);
		verify(excardInventoryPort, times(1)).minusItemStore(1L, 1L, -1, 1L, true);
	}

	@Test
	@DisplayName("analysis §3 步骤 4: selectById 为 null 则 requireNonNull 失败（NPE）")
	void nullEntityFailsBeforeStatusBranch() {
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(List.of(1L));
		when(userDiscountMapper.selectById(1L)).thenReturn(null);
		assertThrows(NullPointerException.class, () -> userDiscountService.scheduleCancelExCard());
	}

	@Test
	@DisplayName("analysis §3 步骤 4-A: status 非 10 不碰库存、不写回")
	void earlyExitWhenNotLocked() {
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(List.of(3L));
		UserDiscount u = new UserDiscount();
		u.setId(3L);
		u.setStatus(1);
		u.setCompanyId(1L);
		when(userDiscountMapper.selectById(3L)).thenReturn(u);
		userDiscountService.scheduleCancelExCard();
		verify(excardInventoryPort, never()).minusItemStore(
				anyLong(), anyLong(), anyInt(), anyLong(), anyBoolean());
		verify(userDiscountMapper, never()).update(isNull(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 4-A: status 为 null 视为非 10 早退")
	void nullStatusEarlyExit() {
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(List.of(3L));
		UserDiscount u = new UserDiscount();
		u.setId(3L);
		u.setStatus(null);
		u.setCompanyId(1L);
		when(userDiscountMapper.selectById(3L)).thenReturn(u);
		userDiscountService.scheduleCancelExCard();
		verify(excardInventoryPort, never()).minusItemStore(
				anyLong(), anyLong(), anyInt(), anyLong(), anyBoolean());
	}

	@Test
	@DisplayName("analysis §3 步骤 4-B+5+6: minus 返回 false 仍更新卡券")
	void minusFalseStillUpdatesCard() {
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(List.of(8L));
		UserDiscount u = buildLocked(8L, 2L, "7", "3");
		when(userDiscountMapper.selectById(8L)).thenReturn(u);
		when(excardInventoryPort.resolveIsTotalStore(2L, 7L, 3L)).thenReturn(false);
		when(excardInventoryPort.minusItemStore(2L, 7L, -1, 3L, false)).thenReturn(false);
		userDiscountService.scheduleCancelExCard();
		verify(excardInventoryPort, times(1)).minusItemStore(2L, 7L, -1, 3L, false);
		verify(userDiscountMapper, times(1)).update(isNull(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 4-B+5+6: 成功主路径"
			+ " minusItemStore 参数与 resolveIsTotalStore 一致")
	void successPath() {
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(List.of(9L));
		UserDiscount u = buildLocked(9L, 3L, "99", "8");
		when(userDiscountMapper.selectById(9L)).thenReturn(u);
		when(excardInventoryPort.resolveIsTotalStore(3L, 99L, 8L)).thenReturn(true);
		when(excardInventoryPort.minusItemStore(3L, 99L, -1, 8L, true)).thenReturn(true);
		userDiscountService.scheduleCancelExCard();
		verify(excardInventoryPort, times(1)).resolveIsTotalStore(3L, 99L, 8L);
		verify(excardInventoryPort, times(1)).minusItemStore(3L, 99L, -1, 8L, true);
		verify(userDiscountMapper, times(1)).update(isNull(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 3-B: 每卡走事务模板（commit 随批内条数）")
	void perCardTransaction() {
		when(userDiscountMapper.selectExpiredLockedCardIds(anyInt(), eq(200)))
				.thenReturn(List.of(1L, 2L));
		when(userDiscountMapper.selectById(anyLong())).thenAnswer(invocation -> {
			long id = invocation.getArgument(0);
			UserDiscount x = new UserDiscount();
			x.setId(id);
			x.setStatus(1);
			x.setCompanyId(1L);
			return x;
		});
		userDiscountService.scheduleCancelExCard();
		verify(platformTransactionManager, atLeast(2)).getTransaction(any(TransactionDefinition.class));
	}

	private static UserDiscount buildLocked(long id, long companyId, String item, String dist) {
		UserDiscount u = new UserDiscount();
		u.setId(id);
		u.setCompanyId(companyId);
		u.setStatus(10);
		u.setRelItemIds(item);
		u.setRelDistributorIds(dist);
		return u;
	}
}
