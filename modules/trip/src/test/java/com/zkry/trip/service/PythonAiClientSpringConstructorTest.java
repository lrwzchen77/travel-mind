package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

class PythonAiClientSpringConstructorTest {

    @Test
    void springCreatesClientFromValueConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                context,
                "travelmind.python-ai.base-url=http://127.0.0.1:19080",
                "travelmind.python-ai.timeout-ms=200",
                "travelmind.python-ai.memory-timeout-ms=400",
                "travelmind.python-ai.memory-service-token=test-token"
            );
            context.register(PythonAiClient.class);

            context.refresh();

            assertThat(context.getBean(PythonAiClient.class)).isNotNull();
        }
    }
}
