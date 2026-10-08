package br.edu.login.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class ThemeAdvice {
  @Value("${app.theme:default}")
  String theme;

  @ModelAttribute("theme")
  String theme() {
    return java.util.Set.of("default", "forest").contains(theme) ? theme : "default";
  }
}
