package com.cmc.eventmanagement.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/** Ghi tên method + thời gian chạy. Không ghi tham số vì có thể chứa qrToken/mật khẩu. */
@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Pointcut("execution(public * com.cmc.eventmanagement.service..*Service.*(..))")
    void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        String name = pjp.getSignature().getDeclaringType().getSimpleName() + "." + pjp.getSignature().getName();
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("{} -> OK ({} ms)", name, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("{} -> {} ({} ms)", name, ex.getClass().getSimpleName(), System.currentTimeMillis() - start);
            throw ex;
        }
    }
}
