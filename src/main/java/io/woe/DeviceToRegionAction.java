package io.woe;

import static io.woe.WorldMap.*;

import kalix.javasdk.action.Action;
import kalix.springsdk.KalixClient;
import kalix.springsdk.annotations.Subscribe;

@Subscribe.EventSourcedEntity(value = DeviceEntity.class, ignoreUnknown = true)
public class DeviceToRegionAction extends Action {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeviceToRegionAction.class);
  private final KalixClient kalixClient;

  public DeviceToRegionAction(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public Effect<String> on(DeviceEntity.DeviceCreatedEvent event) {
    log.info("Event: {}", event);
    var region = regionAtLatLng(zoomMax - 1, event.position());
    var regionId = regionIdFor(region);
    var path = "/region/%s/add-device".formatted(regionId);
    var command = new RegionEntity.AddDeviceCommand(event.deviceId(), event.position(), false);
    var returnType = String.class;
    var deferredCall = kalixClient.put(path, command, returnType);

    return effects().forward(deferredCall);
  }
}
