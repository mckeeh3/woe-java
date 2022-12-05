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
  public void generateWithDelayToCauseGenerateAllDevicesTest() throws InterruptedException {
    var testKit = EventSourcedTestKit.of(GeneratorEntity::new);

    var generatorId = "generator-1";
    var deviceCountLimit = 10_000;
    var ratePerSecond = 100_000;
    var generateDelayMs = 100;
    var command1 = createGeneratorCommand(generatorId, latLng(1, 2), 10.0, deviceCountLimit, ratePerSecond);
    {
      var result = testKit.call(e -> e.create(command1));
      assertFalse(result.isError());
    }

    TimeUnit.MILLISECONDS.sleep(generateDelayMs);

    {
      var command = new GeneratorEntity.GenerateCommand(command1.generatorId());
      var result = testKit.call(e -> e.generate(command));
      assertFalse(result.isError());
      assertTrue(result.didEmitEvents());
      assertTrue(result.getAllEvents().size() > 1);

      var generatedEvent = result.getNextEventOfType(GeneratorEntity.GeneratedEvent.class);
      var deviceCountCurrent = generatedEvent.deviceCountCurrent();
      assertEquals(generatorId, generatedEvent.generatorId());
      assertTrue(deviceCountCurrent > 0);

      var expectedEventCount = 1 + deviceCountLimit / GeneratorEntity.devicesPerGeneratorBatch + (deviceCountLimit % GeneratorEntity.devicesPerGeneratorBatch > 0 ? 1 : 0);
      var allEvents = result.getAllEvents();
      assertEquals(expectedEventCount, allEvents.size());

      var devicesToGenerateEvent = allEvents.get(expectedEventCount - 1);
      assertEquals(GeneratorEntity.DevicesToGenerateEvent.class, devicesToGenerateEvent.getClass());
    }
  }

  @Test
  public void generateWithNoDevicesGeneratedTest() {
    var testKit = EventSourcedTestKit.of(GeneratorEntity::new);

    var generatorId = "generator-1";
    var deviceCountLimit = 10;
    var ratePerSecond = 10;
    var command1 = createGeneratorCommand(generatorId, latLng(1, 2), 10.0, deviceCountLimit, ratePerSecond);
    {
      var result = testKit.call(e -> e.create(command1));
      assertFalse(result.isError());
    }

    {
      var command = new GeneratorEntity.GenerateCommand(command1.generatorId());
      var result = testKit.call(e -> e.generate(command));
      var allEvents = result.getAllEvents();
      var allEventsSize = allEvents.size();

      assertFalse(result.isError());
      assertTrue(result.didEmitEvents());
      assertEquals(1, allEventsSize);

      var generatedEvent = result.getNextEventOfType(GeneratorEntity.GeneratedEvent.class);
      var deviceCountCurrent = generatedEvent.deviceCountCurrent();
      assertEquals(generatorId, generatedEvent.generatorId());
      assertEquals(0, deviceCountCurrent);
    }
  }

  @Test
  public void generateBeyondLimitTest() throws InterruptedException {
    var testKit = EventSourcedTestKit.of(GeneratorEntity::new);

    var generatorId = "generator-1";
    var deviceCountLimit = 10;
    var ratePerSecond = 1_000_000;
    var generateDelayMs = 100;
    var command1 = createGeneratorCommand(generatorId, latLng(1, 2), 10.0, deviceCountLimit, ratePerSecond);
    {
      var result = testKit.call(e -> e.create(command1));
      assertFalse(result.isError());
    }

    TimeUnit.MILLISECONDS.sleep(generateDelayMs);

    {
      var command = new GeneratorEntity.GenerateCommand(command1.generatorId());
      var result = testKit.call(e -> e.generate(command));
      var allEvents = result.getAllEvents();
      var allEventsSize = allEvents.size();

      assertFalse(result.isError());
      assertTrue(result.didEmitEvents());
      assertEquals(2, allEventsSize);
    }

    {
      var command = new GeneratorEntity.GenerateCommand(command1.generatorId());
      var result = testKit.call(e -> e.generate(command));

      assertFalse(result.isError());
      assertFalse(result.didEmitEvents());
    }
  }

  @Test
  public void generateTestToLimit() {
    var testKit = EventSourcedTestKit.of(GeneratorEntity::new);

    var devicesToGenerate = 123;
    var ratePerSecond = 1_000_000;
    var command1 = createGeneratorCommand("generator-1", latLng(1, 2), 10.0, devicesToGenerate, ratePerSecond);
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
      assertEquals(devicesToGenerate, result.getNextEventOfType(GeneratorEntity.GeneratedEvent.class).deviceCountCurrent());

      var state = testKit.getState();
      assertEquals(devicesToGenerate, state.deviceCountCurrent());
      assertEquals(state.deviceCountCurrent(), state.deviceCountLimit());
    }

    { // this second test should generate 0 devices
      var command = new GeneratorEntity.GenerateCommand(command1.generatorId());
      var result = testKit.call(e -> e.generate(command));
      assertFalse(result.isError());
      assertFalse(result.didEmitEvents());
    }
  }

  private static GeneratorEntity.CreateGeneratorCommand createGeneratorCommand(String generatorId, LatLng position, double radiusKm, int deviceCountLimit, int ratePerSecond) {
    return new GeneratorEntity.CreateGeneratorCommand(generatorId, position, radiusKm, deviceCountLimit, ratePerSecond);
  }
}
