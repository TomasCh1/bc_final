package com.basketball.app.util;

import com.basketball.app.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserNamesTest {

    @Test
    void getDisplayName_givenAndSurname() {
        User user = new User();
        user.setName("Ján");
        user.setSurname("Novák");
        assertEquals("Ján Novák", UserNames.getDisplayName(user));
    }

    @Test
    void toSurnameAndGivenName_givenAndSurname() {
        User user = new User();
        user.setName("Ján");
        user.setSurname("Novák");
        assertEquals("Novák Ján", UserNames.toSurnameAndGivenName(user));
    }

    @Test
    void splitFullName_twoParts() {
        String[] split = UserNames.splitFullName("Ján Novák");
        assertEquals("Ján", split[0]);
        assertEquals("Novák", split[1]);
    }
}
