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

    var position = latLng(1.0, 2.0);
    var subRegion = with(regionAtLatLng(zoomMax - 1, position), 1, 0);
    var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
    {
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
      assertEquals(2, result.getAllEvents().size());
    }

    {
      var state = testKit.getState();
      assertEquals(1, state.subRegions().size());
      assertTrue(state.hasChanged());

      assertEquals(subRegion, state.subRegions().get(0));
    }
  }

  @Test
  public void addDeviceIdempotenceTest() {
    var testKit = EventSourcedTestKit.of(RegionEntity::new);

    var position = latLng(1.0, 2.0);
    var subRegion = with(regionAtLatLng(zoomMax - 1, position), 1, 0);
    var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
    {
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
      assertEquals(2, result.getAllEvents().size());
    }

    {
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
      assertEquals(1, result.getAllEvents().size());
    }

    {
      var state = testKit.getState();
      assertEquals(1, state.subRegions().size());
      assertTrue(state.hasChanged());
      assertEquals(1, state.region().deviceCount());
      assertEquals(0, state.region().deviceAlarmCount());
    }
  }

  @Test
  public void addMultipleDevicesTest() {
    var testKit = EventSourcedTestKit.of(RegionEntity::new);

    var region = regionAtLatLng(zoomMax - 1, latLng(1.5, 1.5));
    var subRegions = subRegionsFor(region);
    var subRegion1 = with(subRegions.get(0), 1, 0);
    var subRegion2 = with(subRegions.get(1), 1, 0);

    {
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion1);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion2);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var state = testKit.getState();
      assertEquals(2, state.subRegions().size());
      assertEquals(zoomMax, state.subRegions().get(0).zoom());
      assertEquals(zoomMax, state.subRegions().get(1).zoom());
      assertEquals(subRegion1, state.subRegions().get(0));
      assertEquals(subRegion2, state.subRegions().get(1));
      assertEquals(2, state.region().deviceCount());
      assertEquals(0, state.region().deviceAlarmCount());
    }

    {
      var command = new RegionEntity.ReleaseCurrentStateCommand(region);
      var result = testKit.call(e -> e.releaseCurrentState(command));
      assertFalse(result.isError());

      var events = result.getAllEvents();
      assertEquals(1, events.size());
      var event = result.getNextEventOfType(RegionEntity.CurrentStateReleasedEvent.class);
      assertEquals(2, event.region().deviceCount());
      assertEquals(0, event.region().deviceAlarmCount());

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
      var subRegion = with(subRegions.get(0), deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());

      var event1 = result.getNextEventOfType(RegionEntity.SubRegionUpdatedEvent.class);
      assertEquals(command.subRegion(), event1.subRegion());

      var event2 = result.getNextEventOfType(RegionEntity.RegionUpdatedEvent.class);
      assertEquals(with(region, deviceCount, deviceAlarmCount), event2.region());
    }

    {
      var state = testKit.getState();
      assertEquals(1, state.subRegions().size());
      assertEquals(with(subRegions.get(0), deviceCount, deviceAlarmCount), state.subRegions().get(0));
      assertEquals(with(region, deviceCount, deviceAlarmCount), state.region());
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
      var subRegion = with(subRegions.get(0), deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var subRegion = with(subRegions.get(1), deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var state = testKit.getState();
      assertEquals(2, state.subRegions().size());
      assertEquals(with(subRegions.get(0), deviceCount, deviceAlarmCount), state.subRegions().get(0));
      assertEquals(with(subRegions.get(1), deviceCount, deviceAlarmCount), state.subRegions().get(1));
      assertEquals(with(region, deviceCount * 2, deviceAlarmCount * 2), state.region());
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
      var subRegion = with(subRegions.get(0), deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var deviceCount = 15;
      var deviceAlarmCount = 10;
      var subRegion = with(subRegions.get(0), deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());
    }

    {
      var deviceCount = 20;
      var deviceAlarmCount = 15;
      var subRegion = with(subRegions.get(0), deviceCount, deviceAlarmCount);
      var command = new RegionEntity.UpdateSubRegionCommand(subRegion);
      var result = testKit.call(e -> e.updateSubRegion(command));
      assertFalse(result.isError());

      var state = testKit.getState();
      assertEquals(1, state.subRegions().size());
      assertEquals(with(subRegions.get(0), deviceCount, deviceAlarmCount), state.subRegions().get(0));
      assertEquals(with(region, deviceCount, deviceAlarmCount), state.region());
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

  private static Region with(Region region, int deviceCount, int deviceAlarmCount) {
    return new Region(region.zoom(), region.topLeft(), region.botRight(), deviceCount, deviceAlarmCount);
  }
}
