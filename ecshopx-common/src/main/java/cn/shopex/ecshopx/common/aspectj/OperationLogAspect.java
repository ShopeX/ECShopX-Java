/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.common.aspectj;

import cn.shopex.ecshopx.common.annotation.OperationLog;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面。当前以 SLF4J 记录，后续可扩展为异步写库。
 */
@Aspect
@Component
public class OperationLogAspect {

	private static final Logger log = LoggerFactory.getLogger("operation-log");
	private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

	@Before("@annotation(operationLog)")
	public void doBefore(JoinPoint joinPoint, OperationLog operationLog) {
		START_TIME.set(System.currentTimeMillis());
	}

	@AfterReturning(pointcut = "@annotation(operationLog)", returning = "result")
	public void doAfterReturning(JoinPoint joinPoint, OperationLog operationLog, Object result) {
		try {
			long cost = System.currentTimeMillis() - START_TIME.get();
			String method = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
			HttpServletRequest request = getRequest();
			String uri = request != null ? request.getRequestURI() : "";
			String httpMethod = request != null ? request.getMethod() : "";

			log.info("[{}] {} {} | {} | {}ms | OK",
				operationLog.title(), httpMethod, uri, method, cost);
		} catch (Exception ignored) {
		} finally {
			START_TIME.remove();
		}
	}

	@AfterThrowing(pointcut = "@annotation(operationLog)", throwing = "ex")
	public void doAfterThrowing(JoinPoint joinPoint, OperationLog operationLog, Throwable ex) {
		try {
			long cost = System.currentTimeMillis() - START_TIME.get();
			String method = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
			HttpServletRequest request = getRequest();
			String uri = request != null ? request.getRequestURI() : "";
			String httpMethod = request != null ? request.getMethod() : "";

			log.error("[{}] {} {} | {} | {}ms | ERROR: {}",
				operationLog.title(), httpMethod, uri, method, cost, ex.getMessage());
		} catch (Exception ignored) {
		} finally {
			START_TIME.remove();
		}
	}

	private HttpServletRequest getRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		return attrs != null ? attrs.getRequest() : null;
	}
}
