package com.tcgptrading.tcgptrading.main.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.lexilabs.basic.ads.AdSize
import app.lexilabs.basic.ads.BannerAd
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import com.tcgptrading.tcgptrading.adBannerId
import com.tcgptrading.tcgptrading.app.domain.Detail
import com.tcgptrading.tcgptrading.app.domain.Tab
import com.tcgptrading.tcgptrading.app.presentation.AppIntent
import com.tcgptrading.tcgptrading.core.data.AppPrefs
import com.tcgptrading.tcgptrading.core.data.ChatSocket
import com.tcgptrading.tcgptrading.core.data.Session
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.runApi
import com.tcgptrading.tcgptrading.core.presentation.components.CountBadge
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.features.account.ContactScreen
import com.tcgptrading.tcgptrading.features.account.FriendIdDialog
import com.tcgptrading.tcgptrading.features.account.ProfileScreen
import com.tcgptrading.tcgptrading.features.account.TutorialDialog
import com.tcgptrading.tcgptrading.features.chat.ChatListScreen
import com.tcgptrading.tcgptrading.features.chat.ConversationScreen
import com.tcgptrading.tcgptrading.features.trading.MakeOfferScreen
import com.tcgptrading.tcgptrading.features.trading.TradingSignals
import com.tcgptrading.tcgptrading.features.trading.WantedScreen
import com.tcgptrading.tcgptrading.features.collection.CollectionScreen
import com.tcgptrading.tcgptrading.features.trading.WantedSearchRequests
import com.tcgptrading.tcgptrading.features.wants.MyTradesScreen
import com.tcgptrading.tcgptrading.features.wants.ReviewOffersScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Shows a short message at the bottom of the app (e.g. "Offer sent"). */
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("No snackbar host") }

/**
 * Runs an action that needs a Friend ID (offering, adding wants).
 * If the player has none yet, asks for it first and then continues with the action.
 */
fun interface FriendIdGate {
    fun require(action: () -> Unit)
}

val LocalFriendIdGate = staticCompositionLocalOf<FriendIdGate> { FriendIdGate { it() } }

/** Opens the "how it works" tour on request (Profile > Help). */
val LocalShowTutorial = staticCompositionLocalOf<() -> Unit> { {} }

private data class TabItem(val route: Tab, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Tab.Wanted, "Wanted", Icons.Outlined.Style),
    TabItem(Tab.Collection, "Collection", Icons.Outlined.GridView),
    TabItem(Tab.MyTrades(), "My trades", Icons.Outlined.SwapHoriz),
    TabItem(Tab.Chats, "Chats", Icons.AutoMirrored.Outlined.Chat),
    TabItem(Tab.Profile, "Profile", Icons.Outlined.Person),
)

/**
 * The signed-in app: bottom tabs (Wanted, Add wants, My trades, Chats, Profile) with one banner ad
 * above them. Detail screens (offer, review offers, conversation, contact) hide both.
 */
@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun MainScreen(onAcceptIntent: (AppIntent) -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val session = koinInject<Session>()
    val prefs = koinInject<AppPrefs>()
    val api = koinInject<TradingApi>()
    val chatSocket = koinInject<ChatSocket>()
    val scope = rememberCoroutineScope()
    val me by session.me.collectAsState()

    // Friend ID gate: remember the action to run once the ID is saved
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val gate = remember(session) {
        FriendIdGate { action -> if (session.friendId != null) action() else pendingAction = action }
    }

    // First-time tour (once per account or device), or on request
    var showTutorial by remember { mutableStateOf(false) }
    LaunchedEffect(me?.email) {
        val profile = me ?: return@LaunchedEffect
        if (!prefs.tutorialDone && profile.tutorialCompleted != true) showTutorial = true
        if (profile.tutorialCompleted == true) prefs.tutorialDone = true
    }

    // Unread chat count for the tab badge, refreshed when messages arrive
    var unread by remember { mutableStateOf(0) }
    LaunchedEffect(me?.email) {
        if (me == null) return@LaunchedEffect
        suspend fun refresh() {
            runApi { api.chats() }.onSuccess { list -> unread = list.sumOf { it.unreadCount } }
        }
        refresh()
        chatSocket.messages.collect { refresh() }
    }

    // Trading messages ("Offer sent to Player 1234") live here, so switching tabs or leaving a screen never drops one
    val tradingSignals = koinInject<TradingSignals>()
    LaunchedEffect(tradingSignals) {
        tradingSignals.notices.collect { notice ->
            // A newer message replaces the one showing; showSnackbar waits, so each runs on its own
            snackbar.currentSnackbarData?.dismiss()
            launch {
                val result = snackbar.showSnackbar(
                    message = notice.text,
                    actionLabel = if (notice.seeOffers) "See your offers" else null,
                    duration = if (notice.seeOffers) SnackbarDuration.Long else SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) navController.navigateToTab(Tab.MyTrades(tab = "offers"))
            }
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val onTabRoot = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    CompositionLocalProvider(
        LocalSnackbar provides snackbar,
        LocalFriendIdGate provides gate,
        LocalShowTutorial provides { showTutorial = true },
    ) {
        Scaffold(
            containerColor = TcgColors.Ground,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (onTabRoot) {
                    Column {
                        // One banner, above the tabs; hidden while typing so it never covers the keyboard area
                        if (!keyboardOpen) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                BannerAd(adUnitId = adBannerId, adSize = AdSize.BANNER)
                            }
                        }
                        HorizontalDivider(color = TcgColors.LineSoft)
                        TabBar(navController, unread)
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                MainNavHost(navController, onSignedOut = { onAcceptIntent(AppIntent.LogoutUser) })
            }
        }
    }

    if (pendingAction != null) {
        FriendIdDialog(
            onSaved = { friendId ->
                session.update { it.copy(friendId = friendId) }
                val action = pendingAction
                pendingAction = null
                action?.invoke()
            },
            onDismiss = { pendingAction = null },
        )
    }

    if (showTutorial) {
        TutorialDialog(
            onFinish = { goToWanted ->
                showTutorial = false
                prefs.tutorialDone = true
                scope.launch { runApi { api.setTutorialCompleted(true) } }
                if (goToWanted) navController.navigateToTab(Tab.Wanted)
            },
        )
    }
}

@Composable
private fun TabBar(navController: NavHostController, unreadChats: Int) {
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    NavigationBar(containerColor = TcgColors.Surface, tonalElevation = androidx.compose.ui.unit.Dp(0f)) {
        tabs.forEach { tab ->
            val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTab(tab.route) },
                icon = {
                    if (tab.route == Tab.Chats && unreadChats > 0) {
                        BadgedBox(badge = { CountBadge(unreadChats) }) { Icon(tab.icon, contentDescription = null) }
                    } else {
                        Icon(tab.icon, contentDescription = null)
                    }
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TcgColors.Red,
                    selectedTextColor = TcgColors.Red,
                    indicatorColor = TcgColors.RedSoft,
                    unselectedIconColor = TcgColors.Ink,
                    unselectedTextColor = TcgColors.Ink,
                ),
            )
        }
    }
}

/** Switches tab, keeping each tab's own back stack. */
fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainNavHost(navController: NavHostController, onSignedOut: () -> Unit) {
    NavHost(navController = navController, startDestination = Tab.Wanted) {
        composable<Tab.Wanted> {
            WantedScreen(
                onOffer = { offer -> navController.navigate(offer) },
                onAddWants = { navController.navigateToTab(Tab.Collection) },
                onSeeOffers = { navController.navigateToTab(Tab.MyTrades(tab = "offers")) },
            )
        }
        composable<Tab.Collection> {
            val wantedSearch = koinInject<WantedSearchRequests>()
            CollectionScreen(
                onOpenWanted = { search ->
                    wantedSearch.request(search)
                    navController.navigateToTab(Tab.Wanted)
                },
                onSeeMyWants = { navController.navigateToTab(Tab.MyTrades(tab = "wants")) },
            )
        }
        composable<Tab.MyTrades> { entry ->
            MyTradesScreen(
                initialTab = entry.toRoute<Tab.MyTrades>().tab,
                onReviewOffers = { requestId, cardId -> navController.navigate(Detail.ReviewOffers(requestId, cardId)) },
                onOpenChat = { email -> navController.navigate(Detail.Conversation(email)) },
                onBrowseWanted = { navController.navigateToTab(Tab.Wanted) },
                onAddWants = { navController.navigateToTab(Tab.Collection) },
            )
        }
        composable<Tab.Chats> {
            ChatListScreen(
                onOpen = { email -> navController.navigate(Detail.Conversation(email)) },
                onBrowseWanted = { navController.navigateToTab(Tab.Wanted) },
            )
        }
        composable<Tab.Profile> {
            ProfileScreen(
                onContact = { navController.navigate(Detail.Contact) },
                onSignedOut = onSignedOut,
            )
        }

        composable<Detail.MakeOffer> { entry ->
            MakeOfferScreen(
                route = entry.toRoute(),
                onBack = { navController.popBackStack() },
                onSent = {
                    navController.popBackStack()
                    navController.navigateToTab(Tab.MyTrades(tab = "offers"))
                },
            )
        }
        composable<Detail.ReviewOffers> { entry ->
            ReviewOffersScreen(
                route = entry.toRoute(),
                onBack = { navController.popBackStack() },
                onAccepted = { partnerEmail ->
                    navController.popBackStack()
                    if (partnerEmail != null) navController.navigate(Detail.Conversation(partnerEmail))
                },
            )
        }
        composable<Detail.Conversation> { entry ->
            ConversationScreen(route = entry.toRoute(), onBack = { navController.popBackStack() })
        }
        composable<Detail.Contact> {
            ContactScreen(onBack = { navController.popBackStack() })
        }
    }
}
