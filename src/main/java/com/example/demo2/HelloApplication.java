package com.example.demo2;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import rest.*;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class HelloApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Register all REST resources
        classes.add(PostResource.class);
        classes.add(UserResource.class);
        classes.add(GroupResource.class);
        classes.add(NotificationResource.class);
        classes.add(ConnectionResource.class);
        
        // Register filters and providers
        classes.add(RequestBodyReader.class);
        
        return classes;
    }
}