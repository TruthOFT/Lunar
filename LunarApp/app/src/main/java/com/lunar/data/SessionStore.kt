package com.lunar.data

import android.content.Context
import android.content.SharedPreferences

class SessionStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("lunar_login", Context.MODE_PRIVATE)

    fun load(): AuthSession? {
        val token = prefs.getString(KEY_TOKEN, null).orEmpty()
        if (token.isBlank()) {
            AuthTokenHolder.token = ""
            return null
        }
        AuthTokenHolder.token = token
        return AuthSession(
            token = token,
            userId = prefs.getLong(KEY_USER_ID, 0L),
            account = prefs.getString(KEY_ACCOUNT, "").orEmpty(),
            nickname = prefs.getString(KEY_NICKNAME, "").orEmpty()
        )
    }

    fun save(session: AuthSession) {
        AuthTokenHolder.token = session.token
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putLong(KEY_USER_ID, session.userId)
            .putString(KEY_ACCOUNT, session.account)
            .putString(KEY_NICKNAME, session.nickname)
            .putInt("isLogin", 1)
            .apply()
    }

    fun clear() {
        AuthTokenHolder.token = ""
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "userId"
        private const val KEY_ACCOUNT = "account"
        private const val KEY_NICKNAME = "nickname"
    }
}
