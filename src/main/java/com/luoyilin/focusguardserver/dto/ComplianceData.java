package com.luoyilin.focusguardserver.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public final class ComplianceData {
    public record Day(@NotNull LocalDate day,boolean participated,
                      @NotNull @Pattern(regexp="complete|pending|failed|inactive") String state,@Min(0) long observedThrough) { }
    public record Request(@NotNull @Size(max=14) List<@jakarta.validation.Valid Day> days) { }
    public record Result(long serverTime,List<Day> days) { }
}
