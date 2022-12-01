package io.woe;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import kalix.springsdk.testkit.EventSourcedTestKit;

import static io.woe.WorldMap.*;

public class GeneratorEntityTest {

  @Test
  public void createGeneratorTest() {
    var testKit = EventSourcedTestKit.of(GeneratorEntity::new);

    var generatorId = "generator-1";
    var position = latLng(1.0, 2.0);
    var radiusKm = 10.0;
    var deviceCountLimit = 1000;
    var ratePerSecond = 10;

    {
      var createCommand = new GeneratorEntity.CreateGeneratorCommand(generatorId, position, radiusKm, deviceCountLimit, ratePerSecond);
      var result = testKit.call(e -> e.create(createCommand));
      assertEquals("OK", result.getReply());

      var event = result.getNextEventOfType(GeneratorEntity.GeneratorCreatedEvent.class);
      assertEquals(generatorId, event.generatorId());

      var state = testKit.getState();
      assertEquals(generatorId, state.generatorId());
      assertEquals(position, state.position());
      assertEquals(radiusKm, state.radiusKm());
      assertEquals(deviceCountLimit, state.deviceCountLimit());
      assertEquals(ratePerSecond, state.ratePerSecond());
    }
  }

  @Test
  public void getGeneratorTest() {
    var testKit = EventSourcedTestKit.of(GeneratorEntity::new);

    var command1 = createGeneratorCommand("generator-1", latLng(1, 2), 10.0, 1000, 10);
    {
      var result = testKit.call(e -> e.create(command1));
      assertFalse(result.isError());
    }

    {
      var result = testKit.call(e -> e.get(command1.generatorId()));
      result.isError();
      assertFalse(result.isError());

      var response = result.getReply();
      assertEquals(command1.generatorId(), response.generatorId());
      assertEquals(command1.position(), response.position());
      assertEquals(command1.radiusKm(), response.radiusKm());
      assertEquals(command1.deviceCountLimit(), response.deviceCountLimit());
      assertEquals(command1.ratePerSecond(), response.ratePerSecond());

      var state = testKit.getState();
      assertEquals(command1.generatorId(), state.generatorId());
      assertEquals(command1.position(), state.position());
      assertEquals(command1.radiusKm(), state.radiusKm());
      assertEquals(command1.deviceCountLimit(), state.deviceCountLimit());
      assertEquals(command1.ratePerSecond(), state.ratePerSecond());
    }
  }

  @Test
  public void getGeneratorNonExistingTest() {
    var testKit = EventSourcedTestKit.of(GeneratorEntity::new);

    var result = testKit.call(e -> e.get("generator-999"));
    assertTrue(result.isError());
  }

  @Test
  public void generateTest() throws InterruptedException {
    var testKit = EventSourcedTestKit.of(GeneratorEntity::new);

    var command1 = createGeneratorCommand("generator-1", latLng(1, 2), 10.0, 1_000_000, 100_000);
    {
      var result = testKit.call(e -> e.create(command1));
      assertFalse(result.isError());
    }

    TimeUnit.MILLISECONDS.sleep(10);

    {
      var command = new GeneratorEntity.GenerateCommand(command1.generatorId());
      var result = testKit.call(e -> e.generate(command));
      assertFalse(result.isError());
      assertTrue(result.didEmitEvents());
      assertTrue(result.getAllEvents().size() > 1);
      assertTrue(result.getNextEventOfType(GeneratorEntity.GeneratedEvent.class).devicesGenerated() > 0);
    }
  }

  @Test
  public void generateTestToLimit() {
    var testKit = EventSourcedTestKit.of(GeneratorEntity::new);

    var devicesToGenerate = 123;
    var command1 = createGeneratorCommand("generator-1", latLng(1, 2), 10.0, devicesToGenerate, 1_000_000);
    {
      var result = testKit.call(e -> e.create(command1));
      assertFalse(result.isError());
    }

    { // this first test should generate 123 devices
      var command = new GeneratorEntity.GenerateCommand(command1.generatorId());
      var result = testKit.call(e -> e.generate(command));
      assertFalse(result.isError());
      assertTrue(result.didEmitEvents());
      assertTrue(result.getAllEvents().size() > 1);
      assertEquals(devicesToGenerate, result.getNextEventOfType(GeneratorEntity.GeneratedEvent.class).devicesGenerated());
    }

    { // this second test should generate 0 devices
      var command = new GeneratorEntity.GenerateCommand(command1.generatorId());
      var result = testKit.call(e -> e.generate(command));
      assertFalse(result.isError());
      assertTrue(result.didEmitEvents());
      assertEquals(1, result.getAllEvents().size());
      assertEquals(0, result.getNextEventOfType(GeneratorEntity.GeneratedEvent.class).devicesGenerated());
    }
  }

  private static GeneratorEntity.CreateGeneratorCommand createGeneratorCommand(String generatorId, LatLng position, double radiusKm, int deviceCountLimit, int ratePerSecond) {
    return new GeneratorEntity.CreateGeneratorCommand(generatorId, position, radiusKm, deviceCountLimit, ratePerSecond);
  }
}
