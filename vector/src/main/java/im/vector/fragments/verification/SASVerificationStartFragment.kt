/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.fragments.verification

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.transition.TransitionManager
import butterknife.BindView
import butterknife.OnClick
import im.vector.R
import im.vector.activity.CommonActivityUtils
import im.vector.fragments.VectorBaseFragment
import im.vector.listeners.YesNoListener
import org.matrix.androidsdk.core.callback.SimpleApiCallback
import org.matrix.androidsdk.crypto.data.MXDeviceInfo
import org.matrix.androidsdk.crypto.verification.OutgoingSASVerificationRequest

class SASVerificationStartFragment : VectorBaseFragment() {

    companion object {
        fun newInstance() = SASVerificationStartFragment()
    }

    override fun getLayoutResId() = R.layout.fragment_sas_verification_start

    private lateinit var viewModel: SasVerificationViewModel


    @BindView(R.id.rootLayout)
    lateinit var rootLayout: ViewGroup

//    @BindView(R.id.sas_start_button)
//    lateinit var startButton: Button

    @BindView(R.id.sas_start_button_loading)
    lateinit var startButtonLoading: ProgressBar

    @BindView(R.id.sas_verifying_keys)
    lateinit var loadingText: TextView

    @BindView(R.id.sas_legacy_verification)
    lateinit var legacyVerificationButton: Button

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(SasVerificationViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.transactionState.observe(this, Observer {
            val uxState = (viewModel.transaction as? OutgoingSASVerificationRequest)?.uxState
            when (uxState) {
                OutgoingSASVerificationRequest.State.WAIT_FOR_KEY_AGREEMENT -> {
                    //display loading
                    TransitionManager.beginDelayedTransition(this.rootLayout)
                    this.loadingText.isVisible = true
                    this.legacyVerificationButton.isVisible = true
//                    this.startButton.isInvisible = true
                    this.startButtonLoading.isVisible = true
                    this.startButtonLoading.animate()

                }
                OutgoingSASVerificationRequest.State.SHOW_SAS -> {
                    viewModel.shortCodeReady()
                }
                OutgoingSASVerificationRequest.State.CANCELLED_BY_ME,
                OutgoingSASVerificationRequest.State.CANCELLED_BY_OTHER -> {
                    viewModel.navigateCancel()
                }
                else -> {
                    TransitionManager.beginDelayedTransition(this.rootLayout)
                    this.loadingText.isVisible = false
                    this.legacyVerificationButton.isVisible = false
//                    this.startButton.isVisible = true
                    this.startButtonLoading.isVisible = false
                }
            }
        })

        // Tchap: start directly the verification based on emojis
        viewModel.beginSasKeyVerification()

    }

//    @OnClick(R.id.sas_start_button)
//    fun doStart() {
//        viewModel.beginSasKeyVerification()
//    }

    @OnClick(R.id.sas_legacy_verification)
    fun doLegacy() {
        viewModel.session.crypto?.getDeviceInfo(viewModel.otherUserId ?: "", viewModel.otherDeviceId
                ?: "", object : SimpleApiCallback<MXDeviceInfo>() {
            override fun onSuccess(info: MXDeviceInfo?) {
                info?.let {

                    CommonActivityUtils.displayDeviceVerificationDialogLegacy(it, it.userId, viewModel.session, activity, object : YesNoListener {
                        override fun yes() {
                            viewModel.manuallyVerified()
                        }

                        override fun no() {

                        }
                    })
                }
            }
        })
    }

    @OnClick(R.id.sas_cancel_button)
    fun doCancel() {
        // Transaction may be started, or not
        viewModel.cancelTransaction()
    }


}