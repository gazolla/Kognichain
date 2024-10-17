package com.kognichain.decisionmakers;

import com.kognichain.core.Action
import com.kognichain.core.DecisionMaker

class SimpleDecisionMaker(private val decisionLogic: (Map<String, Any>) -> Action) : DecisionMaker {
    override fun decide(input: Map<String, Any>): Action = decisionLogic(input)
}