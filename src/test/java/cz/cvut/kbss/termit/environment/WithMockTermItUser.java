package cz.cvut.kbss.termit.environment;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockTermItUserFactory.class)
public @interface WithMockTermItUser {
    String uri() default "mock_user_uri";
}
