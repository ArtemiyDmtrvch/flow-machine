package ru.impression.flow_architecture

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

interface FlowPerformer<F : Flow> {

    val flowClass: Class<F>

    val eventEnrichers: List<FlowPerformer<F>> get() = emptyList()

    fun attachToFlow() {
        val flowName = flowClass.notNullName
        val thisName = javaClass.notNullName
        FLOW_PERFORMER_DISPOSABLES[flowName]?.get(thisName)?.dispose()
        FlowManager.startFlowIfNeeded(flowClass)
        ACTION_SUBJECTS[flowName]?.let { actionSubject ->
            actionSubject
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ performAction(it) }) { throw it }
                .let { FLOW_PERFORMER_DISPOSABLES[flowName]?.put(thisName, it) }
        }
    }

    fun eventOccurred(event: Flow.Event) {
        val flowName = flowClass.notNullName
        eventEnrichers.forEach { it.enrichEvent(event) }
        EVENT_SUBJECTS[flowName]?.onNext(event)
    }

    fun enrichEvent(event: Flow.Event) = Unit

    fun performAction(action: Flow.Action) = Unit

    fun detachFromFlow() {
        val flowName = flowClass.notNullName
        val thisName = javaClass.notNullName
        FLOW_PERFORMER_DISPOSABLES[flowName]?.remove(thisName)?.dispose()
        FlowManager.stopFlowIfNeeded(flowClass)
    }
}