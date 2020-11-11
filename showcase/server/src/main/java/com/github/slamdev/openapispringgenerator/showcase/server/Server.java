package com.github.slamdev.openapispringgenerator.showcase.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Server {

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

//    @Configuration
//    public static class SecurityConfig extends WebSecurityConfigurerAdapter {
//        @Override
//        protected void configure(HttpSecurity http) throws Exception {
//            http.
////                    anonymous();
////                    and().
//                    authorizeRequests().anyRequest().permitAll();
//        }
//    }
}
