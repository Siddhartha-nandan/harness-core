/**
 *
 */

package software.wings.api;

import static software.wings.api.InstanceElement.Builder.anInstanceElement;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class InstanceElement.
 *
 * @author Rishi
 */
public class InstanceElement implements ContextElement {
  private String uuid;
  private String displayName;
  private String hostName;
  private String dockerId;
  private HostElement host;
  private ServiceTemplateElement serviceTemplateElement;

  @Override
  public String getName() {
    return displayName;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.INSTANCE;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(INSTANCE, this);
    if (host != null) {
      map.putAll(host.paramMap(context));
    }
    if (serviceTemplateElement != null) {
      map.putAll(serviceTemplateElement.paramMap(context));
    }
    return map;
  }

  public String getUuid() {
    return uuid;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Gets display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets display name.
   *
   * @param displayName the display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getDockerId() {
    return dockerId;
  }

  public void setDockerId(String dockerId) {
    this.dockerId = dockerId;
  }

  /**
   * Gets host element.
   *
   * @return the host element
   */
  public HostElement getHost() {
    return host;
  }

  /**
   * Sets host element.
   *
   * @param hostElement the host element
   */
  public void setHost(HostElement hostElement) {
    this.host = hostElement;
  }

  /**
   * Gets service template element.
   *
   * @return the service template element
   */
  public ServiceTemplateElement getServiceTemplateElement() {
    return serviceTemplateElement;
  }

  /**
   * Sets service template element.
   *
   * @param serviceTemplateElement the service template element
   */
  public void setServiceTemplateElement(ServiceTemplateElement serviceTemplateElement) {
    this.serviceTemplateElement = serviceTemplateElement;
  }

  @Override
  public ContextElement cloneMin() {
    InstanceElement instanceElement =
        anInstanceElement().withUuid(uuid).withDisplayName(displayName).withHostName(hostName).build();
    if (host != null) {
      instanceElement.setHost((HostElement) host.cloneMin());
    }
    return instanceElement;
  }

  @Override
  public String toString() {
    return "InstanceElement{"
        + "uuid='" + uuid + '\'' + ", displayName='" + displayName + '\'' + ", hostName='" + hostName + '\''
        + ", hostElement=" + host + ", serviceTemplateElement=" + serviceTemplateElement + '}';
  }

  public static final class Builder {
    private String uuid;
    private String displayName;
    private String hostName;
    private String dockerId;
    private HostElement host;
    private ServiceTemplateElement serviceTemplateElement;

    private Builder() {}

    public static Builder anInstanceElement() {
      return new Builder();
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withDockerId(String dockerId) {
      this.dockerId = dockerId;
      return this;
    }

    public Builder withHost(HostElement host) {
      this.host = host;
      return this;
    }

    public Builder withServiceTemplateElement(ServiceTemplateElement serviceTemplateElement) {
      this.serviceTemplateElement = serviceTemplateElement;
      return this;
    }

    public Builder but() {
      return anInstanceElement()
          .withUuid(uuid)
          .withDisplayName(displayName)
          .withHostName(hostName)
          .withDockerId(dockerId)
          .withHost(host)
          .withServiceTemplateElement(serviceTemplateElement);
    }

    public InstanceElement build() {
      InstanceElement instanceElement = new InstanceElement();
      instanceElement.setUuid(uuid);
      instanceElement.setDisplayName(displayName);
      instanceElement.setHostName(hostName);
      instanceElement.setDockerId(dockerId);
      instanceElement.setHost(host);
      instanceElement.setServiceTemplateElement(serviceTemplateElement);
      return instanceElement;
    }
  }
}
