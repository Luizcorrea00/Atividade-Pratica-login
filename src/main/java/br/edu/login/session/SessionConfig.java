package br.edu.login.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.*;

@Configuration
@EnableSpringHttpSession
public class SessionConfig {
  @Bean
  CookieSerializer cookieSerializer(@Value("${app.cookie-secure:false}") boolean secure) {
    DefaultCookieSerializer c = new DefaultCookieSerializer();
    c.setUseHttpOnlyCookie(true);
    c.setUseSecureCookie(secure);
    c.setSameSite("Lax");
    c.setCookiePath("/");
    return c;
  }
}
