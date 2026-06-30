package com.example.foodmanager.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private final PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Resource path = new FileSystemResource(".env");
        if (!path.exists()) {
            return;
        }

        try {
            Properties props = new Properties();
            Path envPath = Paths.get(".env");
            if (Files.exists(envPath)) {
                Files.lines(envPath)
                    .filter(line -> !line.startsWith("#") && line.contains("="))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            props.setProperty(parts[0].trim(), parts[1].trim());
                        }
                    });

                PropertySource<?> propertySource = new org.springframework.core.env.PropertiesPropertySource("dotenv", props);
                environment.getPropertySources().addLast(propertySource);

                System.out.println("=== .env ファイル読み込み ===");
                props.forEach((key, value) -> {
                    if (key.toString().contains("API_KEY")) {
                        System.out.println(key + "=" + value.toString().substring(0, Math.min(10, value.toString().length())) + "...");
                    } else {
                        System.out.println(key + "=" + value);
                    }
                });
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load .env file", ex);
        }
    }
}
