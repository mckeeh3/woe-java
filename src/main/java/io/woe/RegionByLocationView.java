package io.woe;

import static io.woe.WorldMap.*;

import java.util.Collection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;

@ViewId("regions-by-location")
@Table("regions_by_location")
@Subscribe.EventSourcedEntity(value = RegionEntity.class, ignoreUnknown = true)
public class RegionByLocationView extends View<RegionByLocationView.RegionViewRow> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegionByLocationView.class);

  @GetMapping("/regions/by-location/{topLeftLat}/{topLeftLng}/{botRightLat}/{botRightLng}")
  @Query("""
      SELECT * AS regions FROM regions_by_location
       WHERE position.lat <= :topLeftLat
         AND position.lng >= :topLeftLng
         AND position.lat >= :botRightLat
         AND position.lng <= :botRightLng
      """)
  public Regions getRegionsByLocation(@PathVariable Double topLeftLat, @PathVariable Double topLeftLng, @PathVariable Double botRightLat, @PathVariable Double botRightLng) {
    return null;
  }

  public UpdateEffect<RegionViewRow> on(RegionEntity.CurrentStateReleasedEvent event) {
    log.info("State: {}\nEvent: {}", viewState(), event);
    return effects().updateState(new RegionViewRow(event.region(), event.deviceCount(), event.deviceAlarmCount()));
  }

  public record RegionViewRow(Region region, int deviceCount, int deviceAlarmCount) {}

  public record Regions(Collection<RegionViewRow> regions) {}
}
