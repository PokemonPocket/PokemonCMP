package com.tcgptrading.tcgptrading

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tcgptrading.tcgptrading.app.data.appModule
import com.tcgptrading.tcgptrading.app.presentation.ui.App
import com.tcgptrading.tcgptrading.core.util.CommonLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (GlobalContext.getOrNull() == null) {
            startKoin {
                logger(CommonLogger(level = Level.DEBUG))
                modules(
                    listOf(
                        module {
                            single<ComponentActivity> { this@MainActivity }
                        },
                        module {
                            single { applicationContext }
                        },
                        appModule
                    )
                )

                createEagerInstances()
            }
        }

        enableEdgeToEdge()

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}