package com.zidio.keystone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling // powers the SLA breach checker
public class KeystoneApplication {
    public static void main(String[] args) {
        // Some JVM/OS combinations report the deprecated zone alias "Asia/Calcutta" instead of
        // "Asia/Kolkata", which newer PostgreSQL builds reject outright. Running the whole app on
        // UTC sidesteps that entirely and is the right call for a deployed service anyway.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(KeystoneApplication.class, args);
    }
}
