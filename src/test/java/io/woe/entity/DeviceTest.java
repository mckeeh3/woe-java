package io.woe.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.Test;

import kalix.springsdk.testkit.EventSourcedTestKit;

public class DeviceTest {

  @Test
  public void createDeviceTest() {
    var testKit = EventSourcedTestKit.of(Device::new);

    var deviceId = "device-1";
    var position = new Device.LatLng(1.0, 2.0);
    var createCommand = new Device.CreateDeviceCommand(deviceId, position);

    var result = testKit.call(e -> e.create(createCommand));
    assertEquals("OK", result.getReply());

    var event = result.getNextEventOfType(Device.DeviceCreatedEvent.class);
    assertEquals(deviceId, event.deviceId());

    var state = testKit.getState();
    assertEquals(deviceId, state.deviceId());
    assertEquals(position, state.position());
  }

  @Test
  public void getDeviceTest() {
    var testKit = EventSourcedTestKit.of(Device::new);

    var command1 = createDeviceCommand("device-1", position(1, 2));
    {
      var result = testKit.call(e -> e.create(command1));
      assertFalse(result.isError());
    }

    {
      var result = testKit.call(e -> e.get(command1.deviceId()));
      assertFalse(result.isError());

      var response = result.getReply();
      assertEquals(command1.deviceId(), response.deviceId());
      assertEquals(command1.position(), response.position());
    }
  }

  @Test
  public void pingDeviceTest() {
    var testKit = EventSourcedTestKit.of(Device::new);

    var command1 = createDeviceCommand("device-1", position(1, 2));
    {
      var result = testKit.call(e -> e.create(command1));
      assertFalse(result.isError());
    }

    {
      var command = new Device.PingCommand(command1.deviceId());
      var result = testKit.call(e -> e.ping(command));
      assertFalse(result.isError());

      var events = result.getAllEvents();
      if (events.size() > 0) {
        var event = events.get(0);
        if (event instanceof Device.PingedEvent) {
          var pingedEvent = (Device.PingedEvent) event;
          assertEquals(command1.deviceId(), pingedEvent.deviceId());
        } else if (event instanceof Device.DeviceAlarmChanged) {
          var deviceNotFoundEvent = (Device.DeviceAlarmChanged) event;
          assertEquals(command1.deviceId(), deviceNotFoundEvent.deviceId());
        }
      }
    }
  }

  private static Device.LatLng position(double lat, double lng) {
    return new Device.LatLng(lat, lng);
  }

  private static Device.CreateDeviceCommand createDeviceCommand(String deviceId, Device.LatLng position) {
    return new Device.CreateDeviceCommand(deviceId, position);
  }
}
