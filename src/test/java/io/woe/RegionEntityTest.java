package io.woe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

import static io.woe.WorldMap.*;

import kalix.springsdk.testkit.EventSourcedTestKit;

public class RegionEntityTest {

  @Test
  public void addDeviceTest() {
    var testKit = EventSourcedTestKit.of(RegionEntity::new);

    var deviceId = "device-1";
    var position = latLng(1.0, 2.0);
    var command1 = new RegionEntity.AddDeviceCommand(deviceId, position, false);
    {
      var result = testKit.call(e -> e.addDevice(command1));
      assertFalse(result.isError());
      assertEquals(2, result.getAllEvents().size());

      var state = testKit.getState();
      assertEquals(1, state.subRegions().size());
      assertTrue(state.hasChanged());

      var subRegion = state.subRegions().get(0);
      assertEquals(position, subRegion.topLeft());
      assertEquals(position, subRegion.botRight());

      assertTrue(state.region().contains(subRegion.topLeft()));
      assertTrue(state.region().contains(subRegion.botRight()));
    }
  }

  @Test
  public void addDeviceIdempotenceTest() {
    var testKit = EventSourcedTestKit.of(RegionEntity::new);

    var deviceId = "device-1";
    var position = latLng(1.0, 2.0);
    var command1 = new RegionEntity.AddDeviceCommand(deviceId, position, false);
    {
      var result = testKit.call(e -> e.addDevice(command1));
      assertFalse(result.isError());
      assertEquals(2, result.getAllEvents().size());
    }

    {
      var result = testKit.call(e -> e.addDevice(command1));
      assertFalse(result.isError());
      assertEquals(1, result.getAllEvents().size());
    }

    {
      var state = testKit.getState();
      assertEquals(1, state.subRegions().size());
      assertTrue(state.hasChanged());
    }
  }

  @Test
  public void addMultipleDevicesTest() {
    var testKit = EventSourcedTestKit.of(RegionEntity::new);

    var region = regionAtLatLng(zoomMax - 1, latLng(1.5, 1.5));
    var subRegions = subRegionsFor(region);
    var position1 = atCenter(subRegions.get(0));
    var position2 = atCenter(subRegions.get(1));

    {
      var deviceId = "device-1";
      var command = new RegionEntity.AddDeviceCommand(deviceId, position1, false);
      var result = testKit.call(e -> e.addDevice(command));
      assertFalse(result.isError());
    }

    {
      var deviceId = "device-2";
      var command = new RegionEntity.AddDeviceCommand(deviceId, position2, false);
      var result = testKit.call(e -> e.addDevice(command));
      assertFalse(result.isError());
    }

    {
      var state = testKit.getState();
      assertEquals(2, state.subRegions().size());
      assertEquals(zoomMax, state.subRegions().get(0).zoom());
      assertEquals(zoomMax, state.subRegions().get(1).zoom());
      assertEquals(position1, state.subRegions().get(0).topLeft());
      assertEquals(position1, state.subRegions().get(0).botRight());
      assertEquals(position2, state.subRegions().get(1).topLeft());
      assertEquals(position2, state.subRegions().get(1).botRight());
    }

    {
      var command = new RegionEntity.ReleaseCurrentStateCommand(regionIdFor(region));
      var result = testKit.call(e -> e.releaseCurrentState(command));
      assertFalse(result.isError());

      var events = result.getAllEvents();
      assertEquals(1, events.size());
      var event = result.getNextEventOfType(RegionEntity.CurrentStateReleasedEvent.class);
      assertEquals(2, event.deviceCount());
      assertEquals(0, event.deviceAlarmCount());

      var state = testKit.getState();
      assertEquals(2, state.region().deviceCount());
      assertEquals(0, state.region().deviceAlarmCount());
    }
  }

  @Test
  public void updateSubRegionTest() {
    var testKit = EventSourcedTestKit.of(RegionEntity::new);

    var deviceCount = 10;
    var deviceAlarmCount = 5;
    var region = regionAtLatLng(zoomMax - 2, latLng(1.5, 1.5));
    var subRegions = subRegionsFor(region);

    {
      var subRegion = subRegions.get(0).with(deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());

      var event1 = result.getNextEventOfType(RegionEntity.SubRegionUpdatedEvent.class);
      assertEquals(command.subRegion(), event1.subRegion());

      var event2 = result.getNextEventOfType(RegionEntity.RegionUpdatedEvent.class);
      assertEquals(region.with(deviceCount, deviceAlarmCount), event2.region());
    }

    {
      var state = testKit.getState();
      assertEquals(1, state.subRegions().size());
      assertEquals(subRegions.get(0).with(deviceCount, deviceAlarmCount), state.subRegions().get(0));
      assertEquals(region.with(deviceCount, deviceAlarmCount), state.region());
    }
  }

  @Test
  public void updateMultipleSubRegionsTest() {
    var testKit = EventSourcedTestKit.of(RegionEntity::new);

    var deviceCount = 10;
    var deviceAlarmCount = 5;
    var region = regionAtLatLng(zoomMax - 2, latLng(1.5, 1.5));
    var subRegions = subRegionsFor(region);

    {
      var subRegion = subRegions.get(0).with(deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var subRegion = subRegions.get(1).with(deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var state = testKit.getState();
      assertEquals(2, state.subRegions().size());
      assertEquals(subRegions.get(0).with(deviceCount, deviceAlarmCount), state.subRegions().get(0));
      assertEquals(subRegions.get(1).with(deviceCount, deviceAlarmCount), state.subRegions().get(1));
      assertEquals(region.with(deviceCount * 2, deviceAlarmCount * 2), state.region());
    }
  }

  @Test
  public void addSameSubRegionTwice() {
    var testKit = EventSourcedTestKit.of(RegionEntity::new);

    var region = regionAtLatLng(zoomMax - 2, latLng(1.5, 1.5));
    var subRegions = subRegionsFor(region);

    {
      var deviceCount = 10;
      var deviceAlarmCount = 5;
      var subRegion = subRegions.get(0).with(deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var deviceCount = 15;
      var deviceAlarmCount = 10;
      var subRegion = subRegions.get(0).with(deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var deviceCount = 20;
      var deviceAlarmCount = 15;
      var subRegion = subRegions.get(0).with(deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());

      var state = testKit.getState();
      assertEquals(1, state.subRegions().size());
      assertEquals(subRegions.get(0).with(deviceCount, deviceAlarmCount), state.subRegions().get(0));
      assertEquals(region.with(deviceCount, deviceAlarmCount), state.region());
    }
  }

  @Test
  public void updateSubRegionAtZoom1Test() {
    var testKit = EventSourcedTestKit.of(RegionEntity::new);

    var subRegion = regionAtLatLng(1, latLng(0.0, 0.0));
    var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
    var result = testKit.call(e -> e.updateSubRegion(command));
    assertTrue(result.isError());
  }
}
