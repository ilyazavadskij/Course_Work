package com.stc.mp.integration;

import com.stc.mp.integration.diarize.Diarizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Field;

@Slf4j
@SpringBootApplication
public class Application {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        String pathSeparator = System.getProperty("path.separator");
        System.setProperty("java.library.path", System.getProperty("java.library.path") + pathSeparator + "." + pathSeparator);
        final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
        sysPathsField.setAccessible(true);
        sysPathsField.set(null, null);
        log.info("java_library_path={}", System.getProperty("java.library.path"));

        String version = Application.class.getPackage().getImplementationVersion();
        if (version != null) log.info("Integration version:{}", version);

        ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);
        applicationContext.getBean(Diarizer.class).process();
    }
}
