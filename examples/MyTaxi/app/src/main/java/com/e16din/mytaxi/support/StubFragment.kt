package com.e16din.mytaxi.support

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.e16din.mytaxi.databinding.FragmentMainBinding
import kotlinx.parcelize.Parcelize


class StubFragment : Fragment() {

    @Parcelize
    class MainScreenData(
        var temp: Int = 0
    ) : Parcelable

    // NOTE: агент для актора работающего приожения
    inner class AppAgent {
    }

    // NOTE: агент для актора экрана
    inner class MainScreenAgent {
        val dataKey = MainScreenData::class.java.simpleName
        var data = MainScreenData()
    }

    // NOTE: агент для актора реального пользователя
    inner class UserAgent {
    }

    // NOTE: агент для актора реального сервера
    inner class ServerAgent {
    }

    // NOTE: агент для актора операционной системы телефона
    inner class SystemAgent {
        lateinit var onCreateView: () -> Unit
        lateinit var onViewCreated: (savedInstanceState: Bundle?) -> Unit
        lateinit var onSaveInstanceState: (outState: Bundle) -> Unit
    }

    private val appAgent = AppAgent()
    private val screenAgent = MainScreenAgent()
    private val userAgent = UserAgent()
    private val systemAgent = SystemAgent()
    private val serverAgent = ServerAgent()

    private lateinit var binding: FragmentMainBinding

    init {
        systemAgent.onCreateView = {
            // todo: let's code
        }
        systemAgent.onViewCreated = { savedInstanceState ->
            savedInstanceState?.getParcelable<MainScreenData>(screenAgent.dataKey)?.let {
                screenAgent.data = it
            }

            // todo: let's code
        }
        systemAgent.onSaveInstanceState = { outState ->
            outState.putParcelable(screenAgent.dataKey, screenAgent.data)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        systemAgent.onCreateView.invoke()
        binding = FragmentMainBinding.inflate(inflater) // todo: replace with your binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        systemAgent.onViewCreated.invoke(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        systemAgent.onSaveInstanceState.invoke(outState)
        super.onSaveInstanceState(outState)
    }
}