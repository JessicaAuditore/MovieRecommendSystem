package com.limitofsoul.business.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
@Controller
public class CommonRestApi {

    @RequestMapping("/")
    public String begin(){
        return "/index.html";
    }
}
