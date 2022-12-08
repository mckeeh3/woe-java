package io.woe;

import java.util.Collection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.woe.WorldMap.Region;
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

  @GetMapping("/regions/by-location/{zoom}/{topLeftLat}/{topLeftLng}/{botRightLat}/{botRightLng}")
  @Query("""
      SELECT * AS regions FROM regions_by_location
       WHERE region.zoom = :zoom
         AND region.topLeft.lat <= :topLeftLat
         AND region.topLeft.lng >= :topLeftLng
         AND region.botRight.lat >= :botRightLat
         AND region.botRight.lng <= :botRightLng
         AND deviceCount > 0
      """)
  public Regions getRegionsByLocation(@PathVariable Integer zoom, @PathVariable Double topLeftLat, @PathVariable Double topLeftLng, @PathVariable Double botRightLat, @PathVariable Double botRightLng) {
    return null;
  }

  public UpdateEffect<RegionViewRow> on(RegionEntity.CurrentStateReleasedEvent event) {
    log.info("State: {}\nEvent: {}", viewState(), event);
    return effects().updateState(RegionViewRow.on(event));
  }

  public record RegionViewRow(Region region, int deviceCount, int deviceAlarmCount) {

    static RegionViewRow on(RegionEntity.CurrentStateReleasedEvent event) {
      return new RegionViewRow(event.region(), event.region().deviceCount(), event.region().deviceAlarmCount());
    }
  }

  public record Regions(Collection<RegionViewRow> regions) {}
}
