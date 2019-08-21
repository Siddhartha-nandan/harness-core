package software.wings.delegatetasks;

import static io.harness.exception.WingsException.ExecutionContext.DELEGATE;
import static org.joor.Reflect.on;
import static software.wings.delegatetasks.RemoteMethodReturnValueData.Builder.aRemoteMethodReturnValueData;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.joor.ReflectException;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
@Slf4j
public class ServiceImplDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private Injector injector;

  public ServiceImplDelegateTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> postExecute, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public RemoteMethodReturnValueData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public RemoteMethodReturnValueData run(Object[] parameters) {
    String key = (String) parameters[0];
    Object service = null;
    try {
      service = injector.getInstance(Key.get(Class.forName(key)));
    } catch (ClassNotFoundException e) {
      logger.error("", e);
    }
    String method = (String) parameters[1];
    Throwable exception = null;
    Object methodReturnValue = null;
    try {
      if (parameters.length == 2) {
        methodReturnValue = on(service).call(method).get();
      } else {
        Object[] methodParameters = new Object[parameters.length - 2];
        System.arraycopy(parameters, 2, methodParameters, 0, parameters.length - 2);
        methodReturnValue = on(service).call(method, methodParameters).get();
      }
    } catch (ReflectException e) {
      if (e.getCause().getClass().isAssignableFrom(NoSuchMethodException.class)) {
        exception = e.getCause();
      } else {
        exception = e.getCause();
        if (exception.getCause() != null) {
          exception = exception.getCause();
        }
      }
      if (exception instanceof WingsException) {
        ExceptionLogger.logProcessedMessages((WingsException) exception, DELEGATE, logger);
      } else {
        logger.error("Task error", exception);
      }
    }
    return aRemoteMethodReturnValueData().withReturnValue(methodReturnValue).withException(exception).build();
  }
}
