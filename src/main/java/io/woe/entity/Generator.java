package io.woe.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;

@EntityKey("generatorId")
@EntityType("generator")
@RequestMapping("/generator")
public class Generator extends EventSourcedEntity<Generator.State> {
  private static final Logger log = LoggerFactory.getLogger(Generator.class);

  @Override
  public State emptyState() {
    return State.empty();
  }

  @PostMapping("/{generatorId}/create")
  public Effect<String> create(@RequestBody CreateGeneratorCommand command) {
    log.info("State: {}\nCommand: {}", currentState(), command);
    return effects()
        .emitEvents(currentState().eventsFor(command))
        .thenReply(__ -> "OK");
  }

  @PutMapping("/{generatorId}/generate")
  public Effect<String> generate(@RequestBody GenerateCommand command) {
    log.info("State: {}\nCommand: {}", currentState(), command);
    return effects()
        .emitEvents(currentState().eventsFor(command))
        .thenReply(__ -> "OK");
  }

  @GetMapping("/{generatorId}")
  public Effect<Generator.State> get(@PathVariable String generatorId) {
    log.info("GeneratorId: {}", generatorId);
    log.info("State: {}", currentState());
    if (currentState().isEmpty()) {
      return effects().error("Generator not created");
    }
    return effects().reply(currentState());
  }

  @EventHandler
  public State on(GeneratorCreatedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(GeneratedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(DevicesToGenerateEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  public record LatLng(
      double lat,
      double lng) {
    static LatLng fromRadians(double lat, double lng) {
      return new LatLng(Math.toDegrees(lat), Math.toDegrees(lng));
    }
  }

  public record State(
      String generatorId,
      LatLng position,
      double radiusKm,
      int ratePerSecond,
      long startTimeMs,
      int deviceCountLimit,
      int deviceCountCurrent) {

    public static State empty() {
      return new State(null, null, 0, 0, epochMsNow(), 0, 0);
    }

    public boolean isEmpty() {
      return generatorId == null;
    }

    List<?> eventsFor(CreateGeneratorCommand command) {
      if (generatorId != null) {
        return List.of(new GeneratorCreatedEvent(
            generatorId,
            position,
            radiusKm,
            deviceCountLimit,
            ratePerSecond));
      }
      var generatorCreatedEvent = new GeneratorCreatedEvent(
          command.generatorId,
          command.position,
          command.radiusKm,
          command.deviceCountLimit,
          command.ratePerSecond);
      var events = new ArrayList<Object>();
      events.add(generatorCreatedEvent);
      events.addAll(createDevicesToGenerateEvents(command.generatorId()));
      return events;
    }

    List<?> eventsFor(GenerateCommand command) {
      var deviceBatches = createDevicesToGenerateEvents(command.generatorId());
      var devicesToBeCreated = deviceBatches.stream()
          .map(e -> e.devices().size())
          .reduce(0, (a, n) -> a + n);
      var events = new ArrayList<Object>();
      events.add(new GeneratedEvent(generatorId(), devicesToBeCreated));
      events.addAll(deviceBatches);
      return events;
    }

    private List<DevicesToGenerateEvent> createDevicesToGenerateEvents(String generatorId) {
      var elapsedMs = epochMsNow() - startTimeMs;
      var devicesPerBatch = 32;
      var devicesToBeCreated = (int) Math.min(deviceCountLimit, (elapsedMs * ratePerSecond / 1000) - deviceCountCurrent);
      var deviceBatches = devicesToBeCreated / devicesPerBatch + (devicesToBeCreated % devicesPerBatch > 0 ? 1 : 0);
      if (deviceBatches == 0) {
        return List.of();
      }
      return IntStream.range(0, deviceBatches)
          .mapToObj(i -> (i + 1) * devicesPerBatch > devicesToBeCreated ? devicesToBeCreated % devicesPerBatch : devicesPerBatch)
          .map(i -> DevicesToGenerateEvent.with(generatorId, position, radiusKm, i))
          .toList();
    }

    State on(GeneratorCreatedEvent event) {
      if (generatorId != null) {
        return this;
      }
      return new State(
          event.generatorId(),
          event.position(),
          event.radiusKm(),
          event.ratePerSecond(),
          epochMsNow(),
          event.deviceCountLimit(),
          0);
    }

    State on(GeneratedEvent event) {
      return new State(
          event.generatorId(),
          position,
          radiusKm,
          ratePerSecond,
          startTimeMs,
          deviceCountLimit,
          deviceCountCurrent + event.devicesGenerated());
    }

    public State on(DevicesToGenerateEvent event) {
      return this;
    }

    private static long epochMsNow() {
      return Instant.now().toEpochMilli();
    }
  }

  public record CreateGeneratorCommand(
      String generatorId,
      LatLng position,
      double radiusKm,
      int deviceCountLimit,
      int ratePerSecond) {
  }

  public record GenerateCommand(
      String generatorId) {
  }

  public record GeneratorCreatedEvent(
      String generatorId,
      LatLng position,
      double radiusKm,
      int deviceCountLimit,
      int ratePerSecond) {
  }

  public record GeneratedEvent(
      String generatorId,
      int devicesGenerated) {
  }

  public record DevicesToGenerateEvent(
      String generatorId,
      List<Device> devices) {
    static DevicesToGenerateEvent with(String generatorId, LatLng position, double radiusKm, int deviceCount) {
      return new DevicesToGenerateEvent(generatorId, generateDevices(generatorId, position, radiusKm, deviceCount));
    }

    private static List<Device> generateDevices(String generatorId, LatLng position, double radiusKm, int deviceCount) {
      return IntStream.range(0, deviceCount)
          .mapToObj(i -> nextDevice(generatorId, position, radiusKm))
          .toList();
    }

    private static Device nextDevice(String generatorId, LatLng position, double radiusKm) {
      final var random = new Random();
      final var earthRadiusKm = 6371;
      final var angle = random.nextDouble() * 2 * Math.PI;
      final var distance = random.nextDouble() * radiusKm;
      final var lat = Math.toRadians(position.lat());
      final var lng = Math.toRadians(position.lng());
      final var lat2 = Math.asin(Math.sin(lat) * Math.cos(distance / earthRadiusKm) +
          Math.cos(lat) * Math.sin(distance / earthRadiusKm) * Math.cos(angle));
      final var lng2 = lng + Math.atan2(Math.sin(angle) * Math.sin(distance / earthRadiusKm) * Math.cos(lat),
          Math.cos(distance / earthRadiusKm) - Math.sin(lat) * Math.sin(lat2));
      var deviceId = "device-id-%1.8f-%1.8f".formatted(position.lat, position.lng);
      return new Device(deviceId, generatorId, LatLng.fromRadians(lat2, lng2));
    }
  }

  public record Device(
      String deviceId,
      String generatorId,
      LatLng position) {
  }
}
