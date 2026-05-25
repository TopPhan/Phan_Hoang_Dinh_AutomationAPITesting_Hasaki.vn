package com.builder;

import com.globals.ConfigsGlobal;
import com.pojoModel.LoginModel;

public class LoginPOJO_Builder {

    /** Default login body sourced from config / system properties. */
    public static LoginModel getDataLogin() {
        return LoginModel.builder()
                .username(ConfigsGlobal.USERNAME)
                .password(ConfigsGlobal.PASSWORD)
                .remember(ConfigsGlobal.REMEMBER)
                .build();
    }

    /** Custom credentials — used for negative / parameterised test cases. */
    public static LoginModel getDataLogin(String username, String password) {
        return LoginModel.builder()
                .username(username)
                .password(password)
                .remember(false)
                .build();
    }
}
