package br.edu.login.user;

import jakarta.validation.constraints.*;

public class Registration {
  @NotBlank
  @Size(min = 2, max = 80)
  public String name;

  @NotBlank
  @Email
  @Size(max = 254)
  public String email;

  @NotBlank
  @Size(min = 12, max = 64)
  @Pattern(
      regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).*$",
      message = "Use maiúscula, minúscula e número")
  public String password;

  public String getName() {
    return name;
  }

  public void setName(String v) {
    name = v;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String v) {
    email = v;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String v) {
    password = v;
  }
}
