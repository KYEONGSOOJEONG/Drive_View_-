package com.driveview.prototype

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper

class Guidance(
    context: Context,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun onChecking()
        fun onHandingOff()
        fun onNotGuidable()
        fun onUnavailable()
    }

    private val main = Handler(Looper.getMainLooper())
    private val naviHelper = NaviHelper(context.applicationContext)
    private var connected = false
    private var failed = false
    private var listenerAdded = false
    private var awaiting = false
    private var requested = false
    private var pending: Place? = null
    private var generation = 0

    private val listener = object : NaviHelperEventListener {
        override fun onRoutingCompleted() {
            if (!awaiting) return
            finishWait()
            handOff(NavigationScreenRequest.Foreground)
        }

        override fun onError(errorCode: NaviErrorCode) {
            if (!awaiting) return
            finishWait()
            when (errorCode) {
                NaviErrorCode.CAUSE_SEARCH_ROUTE,
                NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE,
                NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE,
                -> onMain { callbacks.onNotGuidable() }
                else -> onMain { callbacks.onUnavailable() }
            }
        }
    }

    private val timeout = Runnable {
        if (!awaiting) return@Runnable
        val reachedNavi = connected
        finishWait()
        if (reachedNavi) callbacks.onNotGuidable() else callbacks.onUnavailable()
    }

    init {
        naviHelper.initialize(
            onConnected = {
                onMain {
                    connected = true
                    if (!listenerAdded) {
                        naviHelper.addListener(listener)
                        listenerAdded = true
                    }
                    val place = pending
                    if (awaiting && place != null) requestNow(place)
                }
            },
            onFailed = {
                onMain {
                    connected = false
                    failed = true
                    if (awaiting) {
                        finishWait()
                        callbacks.onUnavailable()
                    }
                }
            },
        )
    }

    fun start(place: Place) {
        callbacks.onChecking()
        if (failed) {
            callbacks.onUnavailable()
            return
        }
        pending = place
        awaiting = true
        generation += 1
        main.removeCallbacks(timeout)
        main.postDelayed(timeout, TIMEOUT_MS)
        if (connected) requestNow(place)
    }

    fun guideNearby(place: Place) {
        if (!connected) {
            callbacks.onUnavailable()
            return
        }
        callbacks.onHandingOff()
        naviHelper.requestNavigationScreen(NavigationScreenRequest.SearchPlace(place.name))
    }

    fun cancel() {
        val routeInFlight = awaiting
        finishWait()
        if (routeInFlight && connected) {
            runCatching { naviHelper.cancelRoute() }
        }
    }

    fun release() {
        finishWait()
        runCatching { naviHelper.removeListener(listener) }
        runCatching { naviHelper.release() }
    }

    private fun requestNow(place: Place) {
        if (requested) return
        requested = true
        val ticket = generation
        try {
            naviHelper.requestRoute(
                RouteInfo(
                    latitude = place.latitude,
                    longitude = place.longitude,
                    displayLatitude = null,
                    displayLongitude = null,
                    poiName = place.name,
                    poiId = "",
                    address = place.address,
                    poiSubId = "0",
                ),
            )
        } catch (_: RuntimeException) {
            if (ticket == generation && awaiting) {
                finishWait()
                callbacks.onNotGuidable()
            }
        }
    }

    private fun handOff(screen: NavigationScreenRequest) {
        onMain {
            callbacks.onHandingOff()
            runCatching { naviHelper.requestNavigationScreen(screen) }
        }
    }

    private fun finishWait() {
        awaiting = false
        requested = false
        pending = null
        generation += 1
        main.removeCallbacks(timeout)
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    private companion object {
        const val TIMEOUT_MS = 12_000L
    }
}
