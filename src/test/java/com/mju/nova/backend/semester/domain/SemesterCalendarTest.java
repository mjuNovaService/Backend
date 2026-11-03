package com.mju.nova.backend.semester.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SemesterCalendarTest {

    @Test
    void 학기_중이면_그_학기() {
        SemesterCalendar.Term term = SemesterCalendar.termOf(LocalDate.of(2027, 4, 15));

        assertThat(term.name()).isEqualTo("2027-1");
        assertThat(term.startDate()).isEqualTo(LocalDate.of(2027, 3, 1));
        assertThat(term.endDate()).isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    void 겨울방학_1_2월은_그해_1학기() {
        assertThat(SemesterCalendar.termOf(LocalDate.of(2027, 1, 1)).name()).isEqualTo("2027-1");
        assertThat(SemesterCalendar.termOf(LocalDate.of(2027, 2, 28)).name()).isEqualTo("2027-1");
    }

    @Test
    void 여름방학_7_8월은_그해_2학기() {
        SemesterCalendar.Term term = SemesterCalendar.termOf(LocalDate.of(2027, 7, 1));

        assertThat(term.name()).isEqualTo("2027-2");
        assertThat(term.startDate()).isEqualTo(LocalDate.of(2027, 9, 1));
        assertThat(term.endDate()).isEqualTo(LocalDate.of(2027, 12, 31));
    }

    @Test
    void 학기_경계() {
        assertThat(SemesterCalendar.termOf(LocalDate.of(2027, 6, 30)).name()).isEqualTo("2027-1");
        assertThat(SemesterCalendar.termOf(LocalDate.of(2027, 12, 31)).name()).isEqualTo("2027-2");
        assertThat(SemesterCalendar.termOf(LocalDate.of(2028, 1, 1)).name()).isEqualTo("2028-1");
    }
}
