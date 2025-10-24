package com.example.foodmanager.controller;


import com.example.foodmanager.model.Food;
import com.example.foodmanager.repository.InMemoryFoodRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.UUID;


@Controller
@RequestMapping("/")
public class FoodController {


private final InMemoryFoodRepository repo = new InMemoryFoodRepository();


@GetMapping
public String index(Model model) {
model.addAttribute("foods", repo.findAll());
model.addAttribute("count", repo.count());
model.addAttribute("warning", repo.countWarning());
model.addAttribute("expired", repo.countExpired());
return "list";
}


@GetMapping("/add")
public String addForm(Model model) {
model.addAttribute("food", new Food("", "", LocalDate.now()));
return "form";
}


@PostMapping("/add")
public String addSubmit(@RequestParam String name, @RequestParam String expirationDate) {
LocalDate d = LocalDate.parse(expirationDate);
repo.save(new Food(UUID.randomUUID().toString(), name, d));
return "redirect:/";
}


@PostMapping("/delete")
public String delete(@RequestParam String id) {
repo.deleteById(id);
return "redirect:/";
}
}