# Декларативная архитектура на акторах 
(или как я задолбался оверхедить и провел рефакторинг)


Начало
-------

Привет, меня зовут Александр и я разрабатываю Android-приложения. 

На моем счету около 50-ти разного рода приложений, и по типу, назначению и по размеру.

У меня есть мечта - сделать разработку мобильных приложений простой и убрать оверхед из своих проектов.

Зачем все это
-------

Я тут хотел обойти острые углы и сосредоточиться на позитиве, но как-то все получается слишком ванильным, добавлю щепотку гнева.

Меня задолбало что для написания простого приложения надо писать горы оторванных от реальности абстракций. Меня задолбало что чем больше становится проект тем сложнее разбираться в его коде. Меня задолбало что я долго разбираюсь с кодом других разработчиков, а другие разработчики долго разбираются с моим кодом. Меня задолбало что подходы призванные упростить работу с кодом (MVC/MVVM/Clean Architecture), усложняют структуру этого кода. Меня задолбало что проект собирается по 5-10 минут, а то и больше. Меня задолбала неразбериха с именованием классов и пакетов. Меня задолбало.. 

(Можем продолжить список в комментариях ;))

Я хотел делать приложения быстро, решать прикладные задачи относящиеся в первую очередь к функционалу приложения, а не возиться во всей этой тягомотине -  в различных архитектурных подходах и фреймворках-костылях которые их обслуживают.

Когда я стал руководить командой, заниматься наемом и менторить ребят, я понял что с такими сложностями сталкиваюсь не я один. 

Особенно сложно приходится начинающим ребятам - куча библиотек-монстров, бородатых аббревиатур и понятий в которые надо врубиться. Да еще надо все правильно понять и применить. 

В индустрии на лицо тренд усложнения разработки, и в текущих реалиях появилось куча костылей и подпорок в виде проросших в проект фреймворков. (Отмечу что есть и обратная тенденция - функциональный подход, декларативный UI, корутины)

Так вот, я искал решение этих проблем - и в итоге нашел архитектуру которая проста в реализации, легко масштабируется, не зависит от фреймворков, сводит к минимуму фрагментацию кода, и проста в понимании. 

Я назвал ее - акторная архитектура (или Actor Based-Architecture). 

История
=======

Как-то раз с моей женой(она как раз училась на философском; любимая, спасибо) мы завели разговор про объективирование и субъект-субъектные отношения, и тут меня осенило!

А что если описать бизнес процессы не упрощенно и утилитарно, как мы привыкли делать, пишем все с позиции приложения, в лучшем случае с позиции приложения и пользователя, что если полностью описать все субъекты участвующие в бизнес-процессе?

Я переложил философские идеи субъект-субъектного взаимодействия на архитектуру мобильного приложения и получил способ моделирования наиболее полного бизнесс-процесса, на основе моделирования действующих субъектов на каждом экране приложения. 

(В последствии я выяснил что у термина "субъект" в разных областях свое значение и это несет некоторую путаницу в смыслах. Я буду говорить о субъектах как о действующих лицах учавствующих в процессе. Или даже давайте сразу использовать термин "действующее лицо" - актор.)

Решение
-------

Представьте себе эко-систему действующего приложения, какие в ней есть действующие лица? 

Если идти снаружи вглубь получится например так:

=> Пользователь</br>
=> Сервер</br>
=> Телефон и OS</br>
&nbsp;&nbsp;&nbsp;&nbsp;=> Приложение</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;=> Экран авторизации</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;=> Главный экран</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;=> ...</br>

Все это акторы - действующие лица.

(В некоторых приложениях бывает несколько типов пользователей - например заказчик и исполнитель - это разные акторы.)

### Подробнее про акторов

Каждый экран по сути это актор. Он может взаимодействовать с пользователем который тоже актор - например экран показывает пользователю сообщение об ошибке, а пользователь жмет на кнопку "ок". 

Экран может взаимодействовать с сервером - отправить данные, получить данные. 

Еще он может взаимодействовать с операционной системой телефона - скрыть системную клавиатуру, получить сообщение о сворачивании приложения. 

Один экран может взаимодействовать с другим экраном - например передавать или получать данные. 

Само приложение это тоже актор, и любые экраны-акторы могут например сохранять/получать данные из приложения, или подписаться на какое-то глобальное событие.

Пример
------

Посмотрим на схему взаимодействия акторов на примере приложения: (Здесь надо нарисовать таблички с действиями)

![taxi_main_screen](taxi_main_screen.svg)


Сущность каждого актора неделима и объемна, а создавать God-объект под каждое действующее лицо так себе затея, поэтому я не описываю акторы напрямую в коде.

Вместо этого я создаю "агентов" - это такие интерфейсы через которые мы общаемся с реальными сущностями-акторами(пользователем, сервером, экраном и т.д.).

Грубо говоря в каждом фрагменте(активити) существует свой набор агентов, эти агенты содержат только необходимый функционал для взаимодействия акторов на этом фрагменте.

Привожу код экрана авторизации тестового приложения: 


```kotlin
class AuthFragment : Fragment(), DataKey {

    companion object {
        val KEY_IS_AUTH_SUCCESS = "${AuthFragment::class.simpleName}_IS_AUTH_SUCCESS"
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
                            mainScreenAgent.appear()
                        }
                    }
                }
            }
        }
        deviceAgent.onSaveInstanceState = { outState ->
            deviceAgent.saveData(outState)
        }
    }

    // ...

    // NOTE: агент для актора работающего приожения
    inner class AppAgent {
        // ...
    }

    // NOTE: агент для актора экрана авторизации
    inner class AuthScreenAgent {
        // ...
    }

    // NOTE: агент для актора главного экрана
    inner class MainScreenAgent {
        // ...
    }

    // NOTE: агент для актора реального пользователя
    inner class UserAgent {
        // ...
    }

    // NOTE: агент для актора реального сервера
    inner class ServerAgent {
        // ...
    }

    // NOTE: агент для актора устройства
    inner class DeviceAgent {
        // ...
    }
}
```

Обратите внимание что вся логика находится в блоке init {}, а реализация в классах агентов.

В каждом классе агенте содержатся публичные функции и каждая из них является своего рода интерактором/юзкейсом. 

Если присмотреться то получается довольно чистая архитектура, где каждая функция представляет собой UseCase :)


Весь код примера можете посмотреть [здесь](https://github.com/e16din/actor_based_architecture/blob/main/examples/MyTaxi/app/src/main/java/com/e16din/mytaxi/screens/auth/AuthFragment.kt)
(еще в работе, все самое главное есть и можно смотреть на код)

### Системный, декларативный, ООП

На такой акторный подход моделирования очень хорошо ложится теория систем, по сути мы явно описываем механизм взаимодействия систем которые учавствуют в работе приложения. 

Еще можно сказать что это новый вариант архитектуры в декларативном стиле. 

Строго говоря - это старый добрый ООП привязанный к реальности, сущности которого описывают не "как", а "что", и уже внутри себя инкапсулируют реализацию. 

Получается довольно просто и удобно.


Плюсы
------

### Этот подход дает ряд преимуществ: 

- не надо городить огород с Clean Architecture;
- не требуется гонять данные из класса в класс, можно убрать LiveData/Rx/Flow (дополнительные абстракции моделей и мэперы туда же);
- сводится к минимуму использование DI-фреймворков;
</br>

- Single Responsibility из коробки со всеми его плюсами (можно даже не знать что это такое);
</br>

- легко и быстро проектировать функционал; (есть даже идея запилить генератор кода)
- агенты акторов описывают реальные сущности(экран, приложение, пользователь, сервер, телефон) это легко понять человеку, даже совсем юному и неопытному;
- логика текущего экрана приложения содержится в одном месте и не размазана по разным файлам и слоям (для сложных экранов можно применить композицию);
</br>

- порог входа в проект для разработчиков снижается (да и самому в своем старом коде проще ориентироваться);
- надо писать меньше кода, разработка ускоряется;
- код хорошо структурирован, такой код легко писать и легко читать;
</br>

- не надо выдумывать много пакетов и решать что где должно храниться, сокращается вложенность пакетов (можно просто разбивать по фичам);
- классы платформы остаются классами платформы, и живут по своим законам, мы не нагружаем их дополнительными смыслами, что убирает путаницу и некорректное понимание MV* концепций;
- на выходе имеем чистую архитектуру без оверхеда по классам и без лишнего мэпинга моделей;
</br>

- эту архитектуру можно переносить из проекта в проект без каких-то фреймворков;
- логику взаимодействия акторов свободно можно шарить между платформами (останется только подставить реализацию);
- легко и просто описывать реальное взаимодействие, а не воображаемые концепции
</br>

- сводится к минимуму мэпинг данных и уходят классы моделей для разных слоев;
- упрощается написание автотестов;
</br>

- это ново, интересно и весело :)
</br>

### Минусы которые я вижу:

- подход не привычный, отличается от всего того что мы привыкли использовать в проде; 
- надо набивать руку, нарабатывать опыт; (что по своему приятно :))
- пока не ясно надо ли тянуть это в легаси-код или использовать только для новых приложений?

Заключение
------------

Хоть начал я с негатива, в конце закончу позитивом:

Благодарю вас друзья за ваш вклад в сообщество и общее дело.

Буду рад если этот подход сделает процесс разработки более простым и удобным для всех нас :)

Призываю открыть репозиторий и поиграться с новым подходом: https://github.com/e16din/actor_based_architecture/tree/main/examples/MyTaxi/app/src/main/java/com/e16din/mytaxi

Обратную связь пишите в комментариях и issues. 

Всем добра :)