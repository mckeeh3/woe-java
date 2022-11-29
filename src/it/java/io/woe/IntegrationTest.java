package io.woe;

import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import io.woe.Main;

/**
 * This is a skeleton for implmenting integration tests for a Kalix application
 * built with the Spring SDK.
 *
 * This test will initiate a Kalix Proxy using testcontainers and therefore it's
 * required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 *
 * Since this is an integration tests, it interacts with the application using a
 * WebClient
 * (already configured and provided automatically through injection).
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class IntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;

  @Test
  public void test() throws Exception {
    // implement your integration tests here by calling your
    // REST endpoints using the provided WebClient
  }
}