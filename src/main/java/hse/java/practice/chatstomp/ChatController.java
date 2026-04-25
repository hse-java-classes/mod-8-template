package hse.java.practice.chatstomp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    @GetMapping("/")
    public String index() {
        return "chat";
    }


//    @MessageMapping("/send")
//    @SendTo("/topic/messages")
//    your function

}
