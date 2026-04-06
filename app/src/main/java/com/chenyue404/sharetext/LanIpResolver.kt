package com.chenyue404.sharetext

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object LanIpResolver {
    fun resolve(context: Context): String {
        val fromActiveNetwork = getIpFromActiveNetwork(context)
        if (fromActiveNetwork != null) return fromActiveNetwork
        return getIpFromInterfaces() ?: "127.0.0.1"
    }

    private fun getIpFromActiveNetwork(context: Context): String? {
        return try {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
            val network = cm.activeNetwork ?: return null
            val link = cm.getLinkProperties(network) ?: return null
            link.linkAddresses
                .mapNotNull { it.address as? Inet4Address }
                .firstOrNull { isUsableLanIpv4(it) }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun getIpFromInterfaces(): String? {
        return try {
            val all = NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .sortedByDescending {
                    val n = it.name.lowercase()
                    when {
                        n.startsWith("wlan") -> 3
                        n.startsWith("eth") -> 2
                        else -> 1
                    }
                }

            all.asSequence()
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { isUsableLanIpv4(it) }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun isUsableLanIpv4(ip: InetAddress): Boolean {
        if (ip.isAnyLocalAddress || ip.isLoopbackAddress || ip.isLinkLocalAddress || ip.isMulticastAddress) {
            return false
        }
        val host = ip.hostAddress ?: return false
        if (host.startsWith("169.254.")) return false
        return host.startsWith("10.")
                || host.startsWith("192.168.")
                || host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..+"))
    }
}
