package br.edu.login.security;

import br.edu.login.user.*;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
public class AdminBootstrap {
  @Bean
  CommandLineRunner admin(
      AccountRepository repo,
      AccountService service,
      Validator validator,
      @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String email,
      @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
    return args -> {
      if (email.isBlank()
          || password.isBlank()
          || repo.findByEmail(AccountService.normalize(email)).isPresent()) return;
      Registration f = new Registration();
      f.name = "Administrador";
      f.email = email;
      f.password = password;
      if (!validator.validate(f).isEmpty())
        throw new IllegalArgumentException("Credenciais iniciais inválidas");
      Account a = service.register(f);
      a.role = br.edu.login.user.Role.ADMIN;
      repo.save(a);
    };
  }
}
