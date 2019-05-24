package ru.impression.flow_architecture.mvvm_impl

import ru.impression.flow_architecture.Flow
import java.util.*

abstract class PrimaryFlowFragment<F : Flow, S : Any>(override val flowClass: Class<F>) : FlowFragment<F, S>(),
    PrimaryFlowView<F, S> {

    override lateinit var groupUUID: UUID

    override val flow by lazy { super<PrimaryFlowView>.flow }
}