package cn.sux1ng.client.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 标记这个注解只能用在方法上
@Target(ElementType.METHOD)
// 标记这个注解在运行时依然存在(反射需要)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventTarget {
}