package io.techery.janet;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.techery.janet.model.LabeledAction;
import io.techery.janet.model.MockServiceAction;
import io.techery.janet.model.MockTestAction1;
import io.techery.janet.model.MockTestAction2;
import io.techery.janet.model.MockTestAction3;
import io.techery.janet.model.OtherServiceAction;
import io.techery.janet.proxy.ServiceMappingRule;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProxyTest {

  protected Janet janet;
  protected ActionService service1;
  protected ActionService service2;
  protected ActionPipe<MockTestAction1> actionPipe1;
  protected ActionPipe<MockTestAction2> actionPipe2;

  @Before public void setup() throws JanetException {
    service1 = provideService();
    service2 = provideService();
    janet = new Janet.Builder()
        .addService(new ProxyService.Builder(MockServiceAction.class)
            .add(service1, new ServiceMappingRule<LabeledAction>() {
              @Override public boolean matches(LabeledAction action) {
                return action.label().equals("service1");
              }
            })
            .add(service2, new ServiceMappingRule<LabeledAction>() {
              @Override public boolean matches(LabeledAction action) {
                return action.label().equals("service2");
              }
            })
            .build()
        ).build();
    actionPipe1 = janet.createPipe(MockTestAction1.class);
    actionPipe2 = janet.createPipe(MockTestAction2.class);
  }

  protected ActionService provideService() throws JanetException {
    ActionService service = spy(ActionService.class);
    when(service.getSupportedAnnotationType()).thenReturn(MockServiceAction.class);
    doAnswer(new AssertUtil.SuccessAnswer(service)).when(service).sendInternal(any(ActionHolder.class));
    return service;
  }

  @Test public void serviceRoutingForObservables() {
    TestSubscriber<ActionState<MockTestAction1>> subscriber1 = new TestSubscriber<ActionState<MockTestAction1>>();
    actionPipe1.createObservable(new MockTestAction1()).subscribe(subscriber1);
    AssertUtil.SuccessAnswer.assertAllStatuses(subscriber1);

    TestSubscriber<ActionState<MockTestAction2>> subscriber2 = new TestSubscriber<ActionState<MockTestAction2>>();
    actionPipe2.createObservable(new MockTestAction2()).subscribe(subscriber2);
    AssertUtil.SuccessAnswer.assertAllStatuses(subscriber2);

    verify(service1, Mockito.times(1)).send(any(ActionHolder.class));
    verify(service2, Mockito.times(1)).send(any(ActionHolder.class));
  }

  @Test public void serviceRoutingForSendObserve() {
    TestSubscriber<ActionState<MockTestAction1>> subscriber1 = new TestSubscriber<ActionState<MockTestAction1>>();
    actionPipe1.observe().subscribe(subscriber1);
    TestSubscriber<ActionState<MockTestAction2>> subscriber2 = new TestSubscriber<ActionState<MockTestAction2>>();
    actionPipe2.observe().subscribe(subscriber2);

    actionPipe1.send(new MockTestAction1());
    //
    AssertUtil.SuccessAnswer.assertAllStatuses(subscriber1);
    verify(service1, Mockito.times(1)).send(any(ActionHolder.class));
    verify(service2, Mockito.never()).send(any(ActionHolder.class));

    actionPipe2.send(new MockTestAction2());
    //
    AssertUtil.SuccessAnswer.assertAllStatuses(subscriber2);
    verify(service1, Mockito.times(1)).send(any(ActionHolder.class));
    verify(service2, Mockito.times(1)).send(any(ActionHolder.class));
  }


  @Test public void serviceCreationChecks() {
    // check service is created of non-annotation class
    Assertions.assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
      @Override public void call() throws Throwable {
        new ProxyService.Builder(AssertUtil.class).build();
      }
    }).isInstanceOf(IllegalArgumentException.class);

    // check service is created with no sub-services
    Assertions.assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
      @Override public void call() throws Throwable {
        new ProxyService.Builder(MockServiceAction.class).build();
      }
    }).isInstanceOf(IllegalStateException.class);


    // check service is created with unsupported sub-service
    Assertions.assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
      @Override public void call() throws Throwable {
        ActionService service = mock(ActionService.class);
        when(service.getSupportedAnnotationType()).thenReturn(OtherServiceAction.class);
        new ProxyService.Builder(MockServiceAction.class)
            .add(service, new ServiceMappingRule() {
              @Override public boolean matches(Object action) {
                return true;
              }
            })
            .build();
      }
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test public void checkWrongActionException() {
    Assertions.assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
      @Override public void call() throws Throwable {
        // no rule assigned for such action label
        janet.createPipe(MockTestAction3.class).send(new MockTestAction3());
      }
    }).hasCauseExactlyInstanceOf(JanetInternalException.class);
  }

}
