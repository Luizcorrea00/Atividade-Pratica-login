package br.edu.login.security;

import br.edu.login.user.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  org.springframework.boot.web.servlet.FilterRegistrationBean<AccountStatusFilter>
      disableRegistration(AccountStatusFilter f) {
    var b = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f);
    b.setEnabled(false);
    return b;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService users(AccountRepository repo) {
    return email -> {
      Account a =
          repo.findByEmail(AccountService.normalize(email))
              .orElseThrow(() -> new UsernameNotFoundException("Credenciais invÃ¡lidas"));
      return User.withUsername(a.email)
          .password(a.passwordHash)
          .roles(a.role.name())
          .disabled(!a.enabled)
          .build();
    };
  }

  @Bean
  SecurityFilterChain filter(HttpSecurity http, AccountStatusFilter status) throws Exception {
    return http.addFilterAfter(
            status,
            org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/", "/login", "/register", "/css/**", "/error")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/manager/**")
                    .hasAnyRole("MANAGER", "ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/dashboard", true).permitAll())
        .logout(l -> l.logoutSuccessUrl("/login?logout").deleteCookies("SESSION"))
        .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; style-src 'self'; form-action 'self';"
                                + " frame-ancestors 'none'; base-uri 'self'")))
        .build();
  }
}
