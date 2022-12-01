package io.woe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class GeneratorIntegrationTest extends KalixIntegrationTestKitSupport {
  @Autowired
  private WebClient webClient;
  private Duration timeout = Duration.of(5 * 60, java.time.temporal.ChronoUnit.SECONDS);

  @Test
  public void create() {
    var generatorId = "generator-1";
    var position = new GeneratorEntity.LatLng(1.0, 2.0);
    var radiusKm = 10.0;
    var deviceCountLimit = 1000;
    var ratePerSecond = 10;
    var createCommand = new GeneratorEntity.CreateGeneratorCommand(generatorId, position, radiusKm, deviceCountLimit, ratePerSecond);

    var createResponse = webClient.post()
        .uri("/generator/{generatorId}/create", generatorId)
        .bodyValue(createCommand)
        .retrieve()
        .toEntity(String.class)
        .block(timeout);

    assertEquals(HttpStatus.OK, createResponse.getStatusCode());
    assertEquals("\"OK\"", createResponse.getBody());

    var getResponse = webClient.get()
        .uri("/generator/{generatorId}", generatorId)
        .retrieve()
        .toEntity(GeneratorEntity.State.class)
        .block(timeout);

    assertEquals(HttpStatus.OK, getResponse.getStatusCode());

    var state = getResponse.getBody();

    assertEquals(generatorId, state.generatorId());
    assertEquals(position, state.position());
    assertEquals(radiusKm, state.radiusKm());
    assertEquals(deviceCountLimit, state.deviceCountLimit());
    assertEquals(ratePerSecond, state.ratePerSecond());
  }

  @Test
  public void generate() {
    var generatorId = "generator-1";
    var position = new GeneratorEntity.LatLng(1.0, 2.0);
    var radiusKm = 10.0;
    var deviceCountLimit = 1000;
    var ratePerSecond = 10;
    var command = new GeneratorEntity.CreateGeneratorCommand(generatorId, position, radiusKm, deviceCountLimit, ratePerSecond);

    var createResponse = webClient.post()
        .uri("/generator/{generatorId}/create", generatorId)
        .bodyValue(command)
        .retrieve()
        .toEntity(String.class)
        .block(timeout);

    assertEquals(HttpStatus.OK, createResponse.getStatusCode());
    assertEquals("\"OK\"", createResponse.getBody());

    var generateCommand = new GeneratorEntity.GenerateCommand(generatorId);

    var generateResponse = webClient.put()
        .uri("/generator/{generatorId}/generate", generatorId)
        .bodyValue(generateCommand)
        .retrieve()
        .toEntity(String.class)
        .block(timeout);

    assertEquals(HttpStatus.OK, generateResponse.getStatusCode());
    assertEquals("\"OK\"", generateResponse.getBody());

    var getResponse = webClient.get()
        .uri("/generator/{generatorId}", generatorId)
        .retrieve()
        .toEntity(GeneratorEntity.State.class)
        .block(timeout);

    assertEquals(HttpStatus.OK, getResponse.getStatusCode());

    var state = getResponse.getBody();

    assertEquals(generatorId, state.generatorId());
    assertEquals(position, state.position());
    assertEquals(radiusKm, state.radiusKm());
    assertEquals(deviceCountLimit, state.deviceCountLimit());
    assertEquals(ratePerSecond, state.ratePerSecond());
    assertEquals(1000, state.deviceCountLimit());
  }
}
