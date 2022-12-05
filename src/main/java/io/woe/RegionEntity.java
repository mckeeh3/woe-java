package io.woe;

import static io.woe.WorldMap.*;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.woe.WorldMap.Region;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;

@EntityKey("regionId")
@EntityType("region")
@RequestMapping("/region")
public class RegionEntity extends EventSourcedEntity<RegionEntity.State> {
  private static final Logger log = LoggerFactory.getLogger(RegionEntity.class);

  @Override
  public State emptyState() {
    return State.empty();
  }

  @PutMapping("/{regionId}/add-device")
  public Effect<String> addDevice(@RequestBody AddDeviceCommand command) {
    log.info("State: {}\nCommand: {}", currentState(), command);
    return effects()
        .emitEvents(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @PutMapping("/{regionId}/add-sub-region")
  public Effect<String> addSubRegion(@RequestBody AddSubRegionCommand command) {
    log.info("State: {}\nCommand: {}", currentState(), command);
    return effects()
        .emitEvents(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @PutMapping("/{regionId}/release-current-state")
  public Effect<String> releaseCurrentState(@RequestBody ReleaseCurrentStateCommand command) {
    log.info("State: {}\nCommand: {}", currentState(), command);
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @EventHandler
  public State on(DeviceAddedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(SubRegionAddedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(RegionChangedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(CurrentStateReleasedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  public record State(String regionId, Region region, List<Region> subRegions, boolean hasChanged) {

    static State empty() {
      return new RegionEntity.State(null, Region.empty(), List.of(), false);
    }

    List<?> eventFor(AddDeviceCommand command) {
      var events = new ArrayList<>();
      events.add(new DeviceAddedEvent(command.deviceId(), command.position(), command.alarmOn()));
      if (!hasChanged) {
        events.add(new RegionChangedEvent(regionId, region));
      }
      return events;
    }

    List<?> eventFor(AddSubRegionCommand command) {
      var events = new ArrayList<>();
      events.add(new SubRegionAddedEvent(command.subRegion()));
      if (!hasChanged) {
        events.add(new RegionChangedEvent(regionId, region));
      }
      return events;
    }

    CurrentStateReleasedEvent eventFor(ReleaseCurrentStateCommand command) {
      var deviceCount = subRegions.stream().mapToInt(Region::deviceCount).sum();
      var deviceAlarmCount = subRegions.stream().mapToInt(Region::deviceAlarmCount).sum();
      return new CurrentStateReleasedEvent(regionId, region, deviceCount, deviceAlarmCount);
    }

    State on(DeviceAddedEvent event) {
      var region = regionAtLatLng(zoomMax - 1, event.position());
      var subRegion = new Region(zoomMax, event.position(), event.position(), 1, event.alarmOn() ? 1 : 0);

      if (regionId == null) {
        return new State(regionIdFor(region), region, List.of(subRegion), true);
      }

      var subRegions = new ArrayList<Region>(this.subRegions);
      subRegions.add(subRegion);
      return this.with(subRegions, true);
    }

    State on(SubRegionAddedEvent event) {
      var region = regionAbove(event.subRegion());

      if (regionId == null) {
        return new State(regionIdFor(region), region, List.of(event.subRegion()), true);
      }

      var subRegions = new ArrayList<Region>(this.subRegions);
      subRegions.add(event.subRegion());
      return this.with(subRegions, true);
    }

    State on(RegionChangedEvent event) {
      return new State(regionId, region, subRegions, true);
    }

    State on(CurrentStateReleasedEvent event) {
      var region = new Region(zoomMax, this.region.topLeft(), this.region.botRight(), event.deviceCount, event.deviceAlarmCount);
      return new State(regionId, region, subRegions, false);
    }

    State with(List<Region> subRegions, boolean changed) {
      return new State(regionId, region, subRegions, changed);
    }
  }

  public record AddDeviceCommand(String deviceId, LatLng position, boolean alarmOn) {}

  public record AddSubRegionCommand(Region subRegion) {}

  public record ReleaseCurrentStateCommand(String regionId) {}

  public record DeviceAddedEvent(String deviceId, LatLng position, boolean alarmOn) {}

  public record SubRegionAddedEvent(Region subRegion) {}

  public record RegionChangedEvent(String regionId, Region region) {}

  public record CurrentStateReleasedEvent(String regionId, Region region, int deviceCount, int deviceAlarmCount) {}
}
