package org.biins.cauchbase;

import java.lang.annotation.*;

/**
 * @author Martin Janys
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface View {

    String name();

    String map();

    String reduce() default "";

    String design() default "";

    String bucket() default "";

}
