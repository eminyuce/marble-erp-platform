package com.ozerler.marble.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeDismissDurationTest {

    @Test
    @DisplayName("A blank or unreadable value falls back to 100 seconds")
    void usesTheDefaultWhenTheValueIsMissing() {
        Assertions.assertThat(NoticeDismissDuration.seconds(null)).isEqualTo(100);
        Assertions.assertThat(NoticeDismissDuration.seconds("")).isEqualTo(100);
        Assertions.assertThat(NoticeDismissDuration.seconds("abc")).isEqualTo(100);
        Assertions.assertThat(NoticeDismissDuration.seconds("12.5")).isEqualTo(100);
    }

    @Test
    @DisplayName("Only a whole number of seconds between 1 and 3600 is saved")
    void acceptsAWholeNumberInsideTheRange() {
        Assertions.assertThat(NoticeDismissDuration.isAcceptable("100")).isTrue();
        Assertions.assertThat(NoticeDismissDuration.isAcceptable(" 30 ")).isTrue();
        Assertions.assertThat(NoticeDismissDuration.seconds(" 30 ")).isEqualTo(30);
        Assertions.assertThat(NoticeDismissDuration.isAcceptable("1")).isTrue();
        Assertions.assertThat(NoticeDismissDuration.isAcceptable("3600")).isTrue();
        Assertions.assertThat(NoticeDismissDuration.isAcceptable("0")).isFalse();
        Assertions.assertThat(NoticeDismissDuration.isAcceptable("3601")).isFalse();
    }
}
