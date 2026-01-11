package com.drlom.reservation;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReservationApplication {

  public static void main(String[] args) {
    SpringApplication.run(ReservationApplication.class, args);
  }

  // JVM 기본 시간대를 UTC로 설정 (DB TIMESTAMP와 일관성 유지)
  @PostConstruct
  void setDefaultTimezone() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }
}
