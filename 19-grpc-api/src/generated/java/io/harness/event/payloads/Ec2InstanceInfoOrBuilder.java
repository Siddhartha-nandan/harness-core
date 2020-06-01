// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/event/payloads/ec2_messages.proto

package io.harness.event.payloads;

@javax.annotation.Generated(value = "protoc", comments = "annotations:Ec2InstanceInfoOrBuilder.java.pb.meta")
public interface Ec2InstanceInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.event.payloads.Ec2InstanceInfo)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>string instance_id = 1[json_name = "instanceId"];</code>
   * @return The instanceId.
   */
  java.lang.String getInstanceId();
  /**
   * <code>string instance_id = 1[json_name = "instanceId"];</code>
   * @return The bytes for instanceId.
   */
  com.google.protobuf.ByteString getInstanceIdBytes();

  /**
   * <code>string instance_type = 2[json_name = "instanceType"];</code>
   * @return The instanceType.
   */
  java.lang.String getInstanceType();
  /**
   * <code>string instance_type = 2[json_name = "instanceType"];</code>
   * @return The bytes for instanceType.
   */
  com.google.protobuf.ByteString getInstanceTypeBytes();

  /**
   * <code>string capacity_reservation_id = 3[json_name = "capacityReservationId"];</code>
   * @return The capacityReservationId.
   */
  java.lang.String getCapacityReservationId();
  /**
   * <code>string capacity_reservation_id = 3[json_name = "capacityReservationId"];</code>
   * @return The bytes for capacityReservationId.
   */
  com.google.protobuf.ByteString getCapacityReservationIdBytes();

  /**
   * <code>string spot_instance_request_id = 4[json_name = "spotInstanceRequestId"];</code>
   * @return The spotInstanceRequestId.
   */
  java.lang.String getSpotInstanceRequestId();
  /**
   * <code>string spot_instance_request_id = 4[json_name = "spotInstanceRequestId"];</code>
   * @return The bytes for spotInstanceRequestId.
   */
  com.google.protobuf.ByteString getSpotInstanceRequestIdBytes();

  /**
   * <code>string instance_lifecycle = 5[json_name = "instanceLifecycle"];</code>
   * @return The instanceLifecycle.
   */
  java.lang.String getInstanceLifecycle();
  /**
   * <code>string instance_lifecycle = 5[json_name = "instanceLifecycle"];</code>
   * @return The bytes for instanceLifecycle.
   */
  com.google.protobuf.ByteString getInstanceLifecycleBytes();

  /**
   * <code>.io.harness.event.payloads.InstanceState instance_state = 6[json_name = "instanceState"];</code>
   * @return Whether the instanceState field is set.
   */
  boolean hasInstanceState();
  /**
   * <code>.io.harness.event.payloads.InstanceState instance_state = 6[json_name = "instanceState"];</code>
   * @return The instanceState.
   */
  io.harness.event.payloads.InstanceState getInstanceState();
  /**
   * <code>.io.harness.event.payloads.InstanceState instance_state = 6[json_name = "instanceState"];</code>
   */
  io.harness.event.payloads.InstanceStateOrBuilder getInstanceStateOrBuilder();

  /**
   * <code>string cluster_arn = 7[json_name = "clusterArn"];</code>
   * @return The clusterArn.
   */
  java.lang.String getClusterArn();
  /**
   * <code>string cluster_arn = 7[json_name = "clusterArn"];</code>
   * @return The bytes for clusterArn.
   */
  com.google.protobuf.ByteString getClusterArnBytes();

  /**
   * <code>string region = 8[json_name = "region"];</code>
   * @return The region.
   */
  java.lang.String getRegion();
  /**
   * <code>string region = 8[json_name = "region"];</code>
   * @return The bytes for region.
   */
  com.google.protobuf.ByteString getRegionBytes();

  /**
   * <code>string cluster_id = 9[json_name = "clusterId"];</code>
   * @return The clusterId.
   */
  java.lang.String getClusterId();
  /**
   * <code>string cluster_id = 9[json_name = "clusterId"];</code>
   * @return The bytes for clusterId.
   */
  com.google.protobuf.ByteString getClusterIdBytes();

  /**
   * <code>string setting_id = 10[json_name = "settingId"];</code>
   * @return The settingId.
   */
  java.lang.String getSettingId();
  /**
   * <code>string setting_id = 10[json_name = "settingId"];</code>
   * @return The bytes for settingId.
   */
  com.google.protobuf.ByteString getSettingIdBytes();
}
