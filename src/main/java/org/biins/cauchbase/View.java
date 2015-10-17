package org.biins.cauchbase;

/**
 * @author Martin Janys
 */
public @interface View {

    String name();

    String map();

    String reduce() default "";

    String design() default "";

    String bucket() default "";

    String bucketPassword() default "";
}
