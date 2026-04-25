package hse.java.practice.chatstomp.controller;

import hse.java.practice.chatstomp.model.HelloMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/v1")
public class HelloWorldController {

    @GetMapping("/hello")
    public HelloMessage getHelloWorld() {
        return new HelloMessage("Hello world");
    }

}
