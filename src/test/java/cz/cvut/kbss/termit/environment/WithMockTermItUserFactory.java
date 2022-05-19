package cz.cvut.kbss.termit.environment;

import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.TermItUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.net.URI;
import java.util.Collections;

public class WithMockTermItUserFactory implements WithSecurityContextFactory<WithMockTermItUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockTermItUser annotation) {
        UserAccount userAccount = new UserAccount();
        userAccount.setUri(URI.create(annotation.uri()));
        TermItUserDetails userDetails = new TermItUserDetails(userAccount);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new AuthenticationToken(Collections.emptyList(), userDetails);
        context.setAuthentication(authentication);
        return context;
    }
}
