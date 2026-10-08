package br.edu.login.web;

import br.edu.login.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class WebController {
  private final AccountService service;
  private final AccountRepository repo;

  public WebController(AccountService service, AccountRepository repo) {
    this.service = service;
    this.repo = repo;
  }

  @GetMapping("/")
  String home() {
    return "redirect:/dashboard";
  }

  @GetMapping("/login")
  String login() {
    return "login";
  }

  @GetMapping("/register")
  String register(Model m) {
    m.addAttribute("registration", new Registration());
    return "register";
  }

  @PostMapping("/register")
  String register(@Valid @ModelAttribute("registration") Registration form, BindingResult errors) {
    if (errors.hasErrors()) return "register";
    try {
      service.register(form);
    } catch (DuplicateKeyException | IllegalArgumentException e) {
      errors.reject(
          "registration", "Não foi possível cadastrar. Confira os dados ou use outro e-mail.");
      return "register";
    }
    return "redirect:/login?registered";
  }

  @GetMapping("/dashboard")
  String dashboard() {
    return "dashboard";
  }

  @GetMapping("/manager")
  String manager() {
    return "manager";
  }

  @GetMapping("/admin/users")
  String users(Model m) {
    m.addAttribute("users", repo.findAll());
    m.addAttribute("roles", Role.values());
    return "users";
  }

  public record Edit(@NotBlank @Size(min = 2, max = 80) String name, @NotNull Role role) {}

  @PostMapping("/admin/users/{id}")
  String update(
      @PathVariable String id,
      @Valid @ModelAttribute Edit form,
      BindingResult errors,
      @RequestParam(defaultValue = "false") boolean enabled,
      Principal principal) {
    if (errors.hasErrors()) return "redirect:/admin/users?invalid";
    Account target = repo.findById(id).orElseThrow();
    if (target.email.equals(principal.getName())) return "redirect:/admin/users?self";
    service.update(id, form.name(), form.role(), enabled);
    return "redirect:/admin/users";
  }

  @PostMapping("/admin/users/{id}/delete")
  String delete(@PathVariable String id, Principal p) {
    Account a = repo.findById(id).orElseThrow();
    if (!a.email.equals(p.getName())) repo.delete(a);
    return "redirect:/admin/users";
  }
}
