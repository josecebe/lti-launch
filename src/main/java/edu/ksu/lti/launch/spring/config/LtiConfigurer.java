package edu.ksu.lti.launch.spring.config;

import edu.ksu.lti.launch.oauth.LtiConsumerDetailsService;
import edu.ksu.lti.launch.oauth.LtiOAuthAuthenticationHandler;
import edu.ksu.lti.launch.service.LtiLoginService;
import edu.ksu.lti.launch.service.ToolConsumerService;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.oauth.provider.nonce.InMemoryNonceServices;
import org.springframework.security.oauth.provider.token.InMemoryProviderTokenServices;
import org.springframework.security.oauth.provider.token.OAuthProviderTokenServices;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * A configurer for enabling LTI logins for a Spring.
 * Loads the {@link LtiLoginService} from the shared object space.
 *
 * @param <B>
 */
public class LtiConfigurer<B extends HttpSecurityBuilder<B>> extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, B> {

    private final String path;

    private LtiConsumerDetailsService oauthConsumerDetailsService;
    private LtiOAuthAuthenticationHandler oauthAuthenticationHandler;
    private OAuthProviderTokenServices oauthProviderTokenServices = new InMemoryProviderTokenServices();

    public LtiConfigurer(ToolConsumerService toolConsumerService, String path, boolean checkInstance) {
        this.oauthAuthenticationHandler = new LtiOAuthAuthenticationHandler(toolConsumerService);
        this.oauthAuthenticationHandler.setCheckInstance(checkInstance);
        this.oauthConsumerDetailsService = new LtiConsumerDetailsService(toolConsumerService);
        this.path = path;

    }

    @Override
    public void configure(B http) {
        LtiLoginService loginService = http.getSharedObject(LtiLoginService.class);
        http
            .addFilterBefore(configureProcessingFilter(), FilterSecurityInterceptor.class)
            .addFilterAfter(configureLoginFilter(loginService), SwitchUserFilter.class);
    }

    private LtiLoginFilter configureLoginFilter(LtiLoginService loginService) {
        LtiLoginFilter filter = new LtiLoginFilter(new AntPathRequestMatcher(path));
        if (loginService != null) {
            filter.setLtiLoginService(loginService);
        }
        return filter;
    }

    private LtiAuthenticationFilter configureProcessingFilter() {
        //Set up nonce service to prevent replay attacks.
        InMemoryNonceServices nonceService = new InMemoryNonceServices();
        nonceService.setValidityWindowSeconds(600);

        LtiAuthenticationFilter ltiAuthenticationFilter = new LtiAuthenticationFilter(new AntPathRequestMatcher(path));
        ltiAuthenticationFilter.setAuthHandler(oauthAuthenticationHandler);
        ltiAuthenticationFilter.setConsumerDetailsService(oauthConsumerDetailsService);
        ltiAuthenticationFilter.setNonceServices(nonceService);
        ltiAuthenticationFilter.setTokenServices(oauthProviderTokenServices);
        ltiAuthenticationFilter.setIgnoreMissingCredentials(false);
        return ltiAuthenticationFilter;
    }
}