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
        .emitEvents(currentState().eventsFor(command))
        .thenReply(__ -> "OK");
  }

  @PutMapping("/{regionId}/update-sub-region")
  public Effect<String> updateSubRegion(@RequestBody UpdateSubRegionCommand command) {
    if (command.subRegion().zoom() == 1) {
      return effects().error("Cannot add sub-region with zoom level 1");
    }
    log.info("State: {}\nCommand: {}", currentState(), command);
    return effects()
        .emitEvents(currentState().eventsFor(command))
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
  public State on(SubRegionUpdatedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(RegionUpdatedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(CurrentStateReleasedEvent event) {
    log.info("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  public record State(Region region, List<Region> subRegions, boolean hasChanged) {

    static State empty() {
      return new RegionEntity.State(Region.empty(), List.of(), false);
    }

    List<?> eventsFor(AddDeviceCommand command) {
      var region = regionFor(this.region, command);
      var events = new ArrayList<>();
      events.add(new DeviceAddedEvent(command.deviceId(), command.position(), command.alarmOn()));
      if (!hasChanged) {
        events.add(new RegionUpdatedEvent(region));
      }
      return events;
    }

    List<?> eventsFor(UpdateSubRegionCommand command) {
      var region = regionFor(this.region, command);
      var events = new ArrayList<>();
      events.add(new SubRegionUpdatedEvent(command.subRegion()));
      if (!hasChanged) {
        var subRegions = new ArrayList<Region>(this.subRegions.stream().filter(r -> !(r.eqShape(command.subRegion))).toList());
        subRegions.add(command.subRegion());
        events.add(new RegionUpdatedEvent(region.with(subRegions)));
      }
      return events;
    }

    CurrentStateReleasedEvent eventFor(ReleaseCurrentStateCommand command) {
      // var deviceCount = subRegions.stream().mapToInt(Region::deviceCount).sum();
      // var deviceAlarmCount = subRegions.stream().mapToInt(Region::deviceAlarmCount).sum();
      // var region = this.region.with(deviceCount, deviceAlarmCount);
      var region = regionFor(this.region, command);
      return new CurrentStateReleasedEvent(region);
    }

    State on(DeviceAddedEvent event) {
      var region = regionAtLatLng(zoomMax - 1, event.position());
      var subRegion = new Region(zoomMax, event.position(), event.position(), 1, event.alarmOn() ? 1 : 0);

      if (region.isEmpty()) {
        return new State(region, List.of(subRegion), true);
      }

      var subRegions = new ArrayList<Region>(this.subRegions.stream().filter(r -> !(r.eqShape(subRegion))).toList());
      subRegions.add(subRegion);
      return with(region, subRegions, true);
    }

    State on(SubRegionUpdatedEvent event) {
      var subRegion = event.subRegion();
      var region = regionAbove(subRegion);

      if (region.isEmpty()) {
        var subRegions = List.of(event.subRegion());
        return new State(region.with(subRegions()), subRegions, true);
      }

      var subRegions = new ArrayList<Region>(this.subRegions.stream().filter(r -> !(r.eqShape(event.subRegion))).toList());
      subRegions.add(event.subRegion());
      return new State(region.with(subRegions), subRegions, true);
    }

    State on(RegionUpdatedEvent event) {
      return new State(region, subRegions, true);
    }

    State on(CurrentStateReleasedEvent event) {
      return with(event.region.deviceCount(), event.region.deviceAlarmCount());
    }

    State with(Region region, List<Region> subRegions, boolean changed) {
      return new State(region, subRegions, changed);
    }

    State with(int deviceCount, int deviceAlarmCount) {
      return new State(region.with(subRegions), subRegions, hasChanged);
    }

    private Region regionFor(Region region, AddDeviceCommand command) {
      if (!region.isEmpty()) {
        return region;
      }
      region = regionAtLatLng(zoomMax - 1, command.position());
      return region;
    }

    private Region regionFor(Region region, UpdateSubRegionCommand command) {
      if (!region.isEmpty()) {
        return region;
      }
      return regionAbove(command.subRegion());
    }

    private Region regionFor(Region region, ReleaseCurrentStateCommand command) {
      if (!region.isEmpty()) {
        return region;
      }
      return command.region();
    }
  }

  public record AddDeviceCommand(String deviceId, LatLng position, boolean alarmOn) {}

  public record UpdateSubRegionCommand(Region subRegion) {}

  public record ReleaseCurrentStateCommand(Region region) {}

  public record DeviceAddedEvent(String deviceId, LatLng position, boolean alarmOn) {}

  public record SubRegionUpdatedEvent(Region subRegion) {}

  public record RegionUpdatedEvent(Region region) {}

  public record CurrentStateReleasedEvent(Region region) {}

  public record PingRequest(Region region) {}

  public record PingResponse(Region region) {}
}
