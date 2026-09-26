package com.ozerler.marble.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeDismissDurationTest {

    @Test
    @DisplayName("A blank or unreadable value falls back to 100 seconds")
    void usesTheDefaultWhenTheValueIsMissing() {
        assertThat(NoticeDismissDuration.seconds(null)).isEqualTo(100);
        assertThat(NoticeDismissDuration.seconds("")).isEqualTo(100);
        assertThat(NoticeDismissDuration.seconds("abc")).isEqualTo(100);
        assertThat(NoticeDismissDuration.seconds("12.5")).isEqualTo(100);
    }

    @Test
    @DisplayName("Only a whole number of seconds between 1 and 3600 is saved")
    void acceptsAWholeNumberInsideTheRange() {
        assertThat(NoticeDismissDuration.isAcceptable("100")).isTrue();
        assertThat(NoticeDismissDuration.isAcceptable(" 30 ")).isTrue();
        assertThat(NoticeDismissDuration.seconds(" 30 ")).isEqualTo(30);
        assertThat(NoticeDismissDuration.isAcceptable("1")).isTrue();
        assertThat(NoticeDismissDuration.isAcceptable("3600")).isTrue();
        assertThat(NoticeDismissDuration.isAcceptable("0")).isFalse();
        assertThat(NoticeDismissDuration.isAcceptable("3601")).isFalse();
    }
}
