package br.edu.login;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.edu.login.security.*;
import br.edu.login.user.*;
import br.edu.login.web.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebController.class)
@Import({SecurityConfig.class, AccountStatusFilter.class, ThemeAdvice.class})
class SecurityTest {
  @Autowired MockMvc mvc;
  @MockitoBean AccountService service;
  @MockitoBean AccountRepository repo;

  void account(Role role) {
    Account a = new Account();
    a.id = "1";
    a.name = "Teste";
    a.email = "person@example.com";
    a.role = role;
    when(repo.findByEmail(a.email)).thenReturn(Optional.of(a));
  }

  @Test
  void anonymousRedirected() throws Exception {
    mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
  }

  @Test
  void csrfRequired() throws Exception {
    mvc.perform(post("/register")).andExpect(status().isForbidden());
  }

  @Test
  void userCannotAccessAdminOrManager() throws Exception {
    account(Role.USER);
    mvc.perform(get("/admin/users").with(user("person@example.com").roles("USER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/manager").with(user("person@example.com").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void managerCanAccessManager() throws Exception {
    account(Role.MANAGER);
    mvc.perform(get("/manager").with(user("person@example.com").roles("MANAGER")))
        .andExpect(status().isOk());
  }

  @Test
  void adminCanListUsers() throws Exception {
    account(Role.ADMIN);
    var a = repo.findByEmail("person@example.com").get();
    when(repo.findAll()).thenReturn(java.util.List.of(a));
    mvc.perform(get("/admin/users").with(user("person@example.com").roles("ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void disabledAccountSessionRevoked() throws Exception {
    account(Role.USER);
    repo.findByEmail("person@example.com").get().enabled = false;
    mvc.perform(get("/dashboard").with(user("person@example.com").roles("USER")))
        .andExpect(redirectedUrl("/login?expired"));
  }

  @Test
  void roleChangeRevokesSession() throws Exception {
    account(Role.USER);
    mvc.perform(get("/admin/users").with(user("person@example.com").roles("ADMIN")))
        .andExpect(redirectedUrl("/login?expired"));
  }

  @Test
  void invalidRegistrationNeverReachesService() throws Exception {
    mvc.perform(
            post("/register")
                .with(csrf())
                .param("name", "x")
                .param("email", "bad")
                .param("password", "123"))
        .andExpect(status().isOk());
    verifyNoInteractions(service);
  }

  @Test
  void loginRendersCsrf() throws Exception {
    mvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
  }

  @Test
  void logoutRequiresPostAndCsrf() throws Exception {
    mvc.perform(post("/logout")).andExpect(status().isForbidden());
    mvc.perform(post("/logout").with(csrf())).andExpect(redirectedUrl("/login?logout"));
  }

  @Test
  void uncheckedStatusDisablesAccount() throws Exception {
    account(Role.ADMIN);
    Account target = new Account();
    target.id = "2";
    target.email = "other@example.com";
    when(repo.findById("2")).thenReturn(Optional.of(target));
    mvc.perform(
            post("/admin/users/2")
                .with(user("person@example.com").roles("ADMIN"))
                .with(csrf())
                .param("name", "Outra Pessoa")
                .param("role", "USER"))
        .andExpect(redirectedUrl("/admin/users"));
    verify(service).update("2", "Outra Pessoa", Role.USER, false);
  }
}
