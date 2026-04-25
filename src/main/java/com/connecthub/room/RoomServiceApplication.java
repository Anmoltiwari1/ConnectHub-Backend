package com.connecthub.room;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RoomServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoomServiceApplication.class, args);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner seeder(com.connecthub.room.service.RoomService roomService) {
        return args -> {
            try {
                roomService.createRoom("General Chat", "Global discussion for everyone.", 
                    com.connecthub.room.entity.Room.RoomType.GROUP, "system", "System", false);
            } catch (Exception e) {
                // Already exists or other error
            }
        };
    }
}
