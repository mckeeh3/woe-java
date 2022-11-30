package io.woe.entity;

import java.time.Instant;
import java.util.List;
import java.util.Random;

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

@EntityKey("deviceId")
@EntityType("device")
@RequestMapping("/device")
public class Device extends EventSourcedEntity<Device.State> {
  private static final Logger log = LoggerFactory.getLogger(Device.class);
  private static final Random random = new Random();

  @Override
  public State emptyState() {
    return State.empty();
  }

  @PostMapping("/{deviceId}/create")
  public Effect<String> create(@RequestBody CreateDeviceCommand command) {
    log.info("State: {}\nCommand: {}", currentState(), command);
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @PutMapping("/{deviceId}/ping")
  public Effect<String> ping(@RequestBody PingCommand command) {
    log.info("State: {}\nCommand: {}", currentState(), command);
    return effects()
        .emitEvents(currentState().eventsFor(command))
        .thenReply(__ -> "OK");
  }

  @GetMapping("/{deviceId}")
  public Effect<Device.State> get(@PathVariable String deviceId) {
    log.info("DeviceId: {}\nState: {}", deviceId, currentState());
    if (currentState().isEmpty()) {
      return effects().error("Device not created");
    }
    return effects().reply(currentState());
  }

  @EventHandler
  public State on(DeviceCreatedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(PingedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(DeviceAlarmChanged event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  public record LatLng(
      double lat,
      double lng) {
  }

  public record State(
      String deviceId,
      LatLng position,
      Instant lastPing,
      boolean alarmOn,
      Instant alarmLastTriggered) {

    public static State empty() {
      return new State(null, new LatLng(0, 0), null, false, null);
    }

    public boolean isEmpty() {
      return deviceId == null;
    }

    public DeviceCreatedEvent eventFor(CreateDeviceCommand command) {
      return new DeviceCreatedEvent(command.deviceId, command.position);
    }

    public List<?> eventsFor(PingCommand command) {
      if (alarmOn && random.nextDouble() * 100 > 95) {
        return List.of(new DeviceAlarmChanged(command.deviceId, true, Instant.now()));
      } else if (!alarmOn && random.nextDouble() * 1_000 > 999) {
        return List.of(new DeviceAlarmChanged(command.deviceId, false, Instant.now()));
      }
      return List.of();
    }

    public State on(DeviceCreatedEvent event) {
      if (deviceId != null) {
        return this;
      }
      return new State(event.deviceId, event.position, Instant.ofEpochSecond(0), alarmOn, Instant.now());
    }

    public State on(PingedEvent event) {
      var lastPinged = Instant.now();
      return new State(deviceId, position, lastPinged, alarmOn, alarmLastTriggered);
    }

    public State on(DeviceAlarmChanged event) {
      return new State(deviceId, position, Instant.now(), event.alarmOn, event.alarmLastTriggered);
    }
  }

  public record CreateDeviceCommand(
      String deviceId,
      LatLng position) {
  }

  public record DeviceCreatedEvent(
      String deviceId,
      LatLng position) {
  }

  public record PingCommand(
      String deviceId) {
  }

  public record PingedEvent(
      String deviceId) {
  }

  public record DeviceAlarmChanged(
      String deviceId,
      boolean alarmOn,
      Instant alarmLastTriggered) {
  }
}
