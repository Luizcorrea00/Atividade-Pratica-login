package br.edu.login.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class Account {
  @Id public String id;
  public String name;

  @Indexed(unique = true)
  public String email;

  public String passwordHash;
  public Role role = Role.USER;
  public boolean enabled = true;
}
