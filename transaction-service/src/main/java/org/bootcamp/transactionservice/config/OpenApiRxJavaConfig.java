package org.bootcamp.transactionservice.config;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiRxJavaConfig {

  static {
    // Esto hace que Swagger ignore el "envoltorio" y muestre el DTO interno
    SpringDocUtils.getConfig()
      .addResponseWrapperToIgnore(Observable.class)
      .addResponseWrapperToIgnore(Single.class)
      .addResponseWrapperToIgnore(Maybe.class)
      .addResponseWrapperToIgnore(Flowable.class)
      .addResponseWrapperToIgnore(Completable.class); // Completable se suele tratar como void (204 No Content)
  }
}
