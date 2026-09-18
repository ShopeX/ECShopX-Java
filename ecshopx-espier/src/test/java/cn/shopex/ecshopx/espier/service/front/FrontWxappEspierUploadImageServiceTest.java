package cn.shopex.ecshopx.espier.service.front;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class FrontWxappEspierUploadImageServiceTest {

	@Mock
	private StringRedisTemplate membersRedis;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private ValueOperations<String, String> valueOps;

	@Test
	@DisplayName("日配额已满时抛出 ResourceException 且不执行 INCR")
	void assertUnderDailyUploadImageLimitAndIncrement_quotaFull_throwsResourceException() {
		when(membersRedis.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("uploadImageNum:1:2")).thenReturn("10");
		FrontWxappEspierUploadImageService svc = new FrontWxappEspierUploadImageService(
				membersRedis, fileStorageService, 10);
		ResourceException ex = assertThrows(
				ResourceException.class,
				() -> svc.getPicUploadToken(1L, 2L, "a.png", "g", "image"));
		assertEquals("今日图片上传超过限制", ex.getMessage());
		verify(valueOps, never()).increment(anyString());
		verify(fileStorageService, never()).getUploadToken(anyString(), anyLong(), any(), any());
	}

	@Test
	@DisplayName("非法 filetype 抛出 ResourceException")
	void getPicUploadToken_invalidFiletype_throwsResourceException() {
		when(membersRedis.opsForValue()).thenReturn(valueOps);
		when(valueOps.get(anyString())).thenReturn(null);
		FrontWxappEspierUploadImageService svc = new FrontWxappEspierUploadImageService(
				membersRedis, fileStorageService, 10);
		ResourceException ex1 = assertThrows(
				ResourceException.class,
				() -> svc.getPicUploadToken(1L, 2L, "a.png", "g", null));
		assertEquals("不支持的文件存储类型", ex1.getMessage());

		ResourceException ex2 = assertThrows(
				ResourceException.class,
				() -> svc.getPicUploadToken(1L, 2L, "a.png", "g", "audio"));
		assertEquals("不支持的文件存储类型audio", ex2.getMessage());
	}

	@Test
	@DisplayName("合法路径先校验 COS 文件名再 getUploadToken 并返回结果")
	void getPicUploadToken_success_invokesCosAssertThenGetUploadTokenInOrder() {
		when(membersRedis.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("uploadImageNum:5:7")).thenReturn("0");
		Map<String, Object> tokenMap = new LinkedHashMap<>();
		tokenMap.put("driver", "local");
		tokenMap.put("token", Map.of());
		when(fileStorageService.getUploadToken(eq("image"), eq(5L), eq("grp"), eq("f.png")))
				.thenReturn(tokenMap);

		FrontWxappEspierUploadImageService svc = new FrontWxappEspierUploadImageService(
				membersRedis, fileStorageService, 10);
		Map<String, Object> result = svc.getPicUploadToken(5L, 7L, "f.png", "grp", "image");

		assertSame(tokenMap, result);
		InOrder inOrder = inOrder(fileStorageService);
		inOrder.verify(fileStorageService).assertCosNonBlankFilenameHasExtensionIfNeeded("f.png");
		inOrder.verify(fileStorageService).getUploadToken("image", 5L, "grp", "f.png");
		verify(valueOps).increment("uploadImageNum:5:7");
		verify(membersRedis).expire(eq("uploadImageNum:5:7"), any(Duration.class));
	}
}
