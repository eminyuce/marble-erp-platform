package com.ozerler.marble.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("dev")
public class DevErrorProbeController {

    @GetMapping("/dev/raise-server-error")
    public String raiseServerError() {
        throw new IllegalStateException("Developer error-page probe");
    }
}
