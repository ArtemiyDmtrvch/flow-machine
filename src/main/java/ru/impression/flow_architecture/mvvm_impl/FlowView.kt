package ru.impression.flow_architecture.mvvm_impl

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import ru.impression.flow_architecture.Flow
import ru.impression.flow_architecture.FlowPerformer
import ru.impression.flow_architecture.attachToFlow
import java.util.concurrent.atomic.AtomicBoolean

interface FlowView<F : Flow, S : Any> : FlowPerformer<F, FlowView.Underlay> {

    val flowViewModelClasses get() = emptyArray<Class<out FlowViewModel<F>>>()

    val viewStateSavingViewModel
        get() = getViewModelProvider()[ViewStateSavingViewModel::class.java] as ViewStateSavingViewModel<S>

    var additionalState: S

    override val observingScheduler: Scheduler get() = AndroidSchedulers.mainThread()

    fun attachToFlow() {
        underlay.apply {
            attachToFlow(
                if (this?.performerIsTemporarilyDetached?.get() == true && !layoutIsSet.get())
                    FlowPerformer.AttachmentType.REPLAY_ATTACHMENT
                else
                    FlowPerformer.AttachmentType.NORMAL_ATTACHMENT
            )
        }
        flowViewModelClasses.forEach { getViewModelProvider()[it] }
    }

    fun getViewModelProvider(factory: ViewModelProvider.Factory? = null) =
        when (this) {
            is Fragment -> ViewModelProviders.of(
                this,
                factory ?: FlowViewModelFactory(activity!!.application, groupUUID)
            )
            is FragmentActivity -> ViewModelProviders.of(
                this,
                factory ?: FlowViewModelFactory(application, groupUUID)
            )
            else -> throw UnsupportedOperationException("FlowView must be either FragmentActivity or Fragment.")
        }

    override fun onInitialActionPerformed() {
        underlay?.layoutIsSet?.set(true)
        super.onInitialActionPerformed()
    }

    override fun groundStateIsSet() {
        viewStateSavingViewModel.additionalViewState?.let { additionalState = it }
        super.groundStateIsSet()
    }

    fun temporarilyDetachFromFlow() {
        if (underlay?.layoutIsSet?.get() == true)
            additionalState
                .takeIf { it !is Unit && it !is Nothing }
                ?.let { viewStateSavingViewModel.additionalViewState = it }
        super.temporarilyDetachFromFlow(true)
    }

    class Underlay : FlowPerformer.Underlay() {
        val layoutIsSet = AtomicBoolean(false)
    }
}