package com.e16din.mytaxi.screens.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.e16din.mytaxi.databinding.FragmentAuthBinding
import com.e16din.mytaxi.server.HttpClient
import com.e16din.mytaxi.support.DataKey
import com.e16din.mytaxi.support.getApplication
import com.e16din.mytaxi.support.setNavigationResult
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable


class AuthFragment : Fragment(), DataKey {

    companion object {
        val KEY_RESULT_DATA = "${AuthFragment::class.simpleName}_RESULT"
    }

    private val appAgent = AppAgent()
    private val authScreenAgent = AuthScreenAgent()
    private val mainScreenAgent = MainScreenAgent()
    private val userAgent = UserAgent()
    private val deviceAgent = DeviceAgent()
    private val serverAgent = ServerAgent()

    private lateinit var binding: FragmentAuthBinding

    init {
        deviceAgent.onViewCreated = { savedInstanceState ->
            authScreenAgent.data = deviceAgent.restoreData(savedInstanceState)
                ?: AuthScreenData(false)

            userAgent.lookAtLoginFields()
            userAgent.onLoginDataChanged = { login, password ->
                val isLoginDataCorrect = login.isNotBlank()
                        && password.isNotBlank()
                        && login.length >= 3
                        && password.length >= 6
                authScreenAgent.data.isLoginDataCorrect = isLoginDataCorrect
                userAgent.lookAtEnterButton(isLoginDataCorrect)
            }

            userAgent.lookAtEnterButton(authScreenAgent.data.isLoginDataCorrect)
            userAgent.onEnterClick = { login, password ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val isAuthSuccess = serverAgent.postLogin(login, password)
                    appAgent.isAuthorized = isAuthSuccess

                    if (isAuthSuccess) {
                        withContext(Dispatchers.Main) {
                            mainScreenAgent.showScreen(isAuthSuccess)
                        }
                    }
                }
            }
        }
        deviceAgent.onSaveInstanceState = { outState ->
            deviceAgent.saveData(outState)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAuthBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceAgent.onViewCreated.invoke(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        deviceAgent.onSaveInstanceState.invoke(outState)
        super.onSaveInstanceState(outState)
    }

    // NOTE: агент для актора работающего приожения
    inner class AppAgent {
        var isAuthorized: Boolean
            get() = getApplication().isAuthorized
            set(value) {
                getApplication().isAuthorized = value
            }
    }

    @Serializable
    class AuthScreenData(var isLoginDataCorrect: Boolean) : java.io.Serializable

    // NOTE: агент для актора экрана авторизации
    inner class AuthScreenAgent {
        lateinit var data: AuthScreenData
    }

    // NOTE: агент для актора главного экрана
    inner class MainScreenAgent {
        fun showScreen(isAuthSuccess: Boolean) {
            setNavigationResult(KEY_RESULT_DATA, isAuthSuccess)
            findNavController().popBackStack()
        }
    }

    // NOTE: агент для актора реального пользователя
    inner class UserAgent {
        lateinit var onEnterClick: (login: String, password: String) -> Unit
        lateinit var onLoginDataChanged: (login: String, password: String) -> Unit

        fun lookAtLoginFields() {
            binding.loginField.requestFocus()

            binding.loginField.doOnTextChanged { text, start, before, count ->
                onLoginDataChanged.invoke(
                    binding.loginField.text.toString(),
                    binding.passwordField.text.toString()
                )
            }

            binding.passwordField.doOnTextChanged { text, start, before, count ->
                onLoginDataChanged.invoke(
                    binding.loginField.text.toString(),
                    binding.passwordField.text.toString()
                )
            }
        }

        fun lookAtEnterButton(isEnabled: Boolean) {
            binding.enterButton.isEnabled = isEnabled

            binding.enterButton.setOnClickListener {
                onEnterClick.invoke(
                    binding.loginField.text.toString(),
                    binding.passwordField.text.toString()
                )
            }
        }
    }

    // NOTE: агент для актора реального сервера
    inner class ServerAgent {
        private val httpClient by lazy { getApplication().httpClient() }

        suspend fun postLogin(login: String, password: String): Boolean {
            return httpClient.post(HttpClient.POST_LOGIN) {
                contentType(ContentType.Application.Json)
                body = mapOf(
                    "login" to login,
                    "password" to password
                )
            }
        }
    }

    // NOTE: агент для актора устройства
    inner class DeviceAgent {
        lateinit var onViewCreated: (savedInstanceState: Bundle?) -> Unit
        lateinit var onSaveInstanceState: (outState: Bundle) -> Unit

        fun restoreData(savedInstanceState: Bundle?): AuthScreenData? {
            return savedInstanceState?.getSerializable(dataKey) as AuthScreenData?
        }

        fun saveData(outState: Bundle) {
            outState.putSerializable(dataKey, authScreenAgent.data)
        }
    }
}