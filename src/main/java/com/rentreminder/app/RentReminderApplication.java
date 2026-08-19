package com.rentreminder.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Entry point for the Rent Payment Reminder Portal.
 * Packaged as a WAR so it can be deployed to an external Tomcat
 * server as part of the Jenkins CI/CD pipeline, while still being
 * runnable standalone during local development (mvn spring-boot:run).
 */
@SpringBootApplication
public class RentReminderApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(RentReminderApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(RentReminderApplication.class);
    }
}
