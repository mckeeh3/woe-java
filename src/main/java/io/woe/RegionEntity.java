package io.woe;

import static io.woe.WorldMap.*;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;

@EntityKey("regionId")
@EntityType("region")
@RequestMapping("/region")
public class RegionEntity extends EventSourcedEntity<RegionEntity.State> {
  private static final Logger log = LoggerFactory.getLogger(RegionEntity.class);
  private final String entityId;

  public RegionEntity(EventSourcedEntityContext context) {
    entityId = context.entityId();
  }

  @Override
  public State emptyState() {
    return State.empty();
  }

  @PutMapping("/{regionId}/update-sub-region")
  public Effect<String> updateSubRegion(@RequestBody UpdateSubRegionCommand command) {
    if (command.subRegion().zoom() < 1) {
      return effects().error("Cannot add sub-region with zoom < 1, zoom: %d".formatted(command.subRegion().zoom()));
    }
    log.info("EntityId: {}\nState: {}\nCommand: {}", entityId, currentState(), command);
    return effects()
        .emitEvents(currentState().eventsFor(command))
        .thenReply(__ -> "OK");
  }

  @PutMapping("/{regionId}/release-current-state")
  public Effect<String> releaseCurrentState(@RequestBody ReleaseCurrentStateCommand command) {
    log.info("EntityId: {}\nState: {}\nCommand: {}", entityId, currentState(), command);
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @GetMapping("/{regionId}")
  public Effect<RegionEntity.State> get(@PathVariable String regionId) {
    log.debug("EntityId: {}\nRegionId: {}\nState: {}", entityId, regionId, currentState());
    if (currentState().isEmpty()) {
      return effects().error("Region: '%s', not created".formatted(regionId));
    }
    return effects().reply(currentState());
  }

  @EventHandler
  public State on(SubRegionUpdatedEvent event) {
    log.debug("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(RegionUpdatedEvent event) {
    log.debug("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(CurrentStateReleasedEvent event) {
    log.debug("State: {}\nEvent: {}", currentState(), event);
    return currentState().on(event);
  }

  public record State(Region region, List<Region> subRegions, boolean hasChanged) {

    static State empty() {
      return new RegionEntity.State(Region.empty(), List.of(), false);
    }

    boolean isEmpty() {
      return region.isEmpty();
    }

    List<?> eventsFor(UpdateSubRegionCommand command) {
      var region = regionFor(this.region, command);
      var events = new ArrayList<>();
      events.add(new SubRegionUpdatedEvent(command.subRegion()));
      if (!hasChanged) {
        var subRegions = updateSubRegions(this.subRegions, command.subRegion());
        events.add(new RegionUpdatedEvent(region.updateCounts(subRegions)));
      }
      return events;
    }

    CurrentStateReleasedEvent eventFor(ReleaseCurrentStateCommand command) {
      var region = regionFor(this.region, command);
      return new CurrentStateReleasedEvent(region);
    }

    State on(SubRegionUpdatedEvent event) {
      var region = regionFor(this.region, event);

      if (this.region.isEmpty()) {
        var subRegions = List.of(event.subRegion());
        return new State(region.updateCounts(subRegions), subRegions, true);
      }

      var subRegions = updateSubRegions(this.subRegions, event.subRegion());
      return new State(region.updateCounts(subRegions), subRegions, true);
    }

    State on(RegionUpdatedEvent event) {
      var region = regionFor(this.region, event);
      return new State(region, subRegions, true);
    }

    State on(CurrentStateReleasedEvent event) {
      return new State(region, subRegions, false);
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

    private Region regionFor(Region region, SubRegionUpdatedEvent event) {
      if (!region.isEmpty()) {
        return region;
      }
      return regionAbove(event.subRegion());
    }

    private Region regionFor(Region region, RegionUpdatedEvent event) {
      if (!region.isEmpty()) {
        return region;
      }
      return event.region();
    }

    private List<Region> updateSubRegions(List<Region> subRegions, Region subRegion) {
      if (subRegions.isEmpty()) {
        return List.of(subRegion);
      }
      var newSubRegions = new ArrayList<Region>(subRegions.stream().filter(r -> !(r.eqShape(subRegion))).toList());
      newSubRegions.add(subRegion);
      return newSubRegions;
    }
  }

  public record UpdateSubRegionCommand(Region subRegion) {}

  public record ReleaseCurrentStateCommand(Region region) {}

  public record SubRegionUpdatedEvent(Region subRegion) {}

  public record RegionUpdatedEvent(Region region) {}

  public record CurrentStateReleasedEvent(Region region) {}

  public record PingRequest(Region region) {}

  public record PingResponse(Region region) {}
}
