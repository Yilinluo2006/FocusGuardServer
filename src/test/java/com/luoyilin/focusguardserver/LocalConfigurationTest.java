package com.luoyilin.focusguardserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalConfigurationTest {
    @TempDir Path root;
    private GenericApplicationContext load(Map<String,Object> override) throws Exception {
        Properties packaged=new Properties();
        try(var input=getClass().getResourceAsStream("/application.properties")){packaged.load(input);}
        String imports=packaged.getProperty("spring.config.import").replace("file:./","file:"+root.toAbsolutePath().toString().replace('\\','/')+"/");
        Path config=root.resolve("probe.properties");Properties fixture=new Properties();fixture.setProperty("spring.config.import",imports);
        try(var output=Files.newOutputStream(config)){fixture.store(output,"fixture");}
        GenericApplicationContext context=new GenericApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("probe-location",Map.of("spring.config.location","file:"+config.toAbsolutePath().toString().replace('\\','/'))));
        if(!override.isEmpty())context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("environment",override));
        new ConfigDataApplicationContextInitializer().initialize(context);return context;
    }
    private void env(Path directory) throws Exception {Files.createDirectories(directory);Files.writeString(directory.resolve(".env"),"ALIBABA_CLOUD_ACCESS_KEY_ID=fixture-id\nALIBABA_CLOUD_ACCESS_KEY_SECRET=fixture-secret\n");}
    @Test void moduleDirectoryLoadsCredentials() throws Exception {env(root);try(var c=load(Map.of())){assertEquals("fixture-id",c.getEnvironment().getProperty("ALIBABA_CLOUD_ACCESS_KEY_ID"));assertEquals("fixture-secret",c.getEnvironment().getProperty("ALIBABA_CLOUD_ACCESS_KEY_SECRET"));}}
    @Test void ideaOuterDirectoryLoadsModuleCredentials() throws Exception {env(root.resolve("FocusGuardServer"));try(var c=load(Map.of())){assertEquals("fixture-secret",c.getEnvironment().getProperty("ALIBABA_CLOUD_ACCESS_KEY_SECRET"));}}
    @Test void environmentOverridesLocalFile() throws Exception {env(root);try(var c=load(Map.of("ALIBABA_CLOUD_ACCESS_KEY_ID","deployment-id"))){assertEquals("deployment-id",c.getEnvironment().getProperty("ALIBABA_CLOUD_ACCESS_KEY_ID"));}}
    @Test void missingLocalFileIsOptional() throws Exception {try(var c=load(Map.of())){assertNull(c.getEnvironment().getProperty("ALIBABA_CLOUD_ACCESS_KEY_ID"));}}
}
