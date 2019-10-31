/*
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gouv.tchap.util

import android.net.Uri
import android.os.Build
import androidx.annotation.VisibleForTesting
import fr.gouv.tchap.config.CERTIFICATE_FINGERPRINT_LIST
import fr.gouv.tchap.config.ENABLE_CERTIFICATE_PINNING
import okhttp3.TlsVersion
import org.matrix.androidsdk.HomeServerConnectionConfig
import org.matrix.androidsdk.ssl.Fingerprint
import java.lang.Long.parseLong

/**
 * Create a HomeServerConnectionConfig with all the required parameters for the Tchap application
 */
fun createHomeServerConnectionConfig(homeServerUrl: String, identityServerUrl: String?): HomeServerConnectionConfig {
    return HomeServerConnectionConfig.Builder()
            .withHomeServerUri(Uri.parse(homeServerUrl))
            .apply {

                identityServerUrl?.let {
                    withIdentityServerUri(Uri.parse(identityServerUrl))
                }

                for (certificateFingerprint in CERTIFICATE_FINGERPRINT_LIST) {
                    addAllowedFingerPrint(Fingerprint(Fingerprint.HashType.SHA256, getBytesFromString(certificateFingerprint)))
                }
            }
            .withTlsLimitations(true, Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
            .withPin(ENABLE_CERTIFICATE_PINNING)
            .build()
            .cleanAcceptedTlsVersion()
}

/**
 * Clean the accepted TLS version by removing the unaccepted versions (which were accepted previously).
 */
fun HomeServerConnectionConfig.cleanAcceptedTlsVersion() = apply {
    acceptedTlsVersions?.remove(TlsVersion.TLS_1_2)
}

@VisibleForTesting
fun getBytesFromString(certificateFingerprint: String): ByteArray {
    // Note: will be reassigned with correct size later
    var bytes = ByteArray(0)

    var idx = 0

    certificateFingerprint.replace(":", "")
            .chunked(2)
            .also {
                bytes = ByteArray(it.size)
            }
            .map {
                bytes[idx++] = parseLong(it, 16).toByte()
            }

    return bytes
}