package com.drlom.reservation;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReservationApplication {

  public static void main(String[] args) {
    // JVM 기본 시간대를 UTC로 설정 (Spring 실행 전에 설정 - DB 연결 시 적용)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(ReservationApplication.class, args);
  }
}
