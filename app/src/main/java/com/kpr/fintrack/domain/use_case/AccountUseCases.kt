package com.kpr.fintrack.domain.use_case

import com.kpr.fintrack.domain.use_case.account.AddAccount
import com.kpr.fintrack.domain.use_case.account.DeleteAccount
import com.kpr.fintrack.domain.use_case.account.GetAccounts
import com.kpr.fintrack.domain.use_case.account.UpdateAccount

data class AccountUseCases(
    val addAccount: AddAccount,
    val deleteAccount: DeleteAccount,
    val getAccounts: GetAccounts,
    val updateAccount: UpdateAccount
)
