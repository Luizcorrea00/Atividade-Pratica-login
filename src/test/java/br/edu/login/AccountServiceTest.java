package br.edu.login;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.edu.login.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccountServiceTest {
  @Test
  void normalizesHashesAndUsesLeastPrivilege() {
    var repo = mock(AccountRepository.class);
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    var encoder = new BCryptPasswordEncoder(4);
    var service = new AccountService(repo, encoder);
    var f = new Registration();
    f.name = " Pessoa ";
    f.email = " USER@EXAMPLE.COM ";
    f.password = "UmaSenhaForte123";
    var a = service.register(f);
    assertEquals("user@example.com", a.email);
    assertEquals(Role.USER, a.role);
    assertNotEquals(f.password, a.passwordHash);
    assertTrue(encoder.matches(f.password, a.passwordHash));
  }

  @Test
  void rejectsBcryptOverflow() {
    var service = new AccountService(mock(AccountRepository.class), new BCryptPasswordEncoder());
    var f = new Registration();
    f.password = "Á".repeat(40);
    assertThrows(IllegalArgumentException.class, () -> service.register(f));
  }
}
