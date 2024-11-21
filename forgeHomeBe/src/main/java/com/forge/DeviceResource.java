package com.forge;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/device")
public class DeviceResource {

    @GetMapping
    public String receiveMessage() {
        return "hello device";
    }
}
