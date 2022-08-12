package org.example;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Slf4j
@SuppressWarnings({"squid:S2095", "squid:S1118"})
@AutoConfigureMockMvc
@SpringBootTest(classes = {App.class})
@ActiveProfiles("test")
public class BaseIntegrationTest {

    private static final String REPOSITORY_HUB = System.getProperty("dockerfile.repository.hub");

    static {
        startAndInitializePulsarTestContainer();
    }

    @SneakyThrows
    private static void startAndInitializePulsarTestContainer() {
        PulsarContainer pulsar = new PulsarContainer(
                DockerImageName.parse((null == REPOSITORY_HUB ? "apachepulsar/" : (REPOSITORY_HUB + "/external/")) + "pulsar:2.10.0")
                        .asCompatibleSubstituteFor("apachepulsar/pulsar")
        )
                .withCopyFileToContainer(MountableFile.forClasspathResource("standalone.conf", 0777), "/pulsar/conf/standalone.conf")
                .withCommand("bin/pulsar standalone")
                .waitingFor(Wait.forListeningPort());
        pulsar.start();
        pulsar.execInContainer("bin/pulsar-admin", "namespaces", "set-retention", "public/default", "--size", "-1", "--time", "-1");
        pulsar.execInContainer("bin/pulsar-admin", "namespaces", "set-schema-compatibility-strategy", "--compatibility", "ALWAYS_COMPATIBLE", "public/default");
        pulsar.execInContainer("bin/pulsar", "initialize-transaction-coordinator-metadata", "-cs", "127.0.0.1:2181", "-c", "standalone");

        System.setProperty("pulsar.service.url", pulsar.getPulsarBrokerUrl());
    }
}
