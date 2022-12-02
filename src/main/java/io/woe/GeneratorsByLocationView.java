package io.woe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;

import static io.woe.WorldMap.*;

import java.util.Collection;

@ViewId("generators-by-location")
@Table("generators_by_location")
public class GeneratorsByLocationView extends View<GeneratorsByLocationView.GeneratorViewRow> {

  @GetMapping("/generators/by-location/{topLeftLat}/{topLeftLng}/{botRightLat}/{botRightLng}")
  @Query("""
      SELECT * AS generators FROM generators_by_location
       WHERE position.lat <= :topLeftLat
         AND position.lng >= :topLeftLng
         AND position.lng >= :topLeftLng
         AND position.lng >= :topLeftLng
      """)
  public Generators getGeneratorsByLocation(@RequestParam("topLeftLat") double topLeftLat, double topLeftLng, double botRightLat, double botRightLng) {
    return null;
  }

  @Subscribe.EventSourcedEntity(GeneratorEntity.class)
  public UpdateEffect<GeneratorViewRow> on(GeneratorEntity.GeneratorCreatedEvent event) {
    return effects().updateState(new GeneratorViewRow(
        event.generatorId(),
        event.position(),
        event.radiusKm(),
        event.ratePerSecond(),
        event.startTimeMs(),
        event.deviceCountLimit(),
        0));
  }

  @Subscribe.EventSourcedEntity(GeneratorEntity.class)
  public UpdateEffect<GeneratorViewRow> on(GeneratorEntity.GeneratedEvent event) {
    return effects().updateState(viewState().on(event));
  }

  @Subscribe.EventSourcedEntity(GeneratorEntity.class)
  public UpdateEffect<GeneratorViewRow> on(GeneratorEntity.DevicesToGenerateEvent event) {
    return effects().ignore();
  }

  public record GeneratorViewRow(
      String generatorId,
      LatLng position,
      double radiusKm,
      int ratePerSecond,
      long startTimeMs,
      int deviceCountLimit,
      int deviceCountCurrent) {
    GeneratorViewRow on(GeneratorEntity.GeneratedEvent event) {
      return new GeneratorViewRow(
          generatorId,
          position,
          radiusKm,
          ratePerSecond,
          startTimeMs,
          deviceCountLimit,
          deviceCountCurrent + event.deviceCountCurrent());
    }
  }

  public record Generators(Collection<GeneratorViewRow> generators) {}
}
