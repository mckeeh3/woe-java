package io.woe;

import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import io.woe.entity.Generator;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class GeneratorTest extends KalixIntegrationTestKitSupport {
  @Autowired
  private WebClient webClient;
  private Duration timeout = Duration.of(5 * 60, java.time.temporal.ChronoUnit.SECONDS);

  @Test
  public void create() {
    var generatorId = "generator-1";
    var position = new Generator.LatLng(1.0, 2.0);
    var radiusKm = 10.0;
    var deviceCountLimit = 1000;
    var ratePerSecond = 10;
    var createCommand = new Generator.CreateGeneratorCommand(generatorId, position, radiusKm, deviceCountLimit, ratePerSecond);

    var createResponse = webClient.post()
        .uri("/generator/{generatorId}/create", generatorId)
        .bodyValue(createCommand)
        .retrieve()
        .toEntity(String.class)
        .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, createResponse.getStatusCode());
    Assertions.assertEquals("\"OK\"", createResponse.getBody());

    var getResponse = webClient.get()
        .uri("/generator/{generatorId}", generatorId)
        .retrieve()
        .toEntity(Generator.State.class)
        .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, getResponse.getStatusCode());

    var state = getResponse.getBody();

    Assertions.assertEquals(generatorId, state.generatorId());
    Assertions.assertEquals(position, state.position());
    Assertions.assertEquals(radiusKm, state.radiusKm());
    Assertions.assertEquals(deviceCountLimit, state.deviceCountLimit());
    Assertions.assertEquals(ratePerSecond, state.ratePerSecond());
  }

  @Test
  public void generate() {
    var generatorId = "generator-1";
    var position = new Generator.LatLng(1.0, 2.0);
    var radiusKm = 10.0;
    var deviceCountLimit = 1000;
    var ratePerSecond = 10;
    var command = new Generator.CreateGeneratorCommand(generatorId, position, radiusKm, deviceCountLimit, ratePerSecond);

    var createResponse = webClient.post()
        .uri("/generator/{generatorId}/create", generatorId)
        .bodyValue(command)
        .retrieve()
        .toEntity(String.class)
        .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, createResponse.getStatusCode());
    Assertions.assertEquals("\"OK\"", createResponse.getBody());

    var generateCommand = new Generator.GenerateCommand(generatorId);

    var generateResponse = webClient.put()
        .uri("/generator/{generatorId}/generate", generatorId)
        .bodyValue(generateCommand)
        .retrieve()
        .toEntity(String.class)
        .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, generateResponse.getStatusCode());
    Assertions.assertEquals("\"OK\"", generateResponse.getBody());

    var getResponse = webClient.get()
        .uri("/generator/{generatorId}", generatorId)
        .retrieve()
        .toEntity(Generator.State.class)
        .block(timeout);

    Assertions.assertEquals(HttpStatus.OK, getResponse.getStatusCode());

    var state = getResponse.getBody();

    Assertions.assertEquals(generatorId, state.generatorId());
    Assertions.assertEquals(position, state.position());
    Assertions.assertEquals(radiusKm, state.radiusKm());
    Assertions.assertEquals(deviceCountLimit, state.deviceCountLimit());
    Assertions.assertEquals(ratePerSecond, state.ratePerSecond());
    Assertions.assertEquals(1, state.deviceCountLimit());
  }
}
