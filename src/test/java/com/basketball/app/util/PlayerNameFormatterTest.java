package com.basketball.app.util;

import com.basketball.app.model.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class PlayerNameFormatterTest {

    @Test
    void toSurnameAndGivenName_twoParts_reorders() {
        assertEquals("Novák Ján", PlayerNameFormatter.toSurnameAndGivenName("Ján Novák"));
    }

    @Test
    void toSurnameAndGivenName_singlePart_unchanged() {
        assertEquals("Novák", PlayerNameFormatter.toSurnameAndGivenName("Novák"));
    }

    @Test
    void toSurnameAndGivenName_blank_empty() {
        assertEquals("", PlayerNameFormatter.toSurnameAndGivenName("  "));
    }

    @Test
    void toSurnameAndGivenName_userEntity_usesSurnameColumn() {
        User user = new User();
        user.setName("Ján");
        user.setSurname("Novák");
        assertEquals("Novák Ján", PlayerNameFormatter.toSurnameAndGivenName(user));
    }
}
