package br.edu.login.user;

import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
  private final AccountRepository repo;
  private final PasswordEncoder encoder;

  public AccountService(AccountRepository repo, PasswordEncoder encoder) {
    this.repo = repo;
    this.encoder = encoder;
  }

  public static String normalize(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  public Account register(Registration form) {
    if (form.password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException("Senha excede 72 bytes UTF-8");
    Account a = new Account();
    a.name = form.name.strip();
    a.email = normalize(form.email);
    a.passwordHash = encoder.encode(form.password);
    return repo.save(a);
  }

  public void update(String id, String name, Role role, boolean enabled) {
    Account a = repo.findById(id).orElseThrow();
    a.name = name.strip();
    a.role = role;
    a.enabled = enabled;
    repo.save(a);
  }
}
