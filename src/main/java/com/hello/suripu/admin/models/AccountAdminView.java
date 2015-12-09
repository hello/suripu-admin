package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.Account;
import org.jetbrains.annotations.NotNull;

public class AccountAdminView {

    @JsonProperty("account")
    public final Account account;

    private AccountAdminView(final Account account) {
        this.account = account;
    }

    @JsonProperty("disabled")
    public final Boolean disabled() {
        return account.email.startsWith("ADMIN-DISABLED");
    }

    public static AccountAdminView fromAccount(@NotNull final Account account) {
        return new AccountAdminView(account);
    }

}
