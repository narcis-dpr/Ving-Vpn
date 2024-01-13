/******************************************************************************
 *                                                                            *
 * Copyright (C) 2021 by nekohasekai <contact-sagernet@sekai.icu>             *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                       *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                            *
 ******************************************************************************/

package io.nekohasekai.sagernet.group

import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.GroupManager
import com.github.shadowsocks.plugin.PluginConfiguration
import com.github.shadowsocks.plugin.PluginOptions
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.ktx.USER_AGENT
import io.nekohasekai.sagernet.ExtraType
import com.narcis.application.presentation.connection.Logs
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SubscriptionBean
import com.narcis.application.presentation.connection.filterIsInstance
import io.nekohasekai.sagernet.ftm.shadowsocks.fixInvalidParams
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ktx.getIntNya
import io.nekohasekai.sagernet.ktx.getStr


import libcore.Libcore
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

object OpenOnlineConfigUpdater : GroupUpdater() {

    override suspend fun doUpdate(
        proxyGroup: ProxyGroup,
        subscription: SubscriptionBean,
        userInterface: GroupManager.Interface?,
        byUser: Boolean
    ) {
        val apiToken: JSONObject
        var baseLink: HttpUrl
        val certSha256: String?
        try {
            apiToken = JSONObject(subscription.token)

            val version = apiToken.getIntNya("version")
            if (version != 1) {
                if (version != null) {
                    error("Unsupported OOC version $version")
                } else {
                    error("Missing field: version")
                }
            }
            val baseUrl = apiToken.getStr("baseUrl")
            when {
                baseUrl.isNullOrBlank() -> {
                    error("Missing field: baseUrl")
                }
                baseUrl.endsWith("/") -> {
                    error("baseUrl must not contain a trailing slash")
                }
                !baseUrl.startsWith("https://") -> {
                    error("Protocol scheme must be https")
                }
                else -> baseLink = baseUrl.toHttpUrl()
            }
            val secret = apiToken.getStr("secret")
            if (secret.isNullOrBlank()) error("Missing field: secret")
            baseLink = baseLink.newBuilder()
                .addPathSegments(secret)
                .addPathSegments("ooc/v1")
                .build()

            val userId = apiToken.getStr("userId")
            if (userId.isNullOrBlank()) error("Missing field: userId")
            baseLink = baseLink.newBuilder().addPathSegment(userId).build()
            certSha256 = apiToken.getStr("certSha256")
            if (!certSha256.isNullOrBlank()) {
                when {
                    certSha256.length != 64 -> {
                        error("certSha256 must be a SHA-256 hexadecimal string")
                    }
                }
            }
        } catch (e: Exception) {
            Logs.v("ooc token check failed, token = ${subscription.token}", e)
            error(app.getString(R.string.ooc_subscription_token_invalid))
        }

        val response = Libcore.newHttpClient().apply {
            restrictedTLS()
            if (certSha256 != null) pinnedSHA256(certSha256)
        }.newRequest().apply {
            setURL(baseLink.toString())
            setUserAgent(subscription.customUserAgent.takeIf { it.isNotBlank() } ?: USER_AGENT)
        }.execute()

        val oocResponse = JSONObject(response.contentString)
        subscription.username = oocResponse.getStr("username")
        subscription.bytesUsed = oocResponse.optLong("bytesUsed", -1)
        subscription.bytesRemaining = oocResponse.optLong("bytesRemaining", -1)
        subscription.expiryDate = oocResponse.optInt("expiryDate", -1)
        subscription.protocols = oocResponse.getJSONArray("protocols").filterIsInstance<String>()
        subscription.applyDefaultValues()

        for (protocol in subscription.protocols) {
            if (protocol !in supportedProtocols) {
                userInterface?.alert(app.getString(R.string.ooc_missing_protocol, protocol))
            }
        }

        var profiles = mutableListOf<io.nekohasekai.sagernet.ftm.AbstractBean>()

        for (protocol in subscription.protocols) {
            val profilesInProtocol = oocResponse.getJSONArray(protocol)
                .filterIsInstance<JSONObject>()

            if (protocol == "shadowsocks") for (profile in profilesInProtocol) {
                val bean = io.nekohasekai.sagernet.ftm.shadowsocks.ShadowsocksBean()

                bean.name = profile.getStr("name")
                bean.serverAddress = profile.getStr("address")
                bean.serverPort = profile.getIntNya("port")
                bean.method = profile.getStr("method")
                bean.password = profile.getStr("password")

                val pluginName = profile.getStr("pluginName")
                if (!pluginName.isNullOrBlank()) {
                    // TODO: check plugin exists
                    // TODO: check pluginVersion
                    // TODO: support pluginArguments

                    val pl = PluginConfiguration()
                    pl.selected = pluginName
                    pl.pluginsOptions[pl.selected] = PluginOptions(profile.getStr("pluginOptions"))
                    pl.fixInvalidParams()
                    bean.plugin = pl.toString()
                }

                appendExtraInfo(profile, bean)

                profiles.add(bean.applyDefaultValues())
            }
        }

        if (subscription.forceResolve) forceResolve(profiles, proxyGroup.id)

        val exists = SagerDatabase.proxyDao.getByGroup(proxyGroup.id)
        val duplicate = ArrayList<String>()
        if (subscription.deduplication) {
            Logs.d("Before deduplication: ${profiles.size}")
            val uniqueProfiles = LinkedHashSet<io.nekohasekai.sagernet.ftm.AbstractBean>()
            val uniqueNames = HashMap<io.nekohasekai.sagernet.ftm.AbstractBean, String>()
            for (proxy in profiles) {
                if (!uniqueProfiles.add(proxy)) {
                    val index = uniqueProfiles.indexOf(proxy)
                    if (uniqueNames.containsKey(proxy)) {
                        val name = uniqueNames[proxy]!!.replace(" ($index)", "")
                        if (name.isNotBlank()) {
                            duplicate.add("$name ($index)")
                            uniqueNames[proxy] = ""
                        }
                    }
                    duplicate.add(proxy.displayName() + " ($index)")
                } else {
                    uniqueNames[proxy] = proxy.displayName()
                }
            }
            uniqueProfiles.retainAll(uniqueNames.keys)
            profiles = uniqueProfiles.toMutableList()
        }

        Logs.d("New profiles: ${profiles.size}")

        val profileMap = profiles.associateBy { it.profileId }
        val toDelete = ArrayList<ProxyEntity>()
        val toReplace = exists.mapNotNull { entity ->
            val profileId = entity.requireBean().profileId
            if (profileMap.contains(profileId)) profileId to entity else let {
                toDelete.add(entity)
                null
            }
        }.toMap()

        Logs.d("toDelete profiles: ${toDelete.size}")
        Logs.d("toReplace profiles: ${toReplace.size}")

        val toUpdate = ArrayList<ProxyEntity>()
        val added = mutableListOf<String>()
        val updated = mutableMapOf<String, String>()
        val deleted = toDelete.map { it.displayName() }

        var userOrder = 1L
        var changed = toDelete.size
        for ((profileId, bean) in profileMap.entries) {
            val name = bean.displayName()
            if (toReplace.contains(profileId)) {
                val entity = toReplace[profileId]!!
                val existsBean = entity.requireBean()
                existsBean.applyFeatureSettings(bean)
                when {
                    existsBean != bean -> {
                        changed++
                        entity.putBean(bean)
                        toUpdate.add(entity)
                        updated[entity.displayName()] = name

                        Logs.d("Updated profile: [$profileId] $name")
                    }
                    entity.userOrder != userOrder -> {
                        entity.putBean(bean)
                        toUpdate.add(entity)
                        entity.userOrder = userOrder

                        Logs.d("Reordered profile: [$profileId] $name")
                    }
                    else -> {
                        Logs.d("Ignored profile: [$profileId] $name")
                    }
                }
            } else {
                changed++
                SagerDatabase.proxyDao.addProxy(
                    ProxyEntity(
                    groupId = proxyGroup.id, userOrder = userOrder
                ).apply {
                    putBean(bean)
                })
                added.add(name)
                Logs.d("Inserted profile: $name")
            }
            userOrder++
        }

        SagerDatabase.proxyDao.updateProxy(toUpdate).also {
            Logs.d("Updated profiles: $it")
        }

        SagerDatabase.proxyDao.deleteProxy(toDelete).also {
            Logs.d("Deleted profiles: $it")
        }

        val existCount = SagerDatabase.proxyDao.countByGroup(proxyGroup.id).toInt()

        if (existCount != profileMap.size) {
            Logs.e("Exist profiles: $existCount, new profiles: ${profileMap.size}")
        }

        subscription.lastUpdated = (System.currentTimeMillis() / 1000).toInt()
        SagerDatabase.groupDao.updateGroup(proxyGroup)
        finishUpdate(proxyGroup)

        userInterface?.onUpdateSuccess(
            proxyGroup, changed, added, updated, deleted, duplicate, byUser
        )
    }

    fun appendExtraInfo(profile: JSONObject, bean: io.nekohasekai.sagernet.ftm.AbstractBean) {
        bean.extraType = ExtraType.OOCv1
        bean.profileId = profile.getStr("id")
        bean.group = profile.getStr("group")
        bean.owner = profile.getStr("owner")
        bean.tags = profile.optJSONArray("tags")?.filterIsInstance()
    }

    val supportedProtocols = arrayOf("shadowsocks")

}