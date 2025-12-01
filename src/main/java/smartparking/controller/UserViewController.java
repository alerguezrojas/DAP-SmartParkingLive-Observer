package smartparking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class UserViewController {

    @GetMapping("/usuario/{name}")
    public String userView(@PathVariable String name) {
        // Forward to the static HTML file. 
        // The client-side JS in user_view.html will parse the URL to get the name.
        return "forward:/user_view.html";
    }
}
