package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.stencils.Stencil;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 3/25/16.
 */
@Api("services")
@Path("services")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@PublicApi
public class ServiceResource {
  private ServiceResourceService serviceResourceService;

  /**
   * Instantiates a new service resource.
   *
   * @param serviceResourceService the service resource service
   */
  @Inject
  public ServiceResource(ServiceResourceService serviceResourceService) {
    this.serviceResourceService = serviceResourceService;
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Service>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Service> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(serviceResourceService.list(pageRequest, true));
  }

  /**
   * Gets the.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param status    the status
   * @return the rest response
   */
  @GET
  @Path("{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> get(@QueryParam("appId") String appId, @PathParam("serviceId") String serviceId,
      @QueryParam("status") SetupStatus status) {
    if (status == null) {
      status = SetupStatus.COMPLETE;
    }
    return new RestResponse<>(serviceResourceService.get(appId, serviceId, status));
  }

  /**
   * Save.
   *
   * @param appId   the app id
   * @param service the service
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Service> save(@QueryParam("appId") String appId, Service service) {
    service.setAppId(appId);
    return new RestResponse<>(serviceResourceService.save(service));
  }

  /**
   * Update.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param service   the service
   * @return the rest response
   */
  @PUT
  @Path("{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> update(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId, Service service) {
    service.setUuid(serviceId);
    service.setAppId(appId);
    return new RestResponse<>(serviceResourceService.update(service));
  }

  /**
   * Delete.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the rest response
   */
  @DELETE
  @Path("{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    serviceResourceService.delete(appId, serviceId);
    return new RestResponse();
  }

  /**
   * Save command.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param command   the command
   * @return the rest response
   */
  @POST
  @Path("{serviceId}/commands")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> saveCommand(@ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId,
      @ApiParam(name = "command", required = true) ServiceCommand command) {
    return new RestResponse<>(serviceResourceService.addCommand(appId, serviceId, command));
  }

  @GET
  @Path("{serviceId}/commands/{commandName}")
  @Timed
  @ExceptionMetered
  public RestResponse<ServiceCommand> getCommand(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("commandName") String commandName,
      @QueryParam("version") int version) {
    return new RestResponse<>(
        serviceResourceService.getCommandByNameAndVersion(appId, serviceId, commandName, version));
  }

  /**
   * Update command.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param command   serviceCommand
   * @return the rest response
   */
  @PUT
  @Path("{serviceId}/commands/{commandName}")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> updateCommand(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId, ServiceCommand command) {
    return new RestResponse<>(serviceResourceService.updateCommand(appId, serviceId, command));
  }

  /**
   * Delete command.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the rest response
   */
  @DELETE
  @Path("{serviceId}/commands/{commandName}")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> deleteCommand(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("commandName") String commandName) {
    return new RestResponse<>(serviceResourceService.deleteCommand(appId, serviceId, commandName));
  }

  /**
   * Stencils rest response.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the rest response
   */
  @GET
  @Path("{serviceId}/commands/stencils")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Stencil>> stencils(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @QueryParam("filterCommand") String commandName) {
    return new RestResponse<>(serviceResourceService.getCommandStencils(appId, serviceId, commandName));
  }

  @POST
  @Path("{serviceId}/containers/tasks")
  @Timed
  @ExceptionMetered
  public RestResponse<ContainerTask> createContainerTask(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId, ContainerTask containerTask) {
    containerTask.setAppId(appId);
    containerTask.setServiceId(serviceId);
    return new RestResponse<>(serviceResourceService.createContainerTask(containerTask));
  }

  @GET
  @Path("{serviceId}/containers/tasks")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ContainerTask>> listContainerTask(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @BeanParam PageRequest<ContainerTask> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("serviceId", serviceId, EQ);
    return new RestResponse<>(serviceResourceService.listContainerTasks(pageRequest));
  }

  @PUT
  @Path("{serviceId}/containers/tasks/{taskId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ContainerTask> createContainerTask(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("taskId") String taskId, ContainerTask containerTask) {
    containerTask.setAppId(appId);
    containerTask.setServiceId(serviceId);
    containerTask.setUuid(taskId);
    return new RestResponse<>(serviceResourceService.updateContainerTask(containerTask));
  }

  @GET
  @Path("{serviceId}/containers/tasks/stencils")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Stencil>> listTaskStencils(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    return new RestResponse<>(serviceResourceService.getContainerTaskStencils(appId, serviceId));
  }
}
