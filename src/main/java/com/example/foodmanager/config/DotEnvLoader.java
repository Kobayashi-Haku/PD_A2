package com.example.foodmanager.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Component
public class DotEnvLoader implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        loadDotEnv(environment);
    }

    private void loadDotEnv(ConfigurableEnvironment environment) {
        try {
            Path envPath = Paths.get(".env");
            if (!Files.exists(envPath)) {
                System.out.println(".envファイルが見つかりません");
                return;
            }

            Properties props = new Properties();
            Files.lines(envPath)
                .filter(line -> !line.trim().isEmpty() && !line.startsWith("#") && line.contains("="))
                .forEach(line -> {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        props.setProperty(key, value);
                        System.setProperty(key, value); // システムプロパティとしても設定
                    }
                });

            if (!props.isEmpty()) {
                PropertiesPropertySource propertySource = new PropertiesPropertySource("dotenv", props);
                environment.getPropertySources().addFirst(propertySource);

                System.out.println("=== .env ファイル読み込み完了 ===");
                props.forEach((key, value) -> {
                    if (key.toString().contains("API_KEY")) {
                        System.out.println(key + " = " + value.toString().substring(0, Math.min(10, value.toString().length())) + "...");
                    } else {
                        System.out.println(key + " = " + value);
                    }
                });
            }
        } catch (IOException e) {
            System.err.println(".envファイルの読み込みエラー: " + e.getMessage());
        }
    }
}
