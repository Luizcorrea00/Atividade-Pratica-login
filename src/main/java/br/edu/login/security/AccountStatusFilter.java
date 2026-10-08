package br.edu.login.security;

import br.edu.login.user.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccountStatusFilter extends OncePerRequestFilter {
  private final AccountRepository repo;

  public AccountStatusFilter(AccountRepository repo) {
    this.repo = repo;
  }

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
      var a = repo.findByEmail(auth.getName());
      if (a.isEmpty()
          || !a.get().enabled
          || auth.getAuthorities().stream()
              .noneMatch(x -> x.getAuthority().equals("ROLE_" + a.get().role.name()))) {
        var session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        res.sendRedirect("/login?expired");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
