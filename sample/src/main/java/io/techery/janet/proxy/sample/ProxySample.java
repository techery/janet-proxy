package io.techery.janet.proxy.sample;

import com.google.gson.Gson;

import io.techery.janet.HttpActionService;
import io.techery.janet.Janet;
import io.techery.janet.ProxyService;
import io.techery.janet.gson.GsonConverter;
import io.techery.janet.http.annotations.HttpAction;
import io.techery.janet.okhttp.OkClient;
import io.techery.janet.proxy.sample.actions.GithubAction;
import io.techery.janet.proxy.sample.actions.base.LabeledAction;
import io.techery.janet.proxy.sample.actions.XkcdAction;
import rx.Observable;
import rx.schedulers.Schedulers;

public class ProxySample {

  public static void main(String... args) throws Throwable {
    OkClient client = new OkClient();
    GsonConverter converter = new GsonConverter(new Gson());

    Janet janet = new Janet.Builder()
        .addService(new SampleLoggingService(new ProxyService.Builder(HttpAction.class)
            .add(
                new HttpActionService("https://api.github.com", client, converter),
                action -> ((LabeledAction) action).label().equals("github"))
            .add(
                new HttpActionService("http://xkcd.com", client, converter),
                action -> ((LabeledAction) action).label().equals("xkcd"))
            .build()
        )).build();

    Observable.combineLatest(
        janet.createPipe(GithubAction.class, Schedulers.io()).createObservableResult(new GithubAction()),
        janet.createPipe(XkcdAction.class, Schedulers.io()).createObservableResult(new XkcdAction()),
        (githubAction, xkcdAction) -> null
    ).toBlocking().subscribe();
  }
}

