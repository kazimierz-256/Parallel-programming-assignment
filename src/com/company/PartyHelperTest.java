package com.company;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PartyHelperTest {

    @Test
    void previousKnightId() {
        Assertions.assertEquals(
                PartyHelper.previousKnightId(0, 4),
                3,
                "PreviousKnight doesn't loop");
    }

    @Test
    void nextKnightId() {
        Assertions.assertEquals(
                PartyHelper.nextKnightId(3, 4),
                0,
                "NextKnight doesn't loop");
    }
}